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
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import java.time.LocalDate

@Composable
internal fun V13AppointmentDialog(
    existing: Appointment?,
    prefillCustomer: String,
    prefillPhone: String,
    serviceRecords: List<ServiceRecord>,
    staffRecords: List<StaffRecord>,
    leaves: List<StaffLeave>,
    appointments: List<Appointment>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit,
    onDelete: ((String) -> Unit)?
) {
    val activeServices = V13BookingCatalog.activeServices(serviceRecords)
    val activeStaff = V13BookingCatalog.activeStaff(staffRecords)
    val key = existing?.id ?: "new-$prefillCustomer-$prefillPhone"
    var customer by rememberSaveable(key) { mutableStateOf(existing?.customer ?: prefillCustomer) }
    var phone by rememberSaveable(key) { mutableStateOf(existing?.phone ?: prefillPhone) }
    var service by rememberSaveable(key) { mutableStateOf(existing?.service ?: activeServices.firstOrNull()?.name.orEmpty()) }
    var staffName by rememberSaveable(key) { mutableStateOf(existing?.staff ?: activeStaff.firstOrNull()?.name.orEmpty()) }
    var date by rememberSaveable(key) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var time by rememberSaveable(key) { mutableStateOf(existing?.time ?: "10:00") }
    var note by rememberSaveable(key) { mutableStateOf(existing?.note.orEmpty()) }
    var statusName by rememberSaveable(key) { mutableStateOf((existing?.status ?: AppointmentStatus.CONFIRMED).name) }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(key) { mutableStateOf(false) }
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
                        Text(if (existing == null) "İşletme kataloğundan seçim yap" else "Düzenle, durum değiştir veya sil", color = RefTextMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.65f))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("Hizmet", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    if (activeServices.isEmpty()) {
                        item { V12EmptyState("Aktif hizmet yok. Daha Fazla > Hizmetler bölümünden ekleyin.", Icons.Rounded.DesignServices) }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(activeServices, key = { it.id }) { option -> V12SelectPill(option.name, service == option.name) { service = option.name } }
                            }
                        }
                    }
                    item { V12Input(customer, { customer = it }, "Müşteri adı", Icons.Rounded.Person) }
                    item { V12Input(phone, { phone = it }, "Telefon", Icons.Rounded.Phone) }
                    item { Text("Personel", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    if (activeStaff.isEmpty()) {
                        item { V12EmptyState("Aktif personel yok. Personel ekranından ekleyin.", Icons.Rounded.PersonAdd) }
                    } else {
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
                                items(AppointmentStatus.entries) { status -> V12SelectPill(status.label, statusName == status.name) { statusName = status.name } }
                            }
                        }
                    }
                    error?.let { message -> item { Text(message, color = RefDanger, fontSize = 10.sp) } }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            val catalogError = V13BookingCatalog.validationMessage(serviceRecords, staffRecords)
                            val basicError = when {
                                catalogError != null -> catalogError
                                customer.trim().length < 2 -> "Müşteri adını kontrol edin."
                                phone.filter(Char::isDigit).length < 10 -> "Telefon numarasını kontrol edin."
                                service.isBlank() -> "Aktif bir hizmet seçin."
                                staffName.isBlank() -> "Aktif bir personel seçin."
                                else -> ReferenceBookingRules.validationError(selectedStaff, leaves, date, time, appointments, existing?.id)
                            }
                            if (basicError != null) error = basicError else onSave(
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
