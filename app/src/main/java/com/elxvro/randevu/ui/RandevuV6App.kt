package com.elxvro.randevu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elxvro.randevu.core.ApiMode
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentAction
import com.elxvro.randevu.core.AppointmentEngine
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.BackendAction
import com.elxvro.randevu.core.BackendEngine
import com.elxvro.randevu.core.BackendState
import com.elxvro.randevu.core.ConnectionState
import com.elxvro.randevu.core.LiveSyncAction
import com.elxvro.randevu.core.LiveSyncEngine
import com.elxvro.randevu.core.LiveSyncState
import com.elxvro.randevu.network.ApiClient
import com.elxvro.randevu.network.LiveApiClient
import com.elxvro.randevu.notifications.AppointmentNotifier
import com.elxvro.randevu.storage.LiveSyncStore
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.launch

private enum class LiveTab(val label: String) {
    DASHBOARD("Özet"),
    HISTORY("Geçmiş"),
    STAFF("Personel"),
    QUEUE("Sıra")
}

@Composable
fun RandevuV6App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LiveSyncStore(context.applicationContext) }
    var backendState by remember { mutableStateOf(store.loadBackendState()) }
    var syncState by remember { mutableStateOf(store.loadSyncState()) }
    val cachedAppointments = remember { store.loadAppointments() }
    val appointments = remember {
        androidx.compose.runtime.mutableStateListOf<Appointment>().apply {
            addAll(if (cachedAppointments.isEmpty()) liveDemoAppointments() else cachedAppointments)
        }
    }
    var showBackend by rememberSaveable { mutableStateOf(false) }
    var showSync by rememberSaveable { mutableStateOf(false) }

    fun replaceAppointments(next: List<Appointment>) {
        appointments.clear()
        appointments.addAll(next)
    }

    fun applyLocal(action: AppointmentAction) {
        replaceAppointments(AppointmentEngine.reduce(appointments.toList(), action))
    }

    fun markFailure(message: String) {
        syncState = LiveSyncEngine.reduce(syncState, LiveSyncAction.SyncFailed(message))
    }

    fun refreshRemote() {
        if (backendState.config.mode == ApiMode.DEMO) {
            syncState = LiveSyncEngine.reduce(
                syncState,
                LiveSyncAction.SyncCompleted(LocalDateTime.now().withNano(0).toString())
            )
            return
        }
        if (!backendState.session.authenticated) {
            markFailure("Önce PHP API oturumu açılmalı")
            return
        }
        scope.launch {
            syncState = LiveSyncEngine.reduce(syncState, LiveSyncAction.SyncStarted)
            val result = LiveApiClient.listAppointments(
                backendState.config.normalizedBaseUrl(),
                backendState.session.token
            )
            if (result.ok && result.value != null) {
                replaceAppointments(result.value)
                syncState = LiveSyncEngine.reduce(
                    syncState,
                    LiveSyncAction.SyncCompleted(LocalDateTime.now().withNano(0).toString())
                )
            } else {
                markFailure(result.message)
            }
        }
    }

    fun syncNow() {
        if (backendState.config.mode == ApiMode.DEMO) {
            syncState = LiveSyncEngine.reduce(
                syncState,
                LiveSyncAction.SyncCompleted(LocalDateTime.now().withNano(0).toString())
            )
            return
        }
        if (!backendState.session.authenticated) {
            markFailure("Bekleyen kayıtları göndermek için API oturumu aç")
            return
        }
        scope.launch {
            var working = LiveSyncEngine.reduce(syncState, LiveSyncAction.SyncStarted)
            syncState = working
            for (operation in working.pending.toList()) {
                val result = LiveApiClient.syncOperation(
                    backendState.config.normalizedBaseUrl(),
                    backendState.session.token,
                    operation
                )
                if (!result.ok) {
                    syncState = LiveSyncEngine.reduce(working, LiveSyncAction.SyncFailed(result.message))
                    return@launch
                }
                working = LiveSyncEngine.reduce(working, LiveSyncAction.OperationSynced(operation.operationId))
                syncState = working
            }
            val remote = LiveApiClient.listAppointments(
                backendState.config.normalizedBaseUrl(),
                backendState.session.token
            )
            if (remote.ok && remote.value != null) {
                replaceAppointments(remote.value)
                syncState = LiveSyncEngine.reduce(
                    working,
                    LiveSyncAction.SyncCompleted(LocalDateTime.now().withNano(0).toString())
                )
            } else {
                syncState = LiveSyncEngine.reduce(working, LiveSyncAction.SyncFailed(remote.message))
            }
        }
    }

    LaunchedEffect(syncState.pending, syncState.lastSyncAt, syncState.nextSequence) {
        store.saveSyncState(syncState)
    }
    LaunchedEffect(appointments.toList()) {
        store.saveAppointments(appointments.toList())
        val reminders = LiveSyncEngine.reminderCandidates(
            appointments = appointments.toList(),
            nowIso = LocalDateTime.now().withSecond(0).withNano(0).toString(),
            withinMinutes = 60
        )
        AppointmentNotifier.notifyUpcoming(context, reminders)
    }
    LaunchedEffect(backendState) {
        store.saveBackendState(backendState)
    }

    RandevuTheme {
        Box(Modifier.fillMaxSize()) {
            RandevuV4App()
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackendChipV6(backendState) { showBackend = true }
                SyncChipV6(syncState) { showSync = true }
            }
        }

        if (showBackend) {
            BackendSettingsV6(
                state = backendState,
                onDismiss = { showBackend = false },
                onState = { backendState = it },
                onTest = { candidate ->
                    backendState = candidate
                    if (candidate.config.mode == ApiMode.DEMO) {
                        backendState = BackendEngine.reduce(
                            candidate,
                            BackendAction.ConnectionSucceeded("Yerel Demo")
                        )
                    } else if (!candidate.config.isRemoteConfigurationValid()) {
                        backendState = BackendEngine.reduce(
                            candidate,
                            BackendAction.ConnectionFailed("Sunucu adresi https:// ile başlamalı")
                        )
                    } else {
                        backendState = BackendEngine.reduce(candidate, BackendAction.CheckConnection)
                        scope.launch {
                            val result = ApiClient.checkHealth(candidate.config.normalizedBaseUrl())
                            backendState = if (result.ok) {
                                BackendEngine.reduce(
                                    backendState,
                                    BackendAction.ConnectionSucceeded(result.serverName)
                                )
                            } else {
                                BackendEngine.reduce(
                                    backendState,
                                    BackendAction.ConnectionFailed(result.message)
                                )
                            }
                        }
                    }
                }
            )
        }

        if (showSync) {
            LiveSyncCenter(
                backendState = backendState,
                syncState = syncState,
                appointments = appointments,
                onDismiss = { showSync = false },
                onRemoteLogin = { name, phone ->
                    if (backendState.config.mode != ApiMode.REMOTE || !backendState.config.isRemoteConfigurationValid()) {
                        markFailure("Önce geçerli PHP API adresini ayarla")
                    } else {
                        scope.launch {
                            syncState = LiveSyncEngine.reduce(syncState, LiveSyncAction.SyncStarted)
                            val result = LiveApiClient.login(
                                backendState.config.normalizedBaseUrl(),
                                name,
                                phone
                            )
                            if (result.ok && result.value != null) {
                                backendState = backendState.copy(
                                    connectionState = ConnectionState.CONNECTED,
                                    serverName = backendState.serverName.ifBlank { "Randevu API" },
                                    errorMessage = null,
                                    session = result.value
                                )
                                syncState = LiveSyncEngine.reduce(
                                    syncState,
                                    LiveSyncAction.SyncCompleted(LocalDateTime.now().withNano(0).toString())
                                )
                                refreshRemote()
                            } else {
                                markFailure(result.message)
                            }
                        }
                    }
                },
                onRemoteLogout = {
                    backendState = BackendEngine.reduce(backendState, BackendAction.SignOut)
                    store.clearSession()
                },
                onRefresh = ::refreshRemote,
                onSync = ::syncNow,
                onStatus = { appointment, status ->
                    applyLocal(AppointmentAction.ChangeStatus(appointment.id, status))
                    if (backendState.config.mode == ApiMode.REMOTE) {
                        if (!backendState.session.authenticated) {
                            syncState = LiveSyncEngine.reduce(
                                syncState,
                                LiveSyncAction.QueueStatus(appointment.id, status)
                            )
                        } else {
                            scope.launch {
                                val result = LiveApiClient.updateStatus(
                                    backendState.config.normalizedBaseUrl(),
                                    backendState.session.token,
                                    appointment.id,
                                    status
                                )
                                if (!result.ok) {
                                    syncState = LiveSyncEngine.reduce(
                                        syncState,
                                        LiveSyncAction.QueueStatus(appointment.id, status)
                                    )
                                    markFailure("${result.message} • değişiklik çevrimdışı sıraya alındı")
                                }
                            }
                        }
                    }
                },
                onCreate = { appointment ->
                    applyLocal(AppointmentAction.Add(appointment))
                    if (backendState.config.mode == ApiMode.REMOTE) {
                        if (!backendState.session.authenticated) {
                            syncState = LiveSyncEngine.reduce(
                                syncState,
                                LiveSyncAction.QueueCreate(appointment)
                            )
                        } else {
                            scope.launch {
                                val result = LiveApiClient.createAppointment(
                                    backendState.config.normalizedBaseUrl(),
                                    backendState.session.token,
                                    appointment
                                )
                                if (result.ok && result.value != null) {
                                    applyLocal(AppointmentAction.Delete(appointment.id))
                                    applyLocal(AppointmentAction.Add(result.value))
                                } else {
                                    syncState = LiveSyncEngine.reduce(
                                        syncState,
                                        LiveSyncAction.QueueCreate(appointment)
                                    )
                                    markFailure("${result.message} • kayıt çevrimdışı sıraya alındı")
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BackendChipV6(state: BackendState, onClick: () -> Unit) {
    val connected = state.connectionState == ConnectionState.CONNECTED
    AssistChip(
        onClick = onClick,
        label = { Text(if (state.config.mode == ApiMode.DEMO) "Demo" else if (connected) "API" else "Sunucu") },
        leadingIcon = {
            Icon(
                if (connected) Icons.Rounded.CloudDone else Icons.Rounded.Dns,
                contentDescription = null,
                tint = if (connected) SuccessGreen else BrandBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun SyncChipV6(state: LiveSyncState, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(if (state.pending.isEmpty()) "Senkron" else "${state.pending.size} bekliyor") },
        leadingIcon = {
            if (state.syncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (state.errorMessage == null) Icons.Rounded.Sync else Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = if (state.errorMessage == null) BrandBlue else DangerRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

@Composable
private fun BackendSettingsV6(
    state: BackendState,
    onDismiss: () -> Unit,
    onState: (BackendState) -> Unit,
    onTest: (BackendState) -> Unit
) {
    var url by rememberSaveable(state.config.baseUrl) { mutableStateOf(state.config.baseUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Veri Bağlantısı", fontWeight = FontWeight.ExtraBold)
                Text("v0.6.0 • Live Sync", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Demo modunda cihaz içi veri kullanılır. PHP API modu HTTPS sunucu ile gerçek senkronizasyon açar.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.config.mode == ApiMode.DEMO) {
                        FilledTonalButton(onClick = {}) { Text("Demo") }
                    } else {
                        OutlinedButton(onClick = {
                            onState(BackendEngine.reduce(state, BackendAction.SetMode(ApiMode.DEMO)))
                        }) { Text("Demo") }
                    }
                    if (state.config.mode == ApiMode.REMOTE) {
                        FilledTonalButton(onClick = {}) { Text("PHP API") }
                    } else {
                        OutlinedButton(onClick = {
                            onState(BackendEngine.reduce(state, BackendAction.SetMode(ApiMode.REMOTE)))
                        }) { Text("PHP API") }
                    }
                }
                if (state.config.mode == ApiMode.REMOTE) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            onState(BackendEngine.reduce(state, BackendAction.SetBaseUrl(it)))
                        },
                        label = { Text("API adresi") },
                        placeholder = { Text("https://site.com/api") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.connectionState == ConnectionState.ERROR) {
                            MaterialTheme.colorScheme.errorContainer
                        } else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            when (state.connectionState) {
                                ConnectionState.CONNECTED -> Icons.Rounded.CloudDone
                                ConnectionState.ERROR -> Icons.Rounded.CloudOff
                                else -> Icons.Rounded.CloudSync
                            },
                            contentDescription = null
                        )
                        Column {
                            Text(
                                when (state.connectionState) {
                                    ConnectionState.CONNECTED -> "Bağlantı hazır"
                                    ConnectionState.CHECKING -> "Kontrol ediliyor"
                                    ConnectionState.ERROR -> "Bağlantı hatası"
                                    ConnectionState.IDLE -> "Kontrol bekleniyor"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                state.errorMessage ?: state.serverName.ifBlank { "Sunucu ayarlarını kontrol et" },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        val candidate = BackendEngine.reduce(state, BackendAction.SetBaseUrl(url))
                        onTest(candidate)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.connectionState != ConnectionState.CHECKING
                ) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Bağlantıyı Test Et")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun LiveSyncCenter(
    backendState: BackendState,
    syncState: LiveSyncState,
    appointments: SnapshotStateList<Appointment>,
    onDismiss: () -> Unit,
    onRemoteLogin: (String, String) -> Unit,
    onRemoteLogout: () -> Unit,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onStatus: (Appointment, AppointmentStatus) -> Unit,
    onCreate: (Appointment) -> Unit
) {
    var tabName by rememberSaveable { mutableStateOf(LiveTab.DASHBOARD.name) }
    var loginName by rememberSaveable { mutableStateOf(backendState.session.displayName) }
    var loginPhone by rememberSaveable { mutableStateOf(backendState.session.phone) }
    var historyQuery by rememberSaveable { mutableStateOf("") }
    var staffQuery by rememberSaveable { mutableStateOf("Elif") }
    var staffDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var customer by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var service by rememberSaveable { mutableStateOf("Saç Kesimi") }
    var staff by rememberSaveable { mutableStateOf("Elif") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var time by rememberSaveable { mutableStateOf("10:00") }
    val tab = LiveTab.valueOf(tabName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(
                            Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Canlı Senkron", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(
                            if (backendState.config.mode == ApiMode.DEMO) "Yerel demo verisi" else "PHP API • çevrimdışı kuyruk aktif",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Kapat") }
                }

                if (backendState.config.mode == ApiMode.REMOTE && !backendState.session.authenticated) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("API Oturumu", fontWeight = FontWeight.Bold)
                            Text("İlk girişte işletme hesabı sunucuda oluşturulabilir.", fontSize = 12.sp)
                            OutlinedTextField(
                                value = loginName,
                                onValueChange = { loginName = it },
                                label = { Text("Ad / işletme yetkilisi") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = loginPhone,
                                onValueChange = { loginPhone = it.take(18) },
                                label = { Text("Telefon") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { onRemoteLogin(loginName, loginPhone) },
                                enabled = loginName.trim().length >= 2 && loginPhone.filter(Char::isDigit).length >= 10,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Login, null)
                                Spacer(Modifier.width(8.dp))
                                Text("API'ye Giriş")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                } else if (backendState.config.mode == ApiMode.REMOTE) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = SuccessGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(backendState.session.displayName.ifBlank { "API oturumu" }, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        TextButton(onClick = onRemoteLogout) { Text("Oturumu Kapat") }
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LiveTab.entries) { item ->
                        FilterChip(
                            selected = tab == item,
                            onClick = { tabName = item.name },
                            label = { Text(item.label) },
                            leadingIcon = if (tab == item) {
                                { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (syncState.errorMessage != null) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(
                                    syncState.errorMessage,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    when (tab) {
                        LiveTab.DASHBOARD -> {
                            val stats = LiveSyncEngine.dashboard(appointments, LocalDate.now().toString())
                            val upcoming = LiveSyncEngine.reminderCandidates(
                                appointments,
                                LocalDateTime.now().withSecond(0).withNano(0).toString(),
                                60
                            )
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LiveMetric("Toplam", stats.total, BrandBlue, Modifier.weight(1f))
                                    LiveMetric("Bugün", stats.today, SuccessGreen, Modifier.weight(1f))
                                    LiveMetric("Bekleyen", stats.pending, WarningOrange, Modifier.weight(1f))
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.Refresh, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Yenile")
                                    }
                                    Button(onClick = onSync, modifier = Modifier.weight(1f), enabled = !syncState.syncing) {
                                        if (syncState.syncing) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                                        else Icon(Icons.Rounded.Sync, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Senkron")
                                    }
                                }
                            }
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text("Yaklaşan hatırlatmalar", fontWeight = FontWeight.Bold)
                                        Text(
                                            if (upcoming.isEmpty()) "Önümüzdeki 60 dakikada onaylı randevu yok."
                                            else "${upcoming.size} müşteriye hatırlatma hazır.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                            item {
                                FilledTonalButton(onClick = { adding = !adding }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (adding) "Hızlı kaydı kapat" else "Hızlı randevu ekle")
                                }
                            }
                            if (adding) {
                                item {
                                    Card {
                                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Hızlı Randevu", fontWeight = FontWeight.Bold)
                                            OutlinedTextField(customer, { customer = it }, label = { Text("Müşteri") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                            OutlinedTextField(phone, { phone = it.take(18) }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                            OutlinedTextField(service, { service = it }, label = { Text("Hizmet") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                            OutlinedTextField(staff, { staff = it }, label = { Text("Personel") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(date, { date = it }, label = { Text("Tarih") }, modifier = Modifier.weight(1f), singleLine = true)
                                                OutlinedTextField(time, { time = it }, label = { Text("Saat") }, modifier = Modifier.weight(1f), singleLine = true)
                                            }
                                            Button(
                                                onClick = {
                                                    onCreate(
                                                        Appointment(
                                                            id = "local-${System.currentTimeMillis()}",
                                                            customer = customer.trim(),
                                                            phone = phone.trim(),
                                                            service = service.trim(),
                                                            staff = staff.trim(),
                                                            date = date.trim(),
                                                            time = time.trim(),
                                                            status = AppointmentStatus.PENDING,
                                                            note = ""
                                                        )
                                                    )
                                                    customer = ""
                                                    phone = ""
                                                    adding = false
                                                },
                                                enabled = customer.trim().length >= 2 && phone.filter(Char::isDigit).length >= 10,
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text("Randevuyu Kaydet") }
                                        }
                                    }
                                }
                            }
                            item { Text("Son randevular", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
                            items(appointments.take(8), key = { it.id }) { appointment ->
                                AppointmentLiveCard(appointment, onStatus)
                            }
                        }

                        LiveTab.HISTORY -> {
                            item {
                                OutlinedTextField(
                                    value = historyQuery,
                                    onValueChange = { historyQuery = it },
                                    label = { Text("Müşteri adı veya telefon") },
                                    leadingIcon = { Icon(Icons.Rounded.History, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            val history = LiveSyncEngine.customerHistory(appointments, historyQuery)
                            if (historyQuery.isBlank()) {
                                item { EmptyLiveCard("Müşteri geçmişi için arama yap.") }
                            } else if (history.isEmpty()) {
                                item { EmptyLiveCard("Bu müşteriye ait kayıt bulunamadı.") }
                            } else {
                                items(history, key = { it.id }) { AppointmentLiveCard(it, onStatus) }
                            }
                        }

                        LiveTab.STAFF -> {
                            item {
                                OutlinedTextField(
                                    value = staffQuery,
                                    onValueChange = { staffQuery = it },
                                    label = { Text("Personel") },
                                    leadingIcon = { Icon(Icons.Rounded.People, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = staffDate,
                                    onValueChange = { staffDate = it },
                                    label = { Text("Tarih (YYYY-MM-DD)") },
                                    leadingIcon = { Icon(Icons.Rounded.Schedule, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            val calendar = LiveSyncEngine.staffCalendar(appointments, staffQuery, staffDate)
                            if (calendar.isEmpty()) item { EmptyLiveCard("Bu personel ve tarih için aktif randevu yok.") }
                            else items(calendar, key = { it.id }) { AppointmentLiveCard(it, onStatus) }
                        }

                        LiveTab.QUEUE -> {
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text("Çevrimdışı kuyruk", fontWeight = FontWeight.ExtraBold)
                                        Text("Bekleyen işlem: ${syncState.pending.size}")
                                        Text(
                                            "Son senkron: ${syncState.lastSyncAt ?: "Henüz yok"}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                            if (syncState.pending.isEmpty()) {
                                item { EmptyLiveCard("Gönderilmeyi bekleyen değişiklik yok.") }
                            } else {
                                items(syncState.pending, key = { it.operationId }) { operation ->
                                    Card {
                                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                            Text(operation.type.name, fontWeight = FontWeight.Bold)
                                            Text("Randevu: ${operation.appointmentId}", fontSize = 12.sp)
                                            operation.status?.let { Text("Durum: ${it.label}", fontSize = 12.sp) }
                                        }
                                    }
                                }
                                item {
                                    Button(onClick = onSync, modifier = Modifier.fillMaxWidth(), enabled = !syncState.syncing) {
                                        Icon(Icons.Rounded.Sync, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Bekleyenleri Gönder")
                                    }
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
private fun LiveMetric(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
    }
}

@Composable
private fun AppointmentLiveCard(
    appointment: Appointment,
    onStatus: (Appointment, AppointmentStatus) -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(appointment.customer, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${appointment.service} • ${appointment.staff}", fontSize = 12.sp)
                }
                Text(appointment.status.label, fontSize = 11.sp, color = statusColor(appointment.status), fontWeight = FontWeight.Bold)
            }
            Text("${appointment.date} • ${appointment.time} • ${appointment.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (appointment.status) {
                    AppointmentStatus.PENDING -> FilledTonalButton(onClick = { onStatus(appointment, AppointmentStatus.CONFIRMED) }) { Text("Onayla") }
                    AppointmentStatus.CONFIRMED -> FilledTonalButton(onClick = { onStatus(appointment, AppointmentStatus.COMPLETED) }) { Text("Tamamla") }
                    AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED -> Unit
                }
                if (appointment.status != AppointmentStatus.CANCELLED && appointment.status != AppointmentStatus.COMPLETED) {
                    OutlinedButton(onClick = { onStatus(appointment, AppointmentStatus.CANCELLED) }) { Text("İptal") }
                }
            }
        }
    }
}

@Composable
private fun EmptyLiveCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

private fun statusColor(status: AppointmentStatus): Color = when (status) {
    AppointmentStatus.PENDING -> WarningOrange
    AppointmentStatus.CONFIRMED -> SuccessGreen
    AppointmentStatus.COMPLETED -> BrandBlue
    AppointmentStatus.CANCELLED -> DangerRed
}

private fun liveDemoAppointments(): List<Appointment> {
    val today = LocalDate.now()
    return listOf(
        Appointment(
            id = "demo-1",
            customer = "Ayşe Yılmaz",
            phone = "05551234567",
            service = "Saç Kesimi",
            staff = "Elif",
            date = today.toString(),
            time = LocalDateTime.now().plusMinutes(45).toLocalTime().withSecond(0).withNano(0).toString().take(5),
            status = AppointmentStatus.CONFIRMED,
            note = ""
        ),
        Appointment(
            id = "demo-2",
            customer = "Mehmet Kaya",
            phone = "05329876543",
            service = "Cilt Bakımı",
            staff = "Mert",
            date = today.plusDays(1).toString(),
            time = "11:30",
            status = AppointmentStatus.PENDING,
            note = ""
        ),
        Appointment(
            id = "demo-3",
            customer = "Zeynep Arslan",
            phone = "05441231212",
            service = "Saç Boyama",
            staff = "Elif",
            date = today.minusDays(2).toString(),
            time = "15:00",
            status = AppointmentStatus.COMPLETED,
            note = "Düzenli müşteri"
        )
    )
}
