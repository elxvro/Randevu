package com.elxvro.randevu.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentAction
import com.elxvro.randevu.core.AppointmentEngine
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.notifications.AppointmentNotifier
import com.elxvro.randevu.notifications.AppointmentReminderScheduler
import com.elxvro.randevu.notifications.NotificationCenterProjection
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffProjection
import com.elxvro.randevu.staff.StaffRecord
import com.elxvro.randevu.storage.LiveSyncStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RandevuV12App() {
    val context = LocalContext.current
    val store = remember { LiveSyncStore(context.applicationContext) }
    val initialAppointments = remember { store.loadAppointments().ifEmpty { v12SeedAppointments() } }
    val initialStaff = remember { store.loadStaff() }
    val initialLeaves = remember { store.loadStaffLeaves() }
    val appointments = remember { mutableStateListOf<Appointment>().apply { addAll(initialAppointments) } }
    val staffRecords = remember { mutableStateListOf<StaffRecord>().apply { addAll(initialStaff) } }
    val staffLeaves = remember { mutableStateListOf<StaffLeave>().apply { addAll(initialLeaves) } }

    var selectedTabName by rememberSaveable { mutableStateOf(ReferenceTab.HOME.name) }
    var showAppointmentEditor by rememberSaveable { mutableStateOf(false) }
    var editingAppointmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var prefillCustomer by rememberSaveable { mutableStateOf("") }
    var prefillPhone by rememberSaveable { mutableStateOf("") }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var showAddStaff by rememberSaveable { mutableStateOf(false) }
    var staffDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var infoTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var infoBody by rememberSaveable { mutableStateOf("") }
    var reminderEnabled by rememberSaveable { mutableStateOf(store.loadReminderEnabled()) }
    var permissionGranted by remember { mutableStateOf(v12HasNotificationPermission(context)) }

    val selectedTab = runCatching { ReferenceTab.valueOf(selectedTabName) }.getOrDefault(ReferenceTab.HOME)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted && reminderEnabled) {
            AppointmentReminderScheduler.rescheduleAll(context, appointments.toList())
        }
    }

    fun persistAppointments(next: List<Appointment>) {
        appointments.clear()
        appointments.addAll(next)
        store.saveAppointments(next)
    }

    fun dispatch(action: AppointmentAction) {
        val next = AppointmentEngine.reduce(appointments.toList(), action)
        persistAppointments(next)
        when (action) {
            is AppointmentAction.Delete -> AppointmentReminderScheduler.cancel(context, action.id)
            is AppointmentAction.Add -> if (reminderEnabled) AppointmentReminderScheduler.schedule(context, action.appointment)
            is AppointmentAction.Update -> if (reminderEnabled) AppointmentReminderScheduler.schedule(context, action.appointment)
            is AppointmentAction.ChangeStatus -> {
                val changed = next.firstOrNull { it.id == action.id }
                if (changed == null || !reminderEnabled) AppointmentReminderScheduler.cancel(context, action.id)
                else AppointmentReminderScheduler.schedule(context, changed)
            }
        }
    }

    fun openNewAppointment(customer: String = "", phone: String = "") {
        editingAppointmentId = null
        prefillCustomer = customer
        prefillPhone = phone
        showAppointmentEditor = true
    }

    fun openAppointment(id: String) {
        editingAppointmentId = id
        prefillCustomer = ""
        prefillPhone = ""
        showAppointmentEditor = true
    }

    fun toggleReminders(enabled: Boolean) {
        reminderEnabled = enabled
        store.saveReminderEnabled(enabled)
        if (!enabled) {
            AppointmentReminderScheduler.cancelAll(context, appointments.toList())
            return
        }
        permissionGranted = v12HasNotificationPermission(context)
        if (permissionGranted) {
            AppointmentReminderScheduler.rescheduleAll(context, appointments.toList())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun saveStaffRecord(updated: StaffRecord) {
        val old = staffRecords.firstOrNull { it.id == updated.id }
        val nextStaff = staffRecords.map { if (it.id == updated.id) updated else it }
        staffRecords.clear()
        staffRecords.addAll(nextStaff)
        store.saveStaff(nextStaff)
        if (old != null && old.name != updated.name) {
            val nextAppointments = appointments.map { appointment ->
                if (appointment.staff == old.name) appointment.copy(staff = updated.name) else appointment
            }
            persistAppointments(nextAppointments)
            if (reminderEnabled) AppointmentReminderScheduler.rescheduleAll(context, nextAppointments)
        }
    }

    LaunchedEffect(Unit) {
        if (store.loadAppointments().isEmpty()) store.saveAppointments(initialAppointments)
        store.saveStaff(initialStaff)
        AppointmentNotifier.ensureChannel(context)
        permissionGranted = v12HasNotificationPermission(context)
        if (reminderEnabled) AppointmentReminderScheduler.rescheduleAll(context, appointments.toList())
    }

    ReferenceRandevuTheme {
        Scaffold(
            containerColor = RefBackground,
            contentColor = RefText,
            bottomBar = {
                V12BottomNavigation(selectedTab) { selectedTabName = it.name }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    ReferenceTab.HOME -> V12HomeScreen(
                        appointments = appointments,
                        onNew = { openNewAppointment() },
                        onOpen = ::openAppointment,
                        onNotifications = { showNotifications = true },
                        onShowCalendar = { selectedTabName = ReferenceTab.CALENDAR.name }
                    )
                    ReferenceTab.CALENDAR -> V12CalendarScreen(
                        appointments = appointments,
                        onNew = { openNewAppointment() },
                        onOpen = ::openAppointment
                    )
                    ReferenceTab.CUSTOMERS -> V12CustomersScreen(
                        appointments = appointments,
                        onNew = { customer, phone -> openNewAppointment(customer, phone) }
                    )
                    ReferenceTab.STAFF -> V12StaffScreen(
                        appointments = appointments,
                        staff = staffRecords,
                        leaves = staffLeaves,
                        onAddStaff = { showAddStaff = true },
                        onOpenStaff = { staffDetailId = it }
                    )
                    ReferenceTab.MORE -> V12MoreScreen(
                        reminderEnabled = reminderEnabled,
                        onToggleReminders = ::toggleReminders,
                        onNotifications = { showNotifications = true },
                        onDataInfo = {
                            infoTitle = "Veri ve Senkronizasyon"
                            infoBody = "Randevular, personel ve izin kayıtları cihazda yerel olarak saklanır. Uygulama internet olmadan çalışmaya devam eder."
                        },
                        onHelp = {
                            infoTitle = "Yardım & Destek"
                            infoBody = "Randevu eklemek için Ana Sayfa veya Takvim ekranındaki Yeni Randevu düğmesini kullanın. Personel izinleri Personel ekranından yönetilir."
                        },
                        onAbout = {
                            infoTitle = "Uygulama Hakkında"
                            infoBody = "Randevu v1.2.0 • ELXVRO\nYerel randevu, müşteri, personel, izin ve hatırlatma yönetimi."
                        }
                    )
                }
            }
        }

        if (showAppointmentEditor) {
            val existing = editingAppointmentId?.let { id -> appointments.firstOrNull { it.id == id } }
            V12AppointmentDialog(
                existing = existing,
                prefillCustomer = prefillCustomer,
                prefillPhone = prefillPhone,
                staffRecords = staffRecords,
                leaves = staffLeaves,
                appointments = appointments,
                onDismiss = { showAppointmentEditor = false },
                onSave = { appointment ->
                    dispatch(if (existing == null) AppointmentAction.Add(appointment) else AppointmentAction.Update(appointment))
                    showAppointmentEditor = false
                },
                onDelete = if (existing == null) null else { id ->
                    dispatch(AppointmentAction.Delete(id))
                    showAppointmentEditor = false
                }
            )
        }

        if (showNotifications) {
            val rows = NotificationCenterProjection.rows(appointments.toList(), LocalDateTime.now())
            V12NotificationCenterDialog(
                enabled = reminderEnabled,
                permissionGranted = permissionGranted,
                rows = rows,
                onToggle = ::toggleReminders,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { showNotifications = false }
            )
        }

        if (showAddStaff) {
            V12AddStaffDialog(
                onDismiss = { showAddStaff = false },
                onSave = { record ->
                    staffRecords.add(record)
                    store.saveStaff(staffRecords.toList())
                    showAddStaff = false
                }
            )
        }

        staffDetailId?.let { id ->
            staffRecords.firstOrNull { it.id == id }?.let { record ->
                V12StaffDetailDialog(
                    record = record,
                    appointments = appointments,
                    leaves = staffLeaves,
                    onDismiss = { staffDetailId = null },
                    onSaveStaff = { updated ->
                        saveStaffRecord(updated)
                        staffDetailId = null
                    },
                    onAddLeave = { leave ->
                        staffLeaves.add(leave)
                        store.saveStaffLeaves(staffLeaves.toList())
                    },
                    onDeleteLeave = { leave ->
                        staffLeaves.removeAll { it.id == leave.id }
                        store.saveStaffLeaves(staffLeaves.toList())
                    }
                )
            }
        }

        infoTitle?.let { title ->
            V12InfoDialog(title, infoBody) { infoTitle = null; infoBody = "" }
        }
    }
}

