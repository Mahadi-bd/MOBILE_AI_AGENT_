<div align="center">
  <img src="app/src/main/res/drawable-nodpi/life_easy_logo.png" width="140" alt="LIFE EASY logo"/>

# LIFE EASY

AI assistant, tasks, notes and weather in one Android app – English & বাংলা.

**Developed by MAHADI HASAN JEWEL**
</div>

## Features
- Animated 3D flying robot assistant that talks (mouth moves while it speaks), male / female voice
- AI chat with Google Gemini – **every user enters their own API key** in *Persona → AI settings* (stored encrypted on the phone, nothing built in)
- Real voice input (English / Bangla) using Android speech recognition
- Tasks, notes, income/expense list – stored locally with Room, no account
- Live weather by city (Open-Meteo, no key)
- Neon "circuit" UI with a floating bottom bar

## Tech
Kotlin · Jetpack Compose · Room · OkHttp · Gemini REST API · Open-Meteo · custom glTF renderer for the robot

## Build
1. Open in Android Studio (JDK 17+ / 21) and run – no `.env` or API key is needed.
2. Or use the GitHub Actions workflow in `.github/workflows/build-and-release.yml` (Actions → *Build APK and publish release* → Run workflow), then download `LIFE-EASY.apk` from **Releases**.

Release / Play Store steps: [`docs/GITHUB_GUIDE_BN.md`](docs/GITHUB_GUIDE_BN.md) and [`docs/GITHUB_AND_PLAY_STORE_GUIDE.md`](docs/GITHUB_AND_PLAY_STORE_GUIDE.md).
Privacy policy: [`docs/privacy-policy.md`](docs/privacy-policy.md). Credits: [`docs/CREDITS.md`](docs/CREDITS.md).

© MAHADI HASAN JEWEL
