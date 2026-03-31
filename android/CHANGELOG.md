# Changelog

## v1.3.1 (versionCode 5)

### ❤️ Health Connect – Zeiträume korrigiert
- Gewicht: Immer den letzten verfügbaren Wert (bis 365 Tage zurück)
- Körpertemperatur: Nur wenn max. 24h alt
- Ruhepuls & SpO₂: Letzte 7 Tage
- Herzfrequenz (Nacht): Während der Schlafzeit
- Schritte: Ganzer Tag des Schlafens
- Health-Daten werden automatisch beim Speichern eines Eintrags gelesen

### 🔐 Google Drive Backup – Cloud Console konfiguriert
- OAuth Client ID für Android registriert
- AuthorizationClient API korrekt eingerichtet

### 💊 Medikamente
- Medikament im Textfeld wird beim „Einstellungen speichern" automatisch übernommen
- Textfelder leben im ViewModel-State (nicht mehr lokal im Screen)

### 🛠️ Code-Cleanup
- Locale-Fixes in CsvExporter und PdfExporter
- mutableIntStateOf in DateTimePickerDialog
- KTX Extension String.toUri() in HealthConnectManager
- ProGuard-Regeln präzisiert
- Unused imports entfernt

## v1.3.0 (versionCode 4)

### 🔐 Google Drive Backup
- Migration von GoogleSignIn (deprecated) zur neuen AuthorizationClient API
- Robusteres Error-Handling mit klarer Fehleranzeige im UI

### ❤️ Health Connect
- Individuelle Datentyp-Auswahl (Gewicht, Temperatur, Puls, HF, SpO₂, Schritte, Schlaf)
- Health-Daten im Schlaf-Logger anzeigen, aktualisieren und entfernen
- Datentyp-Toggles starten deaktiviert – User wählt explizit aus

### 💊 Medikamente
- Feste Medikamente werden sofort beim Hinzufügen/Entfernen gespeichert
- Reguläre Medikamente sind jetzt im Schlaf-Logger sichtbar

### 🛠️ Weitere Verbesserungen
- Versionsnummer dynamisch aus BuildConfig.VERSION_NAME
- Native Debug-Symbole im AAB
- Deprecated Gradle-Properties entfernt
- hiltViewModel Import migriert, LocalContext → LocalActivity
- @Inject Annotation Targets explizit gesetzt

## v1.0.0 (versionCode 1)
- Erste Veröffentlichung
