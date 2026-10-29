package com.example.services

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.Properties
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * MariaDB bağlantı yapılandırma modeli.
 * Değerler `.env` / Secrets Gradle Plugin (`BuildConfig.MARIADB_*`) üzerinden okunur.
 */
data class MariaDbConfig(
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
    val sslEnabled: Boolean = false,
    val maxPoolSize: Int = 8,
    val minIdle: Int = 2,
    val connectTimeoutMs: Int = 8_000,
    val socketTimeoutMs: Int = 20_000,
    val idleTimeoutMs: Long = 180_000L,
    val maxLifetimeMs: Long = 900_000L,
    val validationIntervalMs: Long = 15_000L
) {
    val isConfigured: Boolean
        get() = host.isNotBlank() &&
            host != "YOUR_MARIADB_HOST" &&
            database.isNotBlank() &&
            database != "YOUR_MARIADB_DATABASE" &&
            user.isNotBlank() &&
            user != "YOUR_MARIADB_USER"

    val jdbcUrl: String
        get() = buildString {
            append("jdbc:mysql://")
            append(host.trim())
            append(":")
            append(port)
            append("/")
            append(database.trim())
            append("?useUnicode=true")
            append("&characterEncoding=UTF-8")
            append("&connectTimeout=").append(connectTimeoutMs)
            append("&socketTimeout=").append(socketTimeoutMs)
            append("&rewriteBatchedStatements=true")
            append("&tcpKeepAlive=true")
            append("&autoReconnect=false")
            append("&useSSL=").append(sslEnabled)
            if (sslEnabled) {
                append("&requireSSL=true")
            }
        }
}

/**
 * Bağlantı havuzunun (connection pool) anlık durum metrikleri.
 */
data class PoolMetrics(
    val configured: Boolean = false,
    val activeConnections: Int = 0,
    val idleConnections: Int = 0,
    val totalCreatedConnections: Int = 0,
    val maxPoolSize: Int = 8,
    val minIdle: Int = 2,
    val isHealthy: Boolean = false,
    val lastErrorMessage: String? = null,
    val lastVerifiedAt: Long = 0L
)

/**
 * Uygulama genelinde MariaDB veritabanı ile güvenli, asenkron (`Dispatchers.IO`) ve
 * bağlantı havuzlu (JDBC Connection Pooling) iletişim kuran Singleton yönetici sınıfı.
 *
 * Özellikler:
 * - `.env` / `BuildConfig` üzerinden güvenli kimlik ve havuz parametrelerini okur.
 * - Android çalışma zamanıyla tam uyumlu, thread-safe JDBC Bağlantı Havuzu yönetir
 *   (`minIdle`, `maxPoolSize`, `idleTimeout`, `maxLifetime`, otomatik sağlık doğrulaması).
 * - `Connection.close()` çağrılarını Dynamic Proxy ile yakalayarak fiziksel soketi kapatmak
 *   yerine bağlantıyı sıfırlayıp (`rollback` / `autoCommit = true`) havuza geri bırakır.
 * - Tüm SQL sorgularını `PreparedStatement` parametre bağlama ve Coroutine `Dispatchers.IO`
 *   ile çalıştırarak hem SQL Injection saldırılarını hem de UI/Main Thread bloklanmalarını önler.
 */
object DatabaseManager {

    private const val TAG = "DatabaseManager"
    private const val JDBC_DRIVER_CLASS = "com.mysql.jdbc.Driver"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initMutex = Mutex()

    @Volatile
    private var dotEnvFileOverrides: Map<String, String> = emptyMap()

    @Volatile
    private var currentConfig: MariaDbConfig = loadConfigFromEnv()

    @Volatile
    private var poolSemaphore: Semaphore = Semaphore(currentConfig.maxPoolSize.coerceAtLeast(1), true)

    private val idleConnections = LinkedBlockingDeque<PooledConnectionHolder>()
    private val activeCount = AtomicInteger(0)
    private val totalCreatedCount = AtomicInteger(0)
    private val driverLoaded = AtomicBoolean(false)
    private val housekeeperStarted = AtomicBoolean(false)

