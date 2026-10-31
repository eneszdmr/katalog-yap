package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Order

/**
 * Müşteri sipariş oluşturduğunda (hem uygulama içinden hem de constantinopolishotel.store web vitrininden)
 * mağaza sahibinin telefonuna anında yüksek öncelikli sistem bildirimi gönderen yardımcı sınıf.
 */
object OrderNotificationHelper {

    private const val CHANNEL_ID = "ky_new_orders_channel"
    private const val CHANNEL_NAME = "Yeni Sipariş Bildirimleri"
    private const val CHANNEL_DESC = "Müşterileriniz kataloglarınızdan sipariş oluşturduğunda anlık bildirim gönderir."

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 250L, 150L, 250L)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showNewOrderNotification(
        context: Context,
        order: Order,
        storeName: String = ""
    ) {
        runCatching {
            val appContext = context.applicationContext
            ensureChannel(appContext)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val totalQty = order.items.sumOf { it.quantity }.coerceAtLeast(1)
            val amountStr = "${order.totalAmount.toInt()} ${order.currency.ifBlank { "₺" }}"
            val titlePrefix = if (storeName.isNotBlank()) "[$storeName] " else ""
            val title = "${titlePrefix}Yeni Sipariş Geldi! (${order.orderNumber})"
            val summaryText = "${order.customerName} • $amountStr ($totalQty ürün)"

            val itemsSummary = order.items.take(4).joinToString("\n") { item ->
                "• ${item.quantity}x ${item.productName} (${(item.unitPrice * item.quantity).toInt()} ${order.currency.ifBlank { "₺" }})"
            }
            val bigBody = buildString {
                append("Müşteri: ${order.customerName}")
                if (order.customerPhone.isNotBlank()) append(" (${order.customerPhone})")
                append("\nTutar: $amountStr • Katalog: ${order.catalogTitle.ifBlank { "Dijital Katalog" }}")
                if (itemsSummary.isNotBlank()) {
                    append("\n\nÜrünler:\n$itemsSummary")
                }
            }

            val launchIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_order_id", order.id)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                order.id.hashCode(),
                launchIntent,
                pendingFlags
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(appContext).notify(
                (order.id.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001),
                notification
            )
        }
    }
}
