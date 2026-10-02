package com.elxvro.randevu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentAction
import com.elxvro.randevu.core.AppointmentEngine
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.storage.LiveSyncStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RandevuReferenceApp() {
    val context = LocalContext.current
    val store = remember { LiveSyncStore(context.applicationContext) }
    val initial = remember { store.loadAppointments().ifEmpty { referenceSeedAppointments() } }
    val appointments = remember { mutableStateListOf<Appointment>().apply { addAll(initial) } }
    var selectedTabName by rememberSaveable { mutableStateOf(ReferenceTab.HOME.name) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val selectedTab = runCatching { ReferenceTab.valueOf(selectedTabName) }.getOrDefault(ReferenceTab.HOME)

    fun dispatch(action: AppointmentAction) {
        val next = AppointmentEngine.reduce(appointments.toList(), action)
        appointments.clear()
        appointments.addAll(next)
        store.saveAppointments(next)
    }

    LaunchedEffect(Unit) {
        if (store.loadAppointments().isEmpty()) store.saveAppointments(initial)
    }

    ReferenceRandevuTheme {
        Scaffold(
            containerColor = RefBackground,
            contentColor = RefText,
            bottomBar = {
                ReferenceBottomNavigation(
                    selected = selectedTab,
                    onSelect = { selectedTabName = it.name }
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    ReferenceTab.HOME -> HomeScreen(
                        appointments = appointments,
                        onCreate = { showCreate = true }
                    )
                    ReferenceTab.CALENDAR -> CalendarScreen(
                        appointments = appointments,
                        onCreate = { showCreate = true }
                    )
                    ReferenceTab.CUSTOMERS -> CustomersScreen(
                        appointments = appointments,
                        onCreate = { showCreate = true }
                    )
                    ReferenceTab.STAFF -> StaffScreen(appointments)
                    ReferenceTab.MORE -> MoreScreen()
                }
            }
        }

        if (showCreate) {
            CreateAppointmentDialog(
                onDismiss = { showCreate = false },
                onCreate = {
                    dispatch(AppointmentAction.Add(it))
                    showCreate = false
                }
            )
        }
    }
}

@Composable
private fun ReferenceBottomNavigation(selected: ReferenceTab, onSelect: (ReferenceTab) -> Unit) {
    Surface(
        color = Color(0xFF071923),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ReferenceDesignContract.bottomNavHeightDp.dp)
                .navigationBarsPadding()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReferenceDesignContract.bottomTabs.forEach { tab ->
                val active = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(tab) }
                        .padding(top = 9.dp, bottom = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.label,
                        modifier = Modifier.size(21.dp),
                        tint = if (active) RefCyan else RefTextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        color = if (active) RefCyan else RefTextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun ReferenceTab.icon(): ImageVector = when (this) {
    ReferenceTab.HOME -> Icons.Rounded.Home
    ReferenceTab.CALENDAR -> Icons.Rounded.CalendarMonth
    ReferenceTab.CUSTOMERS -> Icons.Rounded.Groups
    ReferenceTab.STAFF -> Icons.Rounded.Badge
    ReferenceTab.MORE -> Icons.Rounded.Menu
}

@Composable
private fun HomeScreen(appointments: List<Appointment>, onCreate: () -> Unit) {
    val today = LocalDate.now().toString()
    val todayItems = appointments.filter { it.date == today }.sortedBy { it.time }
    val metrics = ReferenceAppModel.metrics(appointments)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReferenceDesignContract.pageInsetDp.dp,
            end = ReferenceDesignContract.pageInsetDp.dp,
            top = 10.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(ReferenceDesignContract.cardGapDp.dp)
    ) {
        item { HomeHeader() }
        item { HeroTodayCard(todayItems.size) }
        item { MetricsRow(metrics) }
        item {
            SectionTitle("Bugünkü Randevular", "Tümünü Gör")
        }
        if (todayItems.isEmpty()) {
            item { EmptyState("Bugün için randevu bulunmuyor", Icons.Rounded.EventAvailable) }
        } else {
            items(todayItems, key = { it.id }) { appointment ->
                AppointmentListCard(appointment)
            }
        }
        item {
            ReferenceOutlineAction(
                text = "Yeni Randevu Oluştur",
                icon = Icons.Rounded.Add,
                onClick = onCreate
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = RefSurfaceRaised, border = BorderStroke(1.dp, RefBorder)) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Text("S", color = RefText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Merhaba,", color = RefTextMuted, style = MaterialTheme.typography.bodySmall)
            Text("Selin Demir", color = RefText, style = MaterialTheme.typography.titleMedium)
        }
        ReferenceIconButton(Icons.Rounded.NotificationsNone, "Bildirimler") {}
    }
}

@Composable
private fun HeroTodayCard(todayCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(ReferenceDesignContract.cardRadiusDp.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A3750), Color(0xFF063A58), Color(0xFF071A26))
                )
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(RefBlue.copy(alpha = 0.25f), Color.Transparent, RefCyan.copy(alpha = 0.12f))
                    )
                )
        )
        Column(Modifier.padding(18.dp)) {
            Text("Bugünkü", color = RefTextMuted, style = MaterialTheme.typography.bodyMedium)
            Text("Randevular", color = RefText, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(todayCount.toString(), color = RefText, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                Text(" / 18", color = RefTextMuted, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(7.dp, RefCyan)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("%67", fontWeight = FontWeight.Bold, color = RefText)
                }
            }
            Spacer(Modifier.height(5.dp))
            Text("Günün verimliliği", color = RefTextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MetricsRow(metrics: ReferenceMetrics) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard("Toplam", metrics.total, RefBlue, Modifier.weight(1f))
        MetricCard("Tamamlandı", metrics.completed, RefSuccess, Modifier.weight(1f))
        MetricCard("Bekliyor", metrics.pending, RefWarning, Modifier.weight(1f))
        MetricCard("İptal", metrics.cancelled, RefDanger, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(14.dp),
        color = RefSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
            Text(value.toString(), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = RefTextMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = RefText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.let { Text(it, color = RefCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun AppointmentListCard(appointment: Appointment) {
    val accent = statusColor(appointment.status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignContract.controlRadiusDp.dp),
        color = RefSurface,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.8f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(10.dp))
            Text(appointment.time, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(48.dp))
            Avatar(appointment.customer, 34)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    appointment.service.ifBlank { "Randevu" },
                    color = RefText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    appointment.customer,
                    color = RefTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusChip(appointment.status)
        }
    }
}

@Composable
private fun CalendarScreen(appointments: List<Appointment>, onCreate: () -> Unit) {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var mode by rememberSaveable { mutableStateOf("Gün") }
    val selected = LocalDate.parse(selectedDate)
    val days = (-3L..3L).map { selected.plusDays(it) }
    val shown = when (mode) {
        "Hafta" -> {
            val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
            val end = start.plusDays(6)
            appointments.filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d -> !d.isBefore(start) && !d.isAfter(end) } == true }
        }
        "Ay" -> appointments.filter { it.date.startsWith(selected.toString().take(7)) }
        else -> appointments.filter { it.date == selectedDate }
    }.sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })

    Column(
        Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)
    ) {
        ReferenceHeader("Takvim", Icons.Rounded.CalendarMonth) { onCreate() }
        MonthSelector(selected)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(days, key = { it.toString() }) { date ->
                DayButton(date, date == selected) { selectedDate = date.toString() }
            }
        }
        Spacer(Modifier.height(12.dp))
        SegmentedControl(listOf("Gün", "Hafta", "Ay"), mode) { mode = it }
        Spacer(Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (shown.isEmpty()) {
                item { EmptyState("Bu aralıkta randevu yok", Icons.Rounded.CalendarToday) }
            } else {
                items(shown, key = { it.id }) { item -> CalendarAppointmentCard(item) }
            }
            item { ReferenceOutlineAction("Yeni Randevu Oluştur", Icons.Rounded.Add, onCreate) }
        }
    }
}

