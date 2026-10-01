package com.elxvro.randevu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.core.Branch
import com.elxvro.randevu.core.BreakWindow
import com.elxvro.randevu.core.BusinessAction
import com.elxvro.randevu.core.BusinessEngine
import com.elxvro.randevu.core.BusinessProfile
import com.elxvro.randevu.core.BusinessState
import com.elxvro.randevu.core.ClosedDay
import com.elxvro.randevu.core.ServiceConfig
import com.elxvro.randevu.core.StaffMember
import com.elxvro.randevu.core.defaultBusinessState

@Composable
fun RandevuV3App() {
    var businessMode by rememberSaveable { mutableStateOf(false) }
    var businessState by remember { mutableStateOf(defaultBusinessState()) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = BrandBlue)
                Text("Randevu", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (businessMode) {
                    OutlinedButton(onClick = { businessMode = false }) { Text("Müşteri") }
                    FilledTonalButton(onClick = {}) { Text("İşletme") }
                } else {
                    FilledTonalButton(onClick = {}) { Text("Müşteri") }
                    OutlinedButton(onClick = { businessMode = true }) { Text("İşletme") }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            if (businessMode) {
                BusinessManager(state = businessState, onState = { businessState = it })
            } else {
                RandevuApp()
            }
        }
    }
}

private enum class BizTab(val label: String, val icon: ImageVector) {
    Panel("Panel", Icons.Rounded.Home),
    Services("Hizmet", Icons.Rounded.Work),
    Staff("Personel", Icons.Rounded.People),
    Schedule("Saatler", Icons.Rounded.Schedule),
    Settings("Daha", Icons.Rounded.Settings)
}

@Composable
private fun BusinessManager(state: BusinessState, onState: (BusinessState) -> Unit) {
    var tabName by rememberSaveable { mutableStateOf(BizTab.Panel.name) }
    val tab = BizTab.valueOf(tabName)
    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                BizTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tabName = item.name },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                BizTab.Panel -> PanelTab(state)
                BizTab.Services -> ServiceTab(state, onState)
                BizTab.Staff -> StaffTab(state, onState)
                BizTab.Schedule -> ScheduleTab(state, onState)
                BizTab.Settings -> SettingsTab(state, onState)
            }
        }
    }
}

