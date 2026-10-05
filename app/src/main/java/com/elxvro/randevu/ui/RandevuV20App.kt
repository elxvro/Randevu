package com.elxvro.randevu.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentAction
import com.elxvro.randevu.online.*
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import com.elxvro.randevu.storage.LiveSyncStore
import com.elxvro.randevu.storage.V2OnlineStore
import com.elxvro.randevu.storage.V2SessionStore
import kotlinx.coroutines.launch

@Composable
fun RandevuV20App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val legacyStore = remember { LiveSyncStore(context.applicationContext) }
    val sessionStore = remember { V2SessionStore(context.applicationContext) }

    var baseUrl by remember { mutableStateOf(sessionStore.loadBaseUrl()) }
    var session by remember { mutableStateOf(sessionStore.loadSession()) }
    var token by remember { mutableStateOf(sessionStore.loadToken()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var connectionState by remember {
        mutableStateOf(if (token.isNullOrBlank()) OnlineConnectionState.AUTH_REQUIRED else OnlineConnectionState.OFFLINE)
    }
    var pendingCount by remember { mutableIntStateOf(0) }
    var showMigration by remember { mutableStateOf(false) }
    var cacheRevision by remember { mutableIntStateOf(0) }

    val ownerName = session?.ownerName.orEmpty()
    val onlineStore = remember(ownerName) {
        V2OnlineStore(context.applicationContext, legacyStore, ownerName)
    }
    val api = remember(baseUrl) {
        runCatching { if (baseUrl.isBlank()) null else V2ApiClient(baseUrl) }.getOrNull()
    }
    val coordinator = remember(api, token, onlineStore) {
        val currentToken = token
        if (api == null || currentToken.isNullOrBlank()) null else V2SyncCoordinator(currentToken, api, onlineStore)
    }

    fun hasLegacyData(): Boolean =
        legacyStore.loadBusinessProfile().businessName.isNotBlank() ||
            legacyStore.loadServices().isNotEmpty() ||
            legacyStore.loadStaff().isNotEmpty() ||
            legacyStore.loadStaffLeaves().isNotEmpty() ||
            legacyStore.loadAppointments().isNotEmpty()

    fun applyOutcome(outcome: V2SyncOutcome) {
        connectionState = outcome.state
        pendingCount = outcome.pendingCount
        message = outcome.message
    }

    fun enqueueCloud(values: List<OnlineMutation>) {
        if (values.isEmpty()) return
        val current = coordinator ?: return
        onlineStore.enqueueAll(values)
        pendingCount = onlineStore.pending().size
        connectionState = OnlineConnectionState.SYNCING
        scope.launch {
            busy = true
            applyOutcome(current.flush())
            busy = false
        }
    }

    fun acceptAuthenticated(url: String, result: V2AuthenticatedSession) {
        sessionStore.saveBaseUrl(url)
        sessionStore.saveAuthenticated(result)
        baseUrl = sessionStore.loadBaseUrl()
        session = result.session
        token = result.token
        message = null
        connectionState = OnlineConnectionState.SYNCING
    }

    fun authMessage(result: V2TransportResult<*>): String = when (result) {
        is V2TransportResult.AuthRequired -> result.message ?: "E-posta veya şifre hatalı."
        is V2TransportResult.Offline -> result.message ?: "Sunucuya ulaşılamıyor."
        is V2TransportResult.Conflict -> "Hesap bilgileri çakıştı."
        is V2TransportResult.Failure -> result.message ?: "İşlem tamamlanamadı."
        is V2TransportResult.Success -> ""
    }

    fun login(url: String, email: String, password: String) {
        val normalized = V2ApiContract.normalizeBaseUrl(url)
        if (normalized.isBlank()) {
            message = "Geçerli bir HTTPS sunucu adresi girin."
            return
        }
        scope.launch {
            busy = true
            val client = V2ApiClient(normalized)
            when (val result = client.login(email, password)) {
                is V2TransportResult.Success -> acceptAuthenticated(normalized, result.value)
                else -> message = authMessage(result)
            }
            busy = false
        }
    }

    fun register(
        url: String,
        ownerNameValue: String,
        businessName: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        timezone: String
    ) {
        val normalized = V2ApiContract.normalizeBaseUrl(url)
        if (normalized.isBlank()) {
            message = "Geçerli bir HTTPS sunucu adresi girin."
            return
        }
        scope.launch {
            busy = true
            val client = V2ApiClient(normalized)
            when (val result = client.registerOwner(
                ownerNameValue,
                businessName,
                email,
                password,
                phone,
                address,
                timezone
            )) {
                is V2TransportResult.Success -> acceptAuthenticated(normalized, result.value)
                else -> message = authMessage(result)
            }
            busy = false
        }
    }

    LaunchedEffect(token, baseUrl, session?.email) {
        if (token.isNullOrBlank() || session == null || coordinator == null) return@LaunchedEffect
        pendingCount = onlineStore.pending().size
        val needsLegacyDecision =
            onlineStore.snapshot() == null &&
                !onlineStore.importHandled() &&
                hasLegacyData()

        if (needsLegacyDecision) {
            showMigration = true
            connectionState = OnlineConnectionState.OFFLINE
            return@LaunchedEffect
        }

        busy = true
        val flushed = coordinator.flush()
        applyOutcome(flushed)
        if (flushed.state == OnlineConnectionState.ONLINE && flushed.pendingCount == 0) {
            applyOutcome(coordinator.bootstrap())
            cacheRevision++
        }
        busy = false
    }

    if (token.isNullOrBlank() || session == null || baseUrl.isBlank()) {
        V20AuthScreen(
            initialBaseUrl = baseUrl,
            busy = busy,
            message = message,
            onLogin = ::login,
            onRegister = ::register
        )
        return
    }

    val statusLabel = OnlineConnectionProjection.label(connectionState, pendingCount)

    key(cacheRevision) {
        RandevuV13App(
            onlineStatusLabel = statusLabel,
            cloudOwnsWhatsApp = true,
            onCloudLogout = {
                val currentApi = api
                val currentToken = token
                scope.launch {
                    if (currentApi != null && !currentToken.isNullOrBlank()) {
                        currentApi.logout(currentToken)
                    }
                    sessionStore.clearSession()
                    token = null
                    session = null
                    connectionState = OnlineConnectionState.AUTH_REQUIRED
                    message = null
                }
            },
            onCloudBusinessChanged = { profile: BusinessProfile ->
                enqueueCloud(listOf(V20MutationPlanner.business(profile, onlineStore.versions())))
            },
            onCloudServicesChanged = { before: List<ServiceRecord>, after: List<ServiceRecord> ->
                enqueueCloud(V20MutationPlanner.serviceChanges(before, after, onlineStore.versions()))
            },
            onCloudStaffChanged = { before: List<StaffRecord>, after: List<StaffRecord> ->
                enqueueCloud(V20MutationPlanner.staffChanges(before, after, onlineStore.versions()))
            },
            onCloudLeavesChanged = { before: List<StaffLeave>, after: List<StaffLeave> ->
                enqueueCloud(V20MutationPlanner.leaveChanges(before, after, onlineStore.versions()))
            },
            onCloudAppointmentAction = {
                    action: AppointmentAction,
                    before: List<Appointment>,
                    services: List<ServiceRecord>,
                    staff: List<StaffRecord> ->
                V20MutationPlanner.appointment(action, before, services, staff, onlineStore.versions())
                    ?.let { enqueueCloud(listOf(it)) }
            }
        )
    }

    if (showMigration) {
        V20MigrationDialog(
            busy = busy,
            onImport = {
                val current = coordinator
                if (current != null) {
                    val plan = V13OnlineMigration.plan(
                        legacyStore.loadBusinessProfile(),
                        legacyStore.loadServices(),
                        legacyStore.loadStaff(),
                        legacyStore.loadStaffLeaves(),
                        legacyStore.loadAppointments(),
                        onlineStore.importedIds()
                    )
                    onlineStore.enqueueAll(plan)
                    pendingCount = onlineStore.pending().size
                    connectionState = OnlineConnectionState.SYNCING
                    scope.launch {
                        busy = true
                        val result = current.flush()
                        applyOutcome(result)
                        if (result.state == OnlineConnectionState.ONLINE && result.pendingCount == 0) {
                            onlineStore.markImportHandled()
                            showMigration = false
                            applyOutcome(current.bootstrap())
                            cacheRevision++
                        }
                        busy = false
                    }
                }
            },
            onLater = {
                onlineStore.markImportHandled()
                showMigration = false
                connectionState = OnlineConnectionState.OFFLINE
                pendingCount = onlineStore.pending().size
                message = "Yerel veriler bu cihazda korunuyor."
            }
        )
    }
}