@Composable
private fun MonthSelector(date: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR")) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.ChevronLeft, null, tint = RefTextMuted, modifier = Modifier.size(18.dp))
        Text(
            date.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() },
            color = RefText,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Icon(Icons.Rounded.ChevronRight, null, tint = RefTextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DayButton(date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("tr", "TR")).take(3)
    Column(
        modifier = Modifier
            .width(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dayName, color = RefTextMuted, fontSize = 9.sp)
        Spacer(Modifier.height(5.dp))
        Surface(shape = CircleShape, color = if (selected) RefCyan else Color.Transparent) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Text(date.dayOfMonth.toString(), color = if (selected) RefBackground else RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RefSurface,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.7f))
    ) {
        Row(Modifier.fillMaxWidth().padding(3.dp)) {
            options.forEach { option ->
                val active = option == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) RefBlue else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option, color = if (active) Color.White else RefTextMuted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CalendarAppointmentCard(item: Appointment) {
    val accent = statusColor(item.status)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (item.status == AppointmentStatus.CONFIRMED) Color(0xFF073B35) else Color(0xFF0B2940),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.time, color = RefTextMuted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
            Avatar(item.customer, 34)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(item.service, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.customer, color = RefTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusChip(item.status)
        }
    }
}

@Composable
private fun CustomersScreen(appointments: List<Appointment>, onCreate: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Tümü") }
    val customers = ReferenceAppModel.customers(appointments).filter { customer ->
        val matchesQuery = query.isBlank() || customer.name.contains(query, true) || customer.phone.contains(query)
        val matchesState = when (filter) {
            "Aktif" -> customer.active
            "Pasif" -> !customer.active
            else -> true
        }
        matchesQuery && matchesState
    }

    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        ReferenceHeader("Müşteriler", Icons.Rounded.PersonAdd) { onCreate() }
        ReferenceSearchField(query, onValueChange = { query = it })
        Spacer(Modifier.height(10.dp))
        SegmentedControl(listOf("Tümü", "Aktif", "Pasif"), filter) { filter = it }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (customers.isEmpty()) {
                item { EmptyState("Müşteri bulunamadı", Icons.Rounded.PersonSearch) }
            } else {
                items(customers, key = { it.phone.ifBlank { it.name } }) { customer ->
                    CustomerCard(
                        customer = customer,
                        onCall = { openDialer(context, customer.phone) },
                        onMessage = { openSms(context, customer.phone) },
                        onAppointment = onCreate
                    )
                }
            }
            item { ReferenceOutlineAction("Yeni Müşteri Kaydı", Icons.Rounded.Add, onCreate) }
        }
    }
}

