package com.elxvro.randevu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentAction
import com.elxvro.randevu.core.AppointmentEngine
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.CalendarView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CoreScreen {
    HOME,
    APPOINTMENTS,
    NEW_APPOINTMENT,
    PROFILE
}

@Composable
fun RandevuCoreApp() {
    val today = remember { LocalDate.now() }
    val initialAppointments = remember { seedAppointments(today) }
    val appointments = remember { mutableStateListOf<Appointment>().apply { addAll(initialAppointments) } }
    val customers = remember {
        mutableStateListOf("Ayşe Yılmaz", "Mehmet Kaya", "Zeynep Arslan", "Emre Çetin")
    }
    var screenName by rememberSaveable { mutableStateOf(CoreScreen.HOME.name) }
    var editing by remember { mutableStateOf<Appointment?>(null) }
    val screen = CoreScreen.valueOf(screenName)

    fun apply(action: AppointmentAction) {
        val next = AppointmentEngine.reduce(appointments.toList(), action)
        appointments.clear()
        appointments.addAll(next)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (screen != CoreScreen.NEW_APPOINTMENT) {
                CoreBottomBar(
                    selected = screen,
                    onSelect = {
                        editing = null
                        screenName = it.name
                    },
                    onNew = {
                        editing = null
                        screenName = CoreScreen.NEW_APPOINTMENT.name
                    }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                CoreScreen.HOME -> CoreHomeScreen(
                    appointments = appointments,
                    onNew = {
                        editing = null
                        screenName = CoreScreen.NEW_APPOINTMENT.name
                    },
                    onAppointments = { screenName = CoreScreen.APPOINTMENTS.name }
                )

                CoreScreen.APPOINTMENTS -> CoreAppointmentsScreen(
                    appointments = appointments,
                    onEdit = {
                        editing = it
                        screenName = CoreScreen.NEW_APPOINTMENT.name
                    },
                    onDelete = { apply(AppointmentAction.Delete(it.id)) },
                    onStatus = { appointment, status ->
                        apply(AppointmentAction.ChangeStatus(appointment.id, status))
                    },
                    onNew = {
                        editing = null
                        screenName = CoreScreen.NEW_APPOINTMENT.name
                    }
                )

                CoreScreen.NEW_APPOINTMENT -> AppointmentEditorScreen(
                    existing = editing,
                    customers = customers,
                    onCustomerAdded = { name ->
                        if (customers.none { it.equals(name, ignoreCase = true) }) customers.add(name)
                    },
                    onBack = {
                        editing = null
                        screenName = CoreScreen.APPOINTMENTS.name
                    },
                    onSave = { appointment ->
                        apply(
                            if (editing == null) AppointmentAction.Add(appointment)
                            else AppointmentAction.Update(appointment)
                        )
                        editing = null
                        screenName = CoreScreen.APPOINTMENTS.name
                    }
                )

                CoreScreen.PROFILE -> CoreProfileScreen(
                    appointments = appointments,
                    onReset = {
                        appointments.clear()
                        appointments.addAll(seedAppointments(today))
                    }
                )
            }
        }
    }
}