    private val _poolMetrics = MutableStateFlow(
        PoolMetrics(
            configured = currentConfig.isConfigured,
            maxPoolSize = currentConfig.maxPoolSize,
            minIdle = currentConfig.minIdle
        )
    )
    val poolMetrics: StateFlow<PoolMetrics> = _poolMetrics.asStateFlow()

    init {
        startHousekeeperIfNeeded()
    }

    /**
     * Havuz içindeki tekil bir fiziksel JDBC bağlantısını ve yaşam döngüsü zaman damgalarını tutar.
     */
    private class PooledConnectionHolder(
        val physicalConnection: Connection,
        val createdAtMs: Long = System.currentTimeMillis(),
        @Volatile var lastBorrowedAtMs: Long = System.currentTimeMillis(),
        @Volatile var lastReturnedAtMs: Long = System.currentTimeMillis(),
        @Volatile var lastValidatedAtMs: Long = System.currentTimeMillis()
    ) {
        fun isExpired(nowMs: Long, config: MariaDbConfig): Boolean {
            val lifetimeExceeded = (nowMs - createdAtMs) >= config.maxLifetimeMs
            val idleExceeded = (nowMs - lastReturnedAtMs) >= config.idleTimeoutMs
            return lifetimeExceeded || idleExceeded
        }
    }

    /**
     * İsteğe bağlı olarak Android `Context` üzerinden yerel `.env` dosyasını (varsa) yükler
     * ve bağlantı havuzunu (`minIdle`) ısıtır.
     */
    fun initialize(context: Context? = null) {
        context?.applicationContext?.let { appCtx ->
            val overrides = readDotEnvFromContext(appCtx)
            if (overrides.isNotEmpty()) {
                dotEnvFileOverrides = overrides
                reloadConfigIfNeeded()
            }
        }
        if (isConfigured()) {
            managerScope.launch {
                runCatching { warmUpPool() }
            }
        }
    }

    /**
     * `.env` ve `BuildConfig` ayarlarının geçerli bir MariaDB bağlantısı için
     * doldurulup doldurulmadığını döner.
     */
    fun isConfigured(): Boolean = currentConfig.isConfigured

    /**
     * Güncel bağlantı yapılandırmasını döner (şifre dışarıya maskelenebilir veya dahili kullanılır).
     */
    fun getConfig(): MariaDbConfig = currentConfig

    /**
     * Havuzdan doğrulanmış bir `java.sql.Connection` ödünç alır, verilen `block` işlemini
     * `Dispatchers.IO` üzerinde çalıştırır ve işlem bitince bağlantıyı otomatik olarak havuza iade eder.
     */
    suspend fun <T> withConnection(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        acquireConnection().use { pooledProxy ->
            block(pooledProxy)
        }
    }

    /**
     * Atomik veritabanı işlemleri (Transaction) için havuzdan bağlantı alır, `autoCommit = false`
     * yapar; blok başarılı olursa `commit()`, hata fırlatırsa `rollback()` uygular ve bağlantıyı
     * havuza geri bırakır.
     */
    suspend fun <T> withTransaction(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        acquireConnection().use { conn ->
            val previousAutoCommit = conn.autoCommit
            try {
                conn.autoCommit = false
                val result = block(conn)
                conn.commit()
                result
            } catch (err: Throwable) {
                runCatching { conn.rollback() }
                throw err
            } finally {
                runCatching { conn.autoCommit = previousAutoCommit }
            }
        }
    }

    /**
     * Parametreli (`PreparedStatement`) `INSERT`, `UPDATE`, `DELETE` veya DDL sorgularını
     * güvenli ve asenkron olarak çalıştırır; etkilenen satır sayısını döner.
     */
    suspend fun executeUpdate(sql: String, vararg params: Any?): Int = withConnection { conn ->
        conn.prepareStatement(sql).use { ps ->
            bindParameters(ps, params)
            ps.executeUpdate()
        }
    }

