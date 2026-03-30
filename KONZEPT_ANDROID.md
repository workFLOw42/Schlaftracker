# SchlafGut — Konzept: Native Android App

## 1. Überblick

**Was:** Umbau der bestehenden SchlafGut PWA (React/TypeScript) in eine native Android-App.

| Eigenschaft | Entscheidung |
|---|---|
| Framework | **Kotlin + Jetpack Compose** |
| Architektur | MVVM + Repository Pattern |
| Datenbank | Room (SQLite) — rein lokal |
| KI-Coach | Entfällt (kein Gemini, kein Internet nötig) |
| Health Connect | Optional: Lesender Zugriff auf Gewicht, Temperatur, Puls, etc. |
| Auth/Sync | Kein Account, kein Cloud-Sync |
| Schutz | Optional: Biometrie / PIN / Muster (Android BiometricPrompt) |
| Export | PDF + CSV |
| Import | JSON (aus PWA-Export) + JSON-Backup |
| Distribution | Google Play Store |

---

## 2. Architektur

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│          Jetpack Compose Screens             │
│  Dashboard │ Logger │ Statistics │ Settings  │
└──────────────────┬──────────────────────────┘
                   │ State (StateFlow)
┌──────────────────▼──────────────────────────┐
│               ViewModel Layer                │
│  DashboardVM │ LoggerVM │ StatsVM │ SettingsVM│
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│             Repository Layer                 │
│          SleepRepository (Single Source)      │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              Data Layer                      │
│     Room DB │ DAOs │ Entities │ Converters   │
└─────────────────────────────────────────────┘
```

### Package-Struktur

```
de.schlafgut.app/
├── data/
│   ├── backup/
│   │   ├── DriveBackupManager.kt      # Google Drive Backup + Restore
│   │   └── BackupEncryption.kt        # PBKDF2 + AES-256-GCM Verschlüsselung
│   ├── db/
│   │   ├── SchlafGutDatabase.kt       # Room Database (v3, 3 Migrationen)
│   │   ├── SleepEntryDao.kt           # DAO
│   │   ├── SettingsDao.kt             # DAO
│   │   ├── HealthSnapshotDao.kt       # DAO
│   │   └── Converters.kt              # TypeConverters (Listen, Dates)
│   ├── entity/
│   │   ├── SleepEntryEntity.kt        # Room Entity (inkl. Substanz-Tracking)
│   │   ├── WakeWindow.kt              # Datenklasse (JSON via TypeConverter)
│   │   ├── UserSettingsEntity.kt      # Einstellungen (inkl. regularMedications)
│   │   └── HealthSnapshotEntity.kt    # Health Connect Körperdaten pro Eintrag
│   ├── repository/
│   │   └── SleepRepository.kt         # Repository (Single Source of Truth)
│   ├── export/
│   │   ├── CsvExporter.kt             # CSV-Export (semikolongetrennt + Health-Spalten)
│   │   ├── PdfExporter.kt             # PDF-Export (Landscape A4)
│   │   └── JsonImportExport.kt        # JSON Import/Export (PWA-kompatibel)
│   └── health/
│       ├── HealthConnectManager.kt    # Health Connect Client + Permissions
│       └── HealthDataRepository.kt    # Lesen von Körperdaten
├── di/
│   └── DatabaseModule.kt              # Hilt DI Module
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                   # Material 3 Dark Theme
│   │   ├── Color.kt                   # Farbpalette
│   │   └── Type.kt                    # Typografie
│   ├── navigation/
│   │   └── NavGraph.kt                # Navigation Compose + Screen-Enum
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── logger/
│   │   ├── SleepLoggerScreen.kt
│   │   └── SleepLoggerViewModel.kt
│   ├── statistics/
│   │   ├── StatisticsScreen.kt
│   │   └── StatisticsViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── allentries/
│   │   └── AllEntriesScreen.kt        # Vollständige Eintrags-Liste
│   ├── onboarding/
│   │   └── OnboardingScreen.kt        # Ersteinrichtung (Name + Standard-Latenz)
│   ├── components/
│   │   ├── SleepTimeline.kt           # Mini Timeline-Composable
│   │   ├── SleepTimeline24h.kt        # Canvas-basierte 24h-Visualisierung
│   │   ├── SummaryCard.kt             # Dashboard-Karte
│   │   ├── SleepEntryRow.kt           # Eintrag in Listen
│   │   ├── DateTimePickerDialog.kt    # Material 3 DateTimePicker
│   │   ├── WakeWindowDialog.kt        # Wachphasen hinzufügen/entfernen
│   │   └── SleepCharts.kt             # Vico-Charts (Dauer, Qualität, Unterbrechungen)
│   └── RootViewModel.kt               # Startup-Logic (Onboarding, App-Lock)
├── util/
│   ├── SleepCalculator.kt             # Dauer-Berechnung, Validierung
│   ├── DateTimeUtil.kt                # Formatierung, Konvertierung
│   └── BiometricHelper.kt             # Biometrie-Wrapper
├── MainActivity.kt                     # Entry Point + Navigation Drawer
├── SchlafGutApp.kt                    # Hilt Application
└── HealthPermissionsRationaleActivity.kt  # Health Connect Privacy Policy Screen
```

---

## 3. Datenmodell (Room)

### SleepEntryEntity

```kotlin
@Entity(tableName = "sleep_entries")
data class SleepEntryEntity(
    @PrimaryKey val id: String,          // UUID
    val isNap: Boolean = false,          // Nickerchen?
    val date: Long,                      // Epoch millis (Nacht-Datum)
    val bedTime: Long,                   // Epoch millis
    val wakeTime: Long,                  // Epoch millis
    val sleepLatency: Int,               // Minuten (0-120)
    val sleepDurationMinutes: Int,       // Berechnete Schlafdauer
    val wakeDurationMinutes: Int,        // Gesamte Wachzeit
    val wakeWindows: List<WakeWindow>,   // JSON via TypeConverter
    val interruptionCount: Int,          // = wakeWindows.size
    val quality: Int,                    // 1-10
    val tags: List<String>,             // JSON via TypeConverter
    val notes: String,                   // Freitext

    // Substanz-Tracking (Schema v2+)
    val alcoholLevel: Int = 0,           // 0=nein, 1=wenig, 2=mittel, 3=viel
    val drugLevel: Int = 0,              // 0=nein, 1=wenig, 2=mittel, 3=viel
    val sleepAid: Boolean = false,       // Schlafmittel eingenommen?
    val medication: Boolean = false,     // Medikament eingenommen?
    val medicationNotes: String = ""     // Freitext zu Medikamenten
)