@Composable
private fun CoreBottomBar(
    selected: CoreScreen,
    onSelect: (CoreScreen) -> Unit,
    onNew: () -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = selected == CoreScreen.HOME,
            onClick = { onSelect(CoreScreen.HOME) },
            icon = { Icon(Icons.Rounded.Home, null) },
            label = { Text("Ana Sayfa", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = selected == CoreScreen.APPOINTMENTS,
            onClick = { onSelect(CoreScreen.APPOINTMENTS) },
            icon = { Icon(Icons.Rounded.CalendarMonth, null) },
            label = { Text("Randevular", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onNew,
            icon = {
                Surface(color = BrandBlue, shape = CircleShape) {
                    Icon(
                        Icons.Rounded.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }
            },
            label = { Text("Yeni", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = selected == CoreScreen.PROFILE,
            onClick = { onSelect(CoreScreen.PROFILE) },
            icon = { Icon(Icons.Rounded.Person, null) },
            label = { Text("Profil", fontSize = 10.sp) }
        )
    }
}

@Composable
private fun CoreHomeScreen(
    appointments: List<Appointment>,
    onNew: () -> Unit,
    onAppointments: () -> Unit
) {
    val today = LocalDate.now().toString()
    val todayItems = appointments.filter { it.date == today && it.status != AppointmentStatus.CANCELLED }
    val upcoming = appointments
        .filter { it.date >= today && it.status != AppointmentStatus.CANCELLED }
        .sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })
        .take(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Randevu", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("İşlerinizi tek ekrandan yönetin", color = TextSecondary)
                }
                Surface(color = Color(0xFFE8F0FF), shape = CircleShape) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        null,
                        tint = BrandBlue,
                        modifier = Modifier.padding(13.dp).size(28.dp)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102A56)),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Bugün", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
                    Text(
                        "${todayItems.size} randevu",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Yeni Randevu")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Toplam", appointments.size.toString(), BrandBlue, Modifier.weight(1f))
                MetricCard(
                    "Onaylı",
                    appointments.count { it.status == AppointmentStatus.CONFIRMED }.toString(),
                    SuccessGreen,
                    Modifier.weight(1f)
                )
                MetricCard(
                    "Bekleyen",
                    appointments.count { it.status == AppointmentStatus.PENDING }.toString(),
                    WarningOrange,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Yaklaşan Randevular", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onAppointments) { Text("Tümünü Gör") }
            }
        }

        if (upcoming.isEmpty()) {
            item { EmptyAppointments(onNew) }
        } else {
            items(upcoming, key = { it.id }) { appointment ->
                CompactAppointmentCard(appointment)
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Surface(color = color.copy(alpha = 0.12f), shape = CircleShape) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(9.dp).background(color, CircleShape))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun CompactAppointmentCard(appointment: Appointment) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = BrandBlue.copy(alpha = 0.09f), shape = RoundedCornerShape(14.dp)) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(appointment.time, color = BrandBlue, fontWeight = FontWeight.ExtraBold)
                    Text(appointment.date.takeLast(5), fontSize = 10.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appointment.customer, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(appointment.service, color = TextSecondary, fontSize = 13.sp)
                Text(appointment.staff, color = TextSecondary, fontSize = 11.sp)
            }
            StatusBadge(appointment.status)
        }
    }
}

@Composable
private fun CoreAppointmentsScreen(
    appointments: List<Appointment>,
    onEdit: (Appointment) -> Unit,
    onDelete: (Appointment) -> Unit,
    onStatus: (Appointment, AppointmentStatus) -> Unit,
    onNew: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusName by rememberSaveable { mutableStateOf("ALL") }
    var viewName by rememberSaveable { mutableStateOf(CalendarView.DAY.name) }
    var anchorDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

    val status = if (statusName == "ALL") null else AppointmentStatus.valueOf(statusName)
    val view = CalendarView.valueOf(viewName)
    val scoped = AppointmentEngine.forView(appointments, view, anchorDate)
    val visible = AppointmentEngine.filter(scoped, query, status)
    val anchor = LocalDate.parse(anchorDate)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Randevular", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Ara, filtrele, düzenle ve durum değiştir", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onNew) {
                    Surface(color = BrandBlue, shape = CircleShape) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.padding(9.dp))
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                label = { Text("Müşteri, hizmet veya personel ara") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = status == null,
                        onClick = { statusName = "ALL" },
                        label = { Text("Tümü") }
                    )
                }
                items(AppointmentStatus.entries) { item ->
                    FilterChip(
                        selected = status == item,
                        onClick = { statusName = item.name },
                        label = { Text(item.label) }
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = view == CalendarView.DAY,
                            onClick = { viewName = CalendarView.DAY.name },
                            label = { Text("Günlük") }
                        )
                        FilterChip(
                            selected = view == CalendarView.WEEK,
                            onClick = { viewName = CalendarView.WEEK.name },
                            label = { Text("Haftalık") }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            anchorDate = anchor.minusDays(if (view == CalendarView.DAY) 1 else 7).toString()
                        }) { Icon(Icons.Rounded.ArrowBack, "Önceki") }
                        Text(
                            if (view == CalendarView.DAY) formatDate(anchor)
                            else weekLabel(anchor),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            anchorDate = anchor.plusDays(if (view == CalendarView.DAY) 1 else 7).toString()
                        }) { Icon(Icons.Rounded.ArrowForward, "Sonraki") }
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item { EmptyAppointments(onNew) }
        } else {
            items(visible, key = { it.id }) { appointment ->
                EditableAppointmentCard(
                    appointment = appointment,
                    onEdit = { onEdit(appointment) },
                    onDelete = { onDelete(appointment) },
                    onStatus = { onStatus(appointment, it) }
                )
            }
        }
    }
}

