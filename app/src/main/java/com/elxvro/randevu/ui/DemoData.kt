package com.elxvro.randevu.ui

data class DemoService(
    val name: String,
    val duration: Int,
    val price: Int
)

data class DemoStaff(
    val name: String,
    val title: String,
    val rating: Double,
    val reviews: Int,
    val initials: String
)

data class DemoAppointment(
    val day: String,
    val month: String,
    val service: String,
    val business: String,
    val time: String,
    val status: String
)

val demoServices = listOf(
    DemoService("Erkek Saç Kesimi", 30, 250),
    DemoService("Sakal Tıraşı", 20, 150),
    DemoService("Saç + Sakal", 50, 350),
    DemoService("Saç Boyama", 60, 600),
    DemoService("Cilt Bakımı", 45, 450)
)

val demoStaff = listOf(
    DemoStaff("Ahmet Demir", "Uzman Kuaför", 4.9, 82, "AD"),
    DemoStaff("Mehmet Kaya", "Kıdemli Kuaför", 4.8, 64, "MK"),
    DemoStaff("Emre Yılmaz", "Saç Tasarım Uzmanı", 4.7, 56, "EY"),
    DemoStaff("Burak Arslan", "Profesyonel Kuaför", 4.6, 38, "BA")
)

val demoAppointments = listOf(
    DemoAppointment("04", "EKİ", "Saç Bakımı", "Elite Kuaför", "14:30", "Onaylandı"),
    DemoAppointment("08", "EKİ", "Araç Bakım", "Auto Servis", "10:00", "Beklemede")
)
