package com.elxvro.randevu.booking

data class BookingDraft(
    val businessName: String = "Elite Kuaför",
    val businessLocation: String = "İstanbul, Kadıköy",
    val serviceName: String? = null,
    val price: Int? = null,
    val staffName: String? = null,
    val staffTitle: String? = null,
    val date: String? = null,
    val time: String? = null,
    val durationMinutes: Int = 30
) {
    val isComplete: Boolean
        get() = !serviceName.isNullOrBlank() &&
            price != null &&
            !staffName.isNullOrBlank() &&
            !date.isNullOrBlank() &&
            !time.isNullOrBlank()
}

sealed interface BookingAction {
    data class SelectService(val name: String, val price: Int) : BookingAction
    data class SelectStaff(val name: String, val title: String) : BookingAction
    data class SelectDate(val date: String) : BookingAction
    data class SelectTime(val time: String) : BookingAction
    data object Reset : BookingAction
}

object BookingReducer {
    fun reduce(state: BookingDraft, action: BookingAction): BookingDraft = when (action) {
        is BookingAction.SelectService -> state.copy(serviceName = action.name, price = action.price)
        is BookingAction.SelectStaff -> state.copy(staffName = action.name, staffTitle = action.title)
        is BookingAction.SelectDate -> state.copy(date = action.date)
        is BookingAction.SelectTime -> state.copy(time = action.time)
        BookingAction.Reset -> BookingDraft()
    }
}
