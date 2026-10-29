package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.i18n.TrStrings
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYBottomSheet
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.components.KYSkeletonCardList
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Tasarım Sistemi ve Ortak Bileşenlerin Canlı Vitrini (Design System Showcase).
 * Oluşturulan tüm bileşenlerin (Button, Input, Card, Badge, BottomSheet,
 * Modal, Toast, EmptyState, Skeleton, TopBar) tek ekranda etkileşimli testini sağlar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignSystemScreen(
    onBackClick: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    var storeInput by remember { mutableStateOf("Kuzey Tasarım Atölyesi") }
    var searchInput by remember { mutableStateOf("") }
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var isModalOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_design_system")
    ) {
        KYTopBar(
            title = TrStrings.DesignShowcase.TITLE,
            subtitle = TrStrings.DesignShowcase.SUBTITLE,
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Butonlar
            item {
                KYCard {
                    Text(
                        text = TrStrings.DesignShowcase.SECTION_BUTTONS,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KYButton(
                            text = TrStrings.DesignShowcase.BTN_PRIMARY,
                            onClick = {
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.DesignShowcase.TOAST_SAMPLE_MESSAGE,
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            },
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.Add,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "showcase_btn_primary"
                        )
                        KYButton(
                            text = TrStrings.DesignShowcase.BTN_SECONDARY,
                            onClick = {
                                onShowToast(
                                    KYToastData(
                                        message = "İkincil buton tıklandı",
                                        type = KYToastType.INFO
                                    )
                                )
                            },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.Share,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "showcase_btn_secondary"
                        )
                        KYButton(
                            text = TrStrings.DesignShowcase.BTN_GHOST,
                            onClick = {
                                onShowToast(
                                    KYToastData(
                                        message = "Hayalet (Ghost) buton tıklandı",
                                        type = KYToastType.WARNING
                                    )
                                )
                            },
                            variant = ButtonVariant.GHOST,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "showcase_btn_ghost"
                        )
                    }
                }
            }

            // 2. Input Bileşeni
            item {
                KYCard {
                    Text(
                        text = TrStrings.DesignShowcase.SECTION_INPUTS,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    KYInput(
                        value = storeInput,
                        onValueChange = { storeInput = it },
                        label = TrStrings.DesignShowcase.INPUT_LABEL,
                        placeholder = TrStrings.DesignShowcase.INPUT_PLACEHOLDER,
                        helperText = TrStrings.DesignShowcase.INPUT_HELPER,
                        leadingIcon = Icons.Outlined.Storefront,
                        tag = "showcase_input_store"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    KYInput(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = TrStrings.Common.SEARCH_PLACEHOLDER,
                        tag = "showcase_input_search"
                    )
                }
            }

            // 3. Rozetler (Badges)
            item {
                KYCard {
                    Text(
                        text = TrStrings.DesignShowcase.SECTION_BADGES,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KYBadge(text = "Ana Renk (#2563EB)", variant = BadgeVariant.PRIMARY)
                        KYBadge(text = "Vurgu (#F59E0B)", variant = BadgeVariant.ACCENT)
                        KYBadge(text = TrStrings.Common.ACTIVE, variant = BadgeVariant.SUCCESS)
                        KYBadge(text = "Bekleyen", variant = BadgeVariant.WARNING)
                        KYBadge(text = "Stok Tükendi", variant = BadgeVariant.DANGER)
                        KYBadge(text = TrStrings.Common.DRAFT, variant = BadgeVariant.NEUTRAL)
                    }
                }
            }

            // 4. BottomSheet, Modal ve Toast Tetikleyicileri
            item {
                KYCard {
                    Text(
                        text = TrStrings.DesignShowcase.SECTION_OVERLAYS,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KYButton(
                            text = TrStrings.DesignShowcase.OPEN_BOTTOM_SHEET,
                            onClick = { isBottomSheetOpen = true },
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            tag = "showcase_open_sheet"
                        )
                        KYButton(
                            text = TrStrings.DesignShowcase.OPEN_MODAL,
                            onClick = { isModalOpen = true },
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            tag = "showcase_open_modal"
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    KYButton(
                        text = TrStrings.DesignShowcase.SHOW_TOAST,
                        onClick = {
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.DesignShowcase.TOAST_SAMPLE_MESSAGE,
                                    type = KYToastType.SUCCESS
                                )
                            )
                        },
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.NotificationsActive,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "showcase_show_toast"
                    )
                }
            }

            // 5. Skeleton Loader
            item {
                Text(
                    text = TrStrings.DesignShowcase.SECTION_FEEDBACK,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                KYSkeletonCardList(itemCount = 2)
            }
        }
    }

    // BottomSheet Önizlemesi
    KYBottomSheet(
        isVisible = isBottomSheetOpen,
        title = TrStrings.DesignShowcase.SHEET_TITLE,
        subtitle = "KATALOG YAP Ortak Bileşen",
        onDismiss = { isBottomSheetOpen = false }
    ) {
        Text(
            text = TrStrings.DesignShowcase.SHEET_DESC,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        KYButton(
            text = TrStrings.Common.CONFIRM,
            onClick = {
                isBottomSheetOpen = false
                onShowToast(
                    KYToastData(
                        message = "BottomSheet aksiyonu tamamlandı",
                        type = KYToastType.SUCCESS
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Modal Önizlemesi
    KYModal(
        isVisible = isModalOpen,
        title = TrStrings.DesignShowcase.MODAL_TITLE,
        description = TrStrings.DesignShowcase.MODAL_DESC,
        onConfirm = {
            isModalOpen = false
            onShowToast(
                KYToastData(
                    message = "Modal onaylandı!",
                    type = KYToastType.SUCCESS
                )
            )
        },
        onDismiss = { isModalOpen = false }
    )
}