data class WakeWindow(
    val start: Long,    // Epoch millis
    val end: Long       // Epoch millis
)
```

### UserSettingsEntity

```kotlin
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,                           // Singleton (nur ein User)
    val userName: String = "",
    val defaultSleepLatency: Int = 15,                     // Minuten
    val appLockEnabled: Boolean = false,                    // Biometrie an/aus
    val healthConnectEnabled: Boolean = false,              // Health Connect Verknüpfung
    val onboardingCompleted: Boolean = false,               // Ersteinrichtung abgeschlossen?

    // Regelmäßige Medikamente (Schema v3)
    val regularMedications: List<MedicationEntry> = emptyList()  // JSON via TypeConverter
)

data class MedicationEntry(
    val name: String,
    val dosage: String = ""
)
```

**Schema-Versionen:**
- v1 → v2: Substanz-Tracking-Felder in `SleepEntryEntity` ergänzt
- v2 → v3: `regularMedications`-Liste in `UserSettingsEntity` ergänzt

### TypeConverters

```kotlin
class Converters {
    @TypeConverter
    fun fromWakeWindowList(value: List<WakeWindow>): String = Json.encodeToString(value)

    @TypeConverter
    fun toWakeWindowList(value: String): List<WakeWindow> = Json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)
}
```

---

## 4. Screens & Navigation

### Navigation (Modal Drawer + FAB)

```
┌──────────────────────────────────────────┐
│  ☰  TopAppBar          [SchlafGut Logo]  │
│                                          │
│         App Content (Screen)             │
│                                          │
│                                [+ FAB]   │
└──────────────────────────────────────────┘

