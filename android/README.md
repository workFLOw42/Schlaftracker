# 🌙 SchlafGut – Schlaftracker für Android

**SchlafGut** ist eine native Android-App zur umfassenden Erfassung und Analyse deines Schlafverhaltens. Die App hilft dir, deinen Schlaf besser zu verstehen und langfristig zu verbessern.

## ✨ Features

- **Schlafprotokoll** – Erfasse Bettzeit, Aufwachzeit, Einschlaflatenz, Wachphasen, Schlafqualität (1–10) und persönliche Notizen
- **Nickerchen-Tracking** – Naps separat erfassen mit eigener Statistik (Häufigkeit, Durchschnittsdauer)
- **Substanzkonsum** – Dokumentiere Alkohol, Drogen, Schlafmittel und Medikamente
- **Dashboard** – Übersicht über letzte Einträge, Durchschnittsqualität, Schlafdauer und Gesundheitsdaten
- **Statistiken & Charts** – Visuelle Auswertungen deines Schlafverhaltens mit Vico-Charts
- **Health Connect Integration** – Automatischer Import von Gewicht, Herzfrequenz, Sauerstoffsättigung, Schritte und Körpertemperatur
- **Datenexport** – Export als CSV, PDF oder JSON
- **Google Drive Backup** – Verschlüsselte Sicherung deiner Daten in Google Drive
- **Onboarding** – Geführte Ersteinrichtung
- **Biometrische Authentifizierung** – Optionaler Schutz per Fingerabdruck / Face Unlock
- **Material Design 3** – Modernes UI mit Jetpack Compose

## 🏗️ Architektur & Tech-Stack

| Bereich | Technologie |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Architektur | MVVM, StateFlow |
| DI | Hilt / Dagger |
| Datenbank | Room |
| Gesundheitsdaten | Health Connect API |
| Charts | Vico (compose-m3) |
| Backup | Google Drive API + Tink Encryption |
| Serialisierung | Kotlinx Serialization |
| Build | Gradle (Kotlin DSL), KSP |

## 📋 Voraussetzungen

- Android Studio Ladybug oder neuer
- JDK 17
- Android SDK 35 (compileSdk)
- Minimum SDK 26 (Android 8.0)

## 🚀 Build & Ausführen

```bash
# Projekt klonen
git clone https://github.com/<user>/Schlaftracker.git
cd Schlaftracker/android

# Debug-Build erstellen
./gradlew assembleDebug

# Auf verbundenem Gerät installieren
./gradlew installDebug
```

## 📁 Projektstruktur

```
app/src/main/java/de/schlafgut/app/
├── data/
│   ├── backup/          # Google Drive Backup & Verschlüsselung
│   ├── db/              # Room Database & DAOs
│   ├── entity/          # Datenmodelle (SleepEntry, HealthSnapshot, UserSettings)
│   ├── export/          # CSV-, PDF- und JSON-Export
│   ├── health/          # Health Connect Repository
│   └── repository/      # Daten-Repository
├── di/                  # Hilt Dependency Injection Module
├── ui/
│   ├── allentries/      # Alle Einträge anzeigen
│   ├── components/      # Wiederverwendbare UI-Komponenten
│   ├── dashboard/       # Startseite / Dashboard
│   ├── logger/          # Schlaf-Eingabeformular
│   ├── navigation/      # Navigation Graph & Screen-Definitionen
│   ├── onboarding/      # Ersteinrichtung
│   ├── settings/        # Einstellungen
│   ├── statistics/      # Statistiken & Charts
│   └── theme/           # Material Theme & Farben
├── util/                # Hilfsfunktionen
├── MainActivity.kt
└── SchlafGutApp.kt      # Application-Klasse (Hilt Entry Point)
```

## 🔒 Datenschutz & Sicherheit

- **Lokale Datenspeicherung** – Alle Schlafdaten werden ausschließlich lokal auf deinem Gerät gespeichert. Es gibt keinen Server, keinen Account und keine Datenübertragung an Dritte.
- **Verschlüsselte Backups** – Google Drive Backups sind mit AES-256-GCM verschlüsselt. Die Verschlüsselung basiert auf deinem persönlichen Passwort (PBKDF2). Ohne dieses Passwort kann niemand – auch nicht Google – den Inhalt lesen.
- **Kein Tracking, keine Werbung** – Die App enthält keine Analyse-SDKs, kein Telemetrie und keine Werbenetzwerke.
- See [Privacy Policy](https://workflow42.github.io/Schlaftracker/privacy-policy.html).

## 📄 Lizenz

Dieses Projekt ist privat. Alle Rechte vorbehalten.
