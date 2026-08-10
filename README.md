# Ailə Nəzarəti — Android APK

Bu, uşağın telefonuna quraşdırılan tətbiqin tam Android Studio layihəsidir.
Şəffaf modeldədir: tətbiqin adı və ikonu görünür, uşaq açanda status ekranı
göstərir, mikrofon/səs funksiyası yoxdur.

## Nə edir

- Hər 1 dəqiqədə lokasiyanı serverə göndərir (ön plan servisi)
- Gündəlik tətbiq istifadə statistikasını sinxronlaşdırır (15 dəqiqədə bir)
- Zəng metadatasını (nömrə/vaxt/müddət — məzmun deyil) göndərir
- Yeni tətbiq quraşdırıldıqda serverə bildirir
- Serverdən blok siyahısını alır
- Device Admin ilə asan silinmənin qarşısını alır
- Cihaz yenidən başladıqda özünü avtomatik bərpa edir

## 1. GitHub-a qoymadan əvvəl: tokeni bişirin

`app/build.gradle` faylını açın, bu sətirləri tapın:

```groovy
buildConfigField "String", "SERVER_BASE_URL", "\"https://hesabat.site/usaq\""
buildConfigField "String", "SERVER_DEVICE_TOKEN", "\"BURAYA_DASHBOARDDAN_ALDIGINIZ_KODU_YAZIN\""
```

`SERVER_DEVICE_TOKEN` yerinə dashboard-da "+ Uşaq əlavə et" edəndə aldığınız
kodu yazın (məs. `3cbd9e8a067cc2d4393116e3cfc3f35eda5d4ecc2fbfe2de684bf41be69dd45c`).

**Diqqət:** hər uşaq üçün fərqli kod olduğundan, hər uşaq üçün bu sətri
dəyişib ayrıca APK build etməlisiniz (və ya eyni kodu bütün uşaqlarınız üçün
paylaşılan bir profildə istifadə edə bilərsiniz, amma tövsiyə olunmur).

## 2. GitHub-a qoyun

```bash
git init
git add .
git commit -m "İlkin versiya"
git remote add origin https://github.com/istifadeciadi/aile-nezareti-apk.git
git push -u origin main
```

## 3. APK-nı build edin

**Android Studio ilə (tövsiyə olunur):**
1. Android Studio-nu açın → "Open" → bu qovluğu seçin
2. Gradle sinxronizasiyasını gözləyin
3. `Build → Generate Signed Bundle / APK → APK` → yeni keystore yaradın → build edin
4. Nəticə: `app/release/app-release.apk`

**Komanda sətri ilə:**
```bash
./gradlew assembleRelease
```

## 4. Quraşdırma (uşağın telefonunda)

1. Telefon Ayarlar → Təhlükəsizlik → "Naməlum mənbələr"ə icazə verin
2. APK faylını telefona köçürüb açın, quraşdırın
3. Tətbiqi açın → "Davam et" düyməsinə basın (kod artıq daxildir)
4. Göstərilən addımları tək-tək tamamlayın (yer icazəsi, tətbiq istifadəsi girişi,
   zəng qeydi icazəsi, qoruma aktivləşdirmə, batareya optimallaşdırması)
5. Bütün addımlar tamamlananda "Təhlükəsizliyiniz nəzarət altındadır" mesajı görünəcək

## Struktur

```
app/src/main/java/com/ailenezareti/monitor/
├── MainActivity.kt              → Cütləmə + status ekranı
├── LocationTrackingService.kt   → Foreground servis, 1 dəqiqədə bir lokasiya
├── SyncWorker.kt                → 15 dəqiqədə bir: istifadə, zənglər, blok siyahısı
├── ApiClient.kt                 → Serverlə HTTP əlaqə
├── Prefs.kt                     → Yerli ayarlar (token, sinxronizasiya vəziyyəti)
├── BootReceiver.kt              → Cihaz yenidən başladıqda servisi bərpa edir
├── PackageAddedReceiver.kt      → Yeni tətbiq quraşdırıldıqda bildirir
└── DeviceAdminReceiver.kt       → Uninstall qoruması
```

## Qeyd

Bu tətbiq mikrofon/audio icazəsi istəmir və səs qeydə almır — yalnız
lokasiya, tətbiq istifadə statistikası, zəng metadatası və quraşdırılan
tətbiqlər haqqında məlumat toplayır.