Drawer (ausklappbar von links):
┌────────────────────┐
│  [Logo]  SchlafGut │
│  ─────────────────  │
│  🏠 Dashboard       │
│  📊 Statistik       │
│  📋 Alle Einträge   │
│  ⚙  Einstellungen  │
└────────────────────┘
```

| Route | Screen | Beschreibung |
|---|---|---|
| `dashboard` | DashboardScreen | Übersicht + letzte Einträge |
| `sleep_logger?entryId={id}` | SleepLoggerScreen | Neuer Eintrag / Bearbeiten |
| `statistics` | StatisticsScreen | Charts, Timeline, Export |
| `all_entries` | AllEntriesScreen | Vollständige Eintragsliste |
| `settings` | SettingsScreen | Einstellungen, Import/Export, App-Schutz |

- **Modal Navigation Drawer** (links): Dashboard, Statistik, Alle Einträge, Einstellungen
- **Floating Action Button** ("+"): Neuer Schlaf-Eintrag
- TopAppBar nur auf Statistik- und Einstellungs-Screen

---

## 5. Screen-Details

### 5.1 Dashboard

**Oberer Bereich — 4 Summary Cards (2×2 Grid):**

| Karte | Inhalt | Icon |
|---|---|---|
| Letzte Nacht | Schlafdauer in Stunden | 🌙 |
| Ø Qualität | Durchschnitt (1-10) | ⭐ |
| Ø Dauer | Durchschnittl. Schlafdauer | ⏱ |
| Einträge | Gesamtanzahl | 📋 |

**Unterer Bereich — Letzte Einträge (max 5):**
- Qualitäts-Badge (farbiger Kreis: grün/indigo/rot)
- Datum (dt. Format: "Montag, 1. Apr.")
- Dauer + Unterbrechungen
- Inline SleepTimeline-Visualisierung
- Tap → öffnet Eintrag zum Bearbeiten
- "Alle anzeigen" → wechselt zu Statistik

### 5.2 Sleep Logger (Neuer Eintrag / Bearbeiten)

**Fullscreen-Sheet oder eigener Screen (via Navigation)**

Formular-Elemente:

1. **Typ-Toggle**: Nachtschlaf 🌙 / Mittagsschlaf ☀️
   - Wechsel setzt Defaults (Nacht: 23:00→7:00, Nickerchen: 13:00→13:45)

2. **Bettzeit**: Material 3 DateTimePicker
3. **Aufwachzeit**: Material 3 DateTimePicker

4. **Einschlaflatenz**: Slider (0–60 Min, Step 1) mit Anzeige

5. **Wachphasen**:
   - Liste bestehender Phasen (Zeitraum + Löschen-Button)
   - "Wachphase hinzufügen"-Button → TimePicker für Start + Ende
   - Summe Minuten + Anzahl angezeigt

6. **Schlafqualität**: Slider (1–10), farbcodiertes Label

7. **Notizen**: TextField, optional

8. **Aktions-Buttons**:
   - Speichern (primär)
   - Löschen (nur bei Bearbeitung, mit Bestätigungsdialog)
   - Abbrechen

**Validierung** (identisch zur PWA):
- Aufwachzeit > Bettzeit
- Berechnete Schlafdauer ≥ 0
- Keine Überlappung mit bestehenden Einträgen
- Wachphasen innerhalb Bett→Aufsteh-Zeitraum

### 5.3 Statistik

**Datumsbereich-Filter**: Start/Ende DatePicker (Standard: letzte 7 Tage)

**24h-Timeline-Visualisierung** (Canvas / Compose Drawing):
- Achse 18:00 → 18:00 (nächster Tag)
- Zeitmarkierungen alle 2 Stunden
- Pro Eintrag ein horizontaler Balken:
  - Grau = Einschlaflatenz
  - Qualitätsfarbe = Schlafzeit
  - Orange-Overlays = Wachphasen
- Gestrichelte Median-Linien (Bett: lila, Aufwach: teal)

**Charts** (via Vico oder MPAndroidChart):
- Liniendiagramm: Schlafdauer-Trend
- Balkendiagramm: Qualität pro Nacht
- Liniendiagramm: Unterbrechungen

**Export-Buttons**:
- 📄 CSV-Export → Share Intent oder Datei speichern
- 📑 PDF-Export → Landscape A4 mit Timeline + Statistiken

### 5.4 Einstellungen

- **Benutzername** (optional, für Export-Header)
- **Standard-Einschlaflatenz** (Number, 0-120 Min)
- **App-Schutz**: Toggle + Biometrie-Einrichtung
- **Health Connect**: Toggle + Permission-Flow
- **Google Drive Backup**: Verschlüsseltes Backup/Restore in Google Drive (AES-256-GCM)
- **Daten**:
  - JSON-Export (komplettes Backup)
  - JSON-Import (aus PWA oder Backup)
  - Alle Daten löschen (Danger Zone mit Bestätigung)
- **Über die App**: Version, Lizenzen

---

## 6. Health Connect Integration (Optional)

### Überblick

**Health Connect** ist Googles zentrale Gesundheitsdaten-Plattform auf Android. Andere Apps (Samsung Health, Fitbit, Google Fit, Withings, etc.) schreiben Daten hinein — SchlafGut liest diese **nur lesend** aus, um den Schlaf im Kontext von Körperdaten zu betrachten.

- Ab Android 14: Health Connect ist ins System eingebaut
- Android 13 und älter: Health Connect App muss aus dem Play Store installiert sein
- Min SDK für Health Connect Client: **API 26** (passt zu unserem Min SDK)

### Datentypen die wir lesen

| Datentyp | Record-Klasse | Permission | Nutzen |
|---|---|---|---|
| **Gewicht** | `WeightRecord` | `READ_WEIGHT` | Korrelation Gewicht ↔ Schlafqualität |
| **Körpertemperatur** | `BodyTemperatureRecord` | `READ_BODY_TEMPERATURE` | Abend-Temp als Schlaf-Indikator |
| **Hauttemperatur** | `SkinTemperatureRecord` | `READ_SKIN_TEMPERATURE` | Von Wearables (Oura, Pixel Watch) |
| **Ruhepuls** | `RestingHeartRateRecord` | `READ_RESTING_HEART_RATE` | Erholung / Fitness-Level |
| **Herzfrequenz** | `HeartRateRecord` | `READ_HEART_RATE` | Nacht-HF während des Schlafs |
| **Blutsauerstoff** | `OxygenSaturationRecord` | `READ_OXYGEN_SATURATION` | SpO2 während der Nacht |
| **Schritte** | `StepsRecord` | `READ_STEPS` | Aktivitätslevel am Tag |
| **Schlaf (andere Apps)** | `SleepSessionRecord` | `READ_SLEEP` | Vergleich mit manuellem Tracking |

> **Wichtig:** Wir schreiben KEINE Daten in Health Connect — nur lesen.

### Architektur

```
┌──────────────────────────────────────────┐
│        Health Connect (System/App)        │
│  Samsung Health, Fitbit, Google Fit, ...  │
└──────────────────┬───────────────────────┘
                   │ READ only