@Composable
private fun V12BottomNavigation(selected: ReferenceTab, onSelect: (ReferenceTab) -> Unit) {
    Surface(
        color = Color(0xFF071923),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.7f))
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(ReferenceDesignContract.bottomNavContentHeightDp.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReferenceDesignContract.bottomTabs.forEach { tab ->
                    val active = tab == selected
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable { onSelect(tab) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(tab.v12Icon(), tab.label, modifier = Modifier.size(21.dp), tint = if (active) RefCyan else RefTextMuted)
                        Spacer(Modifier.height(3.dp))
                        Text(tab.label, color = if (active) RefCyan else RefTextMuted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun ReferenceTab.v12Icon(): ImageVector = when (this) {
    ReferenceTab.HOME -> Icons.Rounded.Home
    ReferenceTab.CALENDAR -> Icons.Rounded.CalendarMonth
    ReferenceTab.CUSTOMERS -> Icons.Rounded.Groups
    ReferenceTab.STAFF -> Icons.Rounded.Badge
    ReferenceTab.MORE -> Icons.Rounded.Menu
}

@Composable
private fun V12HomeScreen(
    appointments: List<Appointment>,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onNotifications: () -> Unit,
    onShowCalendar: () -> Unit
) {
    val today = LocalDate.now().toString()
    val todayItems = appointments.filter { it.date == today }.sortedBy { it.time }
    val metrics = ReferenceAppModel.metrics(appointments)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ReferenceDesignContract.pageInsetDp.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(ReferenceDesignContract.cardGapDp.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(ReferenceDesignContract.headerHeightDp.dp), verticalAlignment = Alignment.CenterVertically) {
                V12Avatar("Selin Demir", 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Merhaba,", color = RefTextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("Selin Demir", color = RefText, style = MaterialTheme.typography.titleMedium)
                }
                V12IconButton(Icons.Rounded.NotificationsNone, "Bildirim Merkezi", onNotifications)
            }
        }
        item { V12HeroTodayCard(todayItems.size) }
        item { V12MetricsRow(metrics) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Bugünkü Randevular", color = RefText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("Takvime Git", color = RefCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onShowCalendar).padding(6.dp))
            }
        }
        if (todayItems.isEmpty()) {
            item { V12EmptyState("Bugün için randevu bulunmuyor", Icons.Rounded.EventAvailable) }
        } else {
            items(todayItems, key = { it.id }) { appointment ->
                V12AppointmentCard(appointment) { onOpen(appointment.id) }
            }
        }
        item { V12OutlineButton("Yeni Randevu Oluştur", Icons.Rounded.Add, onClick = onNew) }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun V12HeroTodayCard(todayCount: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(ReferenceDesignContract.cardRadiusDp.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A3750), Color(0xFF063A58), Color(0xFF071A26))))
    ) {
        Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(RefBlue.copy(alpha = 0.22f), Color.Transparent, RefCyan.copy(alpha = 0.10f)))))
        Column(Modifier.padding(17.dp)) {
            Text("Bugünkü", color = RefTextMuted, style = MaterialTheme.typography.bodyMedium)
            Text("Randevular", color = RefText, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(todayCount.toString(), color = RefText, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(62.dp), shape = CircleShape, color = Color.Transparent, border = BorderStroke(6.dp, RefCyan)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.CalendarMonth, null, tint = RefText, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.height(5.dp))
            Text("Günün planı", color = RefTextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun V12MetricsRow(metrics: ReferenceMetrics) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V12MetricCard("Toplam", metrics.total, RefBlue, Modifier.weight(1f))
        V12MetricCard("Bitti", metrics.completed, RefSuccess, Modifier.weight(1f))
        V12MetricCard("Bekliyor", metrics.pending, RefWarning, Modifier.weight(1f))
        V12MetricCard("İptal", metrics.cancelled, RefDanger, Modifier.weight(1f))
    }
}

@Composable
private fun V12MetricCard(label: String, value: Int, color: Color, modifier: Modifier) {
    Surface(modifier = modifier.height(70.dp), shape = RoundedCornerShape(14.dp), color = RefSurface, border = BorderStroke(1.dp, color.copy(alpha = 0.28f))) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.Center) {
            Text(value.toString(), color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(label, color = RefTextMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun V12AppointmentCard(appointment: Appointment, onClick: () -> Unit) {
    val accent = v12StatusColor(appointment.status)
    V12Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(42.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(10.dp))
            Text(appointment.time, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(48.dp))
            V12Avatar(appointment.customer, 34)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(appointment.service.ifBlank { "Randevu" }, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${appointment.customer} • ${appointment.staff}", color = RefTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            V12StatusChip(appointment.status)
        }
    }
}

@Composable
private fun V12CalendarScreen(appointments: List<Appointment>, onNew: () -> Unit, onOpen: (String) -> Unit) {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var mode by rememberSaveable { mutableStateOf("Gün") }
    val selected = runCatching { LocalDate.parse(selectedDate) }.getOrDefault(LocalDate.now())
    val days = (-3L..3L).map { selected.plusDays(it) }
    val shown = when (mode) {
        "Hafta" -> {
            val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
            val end = start.plusDays(6)
            appointments.filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d -> !d.isBefore(start) && !d.isAfter(end) } == true }
        }
        "Ay" -> appointments.filter { it.date.startsWith(selected.toString().take(7)) }
        else -> appointments.filter { it.date == selected.toString() }
    }.sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })

    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Takvim", Icons.Rounded.Add, "Randevu ekle", onNew)
        V12MonthSelector(selected, onPrevious = { selectedDate = selected.minusMonths(1).toString() }, onNext = { selectedDate = selected.plusMonths(1).toString() })
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(days, key = { it.toString() }) { date -> V12DayButton(date, date == selected) { selectedDate = date.toString() } }
        }
        Spacer(Modifier.height(9.dp))
        V12Segmented(listOf("Gün", "Hafta", "Ay"), mode) { mode = it }
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (shown.isEmpty()) item { V12EmptyState("Bu aralıkta randevu yok", Icons.Rounded.CalendarToday) }
            else items(shown, key = { it.id }) { item -> V12AppointmentCard(item) { onOpen(item.id) } }
            item { V12OutlineButton("Yeni Randevu Oluştur", Icons.Rounded.Add, onClick = onNew) }
        }
    }
}

