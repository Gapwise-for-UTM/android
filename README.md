<p align="center">
  <img src="assets/gapwise-android.svg" width="220" alt="Gapwise for Android logo" />
</p>

<h1 align="center">Gapwise for Android</h1>

<p align="center">
  <strong>Make the time between classes count — natively on Android.</strong>
</p>

<p align="center">
  A native Kotlin + Jetpack Compose client for Gapwise, designed for the University of Toronto Mississauga and St. George campuses from day one.
</p>

> [!IMPORTANT]
> **Early development.** This repository is being built from the ground up as the native Android home for Gapwise. The architecture, timetable model, campus data, and map experience are being designed before the first public release.

## What this repo is

Gapwise for Android is a native Android application for understanding the day around a university timetable: what is next, where it is, how much usable time exists between classes, and where a student can realistically go before they need to leave.

The Android app is not intended to be a wrapper around the web client. It is being designed specifically for Android with native navigation, interactions, haptics, offline-friendly data, and campus-aware maps.

The first supported campuses are:

- **UTM — University of Toronto Mississauga**
- **UTSG — University of Toronto St. George**

The project remains proudly part of **Gapwise for UTM** while making the timetable and map experience useful to students across more of U of T.

## First-release goals

The initial Android release is focused on doing a small number of things exceptionally well:

- Import a student's ACORN timetable.
- Preserve campus identity for every course and meeting.
- Render a polished native timetable with excellent day-to-day interactions.
- Treat UTM and UTSG as first-class campuses rather than using one campus as a fallback for the other.
- Resolve real campus locations such as `MN 1270` and `BA 1170` instead of incorrectly displaying them as `TBA`.
- Open timetable locations directly on the correct campus map.
- Provide a clear **Today / Timetable / Map** experience.
- Keep core timetable calculations deterministic and available locally.

## Campus-aware by design

A short building code is not globally unique across U of T. Gapwise therefore treats campus as part of a location's identity.

```kotlin
CampusLocation(
    campus = Campus.UTSG,
    buildingCode = "BA",
    room = "1170"
)
```

This means a location is never interpreted against the wrong campus simply because two campuses happen to share a building code.

For St. George in particular, the goal is full integration rather than merely preserving raw timetable text. A known UTSG room should resolve to its building, room, map position, and useful navigation context. `TBA` should mean that the source timetable actually has no confirmed location — not that Gapwise does not recognize the campus.

## Planned technical foundation

The Android client is being built around modern native Android tooling:

- **Kotlin**
- **Jetpack Compose**
- **Material 3**, adapted to the Gapwise visual language
- **Navigation Compose**
- **Coroutines + StateFlow**
- **Room** for structured local data where appropriate
- **DataStore** for preferences
- Native Android map integration with campus-specific data layers

The codebase will be organized so timetable logic, campus data, UI, and platform services remain cleanly separated.

```text
app/
core/
  model/
  campus/
  database/
  designsystem/
  navigation/
feature/
  today/
  timetable/
  map/
  settings/
```

## Product principles

**Native, not transplanted.** Android should feel like Android while still unmistakably feeling like Gapwise.

**Campus identity is explicit.** UTM and UTSG data must never be silently mixed or guessed across campuses.

**Source data is preserved.** If ACORN provides a physical location, Gapwise should retain it even when richer map data is temporarily unavailable.

**Privacy first.** Timetable data and deterministic schedule calculations should stay local whenever practical.

**Correctness before cleverness.** Routing, time arithmetic, campus resolution, and timetable interpretation should be explainable and testable rather than delegated to a language model.

## Development status

The repository is currently in the **bootstrap and architecture phase**. The first implementation milestone is:

```text
native shell
→ campus model
→ timetable model
→ ACORN import
→ timetable UI
→ UTM + UTSG building/location data
→ campus maps
→ timetable-to-map integration
```

## Related Gapwise projects

- [Gapwise web app](https://github.com/Gapwise-for-UTM/gapwise)
- [Gapwise mobile](https://github.com/Gapwise-for-UTM/gapwise-mobile)
- [Gapwise data](https://github.com/Gapwise-for-UTM/gapwise-data)
- [Gapwise documentation](https://github.com/Gapwise-for-UTM/gapwise-docs)

## License

MIT License. See [`LICENSE`](LICENSE).

---

<p align="center">
  <strong>Gapwise for UTM</strong><br />
  Privacy-first campus tools built at the University of Toronto.
</p>
