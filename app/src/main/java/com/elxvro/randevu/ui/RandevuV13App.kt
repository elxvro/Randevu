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
import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
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

@Composable
fun RandevuV13App() {
    val context = LocalContext.current
    val store = remember { LiveSyncStore(context.applicationContext) }
    remember { store.migrateV13IfNeeded(); true }

    var profile by remember { mutableStateOf(store.loadBusinessProfile()) }
    var services by remember { mutableStateOf(store.loadServices()) }
    val appointments = remember { mutableStateListOf<Appointment>().apply { addAll(store.loadAppointments()) } }
    val staff = remember { mutableStateListOf<StaffRecord>().apply { addAll(store.loadStaff()) } }
    val leaves = remember { mutableStateListOf<StaffLeave>().apply { addAll(store.loadStaffLeaves()) } }
    var setupStep by rememberSaveable { mutableIntStateOf(store.loadSetupStep()) }
    var reminderEnabled by rememberSaveable { mutableStateOf(store.loadReminderEnabled()) }
    var notificationPermission by remember { mutableStateOf(v13HasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermission = granted
        if (granted && reminderEnabled) AppointmentReminderScheduler.rescheduleAll(context, appointments.toList())
    }

    fun setReminders(enabled: Boolean) {
        reminderEnabled = enabled
        store.saveReminderEnabled(enabled)
        if (!enabled) {
            AppointmentReminderScheduler.cancelAll(context, appointments.toList())
            return
        }
        notificationPermission = v13HasNotificationPermission(context)
        if (notificationPermission) AppointmentReminderScheduler.rescheduleAll(context, appointments.toList())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun persistAppointments(next: List<Appointment>) {
        appointments.clear(); appointments.addAll(next); store.saveAppointments(next)
    }

    fun dispatch(action: AppointmentAction) {
        val next = AppointmentEngine.reduce(appointments.toList(), action)
        persistAppointments(next)
        when (action) {
            is AppointmentAction.Add -> if (reminderEnabled) AppointmentReminderScheduler.schedule(context, action.appointment)
            is AppointmentAction.Update -> if (reminderEnabled) AppointmentReminderScheduler.schedule(context, action.appointment)
            is AppointmentAction.Delete -> AppointmentReminderScheduler.cancel(context, action.id)
            is AppointmentAction.ChangeStatus -> {
                val changed = next.firstOrNull { it.id == action.id }
                if (changed == null || changed.status == AppointmentStatus.CANCELLED || changed.status == AppointmentStatus.COMPLETED || !reminderEnabled) {
                    AppointmentReminderScheduler.cancel(context, action.id)
                } else AppointmentReminderScheduler.schedule(context, changed)
            }
        }
    }

    LaunchedEffect(Unit) {
        AppointmentNotifier.ensureChannel(context)
        notificationPermission = v13HasNotificationPermission(context)
        if (reminderEnabled) AppointmentReminderScheduler.rescheduleAll(context, appointments.toList())
    }

    ReferenceRandevuTheme {
        when (BusinessSetupGate.destination(profile, services)) {
            SetupDestination.SETUP -> V13BusinessSetupFlow(
                profile = profile,
                services = services,
                staff = staff,
                initialStep = setupStep,
                reminderEnabled = reminderEnabled,
                onProfileChanged = { profile = it; store.saveBusinessProfile(it) },
                onServicesChanged = { services = it; store.saveServices(it) },
                onStaffChanged = { next -> staff.clear(); staff.addAll(next); store.saveStaff(next) },
                onReminderChanged = ::setReminders,
                onStepChanged = { setupStep = it; store.saveSetupStep(it) },
                onComplete = { completed ->
                    profile = completed
                    store.saveBusinessProfile(completed)
                    setupStep = 4
                    store.saveSetupStep(4)
                }
            )
            SetupDestination.APP -> V13MainShell(
                profile = profile,
                services = services,
                appointments = appointments,
                staff = staff,
                leaves = leaves,
                reminderEnabled = reminderEnabled,
                notificationPermission = notificationPermission,
                onToggleReminders = ::setReminders,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onAppointmentAction = ::dispatch,
                onStaffChanged = { next -> staff.clear(); staff.addAll(next); store.saveStaff(next) },
                onLeavesChanged = { next -> leaves.clear(); leaves.addAll(next); store.saveStaffLeaves(next) },
                onProfileChanged = { profile = it; store.saveBusinessProfile(it) },
                onServicesChanged = { services = it; store.saveServices(it) },
                onReset = { destructive ->
                    store.clearBusinessSetup(destructive)
                    profile = BusinessProfile.unconfigured()
                    services = emptyList()
                    setupStep = 0
                    if (destructive) {
                        appointments.clear(); staff.clear(); leaves.clear()
                        AppointmentReminderScheduler.cancelAll(context, emptyList())
                    }
                }
            )
        }
    }
}

@Composable
private fun V13MainShell(
    profile: BusinessProfile,
    services: List<ServiceRecord>,
    appointments: List<Appointment>,
    staff: List<StaffRecord>,
    leaves: List<StaffLeave>,
    reminderEnabled: Boolean,
    notificationPermission: Boolean,
    onToggleReminders: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onAppointmentAction: (AppointmentAction) -> Unit,
    onStaffChanged: (List<StaffRecord>) -> Unit,
    onLeavesChanged: (List<StaffLeave>) -> Unit,
    onProfileChanged: (BusinessProfile) -> Unit,
    onServicesChanged: (List<ServiceRecord>) -> Unit,
    onReset: (Boolean) -> Unit
) {
    var tabName by rememberSaveable { mutableStateOf(ReferenceTab.HOME.name) }
    var appointmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAppointment by rememberSaveable { mutableStateOf(false) }
    var prefillCustomer by rememberSaveable { mutableStateOf("") }
    var prefillPhone by rememberSaveable { mutableStateOf("") }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var showAddStaff by rememberSaveable { mutableStateOf(false) }
    var staffDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBusiness by rememberSaveable { mutableStateOf(false) }
    var showServices by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var infoTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var infoBody by rememberSaveable { mutableStateOf("") }
    val tab = runCatching { ReferenceTab.valueOf(tabName) }.getOrDefault(ReferenceTab.HOME)

    fun newAppointment(customer: String = "", phone: String = "") {
        appointmentId = null; prefillCustomer = customer; prefillPhone = phone; showAppointment = true
    }
    fun openAppointment(id: String) {
        appointmentId = id; prefillCustomer = ""; prefillPhone = ""; showAppointment = true
    }

    Scaffold(
        containerColor = RefBackground,
        contentColor = RefText,
        bottomBar = { V13BottomNavigation(tab) { tabName = it.name } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                ReferenceTab.HOME -> V13Home(profile, appointments, { newAppointment() }, ::openAppointment, { showNotifications = true }, { tabName = ReferenceTab.CALENDAR.name })
                ReferenceTab.CALENDAR -> V13Calendar(appointments, { newAppointment() }, ::openAppointment)
                ReferenceTab.CUSTOMERS -> V13Customers(appointments) { name, phone -> newAppointment(name, phone) }
                ReferenceTab.STAFF -> V13Staff(appointments, staff, leaves, { showAddStaff = true }) { staffDetailId = it }
                ReferenceTab.MORE -> V13More(
                    profile = profile,
                    reminderEnabled = reminderEnabled,
                    onToggleReminders = onToggleReminders,
                    onNotifications = { showNotifications = true },
                    onBusiness = { showBusiness = true },
                    onServices = { showServices = true },
                    onWhatsApp = {
                        infoTitle = "WhatsApp Hatırlatmaları"
                        infoBody = "WhatsApp Business Cloud API bağlantısı sunucu üzerinden çalışır. Meta erişim anahtarı APK içinde tutulmaz."
                    },
                    onReset = { showReset = true },
                    onAbout = {
                        infoTitle = "Uygulama Hakkında"
                        infoBody = "Randevu v1.3.0 • ELXVRO\nİşletme, randevu, personel, izin ve bildirim yönetimi."
                    }
                )
            }
        }
    }

    if (showAppointment) {
        val existing = appointmentId?.let { id -> appointments.firstOrNull { it.id == id } }
        V13AppointmentDialog(
            existing = existing,
            prefillCustomer = prefillCustomer,
            prefillPhone = prefillPhone,
            serviceRecords = services,
            staffRecords = staff,
            leaves = leaves,
            appointments = appointments,
            onDismiss = { showAppointment = false },
            onSave = { item -> onAppointmentAction(if (existing == null) AppointmentAction.Add(item) else AppointmentAction.Update(item)); showAppointment = false },
            onDelete = if (existing == null) null else { id -> onAppointmentAction(AppointmentAction.Delete(id)); showAppointment = false }
        )
    }

    if (showNotifications) {
        V12NotificationCenterDialog(
            enabled = reminderEnabled,
            permissionGranted = notificationPermission,
            rows = NotificationCenterProjection.rows(appointments, LocalDateTime.now()),
            onToggle = onToggleReminders,
            onRequestPermission = onRequestPermission,
            onDismiss = { showNotifications = false }
        )
    }

    if (showAddStaff) {
        V12AddStaffDialog(onDismiss = { showAddStaff = false }) { record ->
            onStaffChanged(staff + record); showAddStaff = false
        }
    }

    staffDetailId?.let { id ->
        staff.firstOrNull { it.id == id }?.let { record ->
            V12StaffDetailDialog(
                record = record,
                appointments = appointments,
                leaves = leaves,
                onDismiss = { staffDetailId = null },
                onSaveStaff = { updated ->
                    onStaffChanged(staff.map { if (it.id == updated.id) updated else it })
                    staffDetailId = null
                },
                onAddLeave = { leave -> onLeavesChanged(leaves + leave) },
                onDeleteLeave = { leave -> onLeavesChanged(leaves.filterNot { it.id == leave.id }) }
            )
        }
    }

    if (showBusiness) V13BusinessSettingsDialog(profile, { showBusiness = false }) { onProfileChanged(it); showBusiness = false }
    if (showServices) V13ServiceSettingsDialog(services, { showServices = false }) { onServicesChanged(it); showServices = false }
    if (showReset) V13ResetDialog({ showReset = false }) { destructive -> showReset = false; onReset(destructive) }
    infoTitle?.let { title -> V12InfoDialog(title, infoBody) { infoTitle = null; infoBody = "" } }
}

@Composable
private fun V13BottomNavigation(selected: ReferenceTab, onSelect: (ReferenceTab) -> Unit) {
    Surface(color = Color(0xFF071923), border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.7f))) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(ReferenceDesignContract.bottomNavContentHeightDp.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                ReferenceDesignContract.bottomTabs.forEach { item ->
                    val active = item == selected
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable { onSelect(item) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.v13Icon(), item.label, tint = if (active) RefCyan else RefTextMuted, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(item.label, color = if (active) RefCyan else RefTextMuted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun ReferenceTab.v13Icon(): ImageVector = when (this) {
    ReferenceTab.HOME -> Icons.Rounded.Home
    ReferenceTab.CALENDAR -> Icons.Rounded.CalendarMonth
    ReferenceTab.CUSTOMERS -> Icons.Rounded.Groups
    ReferenceTab.STAFF -> Icons.Rounded.Badge
    ReferenceTab.MORE -> Icons.Rounded.Menu
}

@Composable
private fun V13Home(profile: BusinessProfile, appointments: List<Appointment>, onNew: () -> Unit, onOpen: (String) -> Unit, onNotifications: () -> Unit, onCalendar: () -> Unit) {
    val today = LocalDate.now().toString()
    val todayItems = appointments.filter { it.date == today }.sortedBy { it.time }
    val metrics = ReferenceAppModel.metrics(appointments)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ReferenceDesignContract.pageInsetDp.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(ReferenceDesignContract.cardGapDp.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(ReferenceDesignContract.headerHeightDp.dp), verticalAlignment = Alignment.CenterVertically) {
                V12Avatar(profile.ownerName.ifBlank { profile.businessName }, 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.businessName, color = RefTextMuted, fontSize = 10.sp)
                    Text(profile.ownerName.ifBlank { "İşletme" }, color = RefText, style = MaterialTheme.typography.titleMedium)
                }
                V12IconButton(Icons.Rounded.NotificationsNone, "Bildirim Merkezi", onNotifications)
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(ReferenceDesignContract.cardRadiusDp.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0A3750), Color(0xFF063A58), Color(0xFF071A26))))
            ) {
                Column(Modifier.padding(17.dp)) {
                    Text("Bugünkü Randevular", color = RefTextMuted, fontSize = 12.sp)
                    Text(todayItems.size.toString(), color = RefText, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${profile.openingTime} - ${profile.closingTime}", color = RefCyan, fontSize = 10.sp)
                }
                Icon(Icons.Rounded.EventAvailable, null, tint = RefCyan, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).size(52.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                V13Metric("Toplam", metrics.total, RefBlue, Modifier.weight(1f))
                V13Metric("Bitti", metrics.completed, RefSuccess, Modifier.weight(1f))
                V13Metric("Bekliyor", metrics.pending, RefWarning, Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bugünkü Randevular", color = RefText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("Takvime Git", color = RefCyan, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onCalendar).padding(6.dp))
            }
        }
        if (todayItems.isEmpty()) item { V12EmptyState("Bugün için randevu yok", Icons.Rounded.EventAvailable) }
        else items(todayItems, key = { it.id }) { item -> V13AppointmentCard(item) { onOpen(item.id) } }
        item { V12OutlineButton("Yeni Randevu", Icons.Rounded.Add, onClick = onNew) }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun V13Metric(label: String, value: Int, color: Color, modifier: Modifier) {
    Surface(modifier = modifier.height(66.dp), shape = RoundedCornerShape(14.dp), color = RefSurface, border = BorderStroke(1.dp, color.copy(alpha = 0.25f))) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.Center) {
            Text(value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = RefTextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun V13AppointmentCard(item: Appointment, onClick: () -> Unit) {
    V12Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.time, color = RefCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(48.dp))
            V12Avatar(item.customer, 34)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(item.service, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.customer} • ${item.staff}", color = RefTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            V12StatusChip(item.status)
        }
    }
}

@Composable
private fun V13Calendar(appointments: List<Appointment>, onNew: () -> Unit, onOpen: (String) -> Unit) {
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.now())
    val shown = appointments.filter { it.date == date.toString() }.sortedBy { it.time }
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Takvim", Icons.Rounded.Add, "Randevu ekle", onNew)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dateText = date.minusDays(1).toString() }) { Icon(Icons.Rounded.ChevronLeft, "Önceki gün", tint = RefTextMuted) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.toString(), color = RefText, fontWeight = FontWeight.Bold)
                Text(date.dayOfWeek.name, color = RefTextMuted, fontSize = 9.sp)
            }
            IconButton(onClick = { dateText = date.plusDays(1).toString() }) { Icon(Icons.Rounded.ChevronRight, "Sonraki gün", tint = RefTextMuted) }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (shown.isEmpty()) item { V12EmptyState("Bu gün için randevu yok", Icons.Rounded.CalendarToday) }
            else items(shown, key = { it.id }) { item -> V13AppointmentCard(item) { onOpen(item.id) } }
            item { V12OutlineButton("Yeni Randevu", Icons.Rounded.Add, onClick = onNew) }
        }
    }
}