┌──────────────────▼───────────────────────┐
│        HealthConnectManager.kt            │
│  - Verfügbarkeit prüfen                   │
│  - Permissions anfordern                  │
│  - HealthConnectClient erstellen          │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│        HealthDataRepository.kt            │
│  - Daten für Datum/Zeitraum lesen         │
│  - Caching (kein ständiges Abfragen)      │
│  - Mapping auf eigene Models              │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│           SleepEntry (erweitert)          │
│  + healthData: HealthSnapshot?            │
└──────────────────────────────────────────┘
```

### Erweitertes Datenmodell

```kotlin
// Zusätzliche Entity für Health-Daten pro Schlaf-Eintrag
@Entity(tableName = "health_snapshots")
data class HealthSnapshotEntity(
    @PrimaryKey val sleepEntryId: String,   // FK zu SleepEntry
    val weightKg: Double? = null,            // Letztes Gewicht des Tages
    val bodyTempCelsius: Double? = null,     // Körpertemperatur abends
    val skinTempDeltaCelsius: Double? = null, // Hauttemp-Abweichung (Wearable)
    val restingHeartRate: Int? = null,        // Ruhepuls des Tages
    val avgNightHeartRate: Int? = null,       // Ø Herzfrequenz in der Nacht
    val oxygenSaturation: Double? = null,     // SpO2 (%)
    val stepsTotal: Int? = null,              // Schritte am Tag
    val sleepFromDevice: String? = null,      // Schlaf lt. Wearable (Zusammenfassung)
    val fetchedAt: Long = 0                   // Wann abgerufen (Epoch millis)
)
```

Alle Felder sind **nullable** — nur was verfügbar ist, wird befüllt.

### User Flow

```
Einstellungen
    │
    ▼
