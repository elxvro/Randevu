package com.elxvro.randevu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.booking.BookingAction
import com.elxvro.randevu.booking.BookingDraft
import com.elxvro.randevu.booking.BookingReducer

enum class AppScreen {
    Home,
    Business,
    Staff,
    DateTime,
    Summary,
    Confirmed,
    Appointments,
    Profile,
    Admin,
    AdminMenu
}

@Composable
fun RandevuApp() {
    var screenName by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    var booking by remember { mutableStateOf(BookingDraft()) }
    val screen = AppScreen.valueOf(screenName)
    val navigate: (AppScreen) -> Unit = { screenName = it.name }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            when (screen) {
                AppScreen.Home, AppScreen.Appointments, AppScreen.Profile -> CustomerBottomBar(screen, navigate)
                AppScreen.Admin -> AdminBottomBar()
                else -> Unit
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (screen) {
                AppScreen.Home -> HomeScreen(onBusiness = { navigate(AppScreen.Business) })
                AppScreen.Business -> BusinessScreen(
                    onBack = { navigate(AppScreen.Home) },
                    onService = { service ->
                        booking = BookingReducer.reduce(
                            booking,
                            BookingAction.SelectService(service.name, service.price)
                        )
                        navigate(AppScreen.Staff)
                    }
                )
                AppScreen.Staff -> StaffScreen(
                    booking = booking,
                    onBack = { navigate(AppScreen.Business) },
                    onSelect = { staff ->
                        booking = BookingReducer.reduce(
                            booking,
                            BookingAction.SelectStaff(staff.name, staff.title)
                        )
                    },
                    onContinue = { navigate(AppScreen.DateTime) }
                )
                AppScreen.DateTime -> DateTimeScreen(
                    booking = booking,
                    onBack = { navigate(AppScreen.Staff) },
                    onDate = { booking = BookingReducer.reduce(booking, BookingAction.SelectDate(it)) },
                    onTime = { booking = BookingReducer.reduce(booking, BookingAction.SelectTime(it)) },
                    onContinue = { navigate(AppScreen.Summary) }
                )
                AppScreen.Summary -> SummaryScreen(
                    booking = booking,
                    onBack = { navigate(AppScreen.DateTime) },
                    onConfirm = { navigate(AppScreen.Confirmed) }
                )
                AppScreen.Confirmed -> ConfirmedScreen(
                    booking = booking,
                    onAppointments = { navigate(AppScreen.Appointments) },
                    onHome = {
                        booking = BookingReducer.reduce(booking, BookingAction.Reset)
                        navigate(AppScreen.Home)
                    }
                )
                AppScreen.Appointments -> AppointmentsScreen(
                    booking = booking,
                    onHome = { navigate(AppScreen.Home) }
                )
                AppScreen.Profile -> ProfileScreen(
                    onAdmin = { navigate(AppScreen.Admin) }
                )
                AppScreen.Admin -> AdminScreen(
                    onMenu = { navigate(AppScreen.AdminMenu) }
                )
                AppScreen.AdminMenu -> AdminMenuScreen(
                    onClose = { navigate(AppScreen.Admin) }
                )
            }
        }
    }
}