@Composable
private fun V13Customers(appointments: List<Appointment>, onNew: (String, String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val customers = ReferenceAppModel.customers(appointments).filter { query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) }
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Müşteriler", Icons.Rounded.PersonAdd, "Yeni randevu") { onNew("", "") }
        V12SearchField(query, { query = it }, "Müşteri ara...")
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (customers.isEmpty()) item { V12EmptyState("Müşteri bulunamadı", Icons.Rounded.PersonSearch) }
            else items(customers, key = { it.phone.ifBlank { it.name } }) { customer ->
                V12Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        V12Avatar(customer.name, 42); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(customer.phone.ifBlank { "Telefon yok" }, color = RefTextMuted, fontSize = 10.sp)
                        }
                        V12MiniPill("${customer.appointmentCount} randevu", RefBlue)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        V12TinyButton("Ara", Icons.Rounded.Call, Modifier.weight(1f)) { v13OpenDialer(context, customer.phone) }
                        V12TinyButton("SMS", Icons.Rounded.ChatBubbleOutline, Modifier.weight(1f)) { v13OpenSms(context, customer.phone) }
                        V12TinyButton("Randevu", Icons.Rounded.CalendarMonth, Modifier.weight(1.3f)) { onNew(customer.name, customer.phone) }
                    }
                }
            }
        }
    }
}

