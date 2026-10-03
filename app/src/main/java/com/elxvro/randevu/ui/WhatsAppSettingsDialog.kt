package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elxvro.randevu.core.WhatsAppConnectionState
import com.elxvro.randevu.core.WhatsAppSettings
import com.elxvro.randevu.network.WhatsAppApiContract
import com.elxvro.randevu.network.WhatsAppServerConfig

@Composable
internal fun V13WhatsAppSettingsDialog(
    settings: WhatsAppSettings,
    serverConfig: WhatsAppServerConfig,
    connectionState: WhatsAppConnectionState,
    pendingCount: Int,
    busy: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onConnect: (String, String, WhatsAppSettings) -> Unit,
    onSave: (String, WhatsAppSettings) -> Unit,
    onTest: () -> Unit
) {
    var baseUrl by rememberSaveable(serverConfig.baseUrl) { mutableStateOf(serverConfig.baseUrl) }
    var setupKey by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable(settings.enabled) { mutableStateOf(settings.enabled) }
    var reminder24h by rememberSaveable(settings.reminder24h) { mutableStateOf(settings.reminder24h) }
    var reminder2h by rememberSaveable(settings.reminder2h) { mutableStateOf(settings.reminder2h) }
    var templateName by rememberSaveable(settings.templateName) { mutableStateOf(settings.templateName) }
    var languageCode by rememberSaveable(settings.languageCode) { mutableStateOf(settings.languageCode) }
    var phoneNumberId by rememberSaveable(settings.metaPhoneNumberId) { mutableStateOf(settings.metaPhoneNumberId) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val currentSettings = WhatsAppSettings(
        enabled = enabled,
        reminder24h = reminder24h,
        reminder2h = reminder2h,
        templateName = templateName.trim().ifBlank { "appointment_reminder" },
        languageCode = languageCode.trim().ifBlank { "tr" },
        metaPhoneNumberId = phoneNumberId.trim()
    )
    val statusColor = when (connectionState) {
        WhatsAppConnectionState.CONNECTED -> RefSuccess
        WhatsAppConnectionState.INCOMPLETE -> RefWarning
        WhatsAppConnectionState.UNREACHABLE -> RefDanger
        WhatsAppConnectionState.DISABLED -> RefTextMuted
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.90f),
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V12IconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("WhatsApp Hatırlatmaları", color = RefText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Resmi WhatsApp Business Cloud API", color = RefTextMuted, fontSize = 9.sp)
                    }
                    V12MiniPill(WhatsAppStatusProjection.label(connectionState, pendingCount), statusColor)
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.6f))

                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    item {
                        V12Card {
                            Text(
                                "Meta erişim anahtarı APK içinde tutulmaz. Sunucudaki config.php/env ayarından okunur.",
                                color = RefTextMuted,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                            if (pendingCount > 0) {
                                Spacer(Modifier.height(6.dp))
                                Text("$pendingCount işlem bağlantı gelince gönderilmek üzere cihazda bekliyor.", color = RefWarning, fontSize = 9.sp)
                            }
                        }
                    }

                    item { V12Input(baseUrl, { baseUrl = it }, "Sunucu adresi (HTTPS)", Icons.Rounded.Language) }

                    if (serverConfig.token.isBlank()) {
                        item { V12Input(setupKey, { setupKey = it }, "Sunucu kurulum anahtarı", Icons.Rounded.Key) }
                    }

                    item {
                        V12Card {
                            V12SettingsToggleRow(Icons.Rounded.Send, "WhatsApp otomatik hatırlatma", enabled) { enabled = it }
                            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
                            V12SettingsToggleRow(Icons.Rounded.Schedule, "24 saat önce", reminder24h) { reminder24h = it }
                            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
                            V12SettingsToggleRow(Icons.Rounded.Schedule, "2 saat önce", reminder2h) { reminder2h = it }
                        }
                    }

                    item { V12Input(phoneNumberId, { phoneNumberId = it.filter(Char::isDigit) }, "Meta Phone Number ID", Icons.Rounded.PhoneAndroid) }
                    item { V12Input(templateName, { templateName = it }, "Onaylı şablon adı", Icons.Rounded.Settings) }
                    item { V12Input(languageCode, { languageCode = it }, "Şablon dili", Icons.Rounded.Language) }

                    val feedback = localError ?: message
                    if (!feedback.isNullOrBlank()) {
                        item {
                            Text(
                                feedback,
                                color = if (connectionState == WhatsAppConnectionState.CONNECTED) RefSuccess else RefWarning,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (busy) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(color = RefCyan, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = RefBorder.copy(alpha = 0.6f))
                if (serverConfig.token.isBlank()) {
                    Button(
                        onClick = {
                            localError = when {
                                WhatsAppApiContract.normalizeBaseUrl(baseUrl).isBlank() -> "Geçerli bir HTTPS sunucu adresi girin."
                                setupKey.isBlank() -> "Sunucu kurulum anahtarını girin."
                                else -> null
                            }
                            if (localError == null) onConnect(baseUrl, setupKey, currentSettings)
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(14.dp).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                    ) {
                        Icon(Icons.Rounded.CloudDone, null)
                        Spacer(Modifier.width(7.dp))
                        Text("Sunucuya Bağlan", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTest,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RefBorder)
                        ) {
                            Text("Bağlantıyı Test Et", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                localError = if (WhatsAppApiContract.normalizeBaseUrl(baseUrl).isBlank()) {
                                    "Geçerli bir HTTPS sunucu adresi girin."
                                } else null
                                if (localError == null) onSave(baseUrl, currentSettings)
                            },
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                        ) {
                            Text("Kaydet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