@Composable
private fun EditableAppointmentCard(
    appointment: Appointment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatus: (AppointmentStatus) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = BrandBlue.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Schedule, null, tint = BrandBlue, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(appointment.customer, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${appointment.date} • ${appointment.time}", color = TextSecondary, fontSize = 12.sp)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "Durum") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        AppointmentStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                leadingIcon = {
                                    if (status == appointment.status) Icon(Icons.Rounded.CheckCircle, null, tint = statusColor(status))
                                },
                                onClick = {
                                    menuOpen = false
                                    onStatus(status)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(appointment.service, fontWeight = FontWeight.Bold)
            Text(appointment.staff, color = TextSecondary, fontSize = 13.sp)
            if (appointment.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(appointment.note, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(appointment.status)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Düzenle", tint = BrandBlue) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = DangerRed) }
            }
        }
    }
}

@Composable
private fun AppointmentEditorScreen(
    existing: Appointment?,
    customers: List<String>,
    onCustomerAdded: (String) -> Unit,
    onBack: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    val services = remember { listOf("Erkek Saç Kesimi", "Sakal Tıraşı", "Saç + Sakal", "Saç Boyama", "Cilt Bakımı") }
    val staff = remember { listOf("Ahmet Demir", "Mehmet Kaya", "Emre Yılmaz", "Burak Arslan") }
    val dateOptions = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()) } }
    val timeOptions = remember {
        listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30")
    }

    var customer by remember(existing?.id) { mutableStateOf(existing?.customer.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var newCustomer by remember(existing?.id) { mutableStateOf("") }
    var service by remember(existing?.id) { mutableStateOf(existing?.service.orEmpty()) }
    var selectedStaff by remember(existing?.id) { mutableStateOf(existing?.staff.orEmpty()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var statusName by remember(existing?.id) { mutableStateOf((existing?.status ?: AppointmentStatus.PENDING).name) }

    val canSave = customer.isNotBlank() && service.isNotBlank() && selectedStaff.isNotBlank() && date.isNotBlank() && time.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }
                Column {
                    Text(if (existing == null) "Yeni Randevu" else "Randevuyu Düzenle", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tüm alanlar anında kullanılabilir", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        item { EditorSectionTitle("1. Müşteri") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customers) { name ->
                    FilterChip(
                        selected = customer == name,
                        onClick = { customer = name },
                        label = { Text(name) },
                        leadingIcon = { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hızlı müşteri ekle", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newCustomer,
                        onValueChange = { newCustomer = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val clean = newCustomer.trim()
                            if (clean.isNotEmpty()) {
                                onCustomerAdded(clean)
                                customer = clean
                                newCustomer = ""
                            }
                        },
                        enabled = newCustomer.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Müşteriyi Ekle ve Seç")
                    }
                }
            }
        }

        item { EditorSectionTitle("2. Hizmet") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(services) { item ->
                    FilterChip(selected = service == item, onClick = { service = item }, label = { Text(item) })
                }
            }
        }

        item { EditorSectionTitle("3. Personel") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(staff) { item ->
                    FilterChip(
                        selected = selectedStaff == item,
                        onClick = { selectedStaff = item },
                        label = { Text(item) },
                        leadingIcon = { Icon(Icons.Rounded.People, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        item { EditorSectionTitle("4. Tarih") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dateOptions) { item ->
                    FilterChip(
                        selected = date == item.toString(),
                        onClick = { date = item.toString() },
                        label = { Text(shortDate(item)) },
                        leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        item { EditorSectionTitle("5. Saat") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(timeOptions) { item ->
                    FilterChip(
                        selected = time == item,
                        onClick = { time = item },
                        label = { Text(item) },
                        leadingIcon = { Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        item { EditorSectionTitle("6. Durum ve Not") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppointmentStatus.entries) { item ->
                    FilterChip(
                        selected = statusName == item.name,
                        onClick = { statusName = item.name },
                        label = { Text(item.label) }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            Button(
                onClick = {
                    onSave(
                        Appointment(
                            id = existing?.id ?: "r-${System.currentTimeMillis()}",
                            customer = customer,
                            phone = phone,
                            service = service,
                            staff = selectedStaff,
                            date = date,
                            time = time,
                            status = AppointmentStatus.valueOf(statusName),
                            note = note.trim()
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text(if (existing == null) "Randevuyu Oluştur" else "Değişiklikleri Kaydet", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoreProfileScreen(appointments: List<Appointment>, onReset: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Profil & Uygulama", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Yerel çalışma sürümü", color = TextSecondary)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = BrandBlue.copy(alpha = 0.12f), shape = CircleShape) {
                        Icon(Icons.Rounded.Person, null, tint = BrandBlue, modifier = Modifier.padding(18.dp).size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("İşletme Yöneticisi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Randevu v0.2.0 • Core Booking", color = TextSecondary)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bu sürümde aktif", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    FeatureLine("Randevu ekleme, düzenleme ve silme")
                    FeatureLine("Müşteri hızlı ekleme ve seçme")
                    FeatureLine("Hizmet, personel, tarih ve saat seçimi")
                    FeatureLine("Durum yönetimi")
                    FeatureLine("Günlük / haftalık görünüm")
                    FeatureLine("Arama ve filtreleme")
                }
            }
        }
        item {
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Demo Verilerini Yenile")
            }
            Text("Toplam ${appointments.size} yerel randevu", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun EditorSectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
}

@Composable
private fun EmptyAppointments(onNew: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = BrandBlue, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(10.dp))
            Text("Bu görünümde randevu yok", fontWeight = FontWeight.Bold)
            Text("Yeni bir randevu oluşturabilirsiniz.", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNew) { Text("Yeni Randevu") }
        }
    }
}

@Composable
private fun StatusBadge(status: AppointmentStatus) {
    val color = statusColor(status)
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
        Text(
            status.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

private fun statusColor(status: AppointmentStatus): Color = when (status) {
    AppointmentStatus.PENDING -> WarningOrange
    AppointmentStatus.CONFIRMED -> BrandBlue
    AppointmentStatus.COMPLETED -> SuccessGreen
    AppointmentStatus.CANCELLED -> DangerRed
}

private fun shortDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("tr", "TR"))
    return date.format(formatter)
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
    return date.format(formatter)
}

private fun weekLabel(anchor: LocalDate): String {
    val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val end = start.plusDays(6)
    return "${shortDate(start)} – ${shortDate(end)}"
}

private fun seedAppointments(today: LocalDate): List<Appointment> = listOf(
    Appointment(
        id = "demo-1",
        customer = "Ayşe Yılmaz",
        phone = "0555 123 45 67",
        service = "Erkek Saç Kesimi",
        staff = "Ahmet Demir",
        date = today.toString(),
        time = "10:30",
        status = AppointmentStatus.CONFIRMED,
        note = "Saç modeli kısa olsun"
    ),
    Appointment(
        id = "demo-2",
        customer = "Mehmet Kaya",
        phone = "0555 222 11 00",
        service = "Saç + Sakal",
        staff = "Mehmet Kaya",
        date = today.toString(),
        time = "14:00",
        status = AppointmentStatus.PENDING,
        note = ""
    ),
    Appointment(
        id = "demo-3",
        customer = "Zeynep Arslan",
        phone = "0555 333 22 11",
        service = "Cilt Bakımı",
        staff = "Emre Yılmaz",
        date = today.plusDays(2).toString(),
        time = "15:30",
        status = AppointmentStatus.CONFIRMED,
        note = "İlk ziyaret"
    ),
    Appointment(
        id = "demo-4",
        customer = "Emre Çetin",
        phone = "0555 444 33 22",
        service = "Saç Boyama",
        staff = "Burak Arslan",
        date = today.plusDays(4).toString(),
        time = "11:00",
        status = AppointmentStatus.PENDING,
        note = ""
    )
)
