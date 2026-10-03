package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.BusinessSetupEngine
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.staff.StaffRecord
import java.time.LocalTime
import java.time.ZoneId

@Composable
internal fun V13BusinessSetupFlow(
    profile: BusinessProfile,
    services: List<ServiceRecord>,
    staff: List<StaffRecord>,
    initialStep: Int,
    reminderEnabled: Boolean,
    onProfileChanged: (BusinessProfile) -> Unit,
    onServicesChanged: (List<ServiceRecord>) -> Unit,
    onStaffChanged: (List<StaffRecord>) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onStepChanged: (Int) -> Unit,
    onComplete: (BusinessProfile) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(0, 4)) }
    var businessName by rememberSaveable { mutableStateOf(profile.businessName) }
    var ownerName by rememberSaveable { mutableStateOf(profile.ownerName) }
    var phone by rememberSaveable { mutableStateOf(profile.phone) }
    var address by rememberSaveable { mutableStateOf(profile.address) }
    var timezone by rememberSaveable { mutableStateOf(profile.timezoneId.ifBlank { ZoneId.systemDefault().id }) }
    var opening by rememberSaveable { mutableStateOf(profile.openingTime) }
    var closing by rememberSaveable { mutableStateOf(profile.closingTime) }
    var serviceName by rememberSaveable { mutableStateOf("") }
    var serviceDuration by rememberSaveable { mutableStateOf("30") }
    var staffName by rememberSaveable { mutableStateOf("") }
    var staffTitle by rememberSaveable { mutableStateOf("Personel") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val draft = BusinessProfile(
        businessName = businessName.trim(),
        phone = phone.trim(),
        address = address.trim(),
        timezoneId = BusinessSetupEngine.validatedTimezone(timezone, ZoneId.systemDefault().id),
        ownerName = ownerName.trim(),
        setupCompleted = false,
        openingTime = opening,
        closingTime = closing
    )

    Scaffold(containerColor = RefBackground, contentColor = RefText) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().height(ReferenceDesignContract.headerHeightDp.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("İşletmeni Kur", color = RefText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${step + 1}/5 • Veriler yalnızca sen ekledikçe oluşur", color = RefTextMuted, fontSize = 10.sp)
                }
                V12MiniPill("v1.3", RefCyan)
            }
            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = RefCyan,
                trackColor = RefSurfaceRaised
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                when (step) {
                    0 -> {
                        item { SetupTitle("İşletme Bilgileri", "Hazır işletme yok. Kendi işletme bilgilerini gir.", Icons.Rounded.Business) }
                        item { V12Input(businessName, { businessName = it }, "İşletme adı", Icons.Rounded.Storefront) }
                        item { V12Input(ownerName, { ownerName = it }, "Yetkili / işletme sahibi", Icons.Rounded.Person) }
                        item { V12Input(phone, { phone = it }, "Telefon", Icons.Rounded.Phone) }
                        item { V12Input(address, { address = it }, "Adres", Icons.Rounded.LocationOn, singleLine = false) }
                    }
                    1 -> {
                        item { SetupTitle("Çalışma Düzeni", "Saat dilimi ve temel çalışma saatleri", Icons.Rounded.Schedule) }
                        item { V12Input(timezone, { timezone = it }, "Saat dilimi", Icons.Rounded.Public) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) { V12Input(opening, { opening = it }, "Açılış", Icons.Rounded.Login) }
                                Box(Modifier.weight(1f)) { V12Input(closing, { closing = it }, "Kapanış", Icons.Rounded.Logout) }
                            }
                        }
                        item { SetupHint("Örnek saat dilimi: Europe/Istanbul. Ayrıntılı çalışma istisnaları daha sonra düzenlenebilir.") }
                    }
                    2 -> {
                        item { SetupTitle("Hizmetler", "Randevularda yalnızca burada eklediğin hizmetler görünür.", Icons.Rounded.DesignServices) }
                        if (services.isEmpty()) item { V12EmptyState("Henüz hizmet eklenmedi", Icons.Rounded.AddBusiness) }
                        items(services, key = { it.id }) { service ->
                            V12Card {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(service.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${service.durationMinutes} dakika", color = RefTextMuted, fontSize = 9.sp)
                                    }
                                    Switch(checked = service.active, onCheckedChange = { enabled ->
                                        onServicesChanged(services.map { if (it.id == service.id) it.copy(active = enabled) else it })
                                    })
                                    IconButton(onClick = { onServicesChanged(services.filterNot { it.id == service.id }) }) {
                                        Icon(Icons.Rounded.DeleteOutline, "Sil", tint = RefDanger)
                                    }
                                }
                            }
                        }
                        item { V12Input(serviceName, { serviceName = it }, "Yeni hizmet adı", Icons.Rounded.DesignServices) }
                        item { V12Input(serviceDuration, { serviceDuration = it.filter(Char::isDigit) }, "Süre (dakika)", Icons.Rounded.Timer) }
                        item {
                            V12OutlineButton("Hizmet Ekle", Icons.Rounded.Add) {
                                val duration = serviceDuration.toIntOrNull() ?: 0
                                if (serviceName.trim().length < 2 || duration !in 5..720) {
                                    error = "Hizmet adı ve süreyi kontrol edin."
                                } else {
                                    onServicesChanged(services + ServiceRecord("service-${System.currentTimeMillis()}", serviceName.trim(), duration, true))
                                    serviceName = ""
                                    serviceDuration = "30"
                                    error = null
                                }
                            }
                        }
                    }
                    3 -> {
                        item { SetupTitle("Personel", "İstersen şimdi ekle; bu adım zorunlu değil.", Icons.Rounded.Groups) }
                        if (staff.isEmpty()) item { V12EmptyState("Personel daha sonra da eklenebilir", Icons.Rounded.PersonAdd) }
                        items(staff, key = { it.id }) { person ->
                            V12Card {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    V12Avatar(person.name, 38)
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(person.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(person.title, color = RefTextMuted, fontSize = 9.sp)
                                    }
                                    IconButton(onClick = { onStaffChanged(staff.filterNot { it.id == person.id }) }) {
                                        Icon(Icons.Rounded.DeleteOutline, "Sil", tint = RefDanger)
                                    }
                                }
                            }
                        }
                        item { V12Input(staffName, { staffName = it }, "Personel adı", Icons.Rounded.Person) }
                        item { V12Input(staffTitle, { staffTitle = it }, "Görev", Icons.Rounded.Badge) }
                        item {
                            V12OutlineButton("Personel Ekle", Icons.Rounded.PersonAdd) {
                                if (staffName.trim().length < 2) error = "Personel adını kontrol edin."
                                else {
                                    onStaffChanged(staff + StaffRecord("staff-${System.currentTimeMillis()}", staffName.trim(), staffTitle.trim().ifBlank { "Personel" }, "", true))
                                    staffName = ""
                                    staffTitle = "Personel"
                                    error = null
                                }
                            }
                        }
                    }
                    else -> {
                        item { SetupTitle("Bildirimler", "Yerel randevu hatırlatmalarını seç. WhatsApp bağlantısı daha sonra kurulabilir.", Icons.Rounded.NotificationsActive) }
                        item {
                            V12Card {
                                V12SettingsToggleRow(Icons.Rounded.Schedule, "Randevu hatırlatmaları", reminderEnabled, onReminderChanged)
                            }
                        }
                        item { SetupHint("WhatsApp Business Cloud API ayarları Daha Fazla > WhatsApp Hatırlatmaları bölümünden yapılır. Meta erişim anahtarı uygulamaya kaydedilmez.") }
                    }
                }
                error?.let { item { Text(it, color = RefDanger, fontSize = 10.sp) } }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { error = null; step -= 1; onStepChanged(step) },
                        modifier = Modifier.weight(0.65f).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RefBorder)
                    ) { Text("Geri", color = RefTextMuted) }
                }
                Button(
                    onClick = {
                        error = when (step) {
                            0 -> if (businessName.trim().length < 2) "İşletme adını girin." else null
                            1 -> if (!BusinessSetupEngine.isValidTimezone(timezone) || runCatching { LocalTime.parse(opening) }.isFailure || runCatching { LocalTime.parse(closing) }.isFailure) "Saat dilimi ve çalışma saatlerini kontrol edin." else null
                            2 -> if (BusinessSetupEngine.activeServices(services).isEmpty()) "Devam etmek için en az bir aktif hizmet ekleyin." else null
                            else -> null
                        }
                        if (error != null) return@Button
                        onProfileChanged(draft)
                        if (step < 4) {
                            step += 1
                            onStepChanged(step)
                        } else {
                            val finalProfile = draft.copy(setupCompleted = true)
                            if (BusinessSetupEngine.canComplete(finalProfile, services)) onComplete(finalProfile)
                            else error = "İşletme adı ve aktif hizmet gereklidir."
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                ) {
                    Text(if (step == 4) "Kurulumu Tamamla" else "Devam", fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(if (step == 4) Icons.Rounded.Check else Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SetupTitle(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    V12Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RefCyan, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = RefText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(subtitle, color = RefTextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SetupHint(text: String) {
    Surface(color = RefSurfaceRaised.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
        Text(text, color = RefTextMuted, fontSize = 10.sp, modifier = Modifier.padding(12.dp))
    }
}