@Composable
private fun CustomerCard(customer: ReferenceCustomer, onCall: () -> Unit, onMessage: () -> Unit, onAppointment: () -> Unit) {
    ReferenceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(customer.name, 44)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(customer.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(customer.phone.ifBlank { "Telefon yok" }, color = RefTextMuted, fontSize = 10.sp)
                Text("${customer.appointmentCount} randevu", color = RefTextMuted, fontSize = 9.sp)
            }
            MiniStatePill(if (customer.active) "Aktif" else "Pasif", if (customer.active) RefSuccess else RefDisabled)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TinyAction("Ara", Icons.Rounded.Call, Modifier.weight(1f), onCall)
            TinyAction("Mesaj", Icons.Rounded.ChatBubbleOutline, Modifier.weight(1f), onMessage)
            TinyAction("Randevu", Icons.Rounded.CalendarMonth, Modifier.weight(1.2f), onAppointment)
        }
    }
}

@Composable
private fun StaffScreen(appointments: List<Appointment>) {
    val staff = ReferenceAppModel.staff(appointments)
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        ReferenceHeader("Personel", Icons.Rounded.Badge) {}
        Text("Ekibiniz ve randevu yoğunluğu", color = RefTextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (staff.isEmpty()) {
                item { EmptyState("Henüz personel kaydı yok", Icons.Rounded.Groups) }
            } else {
                items(staff, key = { it.name }) { person ->
                    ReferenceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(person.name, 44)
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(person.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Personel", color = RefTextMuted, fontSize = 10.sp)
                            }
                            MiniStatePill("Aktif", RefSuccess)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StaffMetric("Randevu", person.appointmentCount, Modifier.weight(1f))
                            StaffMetric("Tamamlanan", person.completedCount, Modifier.weight(1f))
                        }
                    }
                }
            }
            item { ReferenceOutlineAction("Yeni Personel", Icons.Rounded.PersonAdd) {} }
        }
    }
}

