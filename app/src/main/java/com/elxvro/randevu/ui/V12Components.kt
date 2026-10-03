package com.elxvro.randevu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elxvro.randevu.core.AppointmentStatus

@Composable
internal fun V12Header(title: String, actionIcon: ImageVector? = null, actionDescription: String = title, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(ReferenceDesignContract.headerHeightDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = RefText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionIcon != null && onAction != null) {
            V12IconButton(actionIcon, actionDescription, onAction)
        }
    }
}

@Composable
internal fun V12IconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
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
internal fun V12Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(ReferenceDesignContract.cardRadiusDp.dp)
    var cardModifier = modifier.fillMaxWidth()
    if (onClick != null) cardModifier = cardModifier.clip(shape).clickable(onClick = onClick)
    Surface(
        modifier = cardModifier,
        shape = shape,
        color = RefSurface,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
internal fun V12OutlineButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF08202D),
        border = BorderStroke(1.dp, RefCyan.copy(alpha = 0.95f))
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RefCyan, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(text, color = RefCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun V12TinyButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = RefSurfaceRaised,
        border = BorderStroke(1.dp, RefBorder.copy(alpha = 0.65f))
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = RefText, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = RefText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun V12Avatar(name: String, sizeDp: Int) {
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "R" }
    Surface(shape = CircleShape, color = Color(0xFF12364A), border = BorderStroke(1.dp, RefCyan.copy(alpha = 0.32f))) {
        Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
            Text(initials, color = RefText, fontSize = (sizeDp / 3).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun V12StatusChip(status: AppointmentStatus) {
    val color = v12StatusColor(status)
    val text = when (status) {
        AppointmentStatus.PENDING -> "Bekliyor"
        AppointmentStatus.CONFIRMED -> "Onaylandı"
        AppointmentStatus.COMPLETED -> "Tamamlandı"
        AppointmentStatus.CANCELLED -> "İptal"
    }
    V12MiniPill(text, color)
}

@Composable
internal fun V12MiniPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.10f), border = BorderStroke(1.dp, color.copy(alpha = 0.78f))) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

internal fun v12StatusColor(status: AppointmentStatus): Color = when (status) {
    AppointmentStatus.PENDING -> RefBlue
    AppointmentStatus.CONFIRMED -> RefSuccess
    AppointmentStatus.COMPLETED -> RefSuccess
    AppointmentStatus.CANCELLED -> RefDanger
}

@Composable
internal fun V12Input(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
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
        leadingIcon = icon?.let { vector -> { Icon(vector, null, tint = RefCyan, modifier = Modifier.size(18.dp)) } },
        shape = RoundedCornerShape(14.dp),
        colors = v12FieldColors()
    )
}

@Composable
internal fun V12SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, color = RefTextMuted, fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = RefTextMuted, modifier = Modifier.size(19.dp)) },
        shape = RoundedCornerShape(14.dp),
        colors = v12FieldColors()
    )
}

@Composable
internal fun V12SelectPill(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val border = when {
        !enabled -> RefBorder.copy(alpha = 0.35f)
        selected -> RefCyan
        else -> RefBorder
    }
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) RefCyan.copy(alpha = 0.14f) else RefSurface,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text,
            color = when {
                !enabled -> RefDisabled
                selected -> RefCyan
                else -> RefTextMuted
            },
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun V12EmptyState(text: String, icon: ImageVector) {
    V12Card {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = RefCyan.copy(alpha = 0.75f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, color = RefTextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun V12SettingsRow(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = RefCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = RefText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        value?.let { Text(it, color = RefTextMuted, fontSize = 10.sp) }
    }
}

@Composable
internal fun V12SettingsToggleRow(icon: ImageVector, title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
private fun v12FieldColors() = OutlinedTextFieldDefaults.colors(
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
