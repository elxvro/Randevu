package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class V20AuthMode { LOGIN, REGISTER }

@Composable
internal fun V20AuthScreen(
    initialBaseUrl: String,
    busy: Boolean,
    message: String?,
    onLogin: (String, String, String) -> Unit,
    onRegister: (String, String, String, String, String, String, String, String) -> Unit
) {
    var modeName by rememberSaveable { mutableStateOf(V20AuthMode.LOGIN.name) }
    val mode = runCatching { V20AuthMode.valueOf(modeName) }.getOrDefault(V20AuthMode.LOGIN)
    var baseUrl by rememberSaveable(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var businessName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var timezone by rememberSaveable { mutableStateOf("Europe/Istanbul") }

    ReferenceRandevuTheme {
        Surface(color = RefBackground, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Randevu Online", color = RefText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (mode == V20AuthMode.LOGIN) "İşletme hesabına giriş yap" else "Yeni işletme hesabı oluştur",
                    color = RefTextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(18.dp))

                V12Card {
                    V12Input(baseUrl, { baseUrl = it }, "Sunucu adresi (HTTPS)", Icons.Rounded.Language)
                    Spacer(Modifier.height(8.dp))
                    V12Input(email, { email = it }, "E-posta", Icons.Rounded.Email)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Şifre") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RefText,
                            unfocusedTextColor = RefText,
                            focusedBorderColor = RefCyan,
                            unfocusedBorderColor = RefBorder,
                            focusedLabelColor = RefCyan,
                            unfocusedLabelColor = RefTextMuted,
                            focusedLeadingIconColor = RefCyan,
                            unfocusedLeadingIconColor = RefTextMuted
                        )
                    )
                    if (mode == V20AuthMode.REGISTER) {
                        Spacer(Modifier.height(8.dp))
                        V12Input(ownerName, { ownerName = it }, "Adınız", Icons.Rounded.Person)
                        Spacer(Modifier.height(8.dp))
                        V12Input(businessName, { businessName = it }, "İşletme adı", Icons.Rounded.Business)
                        Spacer(Modifier.height(8.dp))
                        V12Input(phone, { phone = it }, "Telefon (opsiyonel)", Icons.Rounded.Person)
                        Spacer(Modifier.height(8.dp))
                        V12Input(address, { address = it }, "Adres (opsiyonel)", Icons.Rounded.Business)
                        Spacer(Modifier.height(8.dp))
                        V12Input(timezone, { timezone = it }, "Saat dilimi", Icons.Rounded.Language)
                    }
                }

                if (!message.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = RefWarning, fontSize = 11.sp)
                }
                if (busy) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RefCyan)
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (mode == V20AuthMode.LOGIN) {
                            onLogin(baseUrl, email, password)
                        } else {
                            onRegister(baseUrl, ownerName, businessName, email, password, phone, address, timezone)
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                ) {
                    Text(if (mode == V20AuthMode.LOGIN) "Giriş Yap" else "İşletme Oluştur", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        modeName = if (mode == V20AuthMode.LOGIN) V20AuthMode.REGISTER.name else V20AuthMode.LOGIN.name
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    border = BorderStroke(1.dp, RefBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (mode == V20AuthMode.LOGIN) "Yeni İşletme Oluştur" else "Zaten hesabım var", color = RefText)
                }
            }
        }
    }
}