    /**
     * Çoklu satır güncellemelerini (`PreparedStatement.addBatch` / `executeBatch`)
     * tek bir transaction altında yüksek performansla çalıştırır.
     */
    suspend fun executeBatchWithinTransaction(
        sql: String,
        parameterSets: List<Array<out Any?>>
    ): IntArray = withTransaction { conn ->
        if (parameterSets.isEmpty()) return@withTransaction IntArray(0)
        conn.prepareStatement(sql).use { ps ->
            for (params in parameterSets) {
                bindParameters(ps, params)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    /**
     * Parametreli (`PreparedStatement`) `SELECT` sorgusunu asenkron çalıştırır ve her satırı
     * `mapper` fonksiyonu ile dönüştürerek liste halinde döner.
     */
    suspend fun <T> executeQuery(
        sql: String,
        vararg params: Any?,
        mapper: (ResultSet) -> T
    ): List<T> = withConnection { conn ->
        conn.prepareStatement(sql).use { ps ->
            bindParameters(ps, params)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(mapper(rs))
                    }
                }
            }
        }
    }

    /**
     * Bağlantı havuzunu test eder ve `minIdle` kadar sıcak bağlantıyı hazırlar.
     */
    suspend fun testConnection(): Result<PoolMetrics> = withContext(Dispatchers.IO) {
        runCatching {
            warmUpPool()
            withConnection { conn ->
                conn.prepareStatement("SELECT 1").use { ps ->
                    ps.executeQuery().use { rs ->
                        check(rs.next() && rs.getInt(1) == 1) {
                            "MariaDB doğrulama sorgusu (SELECT 1) beklenen yanıtı döndürmedi."
                        }
                    }
                }
            }
            updateMetrics(isHealthy = true, errorMessage = null)
            _poolMetrics.value
        }.onFailure { err ->
            Log.e(TAG, "MariaDB bağlantı testi başarısız: ${err.message}", err)
            updateMetrics(isHealthy = false, errorMessage = err.message)
        }
    }

