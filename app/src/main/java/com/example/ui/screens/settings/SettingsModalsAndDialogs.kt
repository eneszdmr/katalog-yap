package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.BusinessType
import com.example.model.StoreSettings
import com.example.model.TeamRole
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYInput
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * 1) İŞLETME PROFİLİ DÜZENLEME MODALI:
 * - Logo (Galeriden seç veya hazır logo / URL), işletme adı, sektör, telefon, adres, sosyal medya linkleri
 */
@Composable
fun EditBusinessProfileModal(
    isVisible: Boolean,
    settings: StoreSettings,
    onDismiss: () -> Unit,
    onSave: (StoreSettings) -> Unit
) {
    if (!isVisible) return

    var logoUri by remember(settings) { mutableStateOf(settings.logoUri) }
    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var businessType by remember(settings) { mutableStateOf(settings.businessType) }
    var phoneWhatsapp by remember(settings) { mutableStateOf(settings.phoneWhatsapp) }
    var city by remember(settings) { mutableStateOf(settings.city) }
    var businessAddress by remember(settings) { mutableStateOf(settings.businessAddress) }
    var instagramUrl by remember(settings) { mutableStateOf(settings.instagramUrl) }
    var websiteUrl by remember(settings) { mutableStateOf(settings.websiteUrl) }
    var facebookUrl by remember(settings) { mutableStateOf(settings.facebookUrl) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            logoUri = uri.toString()
        }
    }

    val presetLogos = remember {
        listOf(
            "https://picsum.photos/seed/vera-logo/300/300",
            "https://picsum.photos/seed/boutique-brand/300/300",
            "https://picsum.photos/seed/decor-studio/300/300",
            "https://picsum.photos/seed/atelier-logo/300/300"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("modal_edit_business_profile")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "İşletme Profilini Düzenle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Logo Seçimi ve Önizleme
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = logoUri.ifBlank { "https://picsum.photos/seed/vera-logo/300/300" },
                        contentDescription = "İşletme Logosu",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, PrimaryBlue, RoundedCornerShape(16.dp))
                            .background(SurfaceMuted)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "İşletme Logosu",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            KYButton(
                                text = "Galeriden Seç",
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                variant = ButtonVariant.SECONDARY,
                                leadingIcon = Icons.Outlined.Image,
                                tag = "btn_pick_business_logo"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetLogos) { preset ->
                        val isSelected = logoUri == preset
                        AsyncImage(
                            model = preset,
                            contentDescription = "Hazır Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight,
                                    shape = CircleShape
                                )
                                .clickable { logoUri = preset }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                KYInput(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = "İşletme / Mağaza Adı *",
                    placeholder = "Örn: Vera Tasarım Butik",
                    leadingIcon = Icons.Outlined.Business,
                    tag = "input_profile_business_name"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sektör Çipleri
                Text(
                    text = "Sektör",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BusinessType.entries) { type ->
                        val selected = businessType == type.labelTr
                        Surface(
                            onClick = { businessType = type.labelTr },
                            shape = BadgeRoundedPill,
                            color = if (selected) PrimaryBlue else SurfaceMuted,
                            border = BorderStroke(1.dp, if (selected) PrimaryBlue else BorderLight)
                        ) {
                            Text(
                                text = type.labelTr,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) SurfaceWhite else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = phoneWhatsapp,
                    onValueChange = { phoneWhatsapp = it },
                    label = "Telefon / WhatsApp Hattı *",
                    placeholder = "+90 532 410 20 30",
                    leadingIcon = Icons.Outlined.Phone,
                    tag = "input_profile_phone"
                )

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = businessAddress,
                    onValueChange = { businessAddress = it },
                    label = "Açık Adres",
                    placeholder = "Mahalle, Cadde, No, İlçe / İl",
                    leadingIcon = Icons.Outlined.LocationOn,
                    tag = "input_profile_address"
                )

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = instagramUrl,
                    onValueChange = { instagramUrl = it },
                    label = "Instagram Linki / Kullanıcı Adı",
                    placeholder = "instagram.com/magazam",
                    leadingIcon = Icons.Outlined.Share,
                    tag = "input_profile_instagram"
                )

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = "Web Sitesi",
                    placeholder = "https://magazam.com",
                    leadingIcon = Icons.Outlined.Language,
                    tag = "input_profile_website"
                )

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = facebookUrl,
                    onValueChange = { facebookUrl = it },
                    label = "Facebook / Sosyal Medya Linki",
                    placeholder = "facebook.com/magazam",
                    leadingIcon = Icons.Outlined.Share,
                    tag = "input_profile_facebook"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KYButton(
                        text = "İptal",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        tag = "btn_cancel_profile_edit"
                    )
                    KYButton(
                        text = "Profili Kaydet",
                        onClick = {
                            onSave(
                                settings.copy(
                                    logoUri = logoUri.trim(),
                                    businessName = businessName.trim().ifBlank { settings.businessName },
                                    businessType = businessType,
                                    phoneWhatsapp = phoneWhatsapp.trim().ifBlank { settings.phoneWhatsapp },
                                    city = city.trim().ifBlank { "İstanbul" },
                                    businessAddress = businessAddress.trim(),
                                    instagramUrl = instagramUrl.trim(),
                                    websiteUrl = websiteUrl.trim(),
                                    facebookUrl = facebookUrl.trim()
                                )
                            )
                        },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1.2f),
                        tag = "btn_save_profile_edit"
                    )
                }
            }
        }
    }
}

