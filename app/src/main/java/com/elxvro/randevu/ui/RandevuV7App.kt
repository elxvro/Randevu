package com.elxvro.randevu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.SmartToolsEngine
import com.elxvro.randevu.storage.LiveSyncStore
import java.time.LocalDate

private enum class SmartTab(val label: String) {
    QUICK("Hızlı İşlem"),
    CUSTOMER("Müşteri"),
    REPORT("Rapor")
}

@Composable
fun RandevuV7App() {
    val context = LocalContext.current
    val store = remember { LiveSyncStore(context.applicationContext) }
    var showTools by rememberSaveable { mutableStateOf(false) }

    RandevuTheme {
        Box(Modifier.fillMaxSize()) {
            RandevuV6App()
            AssistChip(
                onClick = { showTools = true },
                label = { Text("Araçlar") },
                leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) },
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 38.dp)
            )
        }

        if (showTools) {
            val appointments = remember(showTools) { store.loadAppointments() }
            SmartToolsCenter(
                appointments = appointments,
                onDismiss = { showTools = false },
                onWhatsApp = { appointment -> openWhatsApp(context, appointment) },
                onSms = { appointment -> openSms(context, appointment) },
                onCall = { appointment -> openDialer(context, appointment.phone) }
            )
        }
    }
}