@Composable
private fun StaffMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(11.dp), color = RefSurfaceRaised.copy(alpha = 0.7f)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value.toString(), color = RefCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, color = RefTextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MoreScreen() {
    var reminders by rememberSaveable { mutableStateOf(true) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        ReferenceHeader("Daha Fazla", Icons.Rounded.Settings) {}
        ReferenceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar("Selin Demir", 48)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Selin Demir", color = RefText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("selin@randevu.com", color = RefTextMuted, fontSize = 10.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = RefTextMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { SettingsToggleRow(Icons.Rounded.NotificationsNone, "Bildirimler", notifications) { notifications = it } }
            item { SettingsToggleRow(Icons.Rounded.Schedule, "Randevu hatırlatmaları", reminders) { reminders = it } }
            item { SettingsRow(Icons.Rounded.Sync, "Veri ve senkronizasyon") }
            item { SettingsRow(Icons.Rounded.ContentCut, "Hizmetler") }
            item { SettingsRow(Icons.Rounded.Business, "İşletme Bilgileri") }
            item { SettingsRow(Icons.Rounded.Security, "Güvenlik") }
            item { SettingsRow(Icons.Rounded.HelpOutline, "Yardım & Destek") }
            item { SettingsRow(Icons.Rounded.Info, "Uygulama Hakkında", "v1.1.0") }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, value: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { }.padding(vertical = 13.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = RefCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = RefText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        value?.let { Text(it, color = RefTextMuted, fontSize = 10.sp) }
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = RefTextMuted, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RefCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = RefText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RefBackground,
                checkedTrackColor = RefCyan,
                uncheckedThumbColor = RefTextMuted,
                uncheckedTrackColor = RefSurfaceRaised,
                uncheckedBorderColor = RefBorder
            )
        )
    }
    HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
}

@Composable
private fun ReferenceHeader(title: String, actionIcon: ImageVector, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = RefText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        ReferenceIconButton(actionIcon, title, onAction)
    }
}

@Composable
private fun ReferenceSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text("Müşteri ara...", color = RefTextMuted, fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = RefTextMuted, modifier = Modifier.size(19.dp)) },
        shape = RoundedCornerShape(14.dp),
        colors = referenceFieldColors()
    )
}

@Composable
private fun ReferenceIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = RefSurface,
        border = BorderStroke(1.dp, RefBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = RefCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ReferenceCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignContract.cardRadiusDp.dp),
        color = RefSurface,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun ReferenceOutlineAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF08202D),
        border = BorderStroke(1.dp, RefCyan.copy(alpha = 0.95f))
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RefCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = RefCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TinyAction(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(35.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = RefSurfaceRaised,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.6f))
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = RefText, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = RefText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Avatar(name: String, sizeDp: Int) {
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "R" }
    Surface(shape = CircleShape, color = Color(0xFF12364A), border = BorderStroke(1.dp, RefCyan.copy(alpha = 0.32f))) {
        Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
            Text(initials, color = RefText, fontSize = (sizeDp / 3).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusChip(status: AppointmentStatus) {
    val color = statusColor(status)
    val text = when (status) {
        AppointmentStatus.PENDING -> "Bekliyor"
        AppointmentStatus.CONFIRMED -> "Onaylandı"
        AppointmentStatus.COMPLETED -> "Tamamlandı"
        AppointmentStatus.CANCELLED -> "İptal"
    }
    MiniStatePill(text, color)
}

@Composable
private fun MiniStatePill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.10f), border = BorderStroke(1.dp, color.copy(alpha = 0.8f))) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

private fun statusColor(status: AppointmentStatus): Color = when (status) {
    AppointmentStatus.PENDING -> RefBlue
    AppointmentStatus.CONFIRMED -> RefSuccess
    AppointmentStatus.COMPLETED -> RefSuccess
    AppointmentStatus.CANCELLED -> RefDanger
}

