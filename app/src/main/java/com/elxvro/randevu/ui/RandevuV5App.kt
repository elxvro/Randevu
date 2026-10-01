package com.elxvro.randevu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.core.ApiMode
import com.elxvro.randevu.core.BackendAction
import com.elxvro.randevu.core.BackendEngine
import com.elxvro.randevu.core.BackendState
import com.elxvro.randevu.core.ConnectionState
import com.elxvro.randevu.network.ApiClient
import kotlinx.coroutines.launch

@Composable
fun RandevuV5App() {
    var backendState by remember { mutableStateOf(BackendState()) }
    var showBackendSettings by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    RandevuTheme {
        Box {
            RandevuV4App()
            BackendStatusChip(
                state = backendState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 38.dp),
                onClick = { showBackendSettings = true }
            )
        }

        if (showBackendSettings) {
            BackendDialog(
                state = backendState,
                onDismiss = { showBackendSettings = false },
                onMode = { mode ->
                    backendState = BackendEngine.reduce(
                        backendState,
                        BackendAction.SetMode(mode)
                    )
                },
                onBaseUrl = { url ->
                    backendState = BackendEngine.reduce(
                        backendState,
                        BackendAction.SetBaseUrl(url)
                    )
                },
                onTestConnection = { url ->
                    var next = BackendEngine.reduce(
                        backendState,
                        BackendAction.SetBaseUrl(url)
                    )
                    backendState = next

                    if (next.config.mode == ApiMode.DEMO) {
                        backendState = BackendEngine.reduce(
                            next,
                            BackendAction.ConnectionSucceeded("Yerel Demo")
                        )
                    } else if (!next.config.isRemoteConfigurationValid()) {
                        backendState = BackendEngine.reduce(
                            next,
                            BackendAction.ConnectionFailed("Sunucu adresi https:// ile başlamalı")
                        )
                    } else {
                        next = BackendEngine.reduce(next, BackendAction.CheckConnection)
                        backendState = next
                        scope.launch {
                            val result = ApiClient.checkHealth(next.config.normalizedBaseUrl())
                            backendState = if (result.ok) {
                                BackendEngine.reduce(
                                    backendState,
                                    BackendAction.ConnectionSucceeded(result.serverName)
                                )
                            } else {
                                BackendEngine.reduce(
                                    backendState,
                                    BackendAction.ConnectionFailed(result.message)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BackendStatusChip(
    state: BackendState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val label = when (state.connectionState) {
        ConnectionState.CONNECTED -> if (state.config.mode == ApiMode.DEMO) "Demo" else "Sunucu"
        ConnectionState.CHECKING -> "Kontrol"
        ConnectionState.ERROR -> "Bağlantı"
        ConnectionState.IDLE -> "Sunucu"
    }
    val icon = when (state.connectionState) {
        ConnectionState.CONNECTED -> Icons.Rounded.CloudDone
        ConnectionState.CHECKING -> Icons.Rounded.CloudSync
        ConnectionState.ERROR -> Icons.Rounded.CloudOff
        ConnectionState.IDLE -> Icons.Rounded.Dns
    }

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = "Sunucu ayarları",
                tint = when (state.connectionState) {
                    ConnectionState.CONNECTED -> SuccessGreen
                    ConnectionState.ERROR -> DangerRed
                    else -> BrandBlue
                },
                modifier = Modifier.size(17.dp)
            )
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BackendDialog(
    state: BackendState,
    onDismiss: () -> Unit,
    onMode: (ApiMode) -> Unit,
    onBaseUrl: (String) -> Unit,
    onTestConnection: (String) -> Unit
) {
    var url by rememberSaveable(state.config.baseUrl) {
        mutableStateOf(state.config.baseUrl)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Veri Bağlantısı", fontWeight = FontWeight.ExtraBold)
                Text(
                    "v0.5.0 • Backend Connect",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Uygulama demo verileriyle çalışmaya devam edebilir veya hazır PHP API sunucusuna bağlanabilir.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.config.mode == ApiMode.DEMO) {
                        FilledTonalButton(onClick = { onMode(ApiMode.DEMO) }) {
                            Text("Demo")
                        }
                    } else {
                        OutlinedButton(onClick = { onMode(ApiMode.DEMO) }) {
                            Text("Demo")
                        }
                    }

                    if (state.config.mode == ApiMode.REMOTE) {
                        FilledTonalButton(onClick = { onMode(ApiMode.REMOTE) }) {
                            Text("PHP API")
                        }
                    } else {
                        OutlinedButton(onClick = { onMode(ApiMode.REMOTE) }) {
                            Text("PHP API")
                        }
                    }
                }

                if (state.config.mode == ApiMode.REMOTE) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            onBaseUrl(it)
                        },
                        label = { Text("API adresi") },
                        placeholder = { Text("https://site.com/api") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Güvenlik için yalnızca HTTPS sunucuları kabul edilir.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                ConnectionCard(state)

                Button(
                    onClick = { onTestConnection(url) },
                    enabled = state.connectionState != ConnectionState.CHECKING,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.connectionState == ConnectionState.CHECKING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Kontrol ediliyor")
                    } else {
                        Icon(Icons.Rounded.CloudSync, null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (state.config.mode == ApiMode.DEMO) "Demo Bağlantısını Kontrol Et" else "Sunucuyu Test Et")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ConnectionCard(state: BackendState) {
    val title = when (state.connectionState) {
        ConnectionState.CONNECTED -> "Bağlantı hazır"
        ConnectionState.CHECKING -> "Sunucu kontrol ediliyor"
        ConnectionState.ERROR -> "Bağlantı kurulamadı"
        ConnectionState.IDLE -> "Kontrol bekleniyor"
    }
    val detail = when (state.connectionState) {
        ConnectionState.CONNECTED -> state.serverName.ifBlank { "Randevu API" }
        ConnectionState.CHECKING -> "Health endpoint yanıtı bekleniyor"
        ConnectionState.ERROR -> state.errorMessage ?: "Bilinmeyen bağlantı hatası"
        ConnectionState.IDLE -> "Sunucu adresini girip bağlantıyı test et"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state.connectionState) {
                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                when (state.connectionState) {
                    ConnectionState.CONNECTED -> Icons.Rounded.CloudDone
                    ConnectionState.ERROR -> Icons.Rounded.CloudOff
                    else -> Icons.Rounded.CloudSync
                },
                contentDescription = null,
                tint = when (state.connectionState) {
                    ConnectionState.CONNECTED -> SuccessGreen
                    ConnectionState.ERROR -> DangerRed
                    else -> BrandBlue
                }
            )
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    detail,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}