@Composable
private fun V13Staff(appointments: List<Appointment>, staff: List<StaffRecord>, leaves: List<StaffLeave>, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Personel", Icons.Rounded.PersonAdd, "Personel ekle", onAdd)
        Text("Ekibiniz ve izin takvimi", color = RefTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(9.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (staff.isEmpty()) item { V12EmptyState("Henüz personel eklenmedi", Icons.Rounded.Groups) }
            else items(staff, key = { it.id }) { person ->
                val nextLeave = StaffProjection.nextLeave(person.id, leaves, LocalDate.now())
                val count = appointments.count { it.staff == person.name }
                V12Card(onClick = { onOpen(person.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        V12Avatar(person.name, 42); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.name, color = RefText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(person.title, color = RefTextMuted, fontSize = 10.sp)
                            if (nextLeave != null) Text("İzin: ${nextLeave.startDate} → ${nextLeave.endDate}", color = RefWarning, fontSize = 9.sp)
                        }
                        V12MiniPill("$count randevu", RefBlue)
                    }
                }
            }
            item { V12OutlineButton("Yeni Personel", Icons.Rounded.PersonAdd, onClick = onAdd) }
        }
    }
}

@Composable
private fun V13More(
    profile: BusinessProfile,
    reminderEnabled: Boolean,
    onToggleReminders: (Boolean) -> Unit,
    onNotifications: () -> Unit,
    onBusiness: () -> Unit,
    onServices: () -> Unit,
    onWhatsApp: () -> Unit,
    onReset: () -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = ReferenceDesignContract.pageInsetDp.dp)) {
        V12Header("Daha Fazla")
        V12Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V12Avatar(profile.businessName, 46); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.businessName, color = RefText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(profile.ownerName.ifBlank { "İşletme sahibi" }, color = RefTextMuted, fontSize = 10.sp)
                }
                V12MiniPill("Kurulu", RefSuccess)
            }
        }
        Spacer(Modifier.height(10.dp))
        V12Card {
            V12SettingsToggleRow(Icons.Rounded.Schedule, "Yerel randevu hatırlatmaları", reminderEnabled, onToggleReminders)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.NotificationsNone, "Bildirim Merkezi", onClick = onNotifications)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.Business, "İşletme Bilgileri", onClick = onBusiness)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.DesignServices, "Hizmetler", onClick = onServices)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.Send, "WhatsApp Hatırlatmaları", "Kurulum", onClick = onWhatsApp)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.RestartAlt, "İşletme Kurulumunu Sıfırla", onClick = onReset)
            HorizontalDivider(color = RefBorder.copy(alpha = 0.45f))
            V12SettingsRow(Icons.Rounded.Info, "Uygulama Hakkında", "v1.3.0", onClick = onAbout)
        }
    }
}

private fun v13HasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun v13OpenDialer(context: Context, phone: String) {
    if (phone.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) }
}

private fun v13OpenSms(context: Context, phone: String) {
    if (phone.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}"))) }
}
