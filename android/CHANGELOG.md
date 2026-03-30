# Changelog

## v1.3.0 (versionCode 4)

### 🔐 Google Drive Backup
- Migration von GoogleSignIn (deprecated) zur neuen AuthorizationClient API
- Robusteres Error-Handling mit klarer Fehleranzeige im UI

### ❤️ Health Connect
- Individuelle Datentyp-Auswahl (Gewicht, Temperatur, Puls, HF, SpO₂, Schritte, Schlaf)
- Health-Daten werden automatisch beim Speichern eines Eintrags gelesen
- Health-Daten im Schlaf-Logger anzeigen, aktualisieren und entfernen
- Datentyp-Toggles starten deaktiviert – User wählt explizit aus

### 💊 Medikamente
- Feste Medikamente werden sofort beim Hinzufügen/Entfernen gespeichert
- Medikament im Textfeld wird beim „Einstellungen speichern" automatisch übernommen
- Reguläre Medikamente sind jetzt im Schlaf-Logger sichtbar

### 🛠️ Weitere Verbesserungen
- Versionsnummer dynamisch aus BuildConfig.VERSION_NAME
- Native Debug-Symbole im AAB (ndk.debugSymbolLevel = FULL)
- Deprecated Gradle-Properties entfernt (AGP 10.0 Vorbereitung)
- hiltViewModel Import auf neues Package migriert
- LocalContext → LocalActivity (Lint-Error behoben)
- @Inject Annotation Targets explizit gesetzt
- Locale-Fixes in CsvExporter und PdfExporter
- ProGuard-Regeln präzisiert (Drive-Modell-Klassen)
- mutableIntStateOf in DateTimePickerDialog
- KTX Extension String.toUri() in HealthConnectManager

## v1.2.0 (versionCode 3) – nicht veröffentlicht
- Zwischenversion (Fixes zusammengeführt in v1.3.0)

## v1.1.0 (versionCode 2) – nicht veröffentlicht
- Zwischenversion (Fixes zusammengeführt in v1.3.0)

## v1.0.0 (versionCode 1)
- Erste Veröffentlichung
