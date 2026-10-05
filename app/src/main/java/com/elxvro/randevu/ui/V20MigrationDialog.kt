package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
internal fun V20MigrationDialog(
    busy: Boolean,
    onImport: () -> Unit,
    onLater: () -> Unit
) {
    Dialog(onDismissRequest = { if (!busy) onLater() }) {
        Surface(
            color = RefBackground,
            shape = RoundedCornerShape(ReferenceDesignContract.sheetRadiusDp.dp),
            border = BorderStroke(1.dp, RefBorder)
        ) {
            Column(Modifier.padding(18.dp)) {
                Icon(Icons.Rounded.CloudUpload, null, tint = RefCyan, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text("Online hesaba geçiş", color = RefText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    V20AppPolicy.importCopy,
                    color = RefTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Aktarım hizmet, personel, izin ve randevularını silmeden sunucuya kopyalar.",
                    color = RefTextMuted,
                    fontSize = 10.sp
                )
                if (busy) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RefCyan)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onLater,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, RefBorder)
                    ) { Text("Şimdi Değil", color = RefText) }
                    Button(
                        onClick = onImport,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RefCyan, contentColor = RefBackground)
                    ) { Text("Aktar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
