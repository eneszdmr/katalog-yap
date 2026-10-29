package com.example.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.auth.AuthContextState
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import kotlinx.coroutines.delay

/**
 * 3) OTP EKRANI (/verify):
 * - 6 haneli kod girişi ve kutular arası otomatik odak geçişi
 * - 30 sn geri sayımlı "Kodu tekrar gönder"
 * - Demo için her zaman 123456 kabul eder ve ekranda ipucu olarak gösterir.
 */
@Composable
fun VerifyOtpScreen(
    onBackToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToLogin)

    val authContext = LocalAuthContext.current
    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdownSeconds by remember { mutableIntStateOf(30) }

    // 30 saniyelik geri sayım sayacı
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds -= 1
        }
    }

    // Ekran açıldığında ilk kutuya odaklan
    LaunchedEffect(Unit) {
        runCatching { focusRequesters[0].requestFocus() }
    }

    fun submitOtp(code: String) {
        val verified = authContext.verifyOtpCode(code) { needsOnboarding ->
            if (needsOnboarding) {
                onNavigateToOnboarding()
            } else {
                onNavigateToHome()
            }
        }
        if (!verified) {
            errorMessage = TrStrings.Auth.OTP_ERROR_INVALID
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_verify_otp")
    ) {
        KYTopBar(
            title = TrStrings.Auth.OTP_TITLE,
            onBackClick = onBackToLogin
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KYCard {
                Text(
                    text = TrStrings.Auth.OTP_TITLE,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${TrStrings.Auth.OTP_SUBTITLE_PREFIX}${authContext.formattedPendingPhone.ifBlank { "+90 555 000 00 00" }}${TrStrings.Auth.OTP_SUBTITLE_SUFFIX}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 6 Haneli OTP Kutuları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (index in 0 until 6) {
                        val digit = otpDigits[index]
                        OutlinedTextField(
                            value = digit,
                            onValueChange = { newValue ->
                                errorMessage = null
                                val cleanDigits = newValue.filter { it.isDigit() }

                                // Kullanıcı 6 haneli kodu yapıştırırsa tüm kutuları doldur
                                if (cleanDigits.length >= 6) {
                                    for (i in 0 until 6) {
                                        otpDigits[i] = cleanDigits[i].toString()
                                    }
                                    submitOtp(cleanDigits.take(6))
                                } else if (cleanDigits.isNotEmpty()) {
                                    otpDigits[index] = cleanDigits.last().toString()
                                    if (index < 5) {
                                        runCatching { focusRequesters[index + 1].requestFocus() }
                                    } else {
                                        val fullCode = otpDigits.joinToString("")
                                        if (fullCode.length == 6) {
                                            submitOtp(fullCode)
                                        }
                                    }
                                } else {
                                    otpDigits[index] = ""
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[index])
                                .onKeyEvent { event ->
                                    if (event.key == Key.Backspace && otpDigits[index].isEmpty() && index > 0) {
                                        otpDigits[index - 1] = ""
                                        runCatching { focusRequesters[index - 1].requestFocus() }
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .testTag("otp_box_$index"),
                            singleLine = true,
                            shape = ButtonRoundedXl,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = errorMessage != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = if (digit.isNotEmpty()) PrimaryBlue else BorderLight,
                                errorBorderColor = DangerRed
                            )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Doğrula Butonu
                val enteredCode = otpDigits.joinToString("")
                KYButton(
                    text = TrStrings.Auth.OTP_VERIFY_BUTTON,
                    onClick = { submitOtp(enteredCode) },
                    enabled = enteredCode.length == 6,
                    leadingIcon = Icons.Outlined.LockOpen,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_verify_otp"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 30 sn Geri Sayımlı "Kodu tekrar gönder"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (countdownSeconds > 0) {
                        val formattedTime = "00:${countdownSeconds.toString().padStart(2, '0')}"
                        Text(
                            text = "${TrStrings.Auth.OTP_RESEND_COUNTDOWN} ($formattedTime)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.testTag("otp_countdown_text")
                        )
                    } else {
                        KYButton(
                            text = TrStrings.Auth.OTP_RESEND_ACTIVE,
                            onClick = {
                                countdownSeconds = 30
                                errorMessage = null
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Auth.OTP_RESENT_TOAST,
                                        type = KYToastType.INFO
                                    )
                                )
                            },
                            variant = ButtonVariant.GHOST,
                            leadingIcon = Icons.Outlined.Refresh,
                            tag = "btn_resend_otp"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Demo İpucu Kartı (123456)
            Surface(
                shape = ButtonRoundedXl,
                color = AccentAmberLight,
                border = BorderStroke(1.dp, AccentAmberDark.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_demo_hint_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = WarningAmberDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = TrStrings.Auth.OTP_DEMO_HINT_TITLE,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WarningAmberDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                KYBadge(
                                    text = AuthContextState.DEMO_OTP_CODE,
                                    variant = BadgeVariant.ACCENT
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TrStrings.Auth.OTP_DEMO_HINT_DESC,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmberDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    KYButton(
                        text = TrStrings.Auth.OTP_AUTO_FILL,
                        onClick = {
                            val demo = AuthContextState.DEMO_OTP_CODE
                            for (i in 0 until 6) {
                                otpDigits[i] = demo[i].toString()
                            }
                            errorMessage = null
                        },
                        variant = ButtonVariant.SECONDARY,
                        tag = "btn_autofill_demo_otp"
                    )
                }
            }
        }
    }
}
