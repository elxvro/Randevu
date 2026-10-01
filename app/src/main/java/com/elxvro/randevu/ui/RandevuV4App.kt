package com.elxvro.randevu.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.core.ThemeMode
import com.elxvro.randevu.core.UserRole
import com.elxvro.randevu.core.UxAction
import com.elxvro.randevu.core.UxEngine
import com.elxvro.randevu.core.UxState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RandevuV4App() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var uxState by remember { mutableStateOf(UxState()) }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = when (uxState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    RandevuTheme(darkTheme = darkTheme) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val notificationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            uxState = UxEngine.reduce(uxState, UxAction.SetNotifications(granted))
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (granted) "Bildirimler açıldı" else "Bildirim izni verilmedi"
                )
            }
        }

        LaunchedEffect(uxState.message) {
            val message = uxState.message ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            uxState = UxEngine.reduce(uxState, UxAction.ClearMessage)
        }

        if (showSplash) {
            LaunchedEffect(Unit) {
                delay(700)
                showSplash = false
            }
            BrandSplash()
            return@RandevuTheme
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            if (!uxState.signedIn) {
                LoginScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    darkTheme = darkTheme,
                    onThemeToggle = {
                        uxState = UxEngine.reduce(
                            uxState,
                            UxAction.SetTheme(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                        )
                    },
                    onContinue = { name, phone ->
                        if (name.trim().length < 2) {
                            scope.launch { snackbarHostState.showSnackbar("Ad soyad alanını doldur") }
                        } else if (phone.filter(Char::isDigit).length < 10) {
                            scope.launch { snackbarHostState.showSnackbar("Geçerli bir telefon numarası gir") }
                        } else {
                            uxState = UxEngine.reduce(
                                uxState,
                                UxAction.SignIn(UserRole.CUSTOMER, name)
                            )
                        }
                    }
                )
            } else {
                Box(Modifier.fillMaxSize().padding(padding)) {
                    RandevuV3App()
                    FloatingActionButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 82.dp),
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Ayarlar")
                    }
                }
            }
        }

        if (showSettings && uxState.signedIn) {
            SettingsDialog(
                state = uxState,
                onDismiss = { showSettings = false },
                onTheme = { mode ->
                    uxState = UxEngine.reduce(uxState, UxAction.SetTheme(mode))
                    scope.launch { snackbarHostState.showSnackbar("Tema güncellendi") }
                },
                onNotifications = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        uxState = UxEngine.reduce(uxState, UxAction.SetNotifications(enabled))
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (enabled) "Bildirimler açıldı" else "Bildirimler kapatıldı"
                            )
                        }
                    }
                },
                onLogout = {
                    showSettings = false
                    uxState = UxEngine.reduce(uxState, UxAction.SignOut)
                }
            )
        }
    }
}

@Composable
private fun BrandSplash() {
    Box(
        Modifier.fillMaxSize().background(BrandBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(size = 112)
            Spacer(Modifier.height(22.dp))
            Text(
                "Randevu",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Zamanını kolayca planla",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun LoginScreen(
    modifier: Modifier,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onContinue: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        IconButton(
            onClick = onThemeToggle,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Icon(
                if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = "Tema"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoMark(size = 96)
            Spacer(Modifier.height(22.dp))
            Text("Randevu", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Randevularını tek yerden oluştur, takip et ve yönet.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 26.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, null, tint = BrandBlue)
                        Spacer(Modifier.size(8.dp))
                        Text("Hızlı profil", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.take(18) },
                        label = { Text("Telefon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onContinue(name, phone) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Devam Et", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Rounded.ArrowForward, null)
                    }
                    Text(
                        "Bilgiler bu sürümde cihaz üzerinde kullanılır.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "v0.4.0 • Brand & UX",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    state: UxState,
    onDismiss: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Uygulama Ayarları", fontWeight = FontWeight.ExtraBold)
                if (state.displayName.isNotBlank()) {
                    Text(
                        state.displayName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Görünüm", fontWeight = FontWeight.Bold)
                ThemeChoice(
                    label = "Sistem",
                    selected = state.themeMode == ThemeMode.SYSTEM,
                    onClick = { onTheme(ThemeMode.SYSTEM) }
                )
                ThemeChoice(
                    label = "Açık tema",
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = { onTheme(ThemeMode.LIGHT) }
                )
                ThemeChoice(
                    label = "Koyu tema",
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = { onTheme(ThemeMode.DARK) }
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Notifications, null)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Randevu bildirimleri", fontWeight = FontWeight.Bold)
                            Text(
                                "Hatırlatma altyapısını aç veya kapat",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            )
                        }
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = onNotifications
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Bildirim merkezi", fontWeight = FontWeight.Bold)
                        Text(
                            "Henüz bildirim yok.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) { Text("Tamam") }
        },
        dismissButton = {
            TextButton(onClick = onLogout) { Text("Çıkış Yap", color = DangerRed) }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ThemeChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CheckCircle, null)
            Spacer(Modifier.size(8.dp))
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun LogoMark(size: Int) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = RoundedCornerShape((size * 0.28f).dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size((size * 0.58f).dp)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding((size * 0.08f).dp)
                    .size((size * 0.30f).dp),
                shape = CircleShape,
                color = SuccessGreen
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding((size * 0.035f).dp)
                )
            }
        }
    }
}
