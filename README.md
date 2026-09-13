<div align="center">

<img src="assets/gapwise-android.svg" width="116" alt="Gapwise for Android logo" />

# Gapwise for Android

### The native Android client for Gapwise.

**A privacy-first Kotlin + Jetpack Compose app for University of Toronto timetables, built around fast local interaction and a UTM-focused campus layer.**

[![Android](https://img.shields.io/badge/Android-Native-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Native_UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![MIT](https://img.shields.io/badge/License-MIT-111111?style=for-the-badge)](LICENSE)

<sub>Kotlin · Jetpack Compose · Material 3 · Android platform APIs</sub>

<br />

**[Gapwise](https://gapwise.ca)** · **[Android](https://github.com/Gapwise-for-UTM/android)** · **[iOS](https://github.com/Gapwise-for-UTM/ios)** · **[AI](https://ai.gapwise.ca)** · **[Data](https://data.gapwise.ca)** · **[Docs](https://docs.gapwise.ca)** · **[Status](https://status.gapwise.ca)**

</div>

---

## What Gapwise for Android is

Gapwise for Android is the native Android client for **Gapwise**, a privacy-first timetable and campus-intelligence platform for University of Toronto students.

The timetable layer is designed for **UTM, UTSG, UTSC, and mixed-campus schedules**. Campus identity and the original ACORN location string are preserved across all three campuses. The native map and routing layer is intentionally **UTM-focused for now** rather than pretending that St. George or Scarborough rooms belong on the UTM map.

The application is built as a real Android app rather than a WebView wrapper. Native Android owns the interaction layer while shared Gapwise contracts remain the source of truth for deterministic product semantics.

---

## Current implementation

The current native shell includes:

- **local ACORN `.ics` import** through the Android system document picker;
- **UTM, UTSG, UTSC, and mixed-campus timetable parsing**;
- **Today** for the current day's imported meetings;
- **Timetable** with term and weekday views;
- **source-backed locations** retained exactly for non-UTM meetings;
- a **UTM-only map boundary** that recognizes imported UTM destinations without inventing cross-campus map identities;
- a mobile visual shell aligned with the Gapwise web experience;
- light/dark-aware Material 3 theming;
- no network permission in the current timetable-import shell.

Imported timetable state is currently held in memory. Secure persistence, account continuity, native UTM map geometry/routing, and the fuller Gap Plan experience are subsequent implementation layers and are **not claimed as complete here**.

---

## Product principles

- **Local first.** Importing a timetable should not require uploading the original calendar file.
- **All-campus timetable identity.** A UTSG or UTSC room stays a UTSG or UTSC room; campus data is never silently remapped to UTM.
- **UTM map honesty.** Native map features only claim coverage backed by the current UTM campus-data layer.
- **Deterministic planning.** Schedule arithmetic, gap boundaries, route timing, and feasibility belong to explicit, testable logic rather than an LLM.
- **Native interaction.** Navigation, system pickers, lifecycle behavior, accessibility, and device integration should feel natural on Android.
- **Privacy by minimization.** Add storage, sync, authentication, and permissions only when their trust boundaries are deliberate and reviewable.

---

## Architecture

The Android client uses modern native Android tooling:

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Navigation Compose**
- **Coroutines / structured concurrency** as native asynchronous behavior grows
- Android platform document and lifecycle APIs

Current source is organized around explicit product boundaries such as domain models, timetable parsing, navigation, design system, and feature UI.

```text
app/src/main/java/ca/gapwise/android/
├── core/
│   └── model/
├── data/
│   └── timetable/
├── feature/
│   ├── today/
│   ├── timetable/
│   └── map/
└── navigation/
```

As secure persistence, account sync, routing, and additional features land, they should remain isolated behind similarly explicit boundaries rather than being folded into one screen-level state model.

---

## Privacy and security

The current timetable flow is deliberately small:

- the ACORN `.ics` file is selected with the Android system picker;
- parsing happens locally on-device;
- the original calendar file is not uploaded merely to render the timetable;
- a conservative input-size limit is applied before parsing;
- non-UTM location strings are preserved rather than transformed into false UTM destinations;
- the current app manifest does **not** request internet access for this shell;
- imported meetings are currently in-memory only, so the app does not imply persistence or encryption that has not yet been implemented.

Future persistence or account sync must preserve the wider Gapwise security model: app-private storage, platform-backed key protection where appropriate, no privileged server credentials in the client, explicit optional cloud behavior, and narrow permissions.

---

## Local development

Open the repository root in Android Studio and use JDK 17.

```bash
git clone https://github.com/Gapwise-for-UTM/android.git
cd android
```

The project uses the Gradle configuration checked into the repository. Test meaningful changes on an emulator and, before release, on physical Android hardware as well.

---

## Gapwise ecosystem

| Repository | Role | Primary surface |
| --- | --- | --- |
| **[`gapwise`](https://github.com/Gapwise-for-UTM/gapwise)** | Core web/PWA, canonical timetable/gap/routing semantics, public API, OpenAPI, and SDK source | [gapwise.ca](https://gapwise.ca) / [api.gapwise.ca](https://api.gapwise.ca/v1) |
| **[`android`](https://github.com/Gapwise-for-UTM/android)** | Native Kotlin + Jetpack Compose Android client | Android app |
| **[`ios`](https://github.com/Gapwise-for-UTM/ios)** | Native Swift + SwiftUI iOS client | iOS app |
| **[`ai`](https://github.com/Gapwise-for-UTM/ai)** | OAuth/MCP layer for explicitly delegated student context and bounded actions | [ai.gapwise.ca](https://ai.gapwise.ca) |
| **[`data`](https://github.com/Gapwise-for-UTM/data)** | Canonical public UTM campus data, provenance, schemas, validation, and distribution | [data.gapwise.ca](https://data.gapwise.ca) |
| **[`docs`](https://github.com/Gapwise-for-UTM/docs)** | Canonical public developer documentation | [docs.gapwise.ca](https://docs.gapwise.ca) |
| **[`status`](https://github.com/Gapwise-for-UTM/status)** | Independent service-health monitoring and incident communication | [status.gapwise.ca](https://status.gapwise.ca) |

The repositories are separate implementation and trust boundaries, but they form one product. Native clients should consume canonical Gapwise behavior rather than silently becoming independent timetable, routing, or campus-data engines.

---

## Independent project

> **Gapwise is an independent student software project created by Andrew Muratov. It is not affiliated with, endorsed by, or an official service of the University of Toronto.**

## License

Original project code and documentation are available under the [MIT License](LICENSE).

<div align="center">

**Built for the spaces between classes — native on Android.**

[Open Gapwise →](https://gapwise.ca)

</div>