    /**
     * Havuzda en az `minIdle` adet açık ve doğrulanmış bağlantı bulunmasını sağlar.
     */
    suspend fun warmUpPool() = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        initMutex.withLock {
            val cfg = currentConfig
            val targetIdle = cfg.minIdle.coerceIn(0, cfg.maxPoolSize)
            val borrowed = mutableListOf<Connection>()
            try {
                while (idleConnections.size + borrowed.size < targetIdle) {
                    borrowed.add(acquireConnection())
                }
            } finally {
                borrowed.forEach { runCatching { it.close() } }
            }
        }
    }

    /**
     * Havuzdan bir JDBC `Connection` (Proxy) ödünç alır.
     * Çağrıcı `connection.close()` dediğinde bağlantı kapatılmaz, havuza iade edilir.
     */
    @Throws(SQLException::class)
    fun acquireConnection(): Connection {
        val cfg = currentConfig
        if (!cfg.isConfigured) {
            throw SQLException(
                "MariaDB yapılandırması eksik. Lütfen .env / AI Studio Secrets panelinde " +
                    "MARIADB_HOST, MARIADB_DATABASE, MARIADB_USER ve MARIADB_PASSWORD değerlerini tanımlayın."
            )
        }

        ensureDriverLoaded()

        val acquired = try {
            poolSemaphore.tryAcquire(cfg.connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            throw SQLException("MariaDB bağlantı havuzu beklemesi kesildi.", ie)
        }

        if (!acquired) {
            val msg = "MariaDB bağlantı havuzu zaman aşımı (maxPoolSize=${cfg.maxPoolSize}, active=${activeCount.get()})"
            updateMetrics(isHealthy = false, errorMessage = msg)
            throw SQLException(msg)
        }

        var holder: PooledConnectionHolder? = null
        try {
            val now = System.currentTimeMillis()
            while (true) {
                val candidate = idleConnections.pollFirst() ?: break
                if (candidate.isExpired(now, cfg) || !isConnectionValid(candidate, now, cfg)) {
                    closePhysicalSilently(candidate.physicalConnection)
                } else {
                    holder = candidate
                    break
                }
            }

            if (holder == null) {
                val physical = createPhysicalConnection(cfg)
                totalCreatedCount.incrementAndGet()
                holder = PooledConnectionHolder(
                    physicalConnection = physical,
                    createdAtMs = now,
                    lastBorrowedAtMs = now,
                    lastReturnedAtMs = now,
                    lastValidatedAtMs = now
                )
            } else {
                holder.lastBorrowedAtMs = now
            }

            activeCount.incrementAndGet()
            updateMetrics(isHealthy = true, errorMessage = null)
            return wrapWithPoolProxy(holder)
        } catch (err: Throwable) {
            poolSemaphore.release()
            updateMetrics(isHealthy = false, errorMessage = err.message)
            if (err is SQLException) throw err
            throw SQLException("MariaDB bağlantısı açılamadı: ${err.message}", err)
        }
    }

    /**
     * Süresi dolan veya boştaki fazla bağlantıları temizler.
     */
    fun evictExpiredConnections() {
        val cfg = currentConfig
        if (!cfg.isConfigured) return
        val now = System.currentTimeMillis()
        val retained = mutableListOf<PooledConnectionHolder>()

        while (true) {
            val holder = idleConnections.pollLast() ?: break
            val shouldEvict = holder.isExpired(now, cfg) &&
                (idleConnections.size + retained.size) >= cfg.minIdle
            if (shouldEvict || isPhysicalClosed(holder.physicalConnection)) {
                closePhysicalSilently(holder.physicalConnection)
            } else {
                retained.add(0, holder)
            }
        }
        retained.forEach { idleConnections.offerLast(it) }
        updateMetrics(isHealthy = _poolMetrics.value.isHealthy, errorMessage = _poolMetrics.value.lastErrorMessage)
    }

    /**
     * Tüm boşta duran fiziksel bağlantıları güvenli şekilde kapatır ve havuzu sıfırlar.
     */
    fun shutdownPool() {
        while (true) {
            val holder = idleConnections.pollFirst() ?: break
            closePhysicalSilently(holder.physicalConnection)
        }
        updateMetrics(isHealthy = false, errorMessage = null)
    }

    /**
     * `PreparedStatement` içerisine dinamik parametreleri tip güvenli olarak bağlar.
     */
    fun bindParameters(ps: PreparedStatement, params: Array<out Any?>) {
        params.forEachIndexed { index, value ->
            val sqlIndex = index + 1
            when (value) {
                null -> ps.setNull(sqlIndex, Types.NULL)
                is String -> ps.setString(sqlIndex, value)
                is Int -> ps.setInt(sqlIndex, value)
                is Long -> ps.setLong(sqlIndex, value)
                is Double -> ps.setDouble(sqlIndex, value)
                is Float -> ps.setFloat(sqlIndex, value)
                is Boolean -> ps.setInt(sqlIndex, if (value) 1 else 0)
                is ByteArray -> ps.setBytes(sqlIndex, value)
                else -> ps.setObject(sqlIndex, value)
            }
        }
    }

    private fun releaseConnection(holder: PooledConnectionHolder, discard: Boolean = false) {
        val cfg = currentConfig
        val now = System.currentTimeMillis()
        try {
            val physical = holder.physicalConnection
            if (discard || isPhysicalClosed(physical) || (now - holder.createdAtMs) >= cfg.maxLifetimeMs) {
                closePhysicalSilently(physical)
                return
            }

            // Yarım kalmış transaction varsa geri al ve autoCommit'i sıfırla
            runCatching {
                if (!physical.autoCommit) {
                    physical.rollback()
                    physical.autoCommit = true
                }
                physical.clearWarnings()
            }.onFailure {
                closePhysicalSilently(physical)
                return
            }

            holder.lastReturnedAtMs = now
            if (idleConnections.size < cfg.maxPoolSize) {
                idleConnections.offerFirst(holder)
            } else {
                closePhysicalSilently(physical)
            }
        } finally {
            activeCount.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
            poolSemaphore.release()
            updateMetrics(isHealthy = true, errorMessage = null)
        }
    }

    private fun wrapWithPoolProxy(holder: PooledConnectionHolder): Connection {
        val closedFlag = AtomicBoolean(false)
        val handler = InvocationHandler { proxy, method: Method, args: Array<out Any?>? ->
            when (method.name) {
                "close" -> {
                    if (closedFlag.compareAndSet(false, true)) {
                        releaseConnection(holder, discard = false)
                    }
                    null
                }
                "isClosed" -> closedFlag.get() || isPhysicalClosed(holder.physicalConnection)
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "PooledMariaDbConnection(created=${holder.createdAtMs}, closed=${closedFlag.get()})"
                else -> {
                    if (closedFlag.get()) {
                        throw SQLException("Bu bağlantı havuza iade edildiği için artık kullanılamaz.")
                    }
                    try {
                        if (args == null) {
                            method.invoke(holder.physicalConnection)
                        } else {
                            method.invoke(holder.physicalConnection, *args)
                        }
                    } catch (ite: InvocationTargetException) {
                        val targetEx = ite.targetException
                        if (targetEx is SQLException && isFatalConnectionError(targetEx)) {
                            if (closedFlag.compareAndSet(false, true)) {
                                releaseConnection(holder, discard = true)
                            }
                        }
                        throw targetEx
                    }
                }
            }
        }

        return Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
            handler
        ) as Connection
    }

    private fun createPhysicalConnection(cfg: MariaDbConfig): Connection {
        val props = Properties().apply {
            setProperty("user", cfg.user)
            setProperty("password", cfg.password)
            setProperty("useUnicode", "true")
            setProperty("characterEncoding", "UTF-8")
            setProperty("connectTimeout", cfg.connectTimeoutMs.toString())
            setProperty("socketTimeout", cfg.socketTimeoutMs.toString())
            setProperty("rewriteBatchedStatements", "true")
            setProperty("tcpKeepAlive", "true")
            setProperty("useSSL", cfg.sslEnabled.toString())
        }
        return DriverManager.getConnection(cfg.jdbcUrl, props)
    }

    private fun isConnectionValid(
        holder: PooledConnectionHolder,
        nowMs: Long,
        cfg: MariaDbConfig
    ): Boolean {
        val conn = holder.physicalConnection
        if (isPhysicalClosed(conn)) return false
        if ((nowMs - holder.lastValidatedAtMs) < cfg.validationIntervalMs) {
            return true
        }
        val valid = runCatching {
            conn.createStatement().use { stmt ->
                stmt.queryTimeout = 3
                stmt.executeQuery("SELECT 1").use { rs -> rs.next() }
            }
        }.getOrDefault(false)
        if (valid) {
            holder.lastValidatedAtMs = nowMs
        }
        return valid
    }

    private fun isPhysicalClosed(conn: Connection): Boolean {
        return runCatching { conn.isClosed }.getOrDefault(true)
    }

    private fun closePhysicalSilently(conn: Connection) {
        runCatching {
            if (!conn.isClosed) {
                conn.close()
            }
        }
    }

    private fun isFatalConnectionError(ex: SQLException): Boolean {
        val state = ex.sqlState.orEmpty()
        return state.startsWith("08") || // SQLState Connection Exception sınıfı
            ex.message?.contains("Communications link failure", ignoreCase = true) == true ||
            ex.message?.contains("Connection reset", ignoreCase = true) == true ||
            ex.message?.contains("Broken pipe", ignoreCase = true) == true
    }

    private fun ensureDriverLoaded() {
        if (driverLoaded.get()) return
        synchronized(this) {
            if (!driverLoaded.get()) {
                Class.forName(JDBC_DRIVER_CLASS)
                driverLoaded.set(true)
            }
        }
    }

    private fun startHousekeeperIfNeeded() {
        if (!housekeeperStarted.compareAndSet(false, true)) return
        managerScope.launch {
            while (isActive) {
                delay(45_000L)
                runCatching { evictExpiredConnections() }
            }
        }
    }

    private fun reloadConfigIfNeeded() {
        val newCfg = loadConfigFromEnv()
        if (newCfg != currentConfig) {
            val maxSizeChanged = newCfg.maxPoolSize != currentConfig.maxPoolSize
            currentConfig = newCfg
            if (maxSizeChanged) {
                poolSemaphore = Semaphore(newCfg.maxPoolSize.coerceAtLeast(1), true)
            }
            shutdownPool()
            updateMetrics(isHealthy = false, errorMessage = null)
        }
    }

    private fun loadConfigFromEnv(): MariaDbConfig {
        fun resolveEnv(key: String, buildConfigValue: String): String {
            val fromDotEnv = dotEnvFileOverrides[key]?.trim()
            if (!fromDotEnv.isNullOrBlank()) return fromDotEnv
            val fromSysEnv = runCatching { System.getenv(key)?.trim() }.getOrNull()
            if (!fromSysEnv.isNullOrBlank()) return fromSysEnv
            return buildConfigValue.trim()
        }

        val host = resolveEnv("MARIADB_HOST", BuildConfig.MARIADB_HOST)
        val port = resolveEnv("MARIADB_PORT", BuildConfig.MARIADB_PORT).toIntOrNull() ?: 3306
        val database = resolveEnv("MARIADB_DATABASE", BuildConfig.MARIADB_DATABASE)
        val user = resolveEnv("MARIADB_USER", BuildConfig.MARIADB_USER)
        val password = dotEnvFileOverrides["MARIADB_PASSWORD"]
            ?: runCatching { System.getenv("MARIADB_PASSWORD") }.getOrNull()
            ?: BuildConfig.MARIADB_PASSWORD

        val sslEnabled = resolveEnv(
            "MARIADB_SSL_ENABLED",
            BuildConfig.MARIADB_SSL_ENABLED
        ).equals("true", ignoreCase = true)

        val maxPoolSize = resolveEnv(
            "MARIADB_POOL_MAX_SIZE",
            BuildConfig.MARIADB_POOL_MAX_SIZE
        ).toIntOrNull()?.coerceIn(1, 32) ?: 8

        val minIdle = resolveEnv(
            "MARIADB_POOL_MIN_IDLE",
            BuildConfig.MARIADB_POOL_MIN_IDLE
        ).toIntOrNull()?.coerceIn(0, maxPoolSize) ?: 2

        val connectTimeoutMs = resolveEnv(
            "MARIADB_CONNECT_TIMEOUT_MS",
            BuildConfig.MARIADB_CONNECT_TIMEOUT_MS
        ).toIntOrNull()?.coerceIn(2_000, 60_000) ?: 8_000

        val socketTimeoutMs = resolveEnv(
            "MARIADB_SOCKET_TIMEOUT_MS",
            BuildConfig.MARIADB_SOCKET_TIMEOUT_MS
        ).toIntOrNull()?.coerceIn(5_000, 120_000) ?: 20_000

        return MariaDbConfig(
            host = host,
            port = port,
            database = database,
            user = user,
            password = password,
            sslEnabled = sslEnabled,
            maxPoolSize = maxPoolSize,
            minIdle = minIdle,
            connectTimeoutMs = connectTimeoutMs,
            socketTimeoutMs = socketTimeoutMs
        )
    }

    private fun readDotEnvFromContext(context: Context): Map<String, String> {
        val result = mutableMapOf<String, String>()
        runCatching {
            val candidates = listOf(
                File(context.filesDir, ".env"),
                File(".env")
            )
            candidates.firstOrNull { it.exists() && it.isFile }?.readLines()?.forEach { line ->
                parseDotEnvLine(line)?.let { (k, v) -> result[k] = v }
            }
        }
        runCatching {
            context.assets.open(".env").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    parseDotEnvLine(line)?.let { (k, v) ->
                        if (k !in result) result[k] = v
                    }
                }
            }
        }
        return result
    }

    private fun parseDotEnvLine(rawLine: String): Pair<String, String>? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        val eqIdx = trimmed.indexOf('=')
        if (eqIdx <= 0) return null
        val key = trimmed.substring(0, eqIdx).trim()
        val value = trimmed.substring(eqIdx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
        return if (key.isNotEmpty()) key to value else null
    }

    private fun updateMetrics(isHealthy: Boolean, errorMessage: String?) {
        val cfg = currentConfig
        _poolMetrics.value = PoolMetrics(
            configured = cfg.isConfigured,
            activeConnections = activeCount.get(),
            idleConnections = idleConnections.size,
            totalCreatedConnections = totalCreatedCount.get(),
            maxPoolSize = cfg.maxPoolSize,
            minIdle = cfg.minIdle,
            isHealthy = isHealthy && cfg.isConfigured,
            lastErrorMessage = errorMessage,
            lastVerifiedAt = System.currentTimeMillis()
        )
    }
}