@Composable
private fun PanelTab(state: BusinessState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("İşletme Yönetimi", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(state.profile.name, color = TextSecondary)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Hizmet", state.services.count { it.active }, Modifier.weight(1f))
                StatCard("Personel", state.staff.count { it.active }, Modifier.weight(1f))
                StatCard("Şube", state.branches.count { it.active }, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Aktif kontroller", fontWeight = FontWeight.Bold)
                    Text("• Randevu çakışma kontrolü", color = TextSecondary)
                    Text("• Çalışma saati ve mola kontrolü", color = TextSecondary)
                    Text("• Kapalı gün kontrolü", color = TextSecondary)
                    Text("• Personel ve şube uygunluğu", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ServiceTab(state: BusinessState, onState: (BusinessState) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("30") }
    var price by rememberSaveable { mutableStateOf("250") }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Title("Hizmet Yönetimi") }
        item {
            Form {
                OutlinedTextField(name, { name = it }, label = { Text("Hizmet adı") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Dakika") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Fiyat ₺") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val d = duration.toIntOrNull()
                    val p = price.toIntOrNull()
                    if (name.isNotBlank() && d != null && d > 0 && p != null) {
                        onState(BusinessEngine.reduce(state, BusinessAction.UpsertService(ServiceConfig("service-${System.nanoTime()}", name.trim(), d, p))))
                        name = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.size(6.dp)); Text("Hizmet Ekle")
                }
            }
        }
        items(state.services, key = { it.id }) { service ->
            ManageCard(
                title = service.name,
                subtitle = "${service.durationMinutes} dk • ${service.price} ₺",
                active = service.active,
                onToggle = { onState(BusinessEngine.reduce(state, BusinessAction.ToggleService(service.id))) },
                onDelete = { onState(BusinessEngine.reduce(state, BusinessAction.DeleteService(service.id))) }
            )
        }
    }
}

@Composable
private fun StaffTab(state: BusinessState, onState: (BusinessState) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Title("Personel Yönetimi") }
        item {
            Form {
                OutlinedTextField(name, { name = it }, label = { Text("Ad soyad") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(title, { title = it }, label = { Text("Uzmanlık") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val branchId = state.branches.firstOrNull { it.active }?.id ?: "main"
                        val member = StaffMember(
                            id = "staff-${System.nanoTime()}",
                            name = name.trim(),
                            title = title.ifBlank { "Personel" }.trim(),
                            serviceIds = state.services.filter { it.active }.map { it.id }.toSet(),
                            branchId = branchId
                        )
                        onState(BusinessEngine.reduce(state, BusinessAction.UpsertStaff(member)))
                        name = ""; title = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Personel Ekle") }
            }
        }
        items(state.staff, key = { it.id }) { member ->
            val branch = state.branches.firstOrNull { it.id == member.branchId }?.name ?: "Şube yok"
            ManageCard(
                title = member.name,
                subtitle = "${member.title} • $branch • ${member.serviceIds.size} hizmet",
                active = member.active,
                onToggle = { onState(BusinessEngine.reduce(state, BusinessAction.ToggleStaff(member.id))) },
                onDelete = { onState(BusinessEngine.reduce(state, BusinessAction.DeleteStaff(member.id))) }
            )
        }
    }
}

@Composable
private fun ScheduleTab(state: BusinessState, onState: (BusinessState) -> Unit) {
    var closedDate by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    val names = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Title("Çalışma Saatleri") }
        items(state.workingDays.sortedBy { it.dayOfWeek }, key = { it.dayOfWeek }) { day ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(names[day.dayOfWeek - 1], fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(day.enabled, { onState(BusinessEngine.reduce(state, BusinessAction.UpsertWorkingDay(day.copy(enabled = it)))) })
                    }
                    if (day.enabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val next = if (day.open == "09:00") "10:00" else "09:00"
                                onState(BusinessEngine.reduce(state, BusinessAction.UpsertWorkingDay(day.copy(open = next))))
                            }, modifier = Modifier.weight(1f)) { Text("Açılış ${day.open}") }
                            OutlinedButton(onClick = {
                                val next = if (day.close == "19:00") "20:00" else "19:00"
                                onState(BusinessEngine.reduce(state, BusinessAction.UpsertWorkingDay(day.copy(close = next))))
                            }, modifier = Modifier.weight(1f)) { Text("Kapanış ${day.close}") }
                        }
                        FilledTonalButton(onClick = {
                            val breaks = if (day.breaks.isEmpty()) listOf(BreakWindow("13:00", "13:30")) else emptyList()
                            onState(BusinessEngine.reduce(state, BusinessAction.UpsertWorkingDay(day.copy(breaks = breaks))))
                        }) { Text(if (day.breaks.isEmpty()) "13:00 mola ekle" else "Molayı kaldır") }
                    }
                }
            }
        }
        item { Title("Kapalı Gün") }
        item {
            Form {
                OutlinedTextField(closedDate, { closedDate = it }, label = { Text("YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(closedDate)) {
                        onState(BusinessEngine.reduce(state, BusinessAction.AddClosedDay(ClosedDay(closedDate, reason.ifBlank { "Kapalı" }))))
                        closedDate = ""; reason = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Kapalı Gün Ekle") }
            }
        }
        items(state.closedDays, key = { it.date }) { day ->
            RowCard(day.date, day.reason) {
                onState(BusinessEngine.reduce(state, BusinessAction.RemoveClosedDay(day.date)))
            }
        }
    }
}

@Composable
private fun SettingsTab(state: BusinessState, onState: (BusinessState) -> Unit) {
    var businessName by rememberSaveable { mutableStateOf(state.profile.name) }
    var phone by rememberSaveable { mutableStateOf(state.profile.phone) }
    var address by rememberSaveable { mutableStateOf(state.profile.address) }
    var description by rememberSaveable { mutableStateOf(state.profile.description) }
    var branchName by rememberSaveable { mutableStateOf("") }
    var branchAddress by rememberSaveable { mutableStateOf("") }
    var branchPhone by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Title("İşletme Bilgileri") }
        item {
            Form {
                OutlinedTextField(businessName, { businessName = it }, label = { Text("İşletme adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Adres") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = {
                    onState(BusinessEngine.reduce(state, BusinessAction.UpdateProfile(BusinessProfile(businessName, phone, address, description))))
                }, modifier = Modifier.fillMaxWidth()) { Text("Bilgileri Kaydet") }
            }
        }
        item { Title("Şubeler") }
        item {
            Form {
                OutlinedTextField(branchName, { branchName = it }, label = { Text("Şube adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(branchAddress, { branchAddress = it }, label = { Text("Adres") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(branchPhone, { branchPhone = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (branchName.isNotBlank()) {
                        onState(BusinessEngine.reduce(state, BusinessAction.UpsertBranch(Branch("branch-${System.nanoTime()}", branchName, branchAddress, branchPhone))))
                        branchName = ""; branchAddress = ""; branchPhone = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Şube Ekle") }
            }
        }
        items(state.branches, key = { it.id }) { branch ->
            ManageCard(
                title = branch.name,
                subtitle = "${branch.address} • ${branch.phone}",
                active = branch.active,
                onToggle = { onState(BusinessEngine.reduce(state, BusinessAction.ToggleBranch(branch.id))) },
                onDelete = {
                    if (state.branches.size > 1) onState(BusinessEngine.reduce(state, BusinessAction.DeleteBranch(branch.id)))
                }
            )
        }
    }
}

@Composable
private fun ManageCard(title: String, subtitle: String, active: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Switch(active, { onToggle() })
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = DangerRed) }
        }
    }
}

@Composable
private fun RowCard(title: String, subtitle: String, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = TextSecondary) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = DangerRed) }
        }
    }
}

@Composable
private fun Form(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { content() }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
}
