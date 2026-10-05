# Veritabanı Migrationları

- Yeni kurulum: `../schema.sql`
- v1.3.1 → v2.0.0: `2026_10_04_v2_core.sql`

Her migration öncesinde tam veritabanı yedeği alın. Aynı migration dosyasını ikinci kez çalıştırmayın. v2 uygulamasını üretime bağlamadan önce migration'ın hatasız tamamlandığını ve `/health` yanıtının çalıştığını kontrol edin.
