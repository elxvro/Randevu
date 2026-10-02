package com.elxvro.randevu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.QualityEngine
import com.elxvro.randevu.storage.LiveSyncStore

@Composable
fun RandevuV8App() {
    val context = LocalContext.current
    val store = remember { LiveSyncStore(context.applicationContext) }
    var showQuality by rememberSaveable { mutableStateOf(false) }

    RandevuTheme {
        Box(Modifier.fillMaxSize()) {
            RandevuV7App()
            AssistChip(
                onClick = { showQuality = true },
                label = { Text("Kontrol") },
                leadingIcon = { Icon(Icons.Rounded.FactCheck, null, Modifier.size(18.dp)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 92.dp)
            )
        }

        if (showQuality) {
            val appointments = remember(showQuality) { store.loadAppointments() }
            QualityDialog(appointments) { showQuality = false }
        }
    }
}

@Composable
private fun QualityDialog(appointments: List<Appointment>, onDismiss: () -> Unit) {
    val summary = QualityEngine.summary(appointments)
    val conflicts = QualityEngine.conflicts(appointments)
    val invalid = appointments.mapNotNull { appointment ->
        val issues = QualityEngine.validate(appointment)
        if (issues.isEmpty()) null else appointment to issues
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Uygulama Kontrolü", fontWeight = FontWeight.ExtraBold)
                Text("v1.0.0 • Final", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (summary.ready) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (summary.ready) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (summary.ready) SuccessGreen else WarningOrange
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(if (summary.ready) "Kayıtlar temiz" else "Kontrol gereken kayıtlar var", fontWeight = FontWeight.Bold)
                                Text("${summary.total} kayıt • ${summary.invalidRecords} geçersiz • ${summary.conflicts} çakışma", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (invalid.isNotEmpty()) {
                    item { Text("Geçersiz kayıtlar", fontWeight = FontWeight.ExtraBold) }
                    items(invalid, key = { it.first.id.ifBlank { it.first.hashCode().toString() } }) { (appointment, issues) ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(appointment.customer.ifBlank { "İsimsiz kayıt" }, fontWeight = FontWeight.Bold)
                                }
                                Text(issues.joinToString(" • ") { it.message }, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (conflicts.isNotEmpty()) {
                    item { Text("Saat çakışmaları", fontWeight = FontWeight.ExtraBold) }
                    items(conflicts, key = { "${it.staff}-${it.date}-${it.time}" }) { conflict ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(conflict.staff, fontWeight = FontWeight.Bold)
                                Text("${conflict.date} • ${conflict.time} • ${conflict.appointmentIds.size} randevu", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (summary.ready) {
                    item {
                        Text(
                            "Form alanları, telefon/tarih/saat biçimleri ve aynı personelin aynı saatteki aktif randevuları kontrol edildi.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } },
        shape = RoundedCornerShape(24.dp)
    )
}