@Composable
private fun CustomerBottomBar(screen: AppScreen, navigate: (AppScreen) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        val items = listOf(
            Triple(AppScreen.Home, "Ana Sayfa", Icons.Rounded.Home),
            Triple(AppScreen.Home, "Keşfet", Icons.Rounded.Search),
            Triple(AppScreen.Appointments, "Randevularım", Icons.Rounded.CalendarMonth),
            Triple(AppScreen.Profile, "Profil", Icons.Rounded.Person)
        )
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = if (index == 1) false else screen == item.first,
                onClick = { navigate(item.first) },
                icon = { Icon(item.third, contentDescription = item.second) },
                label = { Text(item.second, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
private fun AdminBottomBar() {
    NavigationBar(containerColor = Color.White) {
        listOf(
            "Panel" to Icons.Rounded.Home,
            "Randevular" to Icons.Rounded.CalendarMonth,
            "Müşteriler" to Icons.Rounded.People,
            "Daha" to Icons.Rounded.MoreHoriz
        ).forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == 0,
                onClick = {},
                icon = { Icon(item.second, contentDescription = item.first) },
                label = { Text(item.first, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
private fun HomeScreen(onBusiness: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = BrandBlue)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Konum", color = TextSecondary, fontSize = 12.sp)
                    Text("İstanbul", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Notifications, "Bildirimler")
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0E2A5A), BrandBlue, Color(0xFF5C6CFF))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(Modifier.fillMaxWidth(0.78f)) {
                        Text(
                            "Kolay ve Hızlı\nRandevu Al",
                            color = Color.White,
                            fontSize = 29.sp,
                            lineHeight = 33.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Güvenilir işletmelerle zamanından tasarruf et.",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Bugün için yer bul", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text("Hizmet, işletme veya kategori ara...", color = TextSecondary)
                }
            }
        }
        item {
            SectionHeader("Kategoriler", "Tümünü Gör")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf("Kuaför", "Oto Servis", "Sağlık", "Güzellik", "Teknik Servis", "Spor")) { label ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Icon(
                                    Icons.Rounded.Business,
                                    null,
                                    tint = BrandBlue,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Öne Çıkan İşletmeler", "Tümünü Gör")
            Spacer(Modifier.height(10.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBusiness),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF171A24), Color(0xFF293B63), Color(0xFF785B45))
                            )
                        )
                ) {
                    Icon(
                        Icons.Rounded.Business,
                        null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.align(Alignment.Center).size(76.dp)
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Icon(Icons.Rounded.Star, null, tint = WarningOrange, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("4.8 (120)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    Text("Elite Kuaför", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Saç • Sakal • Bakım", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text(" İstanbul, Kadıköy", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("800 m", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessScreen(
    onBack: () -> Unit,
    onService: (DemoService) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF101722), Color(0xFF334A69), Color(0xFF755B48))
                        )
                    )
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(12.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Rounded.ArrowBack, "Geri")
                }
                Icon(
                    Icons.Rounded.Business,
                    null,
                    tint = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.Center).size(100.dp)
                )
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Elite Kuaför", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                            Text(" İstanbul, Kadıköy", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    Surface(color = Color(0xFFFFF6DF), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = WarningOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("4.8", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistChip(onClick = {}, label = { Text("Hizmetler") })
                    AssistChip(onClick = {}, label = { Text("Hakkında") })
                    AssistChip(onClick = {}, label = { Text("Yorumlar") })
                }
                Spacer(Modifier.height(16.dp))
                Text("Hizmetler", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        items(demoServices) { service ->
            ServiceRow(service = service, onClick = { onService(service) })
        }
        item {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onService(demoServices.first()) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Randevu Oluştur", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ServiceRow(service: DemoService, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.List, null, tint = BrandBlue, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold)
                Text("${service.duration} dk", color = TextSecondary, fontSize = 12.sp)
            }
            Text("₺${service.price}", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.ArrowForward, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun StaffScreen(
    booking: BookingDraft,
    onBack: () -> Unit,
    onSelect: (DemoStaff) -> Unit,
    onContinue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader("Uzman Seçin", "Hizmeti gerçekleştirecek personeli seçin.", onBack) }
        items(demoStaff) { staff ->
            val selected = booking.staffName == staff.name
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(staff) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFFEAF1FF) else Color.White
                )
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(staff.initials, if (selected) BrandBlue else Color(0xFF334155))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(staff.name, fontWeight = FontWeight.ExtraBold)
                        Text(staff.title, color = TextSecondary, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                            Text(" ${staff.rating} (${staff.reviews})", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Surface(
                        color = if (selected) BrandBlue else Color.Transparent,
                        shape = CircleShape,
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                    ) {
                        Box(Modifier.size(24.dp)) {
                            if (selected) Icon(
                                Icons.Rounded.CheckCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center).size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onContinue,
                enabled = !booking.staffName.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Devam Et", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DateTimeScreen(
    booking: BookingDraft,
    onBack: () -> Unit,
    onDate: (String) -> Unit,
    onTime: (String) -> Unit,
    onContinue: () -> Unit
) {
    val dates = listOf(
        "20" to "Pzt",
        "21" to "Sal",
        "22" to "Çar",
        "23" to "Per",
        "24" to "Cum",
        "25" to "Cmt"
    )
    val times = listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30", "18:00", "18:30")
    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ScreenHeader("Tarih ve Saat Seçin", "Uygun tarih ve saatleri görüntüleyin.", onBack)
        Spacer(Modifier.height(12.dp))
        Text("Ekim 2026", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates) { item ->
                val fullDate = "${item.first} Ekim 2026"
                val selected = booking.date == fullDate
                Surface(
                    modifier = Modifier.clickable { onDate(fullDate) },
                    color = if (selected) BrandBlue else Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.second, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp)
                        Text(item.first, color = if (selected) Color.White else TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Uygun Saatler", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        times.chunked(3).forEach { rowTimes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTimes.forEach { time ->
                    val selected = booking.time == time
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onTime(time) },
                        color = if (selected) BrandBlue else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                    ) {
                        Text(
                            time,
                            modifier = Modifier.padding(vertical = 11.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (selected) Color.White else TextPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = !booking.date.isNullOrBlank() && !booking.time.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Devam Et", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SummaryScreen(
    booking: BookingDraft,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ScreenHeader("Randevu Özeti", "Seçimlerinizi kontrol edin.", onBack)
        Spacer(Modifier.height(12.dp))
        SummaryInfoCard(Icons.Rounded.Business, "İşletme", booking.businessName, booking.businessLocation)
        SummaryInfoCard(Icons.Rounded.List, "Hizmet", booking.serviceName ?: "-", booking.price?.let { "₺$it" } ?: "-")
        SummaryInfoCard(Icons.Rounded.Person, "Personel", booking.staffName ?: "-", booking.staffTitle ?: "-")
        SummaryInfoCard(Icons.Rounded.CalendarMonth, "Tarih & Saat", booking.date ?: "-", booking.time ?: "-")
        SummaryInfoCard(Icons.Rounded.AccessTime, "Süre", "${booking.durationMinutes} dakika", "")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Not (Opsiyonel)") },
            placeholder = { Text("Randevu ile ilgili notunuzu ekleyin...") },
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onConfirm,
            enabled = booking.isComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Randevu Oluştur", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ConfirmedScreen(
    booking: BookingDraft,
    onAppointments: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(44.dp))
        Surface(color = Color(0xFFE7F8EF), shape = CircleShape) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint = SuccessGreen,
                modifier = Modifier.padding(22.dp).size(64.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Randevunuz Oluşturuldu!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Belirttiğiniz tarih ve saatte sizi bekliyoruz.",
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppBackground)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(booking.businessName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                SimpleInfoRow(Icons.Rounded.List, booking.serviceName ?: "Hizmet")
                SimpleInfoRow(Icons.Rounded.Person, booking.staffName ?: "Personel")
                SimpleInfoRow(Icons.Rounded.CalendarMonth, "${booking.date} • ${booking.time}")
            }
        }
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = onAppointments,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Randevularımı Gör", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Ana Sayfaya Dön", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AppointmentsScreen(booking: BookingDraft, onHome: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("Yaklaşan") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Randevularım", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onHome) { Icon(Icons.Rounded.Home, "Ana Sayfa") }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Yaklaşan", "Geçmiş", "İptal Edilen").forEach { label ->
                    val selected = tab == label
                    Surface(
                        modifier = Modifier.clickable { tab = label },
                        color = if (selected) BrandBlue else Color.White,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = if (selected) Color.White else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        if (tab == "Yaklaşan") {
            if (booking.isComplete) {
                item {
                    AppointmentCard(
                        day = booking.date?.substringBefore(" ") ?: "22",
                        month = "EKİ",
                        service = booking.serviceName ?: "Hizmet",
                        business = booking.businessName,
                        time = booking.time ?: "13:30",
                        status = "Onaylandı"
                    )
                }
            }
            items(demoAppointments) { appointment ->
                AppointmentCard(
                    appointment.day,
                    appointment.month,
                    appointment.service,
                    appointment.business,
                    appointment.time,
                    appointment.status
                )
            }
        } else {
            item {
                EmptyState("Bu bölüm için henüz randevu bulunmuyor.")
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    day: String,
    month: String,
    service: String,
    business: String,
    time: String,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(14.dp)) {
            Surface(color = Color(0xFFEAF1FF), shape = RoundedCornerShape(16.dp)) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(day, color = BrandBlue, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(month, color = BrandBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(service, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    StatusPill(status)
                }
                Text(business, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    Text(" $time", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {}, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                        Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                        Text(" Düzenle", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = {}, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(16.dp), tint = DangerRed)
                        Text(" İptal", fontSize = 11.sp, color = DangerRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(onAdmin: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0F2A57), Color(0xFF174A8E)))
                    )
                    .padding(vertical = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar("DU", BrandBlue, size = 72)
                    Spacer(Modifier.height(10.dp))
                    Text("Demo Kullanıcı", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("demo@randevu.app", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                }
            }
        }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileRow(Icons.Rounded.Person, "Kişisel Bilgiler")
                ProfileRow(Icons.Rounded.CalendarMonth, "Randevu Geçmişi")
                ProfileRow(Icons.Rounded.Favorite, "Favori İşletmelerim")
                ProfileRow(Icons.Rounded.Notifications, "Bildirim Ayarları")
                ProfileRow(Icons.Rounded.Description, "Gizlilik Politikası")
                ProfileRow(Icons.Rounded.Settings, "Ayarlar")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAdmin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Business, null)
                    Spacer(Modifier.width(8.dp))
                    Text("İşletme Paneline Geç", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Icon(Icons.Rounded.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Çıkış Yap")
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(onMenu: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("İşletme Paneli", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Elite Kuaför", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onMenu) {
                    Icon(Icons.Rounded.Menu, "Menü")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("12", "Bugünkü Randevu", BrandBlue, Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                MetricCard("48", "Toplam Randevu", SuccessGreen, Icons.Rounded.CheckCircle, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("15", "Yeni Müşteri", WarningOrange, Icons.Rounded.People, Modifier.weight(1f))
                MetricCard("₺12.450", "Bu Ay Gelir", Color(0xFF7C4DFF), Icons.Rounded.AttachMoney, Modifier.weight(1f))
            }
        }
        item { SectionHeader("Bugünkü Randevular", "Tümünü Gör") }
        items(
            listOf(
                Triple("13:30", "Ahmet Yılmaz", "Saç Kesimi"),
                Triple("14:30", "Mehmet Demir", "Sakal Tıraşı"),
                Triple("15:00", "Ayşe Kaya", "Saç Boyama"),
                Triple("16:30", "Emre Arslan", "Saç Kesimi")
            )
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFEAF1FF), shape = RoundedCornerShape(12.dp)) {
                        Text(item.first, modifier = Modifier.padding(10.dp), color = BrandBlue, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.second, fontWeight = FontWeight.Bold)
                        Text(item.third, color = TextSecondary, fontSize = 12.sp)
                    }
                    StatusPill(if (item.first == "15:00") "Beklemede" else "Onaylandı")
                }
            }
        }
    }
}

@Composable
private fun AdminMenuScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101827))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("İşletme Menüsü", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Kapat", tint = Color.White) }
        }
        Spacer(Modifier.height(18.dp))
        listOf(
            Triple(Icons.Rounded.CalendarMonth, "Randevular", "Takvim ve randevu yönetimi"),
            Triple(Icons.Rounded.List, "Hizmetler", "Hizmet ve fiyatlar"),
            Triple(Icons.Rounded.People, "Personel", "Ekip ve yetkiler"),
            Triple(Icons.Rounded.Schedule, "Çalışma Saatleri", "Mesai ve izin günleri"),
            Triple(Icons.Rounded.Person, "Müşteriler", "Müşteri kayıtları"),
            Triple(Icons.Rounded.AttachMoney, "Gelir Raporları", "Gelir ve performans"),
            Triple(Icons.Rounded.Favorite, "Yorumlar", "Müşteri değerlendirmeleri"),
            Triple(Icons.Rounded.Settings, "Ayarlar", "İşletme ayarları")
        ).forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                color = Color(0xFF182235),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF24314A), shape = RoundedCornerShape(12.dp)) {
                        Icon(item.first, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.second, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(item.third, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                    Icon(Icons.Rounded.ArrowForward, null, tint = Color.White.copy(alpha = 0.55f))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2C44))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar("EK", BrandBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Elite Kuaför", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Pro İşletme Paketi", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.padding(top = 6.dp)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        Text(action, color = BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun SummaryInfoCard(icon: ImageVector, label: String, value: String, supporting: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFEAF1FF), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = BrandBlue, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TextSecondary, fontSize = 11.sp)
                Text(value, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (supporting.isNotBlank()) Text(supporting, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SimpleInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, text: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BrandBlue)
            Spacer(Modifier.width(12.dp))
            Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ArrowForward, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(icon, null, tint = Color.White.copy(alpha = 0.75f))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = if (status == "Onaylandı") SuccessGreen else WarningOrange
    Surface(color = color.copy(alpha = 0.13f), shape = RoundedCornerShape(20.dp)) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Avatar(initials: String, color: Color, size: Int = 50) {
    Surface(color = color, shape = CircleShape) {
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (size > 60) 22.sp else 14.sp)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = DividerColor, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = TextSecondary)
    }
}