@Composable
private fun V12MonthSelector(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR")) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronLeft, "Önceki ay", tint = RefTextMuted, modifier = Modifier.size(19.dp)) }
        Text(
            date.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() },
            color = RefText,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, "Sonraki ay", tint = RefTextMuted, modifier = Modifier.size(19.dp)) }
    }
}

@Composable
private fun V12DayButton(date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("tr", "TR")).take(3)
    Column(
        Modifier.width(42.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dayName, color = RefTextMuted, fontSize = 9.sp)
        Spacer(Modifier.height(4.dp))
        Surface(shape = CircleShape, color = if (selected) RefCyan else Color.Transparent) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Text(date.dayOfMonth.toString(), color = if (selected) RefBackground else RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun V12Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = RefSurface, border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.7f))) {
        Row(Modifier.fillMaxWidth().padding(3.dp)) {
            options.forEach { option ->
                val active = option == selected
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (active) RefBlue else Color.Transparent).clickable { onSelect(option) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option, color = if (active) Color.White else RefTextMuted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun V12CustomersScreen(appointments: List<Appointment>, onNew: (String, String) -> Unit) {
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
        V12Header("Müşteriler", Icons.Rounded.PersonAdd, "Yeni müşteri randevusu") { onNew("", "") }
        V12SearchField(query, { query = it }, "Müşteri ara...")
        Spacer(Modifier.height(8.dp))
        V12Segmented(listOf("Tümü", "Aktif", "Pasif"), filter) { filter = it }
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (customers.isEmpty()) item { V12EmptyState("Müşteri bulunamadı", Icons.Rounded.PersonSearch) }
            else items(customers, key = { it.phone.ifBlank { it.name } }) { customer ->
                V12Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        V12Avatar(customer.name, 42)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(customer.phone.ifBlank { "Telefon yok" }, color = RefTextMuted, fontSize = 10.sp)
                            Text("${customer.appointmentCount} randevu", color = RefTextMuted, fontSize = 9.sp)
                        }
                        V12MiniPill(if (customer.active) "Aktif" else "Pasif", if (customer.active) RefSuccess else RefDisabled)
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        V12TinyButton("Ara", Icons.Rounded.Call, Modifier.weight(1f)) { v12OpenDialer(context, customer.phone) }
                        V12TinyButton("Mesaj", Icons.Rounded.ChatBubbleOutline, Modifier.weight(1f)) { v12OpenSms(context, customer.phone) }
                        V12TinyButton("Randevu", Icons.Rounded.CalendarMonth, Modifier.weight(1.25f)) { onNew(customer.name, customer.phone) }
                    }
                }
            }
            item { V12OutlineButton("Yeni Müşteri Randevusu", Icons.Rounded.Add, onClick = { onNew("", "") }) }
        }
    }
}