"Health Connect verbinden"  ──── Toggle (aus/an)
    │
   An
    │
    ▼
Health Connect verfügbar?
    │
    ├── Nein (< Android 14 ohne App)
    │   └── "Bitte Health Connect installieren" → Play Store Link
    │
    └── Ja
        │
        ▼
    Permissions anfragen (System-Dialog)
    "SchlafGut möchte folgende Daten lesen: ..."
        │
        ├── Alle gewährt → ✅ Aktiv
        ├── Teilweise → ✅ Nur verfügbare Daten
        └── Abgelehnt → Toggle bleibt aus
```

### Anzeige in der App

**Im Sleep Logger (nach Speichern):**
- Automatischer Abruf der Health-Daten für den Schlafzeitraum
- Kleine Info-Karte unter dem Eintrag: "Körperdaten der Nacht"

**Im Dashboard (Eintragsliste):**
- Kleine Icons neben dem Eintrag wenn Health-Daten vorhanden (❤️ 58 bpm, 🏃 8.432)

**In der Statistik (neuer Abschnitt):**
- Optionaler "Körperdaten"-Tab oder Erweiterung der bestehenden Charts:
  - Gewichtsverlauf als Overlay oder separater Chart
  - Ruhepuls-Trend
  - Korrelation: Schritte ↔ Schlafqualität (Scatter-Plot)

**Im CSV/PDF-Export:**
- Zusätzliche Spalten: Gewicht, Ruhepuls, SpO2, Schritte

### Manifest-Deklarationen

```xml
<!-- Health Connect Permissions (nur READ) -->
<uses-permission android:name="android.permission.health.READ_WEIGHT" />
<uses-permission android:name="android.permission.health.READ_BODY_TEMPERATURE" />
<uses-permission android:name="android.permission.health.READ_SKIN_TEMPERATURE" />
<uses-permission android:name="android.permission.health.READ_RESTING_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_OXYGEN_SATURATION" />
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />

<!-- Health Connect Package Query -->
<queries>
    <package android:name="com.google.android.apps.healthdata" />
</queries>

<!-- Privacy Policy Activity (Pflicht für Health Connect) -->
<activity
    android:name=".HealthPermissionsRationaleActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
    </intent-filter>
</activity>
```

### Gradle Dependency

```gradle
implementation "androidx.health.connect:connect-client:1.1.0-alpha10"
```

### Play Store Auswirkung

- Health Connect Datentypen müssen in der **Play Console deklariert** werden
- Privacy Policy muss Health-Daten-Nutzung erwähnen (nur lesen, nicht teilen)
- Data Safety: "Gesundheitsdaten werden lokal gelesen, nicht erhoben oder geteilt"
- Review kann etwas länger dauern wegen Gesundheitsdaten-Zugriff

### Design-Prinzipien

1. **Komplett optional** — App funktioniert zu 100% ohne Health Connect
2. **Nur lesen** — wir schreiben nie in Health Connect
3. **Graceful Degradation** — fehlende Datentypen werden einfach übersprungen
4. **Kein ständiges Polling** — Daten werden einmalig beim Speichern/Öffnen eines Eintrags abgerufen
5. **Transparent** — User sieht genau welche Daten gelesen werden und kann jederzeit trennen

---

## 7. App-Schutz (Biometrie)

```
App-Start
    │
    ▼
