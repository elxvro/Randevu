package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.notifications.NotificationCenterRow
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffLeaveEngine
import com.elxvro.randevu.staff.StaffRecord
import com.elxvro.randevu.staff.StaffProjection
import java.time.LocalDate

@Composable
internal fun V12AppointmentDialog(
    existing: Appointment?,
    prefillCustomer: String,
    prefillPhone: String,
    staffRecords: List<StaffRecord>,
    leaves: List<StaffLeave>,
    appointments: List<Appointment>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit,
    onDelete: ((String) -> Unit)?
) {
    val key = existing?.id ?: "new-$prefillCustomer-$prefillPhone"
    var customer by rememberSaveable(key) { mutableStateOf(existing?.customer ?: prefillCustomer) }
    var phone by rememberSaveable(key) { mutableStateOf(existing?.phone ?: prefillPhone) }
    var service by rememberSaveable(key) { mutableStateOf(existing?.service ?: "Saç Kesimi") }
    var staffName by rememberSaveable(key) { mutableStateOf(existing?.staff ?: staffRecords.firstOrNull { it.active }?.name.orEmpty()) }
    var date by rememberSaveable(key) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var time by rememberSaveable(key) { mutableStateOf(existing?.time ?: "10:00") }
    var note by rememberSaveable(key) { mutableStateOf(existing?.note.orEmpty()) }
    var statusName by rememberSaveable(key) { mutableStateOf((existing?.status ?: AppointmentStatus.CONFIRMED).name) }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(key) { mutableStateOf(false) }
    val services = listOf("Saç Kesimi", "Cilt Bakımı", "Saç Boyama", "Manikür", "Danışmanlık")
    val selectedStaff = staffRecords.firstOrNull { it.name == staffName }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.91f),
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    V12IconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (existing == null) "Yeni Randevu" else "Randevu Detayı", color = RefText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(if (existing == null) "Randevu bilgilerini girin" else "Düzenle, durum değiştir veya sil", color = RefTextMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.65f))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("Hizmet", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(services) { option -> V12SelectPill(option, service == option) { service = option } }
                        }
                    }
                    item { V12Input(customer, { customer = it }, "Müşteri adı", Icons.Rounded.Person) }
                    item { V12Input(phone, { phone = it }, "Telefon", Icons.Rounded.Phone) }
                    item { Text("Personel", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(staffRecords, key = { it.id }) { member ->
                                V12SelectPill(
                                    text = if (member.active) member.name else "${member.name} • Pasif",
                                    selected = staffName == member.name,
                                    enabled = member.active || existing?.staff == member.name
                                ) { staffName = member.name }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { V12Input(date, { date = it }, "Tarih YYYY-AA-GG", Icons.Rounded.CalendarToday) }
                            Box(Modifier.weight(0.72f)) { V12Input(time, { time = it }, "Saat SS:DD", Icons.Rounded.Schedule) }
                        }
                    }
                    item { V12Input(note, { note = it }, "Not", Icons.Rounded.Description, singleLine = false) }
                    if (existing != null) {
                        item { Text("Durum", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(AppointmentStatus.entries) { status ->
                                    V12SelectPill(status.label, statusName == status.name) { statusName = status.name }
                                }
                            }
                        }
                    }
                    error?.let { message -> item { Text(message, color = RefDanger, fontSize = 10.sp) } }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existing != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.weight(0.7f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RefDanger),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RefDanger)
                        ) { Icon(Icons.Rounded.DeleteOutline, null); Spacer(Modifier.width(5.dp)); Text("Sil") }
                    }
                    Button(
                        onClick = {
                            val basicError = when {
                                customer.trim().length < 2 -> "Müşteri adını kontrol edin."
                                phone.filter(Char::isDigit).length < 10 -> "Telefon numarasını kontrol edin."
                                else -> ReferenceBookingRules.validationError(
                                    selectedStaff,
                                    leaves,
                                    date,
                                    time,
                                    appointments,
                                    existing?.id
                                )
                            }
                            if (basicError != null) {
                                error = basicError
                            } else {
                                onSave(
                                    Appointment(
                                        id = existing?.id ?: "local-${System.currentTimeMillis()}",
                                        customer = customer.trim(),
                                        phone = phone.trim(),
                                        service = service,
                                        staff = staffName,
                                        date = date,
                                        time = time,
                                        status = runCatching { AppointmentStatus.valueOf(statusName) }.getOrDefault(AppointmentStatus.CONFIRMED),
                                        note = note.trim()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                    ) {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (existing == null) "Randevuyu Kaydet" else "Değişiklikleri Kaydet", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (confirmDelete && existing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = RefSurface,
            title = { Text("Randevuyu sil?", color = RefText) },
            text = { Text("Bu işlem cihazdaki randevu kaydını kalıcı olarak siler.", color = RefTextMuted) },
            confirmButton = { TextButton(onClick = { onDelete(existing.id); confirmDelete = false }) { Text("Sil", color = RefDanger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç", color = RefCyan) } }
        )
    }
}

@Composable
internal fun V12AddStaffDialog(onDismiss: () -> Unit, onSave: (StaffRecord) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("Uzman") }
    var phone by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RefSurface,
        title = { Text("Yeni Personel", color = RefText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                V12Input(name, { name = it }, "Ad soyad", Icons.Rounded.Person)
                V12Input(title, { title = it }, "Görev", Icons.Rounded.Badge)
                V12Input(phone, { phone = it }, "Telefon (isteğe bağlı)", Icons.Rounded.Phone)
                error?.let { Text(it, color = RefDanger, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().length < 2) error = "Personel adını kontrol edin."
                else onSave(StaffRecord("staff-${System.currentTimeMillis()}", name.trim(), title.trim().ifBlank { "Personel" }, phone.trim(), true))
            }) { Text("Kaydet", color = RefCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = RefTextMuted) } }
    )
}

@Composable
internal fun V12StaffDetailDialog(
    record: StaffRecord,
    appointments: List<Appointment>,
    leaves: List<StaffLeave>,
    onDismiss: () -> Unit,
    onSaveStaff: (StaffRecord) -> Unit,
    onAddLeave: (StaffLeave) -> Unit,
    onDeleteLeave: (StaffLeave) -> Unit
) {
    var name by rememberSaveable(record.id) { mutableStateOf(record.name) }
    var title by rememberSaveable(record.id) { mutableStateOf(record.title) }
    var phone by rememberSaveable(record.id) { mutableStateOf(record.phone) }
    var active by rememberSaveable(record.id) { mutableStateOf(record.active) }
    var showAddLeave by rememberSaveable(record.id) { mutableStateOf(false) }
    var leaveToDelete by remember { mutableStateOf<StaffLeave?>(null) }
    val ownLeaves = StaffProjection.leavesFor(record.id, leaves)
    val ownAppointments = appointments.filter { it.staff == record.name }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f),
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V12IconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Personel Detayı", color = RefText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Profil ve izin takvimi", color = RefTextMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.65f))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { V12Input(name, { name = it }, "Ad soyad", Icons.Rounded.Person) }
                    item { V12Input(title, { title = it }, "Görev", Icons.Rounded.Badge) }
                    item { V12Input(phone, { phone = it }, "Telefon", Icons.Rounded.Phone) }
                    item {
                        V12Card {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Aktif personel", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Pasif personel yeni randevu alamaz.", color = RefTextMuted, fontSize = 9.sp)
                                }
                                Switch(checked = active, onCheckedChange = { active = it })
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V12MiniPill("${ownAppointments.size} randevu", RefBlue)
                            V12MiniPill("${ownLeaves.size} izin", RefWarning)
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("İzin Takvimi", color = RefText, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            V12TinyButton("İzin Ekle", Icons.Rounded.Add, Modifier.width(94.dp)) { showAddLeave = true }
                        }
                    }
                    if (ownLeaves.isEmpty()) {
                        item { V12EmptyState("Kayıtlı izin bulunmuyor", Icons.Rounded.EventAvailable) }
                    } else {
                        items(ownLeaves, key = { it.id }) { leave ->
                            V12Card {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${leave.startDate} → ${leave.endDate}", color = RefText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        val timeLabel = if (leave.startTime == null) "Tam gün" else "${leave.startTime} - ${leave.endTime}"
                                        Text("$timeLabel • ${leave.reason.ifBlank { "İzin" }}", color = RefTextMuted, fontSize = 9.sp)
                                    }
                                    IconButton(onClick = { leaveToDelete = leave }) {
                                        Icon(Icons.Rounded.DeleteOutline, "İzni sil", tint = RefDanger, modifier = Modifier.size(19.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { onSaveStaff(record.copy(name = name.trim().ifBlank { record.name }, title = title.trim().ifBlank { "Personel" }, phone = phone.trim(), active = active)) },
                    modifier = Modifier.fillMaxWidth().padding(14.dp).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(7.dp)); Text("Personeli Kaydet", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showAddLeave) {
        V12AddLeaveDialog(record.id, onDismiss = { showAddLeave = false }) {
            onAddLeave(it)
            showAddLeave = false
        }
    }

    leaveToDelete?.let { leave ->
        AlertDialog(
            onDismissRequest = { leaveToDelete = null },
            containerColor = RefSurface,
            title = { Text("İzin kaydını sil?", color = RefText) },
            text = { Text("${leave.startDate} tarihli izin kaydı kaldırılacak.", color = RefTextMuted) },
            confirmButton = { TextButton(onClick = { onDeleteLeave(leave); leaveToDelete = null }) { Text("Sil", color = RefDanger) } },
            dismissButton = { TextButton(onClick = { leaveToDelete = null }) { Text("Vazgeç", color = RefCyan) } }
        )
    }
}

@Composable
private fun V12AddLeaveDialog(staffId: String, onDismiss: () -> Unit, onSave: (StaffLeave) -> Unit) {
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var startTime by rememberSaveable { mutableStateOf("") }
    var endTime by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RefSurface,
        title = { Text("İzin Ekle", color = RefText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                V12Input(startDate, { startDate = it }, "Başlangıç YYYY-AA-GG", Icons.Rounded.Event)
                V12Input(endDate, { endDate = it }, "Bitiş YYYY-AA-GG", Icons.Rounded.Event)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { V12Input(startTime, { startTime = it }, "Başlangıç saati") }
                    Box(Modifier.weight(1f)) { V12Input(endTime, { endTime = it }, "Bitiş saati") }
                }
                Text("Saatleri boş bırakırsanız izin tam gün olarak kaydedilir.", color = RefTextMuted, fontSize = 9.sp)
                V12Input(reason, { reason = it }, "Açıklama", Icons.Rounded.Description)
                error?.let { Text(it, color = RefDanger, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val leave = StaffLeave(
                    id = "leave-${System.currentTimeMillis()}",
                    staffId = staffId,
                    startDate = startDate.trim(),
                    endDate = endDate.trim(),
                    startTime = startTime.trim().takeIf { it.isNotBlank() },
                    endTime = endTime.trim().takeIf { it.isNotBlank() },
                    reason = reason.trim(),
                    createdAt = System.currentTimeMillis()
                )
                if (!StaffLeaveEngine.isValid(leave)) error = "İzin tarih/saat aralığını kontrol edin." else onSave(leave)
            }) { Text("Kaydet", color = RefCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = RefTextMuted) } }
    )
}

@Composable
internal fun V12NotificationCenterDialog(
    enabled: Boolean,
    permissionGranted: Boolean,
    rows: List<NotificationCenterRow>,
    onToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.80f),
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bildirim Merkezi", color = RefText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Randevu hatırlatmalarını yönetin", color = RefTextMuted, fontSize = 9.sp)
                    }
                    V12IconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                }
                Spacer(Modifier.height(12.dp))
                V12Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Randevu hatırlatmaları", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("24 saat ve 2 saat önce", color = RefTextMuted, fontSize = 9.sp)
                        }
                        Switch(checked = enabled, onCheckedChange = onToggle)
                    }
                    Spacer(Modifier.height(8.dp))
                    V12MiniPill(if (permissionGranted) "Sistem bildirimi açık" else "Sistem izni kapalı", if (permissionGranted) RefSuccess else RefWarning)
                    if (!permissionGranted) {
                        Spacer(Modifier.height(9.dp))
                        V12OutlineButton("Bildirim İzni Ver", Icons.Rounded.NotificationsActive, onClick = onRequestPermission)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Yaklaşan Hatırlatmalar", color = RefText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (rows.isEmpty()) {
                        item { V12EmptyState("Yaklaşan hatırlatma bulunmuyor", Icons.Rounded.NotificationsNone) }
                    } else {
                        items(rows, key = { "${it.appointmentId}-${it.timingLabel}" }) { row ->
                            V12Card {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Alarm, null, tint = RefCyan, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("${row.customer} • ${row.service}", color = RefText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("${row.appointmentDate} ${row.appointmentTime} • ${row.staff}", color = RefTextMuted, fontSize = 9.sp)
                                    }
                                    V12MiniPill(row.timingLabel, RefBlue)
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
internal fun V12InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RefSurface,
        title = { Text(title, color = RefText) },
        text = { Text(body, color = RefTextMuted, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam", color = RefCyan) } }
    )
}