@Composable
private fun EmptyState(text: String, icon: ImageVector) {
    ReferenceCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = RefCyan.copy(alpha = 0.75f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, color = RefTextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CreateAppointmentDialog(onDismiss: () -> Unit, onCreate: (Appointment) -> Unit) {
    var customer by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var service by rememberSaveable { mutableStateOf("Saç Kesimi") }
    var staff by rememberSaveable { mutableStateOf("Mert Yılmaz") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable { mutableStateOf("10:00") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val services = listOf("Saç Kesimi", "Cilt Bakımı", "Saç Boyama", "Manikür", "Danışmanlık")
    val staffOptions = listOf("Mert Yılmaz", "Zeynep Arslan", "Deniz Arıcı")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = RefBackground,
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReferenceIconButton(Icons.Rounded.Close, "Kapat", onDismiss)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Yeni Randevu Oluştur", color = RefText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Hizmet • Zaman • Müşteri • Onay", color = RefTextMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = RefBorder.copy(alpha = 0.7f))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    item { DialogLabel("Hizmet Seçin") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(services) { option -> SelectPill(option, service == option) { service = option } }
                        }
                    }
                    item { ReferenceInput(customer, { customer = it }, "Müşteri adı", Icons.Rounded.Person) }
                    item { ReferenceInput(phone, { phone = it }, "Telefon", Icons.Rounded.Phone) }
                    item { DialogLabel("Personel") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(staffOptions) { option -> SelectPill(option, staff == option) { staff = option } }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { ReferenceInput(date, { date = it }, "Tarih", Icons.Rounded.CalendarToday) }
                            Box(Modifier.weight(0.72f)) { ReferenceInput(time, { time = it }, "Saat", Icons.Rounded.Schedule) }
                        }
                    }
                    item { ReferenceInput(note, { note = it }, "Not (isteğe bağlı)", Icons.Rounded.Notes, singleLine = false) }
                    error?.let { message ->
                        item { Text(message, color = RefDanger, fontSize = 10.sp) }
                    }
                }
                Button(
                    onClick = {
                        val validDate = runCatching { LocalDate.parse(date) }.isSuccess
                        if (customer.trim().length < 2 || phone.filter(Char::isDigit).length < 10 || !validDate || time.length < 4) {
                            error = "Müşteri, telefon, tarih ve saat alanlarını kontrol edin."
                        } else {
                            onCreate(
                                Appointment(
                                    id = "local-${System.currentTimeMillis()}",
                                    customer = customer.trim(),
                                    phone = phone.trim(),
                                    service = service,
                                    staff = staff,
                                    date = date,
                                    time = time,
                                    status = AppointmentStatus.CONFIRMED,
                                    note = note.trim()
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                ) {
                    Text("Randevuyu Kaydet", fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DialogLabel(text: String) {
    Text(text, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun SelectPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) RefCyan.copy(alpha = 0.14f) else RefSurface,
        border = BorderStroke(1.dp, if (selected) RefCyan else RefBorder)
    ) {
        Text(text, color = if (selected) RefCyan else RefTextMuted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
    }
}

@Composable
private fun ReferenceInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = MaterialTheme.typography.bodyMedium,
        label = { Text(label, fontSize = 10.sp) },
        leadingIcon = { Icon(icon, null, tint = RefCyan, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(14.dp),
        colors = referenceFieldColors()
    )
}

@Composable
private fun referenceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RefText,
    unfocusedTextColor = RefText,
    focusedBorderColor = RefCyan,
    unfocusedBorderColor = RefBorder,
    focusedLabelColor = RefCyan,
    unfocusedLabelColor = RefTextMuted,
    cursorColor = RefCyan,
    focusedContainerColor = RefSurface,
    unfocusedContainerColor = RefSurface
)

private fun openDialer(context: Context, phone: String) {
    if (phone.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) }
}

private fun openSms(context: Context, phone: String) {
    if (phone.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}"))) }
}

private fun referenceSeedAppointments(): List<Appointment> {
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(2).toString()
    return listOf(
        Appointment("seed-1", "Mert Yılmaz", "+905339876543", "Saç Kesimi", "Mert Yılmaz", today, "10:30", AppointmentStatus.PENDING, ""),
        Appointment("seed-2", "Zeynep Arslan", "+905321234567", "Cilt Bakımı", "Zeynep Arslan", today, "11:15", AppointmentStatus.CONFIRMED, ""),
        Appointment("seed-3", "Deniz Arıcı", "+905327894561", "Saç Boyama", "Deniz Arıcı", today, "13:00", AppointmentStatus.PENDING, ""),
        Appointment("seed-4", "Elif Kaya", "+905324567890", "Manikür", "Zeynep Arslan", today, "14:30", AppointmentStatus.CONFIRMED, ""),
        Appointment("seed-5", "Ahmet Koç", "+905333210987", "Danışmanlık", "Mert Yılmaz", today, "17:00", AppointmentStatus.CONFIRMED, ""),
        Appointment("seed-6", "Zeynep Arslan", "+905321234567", "Saç Kesimi", "Mert Yılmaz", yesterday, "12:00", AppointmentStatus.COMPLETED, "")
    )
}
