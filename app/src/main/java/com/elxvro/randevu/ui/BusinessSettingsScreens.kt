package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.BusinessSetupEngine
import com.elxvro.randevu.business.ServiceRecord
import java.time.LocalTime

@Composable
internal fun V13BusinessSettingsDialog(profile: BusinessProfile, onDismiss: () -> Unit, onSave: (BusinessProfile) -> Unit) {
    var name by rememberSaveable(profile.businessName) { mutableStateOf(profile.businessName) }
    var owner by rememberSaveable(profile.ownerName) { mutableStateOf(profile.ownerName) }
    var phone by rememberSaveable(profile.phone) { mutableStateOf(profile.phone) }
    var address by rememberSaveable(profile.address) { mutableStateOf(profile.address) }
    var timezone by rememberSaveable(profile.timezoneId) { mutableStateOf(profile.timezoneId) }
    var opening by rememberSaveable(profile.openingTime) { mutableStateOf(profile.openingTime) }
    var closing by rememberSaveable(profile.closingTime) { mutableStateOf(profile.closingTime) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.86f),
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V12IconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("İşletme Bilgileri", color = RefText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Kurulumdan sonra da düzenlenebilir", color = RefTextMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.6f))
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    item { V12Input(name, { name = it }, "İşletme adı", Icons.Rounded.Storefront) }
                    item { V12Input(owner, { owner = it }, "Yetkili", Icons.Rounded.Person) }
                    item { V12Input(phone, { phone = it }, "Telefon", Icons.Rounded.Phone) }
                    item { V12Input(address, { address = it }, "Adres", Icons.Rounded.LocationOn, singleLine = false) }
                    item { V12Input(timezone, { timezone = it }, "Saat dilimi", Icons.Rounded.Public) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { V12Input(opening, { opening = it }, "Açılış", Icons.Rounded.Schedule) }
                            Box(Modifier.weight(1f)) { V12Input(closing, { closing = it }, "Kapanış", Icons.Rounded.Schedule) }
                        }
                    }
                    error?.let { item { Text(it, color = RefDanger, fontSize = 10.sp) } }
                }
                Button(
                    onClick = {
                        error = when {
                            name.trim().length < 2 -> "İşletme adını kontrol edin."
                            !BusinessSetupEngine.isValidTimezone(timezone) -> "Saat dilimini kontrol edin."
                            runCatching { LocalTime.parse(opening) }.isFailure || runCatching { LocalTime.parse(closing) }.isFailure -> "Çalışma saatlerini SS:DD biçiminde girin."
                            else -> null
                        }
                        if (error == null) onSave(profile.copy(
                            businessName = name.trim(), ownerName = owner.trim(), phone = phone.trim(), address = address.trim(),
                            timezoneId = timezone.trim(), openingTime = opening, closingTime = closing, setupCompleted = true
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().padding(14.dp).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(7.dp)); Text("Kaydet", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
internal fun V13ServiceSettingsDialog(services: List<ServiceRecord>, onDismiss: () -> Unit, onSave: (List<ServiceRecord>) -> Unit) {
    var current by remember(services) { mutableStateOf(services) }
    var name by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("30") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f),
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V12IconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Hizmetler", color = RefText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Randevu ekranındaki hizmet kataloğu", color = RefTextMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.6f))
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (current.isEmpty()) item { V12EmptyState("Henüz hizmet yok", Icons.Rounded.DesignServices) }
                    items(current, key = { it.id }) { service ->
                        V12Card {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(service.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${service.durationMinutes} dakika", color = RefTextMuted, fontSize = 9.sp)
                                }
                                Switch(checked = service.active, onCheckedChange = { active -> current = current.map { if (it.id == service.id) it.copy(active = active) else it } })
                                IconButton(onClick = { current = current.filterNot { it.id == service.id } }) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = RefDanger) }
                            }
                        }
                    }
                    item { V12Input(name, { name = it }, "Yeni hizmet", Icons.Rounded.DesignServices) }
                    item { V12Input(duration, { duration = it.filter(Char::isDigit) }, "Süre (dakika)", Icons.Rounded.Timer) }
                    item {
                        V12OutlineButton("Hizmet Ekle", Icons.Rounded.Add) {
                            val minutes = duration.toIntOrNull() ?: 0
                            if (name.trim().length < 2 || minutes !in 5..720) error = "Hizmet adı ve süreyi kontrol edin."
                            else {
                                current = current + ServiceRecord("service-${System.currentTimeMillis()}", name.trim(), minutes, true)
                                name = ""; duration = "30"; error = null
                            }
                        }
                    }
                    error?.let { item { Text(it, color = RefDanger, fontSize = 10.sp) } }
                }
                Button(
                    onClick = {
                        if (BusinessSetupEngine.activeServices(current).isEmpty()) error = "En az bir aktif hizmet kalmalıdır."
                        else onSave(current)
                    },
                    modifier = Modifier.fillMaxWidth().padding(14.dp).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(7.dp)); Text("Hizmetleri Kaydet", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
internal fun V13ResetDialog(onDismiss: () -> Unit, onReset: (Boolean) -> Unit) {
    var destructiveConfirm by rememberSaveable { mutableStateOf(false) }
    if (!destructiveConfirm) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = RefSurface,
            title = { Text("İşletme kurulumunu sıfırla", color = RefText) },
            text = { Text("Sadece işletme kurulumu ve hizmetleri sıfırlayabilir veya tüm randevu/personel/izin verilerini de silebilirsin.", color = RefTextMuted) },
            confirmButton = { TextButton(onClick = { onReset(false) }) { Text("Sadece Kurulum", color = RefCyan) } },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismiss) { Text("Vazgeç", color = RefTextMuted) }
                    TextButton(onClick = { destructiveConfirm = true }) { Text("Tüm Verileri Sil", color = RefDanger) }
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { destructiveConfirm = false },
            containerColor = RefSurface,
            title = { Text("Tüm yerel verileri sil?", color = RefText) },
            text = { Text("İşletme, hizmetler, randevular, personel ve izin kayıtları bu cihazdan kalıcı olarak silinecek. Bu ikinci onay geri alınamaz.", color = RefTextMuted) },
            confirmButton = { TextButton(onClick = { onReset(true) }) { Text("Evet, Tümünü Sil", color = RefDanger, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { destructiveConfirm = false }) { Text("Geri", color = RefCyan) } }
        )
    }
}
