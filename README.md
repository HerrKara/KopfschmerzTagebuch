# Kopfschmerztagebuch (Android)

Tagebuch für Kopfschmerzen: mehrere Einträge pro Tag mit Beginn, Ende, Stärke (1–10), Medikament und Notiz.
Export als PDF-Bericht (Zusammenfassung, Verlaufsgrafik, Tabelle) über den Android-Druckdialog – „Als PDF speichern“ oder direkt drucken.

- Fertige App: `Kopfschmerztagebuch.apk` (Installation von „unbekannten Quellen“ erlauben)
- Neu bauen: `./build.sh` (JDK, aapt, dx, zipalign, apksigner, android.jar API 23)
- Oberfläche: `app/src/main/assets/index.html`, native Hülle: `MainActivity.java`
- Daten liegen lokal in der App (`files/tagebuch.json`); Sicherung/Wiederherstellung über „Auswertung & PDF“.
