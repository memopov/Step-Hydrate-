package com.example.ui

object LanguageManager {
    enum class Language { TR, EN }

    private val trStrings = mapOf(
        "app_title" to "Adım & Hidrasyon Takibi",
        "steps" to "Adımlar",
        "step_goal" to "Adım Hedefi",
        "hydration" to "Bugünkü Sıvı Alımı",
        "hydration_goal" to "Sıvı Hedefi",
        "calories" to "Yakılan Kalori",
        "distance" to "Mesafe",
        "active_speed" to "Hız",
        "gps_mode" to "Uzun Mesafe GPS Modu",
        "gps_enabled" to "GPS Aktif",
        "gps_disabled" to "GPS Kapalı (Düşük Güç)",
        "gps_signal_loss" to "Sinyal Yok (Adımsayar Odaklı Ölçüm)",
        "add_drink" to "İçecek Ekle",
        "drink_type" to "İçecek Türü",
        "cup_size" to "Bardak Boyutu",
        "save" to "Kaydet",
        "cancel" to "İptal",
        "select_account" to "Google Hesabı Seç",
        "switch_account" to "Profil",
        "sign_in_google" to "Google ile Giriş Yap",
        "guest_mode" to "Misafir Modu",
        "merge_success" to "Yerel misafir verileriniz Google hesabınızla başarıyla birleştirildi!",
        "health_data_secure" to "Kişisel sağlık verileriniz uçtan uca AES-2CM ile şifrelenmiştir.",
        "anti_cheat_active" to "Akıllı Filtreleme Aktif (Kalman & Çalkalama Koruması)",
        "sensor_debug" to "Sensör Bilgisi (Fusion)",
        "kalman_magnitude" to "Kalman Filtreli İvme",
        "water" to "Su",
        "coffee" to "Kahve (İdrar Söktürücü - %20 Dehidratasyon)",
        "tea" to "Çay (Hafif Kafein - %60 Hidrasyon)",
        "soda" to "Asitli İçecek (%50 Hidrasyon)",
        "sports_drink" to "Sporcu İçeceği (%110 Elektrolit Destek)",
        "hydration_history" to "Sıvı Geçmişi",
        "clear_entry" to "Sil",
        "height" to "Boy (cm)",
        "weight" to "Kilo (kg)",
        "update_profile" to "Profili Güncelle",
        "notification_dnd_active" to "Gece Rahatsız Etmeyin (DND) modu aktif. Bildirimler sessizlenecektir.",
        "sample_step_btn" to "Adım Simüle Et (Doğrulama)",
        "steps_log_history" to "Günlük Adım Geçmişi"
    )

    private val enStrings = mapOf(
        "app_title" to "Step & Hydrate Tracker",
        "steps" to "Steps",
        "step_goal" to "Step Goal",
        "hydration" to "Today's Hydration",
        "hydration_goal" to "Hydration Goal",
        "calories" to "Calories Burnt",
        "distance" to "Distance",
        "active_speed" to "Speed",
        "gps_mode" to "Long Distance GPS Mode",
        "gps_enabled" to "GPS Active",
        "gps_disabled" to "GPS Off (Low Power Direct)",
        "gps_signal_loss" to "No GPS Signal (Stride Estimation Active)",
        "add_drink" to "Log Drink",
        "drink_type" to "Drink Type",
        "cup_size" to "Cup Size",
        "save" to "Save",
        "cancel" to "Cancel",
        "select_account" to "Select Google Account",
        "switch_account" to "Profile",
        "sign_in_google" to "Sign In with Google",
        "guest_mode" to "Guest Mode",
        "merge_success" to "Local guest data synced securely to your Google Account!",
        "health_data_secure" to "Personal metrics are protected with E2E AES-2CM encryption.",
        "anti_cheat_active" to "Smart Filtering Active (Kalman & Anti-Shake protection)",
        "sensor_debug" to "Sensor Fusion Info",
        "kalman_magnitude" to "Kalman Filtered Accel",
        "water" to "Water",
        "coffee" to "Coffee (Diuretic effect - 20% dehydration)",
        "tea" to "Tea (Light caffeine - 60% hydration)",
        "soda" to "Soda (50% hydration)",
        "sports_drink" to "Sports Drink (110% hydration)",
        "hydration_history" to "Hydration History",
        "clear_entry" to "Delete",
        "height" to "Height (cm)",
        "weight" to "Weight (kg)",
        "update_profile" to "Update Profile",
        "notification_dnd_active" to "Nocturnal Do-Not-Disturb active. Alerts will be muted.",
        "sample_step_btn" to "Simulate Step (Validate)",
        "steps_log_history" to "Daily Step Logs"
    )

    fun getString(key: String, lang: Language): String {
        return if (lang == Language.TR) {
            trStrings[key] ?: enStrings[key] ?: key
        } else {
            enStrings[key] ?: key
        }
    }
}
