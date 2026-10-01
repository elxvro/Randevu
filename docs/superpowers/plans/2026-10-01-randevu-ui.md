# Randevu UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Backend olmadan çalışan, profesyonel randevu akışına sahip Android debug APK üretmek.

**Architecture:** Tek Activity + Jetpack Compose. Saf Kotlin `BookingReducer` randevu seçim state'ini yönetir; Compose ekranları yalnızca state'i gösterir ve reducer/action çağırır. Demo veri uygulama içinde tutulur.

**Tech Stack:** Kotlin 1.9.24, Android Gradle Plugin 8.5.2, Gradle 8.7, Jetpack Compose Material3, JUnit4, GitHub Actions, Java 17.

**Spec:** `docs/superpowers/specs/2026-10-01-randevu-ui-design.md`

## Global Constraints
- package: `com.elxvro.randevu`
- versionName: `0.1.0`
- minSdk 26, compileSdk/targetSdk 35
- PHP/MySQL ve ağ katmanı bu sürümde yok.
- Türkçe UI ve yerel demo veriler kullanılacak.

## Review Focus
- Hizmet/personel/tarih/saat seçimi sırası bozulmamalı.
- Eksik seçimle özet ekranına geçilmemeli.
- Alt menü ekran değişimleri state'i gereksiz sıfırlamamalı.
- Compose ekranları küçük telefonlarda taşmamalı; kaydırma kullanılmalı.
- GitHub Actions temiz checkout üzerinde test ve APK üretmeli.

---

### Task 1: Proje iskeleti ve randevu state mantığı

**Files:**
- Create: Gradle/Android proje yapılandırmaları
- Create: `app/src/main/java/com/elxvro/randevu/booking/BookingModels.kt`
- Test: `app/src/test/java/com/elxvro/randevu/booking/BookingReducerTest.kt`

**Interfaces:**
- Produces: `BookingDraft`, `BookingAction`, `BookingReducer.reduce(...)`

- [ ] Test dosyasını önce ekle; hizmet, personel, tarih ve saat seçimlerinin draft'ı güncellediğini doğrula.
- [ ] Testi çalıştır; production sınıfları bulunmadığı için başarısız olduğunu doğrula.
- [ ] Minimal reducer/model implementasyonunu ekle.
- [ ] Tüm unit testleri çalıştır ve geçir.

### Task 2: Compose tasarım ve ekran akışları

**Files:**
- Create: `MainActivity.kt`
- Create: `ui/RandevuApp.kt`
- Create: `ui/AppTheme.kt`
- Create: `ui/DemoData.kt`
- Create: gerekli Android resource dosyaları

**Interfaces:**
- Consumes: Task 1 `BookingDraft` ve reducer
- Produces: 10 ekranlık çalışan demo navigasyon akışı

- [ ] Ana ekran, işletme detay, personel, tarih/saat, özet ve onay ekranlarını bağla.
- [ ] Randevular, profil, işletme paneli ve işletme menüsünü bağla.
- [ ] Alt navigasyon ve geri hareketlerini uygula.
- [ ] Unit testleri tekrar çalıştır.

### Task 3: CI ve APK doğrulama

**Files:**
- Create: `.github/workflows/android.yml`

**Interfaces:**
- Consumes: Gradle projesi
- Produces: `randevu-debug-apk` GitHub Actions artifact'i

- [ ] GitHub Actions'ta Java 17 + Gradle 8.7 kur.
- [ ] `gradle testDebugUnitTest assembleDebug` çalıştır.
- [ ] `app/build/outputs/apk/debug/app-debug.apk` dosyasını artifact olarak yükle.
- [ ] Workflow sonucunu ve artifact varlığını doğrula.