/**
 * 2) EKİP KULLANICI DAVET ETME MODALI:
 * - Ad Soyad, E-posta veya Telefon ve Rol Seçimi (Yönetici, Editör, Sadece görüntüleme)
 */
@Composable
fun InviteTeamMemberModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onInvite: (name: String, emailOrPhone: String, role: TeamRole) -> Unit
) {
    if (!isVisible) return

    var name by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(TeamRole.EDITOR) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("modal_invite_team_member")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ekip Üyesi Davet Et",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Ad Soyad *",
                    placeholder = "Örn: Canan Yıldız",
                    tag = "input_team_member_name"
                )

                Spacer(modifier = Modifier.height(10.dp))

                KYInput(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    label = "E-posta veya Telefon *",
                    placeholder = "Örn: canan@veratasarim.com",
                    leadingIcon = Icons.Outlined.Email,
                    tag = "input_team_member_contact"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Yetki Rolü Seçin",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TeamRole.entries.forEach { role ->
                    val isSelected = selectedRole == role
                    Surface(
                        onClick = { selectedRole = role },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryBlueSubtle else BackgroundSlate,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) PrimaryBlue else BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("team_role_option_${role.name}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = role.labelTr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) PrimaryBlue else TextPrimary
                            )
                            Text(
                                text = role.descriptionTr,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KYButton(
                        text = "İptal",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        tag = "btn_cancel_team_invite"
                    )
                    KYButton(
                        text = "Davet Gönder",
                        onClick = {
                            if (emailOrPhone.isNotBlank()) {
                                onInvite(
                                    name.ifBlank { emailOrPhone.substringBefore("@") },
                                    emailOrPhone,
                                    selectedRole
                                )
                            }
                        },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1.2f),
                        tag = "btn_confirm_team_invite"
                    )
                }
            }
        }
    }
}

/**
 * 3) YARDIM VE DESTEK MODALI
 */
@Composable
fun HelpAndSupportModal(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_help_and_support")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yardım ve Destek Merkezi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val faqs = listOf(
                    "Katalog linkini müşterilerimle nasıl paylaşırım?" to "Katalog kartındaki 'Paylaş' butonuna tıklayarak kısa link (/c/...), PDF broşür veya filigranlı ürün görseli olarak WhatsApp'tan anında gönderebilirsiniz.",
                    "Bayi özel fiyatlı katalog nasıl çalışır?" to "Ayarlar > Bayiler bölümünden bayi ekleyip marj/indirim oranı belirlediğinizde o bayiye özel link üretilir. Siparişler 'Bayi' etiketiyle düşer.",
                    "Stoklar sipariş geldiğinde otomatik düşer mi?" to "Evet, ürünlerde stok takibi açıksa yeni sipariş geldiğinde stok otomatik düşer, sipariş iptal edilirse stok iade edilir."
                )

                faqs.forEach { (q, a) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BackgroundSlate,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = q,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = a,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                KYButton(
                    text = "WhatsApp Canlı Destek Hattı",
                    onClick = {
                        runCatching {
                            val uri = Uri.parse("https://wa.me/905324102030?text=${Uri.encode("Merhaba, Katalog Yap destek hattından bilgi almak istiyorum.")}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    },
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.AutoMirrored.Outlined.Chat,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_help_whatsapp_support"
                )

                Spacer(modifier = Modifier.height(8.dp))

                KYButton(
                    text = "Kapat",
                    onClick = onDismiss,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_close_help_modal"
                )
            }
        }
    }
}

/**
 * 4) GİZLİLİK POLİTİKASI VE KVKK MODALI
 */
@Composable
fun PrivacyPolicyModal(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_privacy_policy")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.PrivacyTip,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gizlilik Politikası ve KVKK",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "1. Veri Güvenliği ve KVKK Aydınlatma Metni\n" +
                        "Katalog Yap üzerinde oluşturduğunuz kataloglar, ürün görselleri, bayi bilgileri ve müşteri sipariş verileri 6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) kapsamında uçtan uca şifrelenerek korunur.\n\n" +
                        "2. Görsel ve İçerik Koruması\n" +
                        "Katalog görünümü ayarlarından 'Ekran görüntüsü ve görsel kaydetme koruması' aktif edildiğinde herkese açık katalog sayfalarında uzun basma ve sağ tık ile görsel indirme engellenir.\n\n" +
                        "3. Hesap ve Veri Silme Hakkı\n" +
                        "Dilediğiniz zaman Ayarlar > Hesabı Sil adımından tüm mağaza, katalog ve sipariş verilerinizi kalıcı olarak silebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(12.dp))

                KYButton(
                    text = "Okudum, Anladım",
                    onClick = onDismiss,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_close_privacy_modal"
                )
            }
        }
    }
}

/**
 * 5) HESABI SİL ONAY MODALI
 */
@Composable
fun DeleteAccountConfirmModal(
    isVisible: Boolean,
    businessName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (!isVisible) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_delete_account_confirm")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DangerRedLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hesabı Kalıcı Olarak Sil",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DangerRed
                        )
                        Text(
                            text = businessName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hesabınızı sildiğinizde tüm kataloglarınız, ürünleriniz, sipariş geçmişiniz, bayileriniz ve ekip ayarlarınız kalıcı olarak silinecektir. Bu işlem geri alınamaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KYButton(
                        text = "Vazgeç",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        tag = "btn_cancel_delete_account"
                    )
                    Surface(
                        onClick = onConfirmDelete,
                        shape = ButtonRoundedXl,
                        color = DangerRed,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_confirm_delete_account")
                    ) {
                        Text(
                            text = "Evet, Hesabımı Sil",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SurfaceWhite,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