appLockEnabled?  ──Nein──▶  Dashboard
    │
   Ja
    │
    ▼
BiometricPrompt anzeigen
(Fingerabdruck / Gesicht / PIN / Muster)
    │
    ├── Erfolg ──▶ Dashboard
    └── Fehlschlag ──▶ Erneut versuchen / App bleibt gesperrt
```

- Verwendet `androidx.biometric:biometric` (BiometricPrompt API)
- Unterstützt automatisch alle vom Gerät angebotenen Methoden
- Kein eigenes PIN-System — delegiert an Android-Geräteschutz
- `BIOMETRIC_WEAK` oder `DEVICE_CREDENTIAL` als Fallback
- Aktivierung/Deaktivierung in Einstellungen (erfordert einmalige Bestätigung)

---

## 7. Import / Export

### JSON-Format (kompatibel mit PWA)

```json
{
  "version": 1,
  "exportDate": "2026-03-26T10:00:00Z",
  "source": "schlafgut-android",
  "entries": [
    {
      "id": "uuid-xxx",
      "isNap": false,
      "date": "2026-03-25",
      "bedTime": "2026-03-24T23:00:00Z",
      "wakeTime": "2026-03-25T07:00:00Z",
      "sleepLatency": 15,
      "sleepDurationMinutes": 465,
      "wakeDurationMinutes": 0,
      "wakeWindows": [],
      "interruptionCount": 0,
      "quality": 8,
      "tags": ["Kaffee"],
      "notes": "Guter Schlaf"
    }
  ],
  "settings": {
    "userName": "Florian",
    "defaultSleepLatency": 15
  }
}
```

### PWA-Import
- PWA hat Daten unter `schlafgut_entries` und `schlafgut_users` in localStorage
- Android-App bietet "Aus PWA importieren"-Option
- User exportiert localStorage als JSON-Datei (z.B. via Browser-Konsole oder kleines Export-Tool in der PWA)
- Android liest das JSON, konvertiert ISO-Strings zu Epoch millis, schreibt in Room
- Duplikate werden anhand der UUID erkannt und übersprungen

### CSV-Export

```csv
Datum;Typ;Bettzeit;Aufwachzeit;Schlafdauer (h);Wachzeit (min);Einschlafzeit (min);Qualität;Unterbrechungen;Gewicht (kg);Ruhepuls;SpO2 (%);Schritte;Notizen
2026-03-25;Nachtschlaf;23:00;07:00;7.75;0;15;8;0;78.5;58;97;8432;"Guter Schlaf"
```

- Health-Connect-Spalten sind leer wenn keine Daten vorhanden

- Semikolon-getrennt (DE-Standard)
- Via Android Share Intent oder direkter Datei-Export

### PDF-Export
- Landscape A4
- Header: "SchlafGut — Schlafbericht" + Zeitraum
- 24h-Timeline-Grafik (wie auf dem Statistik-Screen)
- Zusammenfassung: Ø Dauer, Ø Qualität, Ø Unterbrechungen
- Bibliothek: **iText** oder **Android PDF Document API**

---

## 8. Farbschema & Theme

### Material 3 Dark Theme

```kotlin
// Basis-Farben (Night-Palette aus der PWA übernommen)
val Night900 = Color(0xFF0F172A)   // Hintergrund
val Night800 = Color(0xFF1E293B)   // Cards
val Night700 = Color(0xFF334155)   // Hover/Pressed
val Night600 = Color(0xFF475569)   // Borders

val Dream500 = Color(0xFF6366F1)   // Primary (Indigo)
val Dream400 = Color(0xFF818CF8)   // Primary Light

// Qualitäts-Farben
val QualityGood = Color(0xFF10B981)     // Emerald (≥8)
val QualityMedium = Color(0xFF6366F1)   // Indigo (5-7)
val QualityPoor = Color(0xFFF43F5E)     // Rose (<5)