@Composable
private fun V12StaffScreen(
    appointments: List<Appointment>,
    staff: List<StaffRecord>,
    leaves: List<StaffLeave>,
    onAddStaff: () -> Unit,
    onOpenStaff: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Personel", Icons.Rounded.PersonAdd, "Personel ekle", onAddStaff)
        Text("Ekibiniz, randevu yoğunluğu ve izin takvimi", color = RefTextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (staff.isEmpty()) item { V12EmptyState("Henüz personel kaydı yok", Icons.Rounded.Groups) }
            else items(staff, key = { it.id }) { person ->
                val personAppointments = appointments.filter { it.staff == person.name }
                val nextLeave = StaffProjection.nextLeave(person.id, leaves, LocalDate.now())
                V12Card(onClick = { onOpenStaff(person.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        V12Avatar(person.name, 42)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(person.title, color = RefTextMuted, fontSize = 10.sp)
                            if (nextLeave != null) Text("İzin: ${nextLeave.startDate} → ${nextLeave.endDate}", color = RefWarning, fontSize = 9.sp)
                        }
                        V12MiniPill(if (person.active) "Aktif" else "Pasif", if (person.active) RefSuccess else RefDisabled)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V12MiniPill("${personAppointments.size} randevu", RefBlue)
                        V12MiniPill("${personAppointments.count { it.status == AppointmentStatus.COMPLETED }} tamamlandı", RefSuccess)
                    }
                }
            }
            item { V12OutlineButton("Yeni Personel", Icons.Rounded.PersonAdd, onClick = onAddStaff) }
        }
    }
}

