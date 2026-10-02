package com.elxvro.randevu.ui

enum class ReferenceTab(val label: String) {
    HOME("Ana Sayfa"),
    CALENDAR("Takvim"),
    CUSTOMERS("Müşteriler"),
    STAFF("Personel"),
    MORE("Daha Fazla")
}

object ReferenceDesignContract {
    const val supportsLightTheme: Boolean = false

    const val backgroundArgb: Long = 0xFF06131C
    const val surfaceArgb: Long = 0xFF0B1E29
    const val surfaceRaisedArgb: Long = 0xFF102A36
    const val cyanArgb: Long = 0xFF00E6E6
    const val blueArgb: Long = 0xFF159CFC
    const val textPrimaryArgb: Long = 0xFFF4FBFF
    const val textSecondaryArgb: Long = 0xFF93A8B5
    const val borderArgb: Long = 0xFF174557
    const val successArgb: Long = 0xFF00D7A0
    const val warningArgb: Long = 0xFFFFC857
    const val dangerArgb: Long = 0xFFFF5D6C

    const val pageInsetDp: Int = 16
    const val compactGapDp: Int = 8
    const val cardGapDp: Int = 12
    const val sectionGapDp: Int = 16
    const val cardRadiusDp: Int = 18
    const val controlRadiusDp: Int = 14
    const val sheetRadiusDp: Int = 24
    const val headerHeightDp: Int = 64
    const val bottomNavHeightDp: Int = 72

    val bottomTabs: List<ReferenceTab> = ReferenceTab.entries
}