// Funktions-Farben
val WakeWindowColor = Color(0xFFFB923C) // Orange
val LatencyColor = Color(0xFF4B5563)    // Gray
val NapColor = Color(0xFFFB923C)        // Orange
val MedianBed = Color(0xFF6366F1)       // Dream/Purple
val MedianWake = Color(0xFF14B8A6)      // Teal
```

---

## 9. Technologie-Stack & Libraries

### Core
| Library | Zweck | Version |
|---|---|---|
| Kotlin | Sprache | 2.0+ |
| Jetpack Compose | UI | BOM 2024+ |
| Compose Navigation | Screen-Routing | 2.8+ |
| Room | Lokale Datenbank | 2.6+ |
| Kotlin Coroutines + Flow | Async + Reactive | 1.8+ |
| Hilt | Dependency Injection | 2.51+ |
| kotlinx.serialization | JSON Parsing | 1.7+ |

### UI & Visualisierung
| Library | Zweck |
|---|---|
| Material 3 | Design System, Components |
| Vico | Charts (Compose-nativ) |
| Compose Canvas | Timeline-Zeichnung (custom) |

### Export
| Library | Zweck |
|---|---|
| Android PdfDocument API | PDF-Erzeugung (built-in) |
| Apache POI (optional) | Excel-Export (falls gewünscht) |

### Health Connect
| Library | Zweck |
|---|---|
| androidx.health.connect:connect-client | Health Connect API (lesen) |

### Backup & Verschlüsselung
| Library | Zweck |
|---|---|
| Google Drive API (play-services-auth + drive v3) | Cloud-Backup in Google Drive |
| Tink (google.crypto.tink) | PBKDF2 + AES-256-GCM Verschlüsselung des Backups |

### Sicherheit
| Library | Zweck |
|---|---|
| androidx.biometric | Fingerabdruck / Gesicht / PIN |

### Testing
| Library | Zweck |
|---|---|
| JUnit 5 | Unit Tests |
| Compose UI Test | UI Tests |
| Turbine | Flow-Testing |

---

## 10. Play Store Vorbereitung

### App-Identität
| Feld | Wert |
|---|---|
| App-Name | **SchlafGut — Schlaftracker** |
| Package | `de.schlafgut.app` |
| Min SDK | 26 (Android 8.0) — ~97% Abdeckung |
| Target SDK | 35 (aktuell) |
| Sprache | Deutsch (Englisch als Stretch Goal) |

### Erforderliche Assets
- App-Icon: 512×512 PNG (Play Store) + Adaptive Icon
- Feature-Graphic: 1024×500 PNG
- Screenshots: Mind. 2 Phone + 2 Tablet (7" und 10")
- Kurzbeschreibung (80 Zeichen): "Tracke deinen Schlaf — lokal, privat, ohne Account"
- Ausführliche Beschreibung (4000 Zeichen)

### Datenschutz
- **Keine Datenerhebung** — alles lokal auf dem Gerät
- Einfache Privacy Policy nötig (kann auf GitHub Pages gehostet werden)
- Data Safety Section: "Keine Daten werden erhoben oder geteilt"
- Keine Werbe-SDKs, kein Analytics, kein Tracking

### Kategorisierung
- Kategorie: **Gesundheit & Fitness**
- Content Rating: **PEGI 3 / Everyone**
- Kostenlos (kein In-App-Purchase)

---

## 11. Implementierungs-Phasen

### Phase 1 — Grundgerüst ✅ (abgeschlossen)
- [x] Android-Projekt aufsetzen (Gradle, Dependencies)
- [x] Room-Datenbank + Entities + DAOs (Schema v1)
- [x] Repository + ViewModels
- [x] Theme (Dark, Farbpalette)
- [x] Navigation Scaffold (Modal Drawer + FAB)
- [x] Settings-Screen (Basis)

### Phase 2 — Sleep Logger ✅ (abgeschlossen)
- [x] SleepLogger-Screen komplett
- [x] DateTime-Picker (Bett/Aufwach)
- [x] Latenz-Slider
- [x] Wachphasen-Editor (Add/Remove/Validierung)
- [x] Qualitäts-Slider
- [x] Validierungslogik
- [x] Speichern + Bearbeiten + Löschen

### Phase 3 — Dashboard ✅ (abgeschlossen)
- [x] Summary Cards (4 Kennzahlen)
- [x] Letzte-Einträge-Liste
- [x] SleepTimeline Composable (Canvas)
- [x] Tap-to-Edit Navigation

### Phase 4 — Statistik ✅ (abgeschlossen)
- [x] Datumsbereich-Filter
- [x] 24h-Timeline (Canvas-Zeichnung, SleepTimeline24h)
- [x] Median-Berechnung + -Linien
- [x] Charts (Vico): Dauer, Qualität, Unterbrechungen
- [x] CSV-Export (semikolongetrennt, Health-Spalten)
- [x] PDF-Export (Landscape A4)

### Phase 5 — Extras ✅ (abgeschlossen)
- [x] Biometrie / App-Schutz (BiometricHelper + RootViewModel)
- [x] JSON-Export (Backup)
- [x] JSON-Import (PWA + Backup)
- [x] Onboarding (erster Start: Name + Latenz einstellen)
- [x] Substanz-Tracking: Alkohol, Drogen, Schlafmittel, Medikamente (Schema v2)
- [x] AllEntries-Screen (vollständige Eintragsliste)

### Phase 5b — Health Connect ✅ (abgeschlossen)
- [x] HealthConnectManager (Verfügbarkeit, Permissions)
- [x] HealthDataRepository (Daten lesen + cachen)
- [x] HealthSnapshot Entity + DAO + Migration (Schema v2)
- [x] Einstellungen: Health Connect Toggle + Permission-Flow
- [x] Anzeige im Dashboard (Icons: Puls, Schritte)
- [x] Export: Health-Spalten in CSV + PDF
- [x] Manifest + Play Console Deklarationen (HealthPermissionsRationaleActivity)

### Phase 5c — Google Drive Backup ✅ (abgeschlossen)
- [x] DriveBackupManager (Upload/Download)
- [x] BackupEncryption (PBKDF2 + AES-256-GCM)
- [x] RegularMedications-Liste in UserSettings (Schema v3)
- [x] Einstellungen: Drive Backup UI

### Phase 6 — Polish & Release ✅ (abgeschlossen)
- [x] ProGuard-Regeln (R8, minify + shrink resources)
- [x] Release-Signing konfiguriert (local.properties)
- [x] Privacy Policy (PRIVACY_POLICY.md)
- [x] targetSdk/compileSdk 35 (Android 15)
- [ ] App-Icon & Store Assets (offen)
- [ ] Play Store Listing + Beta-Test → Release (offen)

---

## 12. Offene Entscheidungen / Hinweise

1. **PWA-Export-Tool**: Die bestehende PWA braucht einen "Daten exportieren"-Button, der localStorage als JSON-Datei herunterlädt. Das ist ein kleines Feature, das wir in der PWA ergänzen sollten.

2. **Onboarding**: Da kein Login mehr existiert, braucht die App einen simplen Erststart-Flow (Name eingeben, Standard-Latenz wählen).

3. **Backup-Erinnerung**: Optional eine sanfte Erinnerung alle 30 Tage: "Möchtest du ein Backup deiner Daten erstellen?"

4. **Tablet-Layout**: Compose unterstützt adaptive Layouts. Für Tablets können wir Dashboard + Detail nebeneinander zeigen (Two-Pane).

5. **Health Connect Scope**: Erstmal nur die wichtigsten Datentypen (Gewicht, Ruhepuls, Schritte, SpO2). Weitere (Blutdruck, Blutzucker, etc.) können später einfach ergänzt werden, da die Architektur modular ist.

6. **Health Connect ohne Wearable**: Viele Daten (Herzfrequenz nachts, SpO2, Hauttemperatur) kommen nur von Wearables. Ohne Wearable sind hauptsächlich Gewicht und Schritte verfügbar — das ist OK, die App zeigt nur was da ist.

7. **Zukünftige Features** (nicht im Scope, aber vorbereitet):
   - Schlaf-Erinnerung (Notification: "Zeit fürs Bett!")
   - Widgets (Letzte Nacht auf dem Homescreen)
   - Wear OS Companion
   - Dunkelmodus / Hellmodus Toggle
   - Health Connect: Schreiben von Schlaf-Sessions (bidirektional)