@Composable
private fun V12MoreScreen(
    reminderEnabled: Boolean,
    onToggleReminders: (Boolean) -> Unit,
    onNotifications: () -> Unit,
    onDataInfo: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Daha Fazla")
        V12Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V12Avatar("Selin Demir", 46)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Selin Demir", color = RefText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Yerel işletme profili", color = RefTextMuted, fontSize = 10.sp)
                }
                V12MiniPill("Çevrimdışı", RefSuccess)
            }
        }
        Spacer(Modifier.height(10.dp))
        V12Card {
            V12SettingsToggleRow(Icons.Rounded.Schedule, "Randevu hatırlatmaları", reminderEnabled, onToggleReminders)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.NotificationsNone, "Bildirim Merkezi", onClick = onNotifications)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.Sync, "Veri ve Senkronizasyon", "Yerel", onClick = onDataInfo)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.HelpOutline, "Yardım & Destek", onClick = onHelp)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.Info, "Uygulama Hakkında", "v1.2.0", onClick = onAbout)
        }
    }
}

private fun v12HasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun v12OpenDialer(context: Context, phone: String) {
    if (phone.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) }
}

private fun v12OpenSms(context: Context, phone: String) {
    if (phone.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}"))) }
}

private fun v12SeedAppointments(): List<Appointment> {
    val today = LocalDate.now().toString()
    val tomorrow = LocalDate.now().plusDays(1).toString()
    return listOf(
        Appointment("seed-1", "Ayşe Kaya", "+905339876543", "Saç Kesimi", "Mert Yılmaz", today, "10:30", AppointmentStatus.PENDING, ""),
        Appointment("seed-2", "Elif Arslan", "+905321234567", "Cilt Bakımı", "Zeynep Arslan", today, "11:15", AppointmentStatus.CONFIRMED, ""),
        Appointment("seed-3", "Can Demir", "+905327894561", "Saç Boyama", "Deniz Arıcı", today, "13:00", AppointmentStatus.PENDING, ""),
        Appointment("seed-4", "Burcu Kaya", "+905324567890", "Manikür", "Zeynep Arslan", tomorrow, "14:30", AppointmentStatus.CONFIRMED, "")
    )
}