@Composable
private fun SmartToolsCenter(
    appointments: List<Appointment>,
    onDismiss: () -> Unit,
    onWhatsApp: (Appointment) -> Unit,
    onSms: (Appointment) -> Unit,
    onCall: (Appointment) -> Unit
) {
    var tabName by rememberSaveable { mutableStateOf(SmartTab.QUICK.name) }
    var customerQuery by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val tab = SmartTab.valueOf(tabName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Tune, null, tint = BrandBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Akıllı Araçlar", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("v0.7.0 • iletişim, müşteri geçmişi ve raporlama", fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Kapat") }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SmartTab.entries) { item ->
                        FilterChip(
                            selected = tab == item,
                            onClick = { tabName = item.name },
                            label = { Text(item.label) },
                            leadingIcon = if (tab == item) {
                                { Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (tab) {
                        SmartTab.QUICK -> {
                            val active = appointments
                                .filter { it.status != AppointmentStatus.CANCELLED && it.date >= LocalDate.now().toString() }
                                .sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text("Hızlı iletişim", fontWeight = FontWeight.ExtraBold)
                                        Text("WhatsApp, SMS ve arama işlemleri telefonun ilgili uygulamasını açar.", fontSize = 12.sp)
                                    }
                                }
                            }
                            if (active.isEmpty()) {
                                item { ToolEmpty("Aktif randevu bulunamadı.") }
                            } else {
                                items(active.take(30), key = { it.id }) { appointment ->
                                    ContactCard(appointment, onWhatsApp, onSms, onCall)
                                }
                            }
                        }

                        SmartTab.CUSTOMER -> {
                            item {
                                OutlinedTextField(
                                    value = customerQuery,
                                    onValueChange = { customerQuery = it },
                                    label = { Text("Müşteri adı veya telefon") },
                                    leadingIcon = { Icon(Icons.Rounded.People, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            val match = appointments.firstOrNull {
                                customerQuery.isNotBlank() && (
                                    it.customer.contains(customerQuery, ignoreCase = true) ||
                                        it.phone.filter(Char::isDigit).contains(customerQuery.filter(Char::isDigit))
                                    )
                            }
                            if (match == null) {
                                item { ToolEmpty(if (customerQuery.isBlank()) "Müşteri araması yap." else "Müşteri bulunamadı.") }
                            } else {
                                val summary = SmartToolsEngine.customerSummary(appointments, match.phone)
                                item {
                                    Card {
                                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(summary.customer, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                            Text(summary.phone)
                                            Text("Toplam ${summary.totalAppointments} • Tamamlanan ${summary.completedAppointments} • İptal ${summary.cancelledAppointments}")
                                            Text(
                                                if (summary.tags.isEmpty()) "Etiket yok — randevu notuna #VIP gibi etiket ekleyebilirsin."
                                                else "Etiketler: ${summary.tags.joinToString(" • ")}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                                            )
                                        }
                                    }
                                }
                                items(
                                    appointments.filter { it.phone.filter(Char::isDigit) == match.phone.filter(Char::isDigit) }
                                        .sortedWith(compareByDescending<Appointment> { it.date }.thenByDescending { it.time }),
                                    key = { it.id }
                                ) { appointment ->
                                    ContactCard(appointment, onWhatsApp, onSms, onCall)
                                }
                            }
                        }

                        SmartTab.REPORT -> {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = startDate,
                                        onValueChange = { startDate = it.take(10) },
                                        label = { Text("Başlangıç") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = endDate,
                                        onValueChange = { endDate = it.take(10) },
                                        label = { Text("Bitiş") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            val report = SmartToolsEngine.report(
                                appointments,
                                defaultPrices,
                                startDate,
                                endDate
                            )
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ToolMetric("Aktif", report.activeAppointments.toString(), Modifier.weight(1f))
                                    ToolMetric("Tamam", report.completedAppointments.toString(), Modifier.weight(1f))
                                    ToolMetric("Bekleyen", report.pendingAppointments.toString(), Modifier.weight(1f))
                                }
                            }
                            item {
                                Card {
                                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Insights, null, tint = BrandBlue)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Gelir Özeti", fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text("Beklenen: ${report.expectedRevenue.toInt()} ₺", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text("Gerçekleşen: ${report.realizedRevenue.toInt()} ₺")
                                        Text("Onaylı: ${report.confirmedAppointments} • İptal: ${report.cancelledAppointments}")
                                        Text("En sık hizmet: ${report.topService ?: "—"}")
                                        Text("Gelir hesabı kayıtlı varsayılan hizmet fiyatlarını kullanır.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    appointment: Appointment,
    onWhatsApp: (Appointment) -> Unit,
    onSms: (Appointment) -> Unit,
    onCall: (Appointment) -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(appointment.customer, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${appointment.date} • ${appointment.time} • ${appointment.service}", fontSize = 12.sp)
                }
                Text(appointment.status.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onWhatsApp(appointment) },
                    enabled = SmartToolsEngine.canContact(appointment.phone),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Message, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { onSms(appointment) },
                    enabled = SmartToolsEngine.canContact(appointment.phone),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Sms, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SMS", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { onCall(appointment) },
                    enabled = SmartToolsEngine.canContact(appointment.phone)
                ) { Icon(Icons.Rounded.Call, "Ara") }
            }
        }
    }
}

@Composable
private fun ToolMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
            Text(label, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ToolEmpty(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(message, Modifier.fillMaxWidth().padding(16.dp), fontSize = 13.sp)
    }
}

private val defaultPrices = mapOf(
    "Saç Kesimi" to 250.0,
    "Erkek Saç Kesimi" to 250.0,
    "Sakal" to 150.0,
    "Sakal Tıraşı" to 150.0,
    "Saç + Sakal" to 350.0,
    "Saç Boyama" to 600.0,
    "Cilt Bakımı" to 450.0,
    "Saç Bakımı" to 400.0,
    "Araç Bakım" to 1500.0
)

private fun openWhatsApp(context: Context, appointment: Appointment) {
    val digits = SmartToolsEngine.digitsOnlyPhone(appointment.phone).let { phone ->
        if (phone.startsWith("0") && phone.length == 11) "90${phone.drop(1)}" else phone
    }
    val message = SmartToolsEngine.whatsappMessage(appointment, "Randevu")
    val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openSms(context: Context, appointment: Appointment) {
    val uri = Uri.parse("smsto:${Uri.encode(appointment.phone)}")
    val intent = Intent(Intent.ACTION_SENDTO, uri)
        .putExtra("sms_body", SmartToolsEngine.smsMessage(appointment, "Randevu"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openDialer(context: Context, phone: String) {
    val uri = Uri.parse("tel:${Uri.encode(phone)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
