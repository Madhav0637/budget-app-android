# BudgetApp for Android — Specification

> Status: **built** (all milestones complete, 2026-09-25). The iPhone version lives in its own repo,
> [bughet-app-ios](https://github.com/Madhav0637/bughet-app-ios), whose
> [spec](https://github.com/Madhav0637/bughet-app-ios/blob/main/docs/SPEC.md) holds the full product decisions.

## 1. Overview

A minimal personal expense tracker whose core advantage is **speed of capture**: one gesture opens a pop-up that asks "On what?", the amount and the category, then saves silently. A dashboard shows where the money went. The Android app has the same features and rules as the iPhone app.

## 2. Shared rules (same as iOS)

| Area | Rule |
|---|---|
| Currency | INR only, whole rupees (no paise), shown with Indian digit grouping (₹1,23,456) |
| Data | On the phone only. No account, no server, no sync |
| Entry | Merchant, amount and category are all required; the date is "now" and editable later |
| Categories | Seven defaults (🍔 Food, 🚕 Transport, 🛍️ Shopping, 🧾 Bills, 🎬 Entertainment, 💊 Health, 📦 Other); user-managed with a name and one emoji; names unique ignoring case |
| Category order | Most-used first; ties alphabetical |
| Category deletion | Blocked while in use or if it's the last one; "Move all expenses to…" empties a category |
| Dashboard | Week, Month or Year (current period only), weeks start Monday, months on the 1st; lists, no charts |
| History | Search by merchant (ignores case and accents), category filter, grouped by day, edit and delete |
| Export | CSV (`Date,Merchant,Category,Amount`, escaped, formula-safe) and an A4 PDF report |
| Lock state | Quick entry works only when the phone is unlocked |

## 3. Decision log (Android-specific)

| Area | Decision |
|---|---|
| Stack | Kotlin, Jetpack Compose, Room, ViewModels; JUnit for logic and instrumented tests for the database; `android.graphics.pdf.PdfDocument` for PDF; only Google's Jetpack libraries |
| Device support | Android 8.0 (API 26) and newer, about 97% of phones in use; targets API 37 |
| Package | `com.madhav0637.budgetapp` (same as the iOS bundle ID) |
| Distribution | Personal use; shared as a release APK signed with the owner's key (kept outside the repo); no Play Store for now |
| Delete | Swipe to delete with a 10-second Undo bar (the Android convention) |

## 4. Quick entry on Android

No hardware gesture exists on every Android phone, and apps can't listen to the power button. So quick entry is reachable in layers:

| Entry point | Availability |
|---|---|
| **Quick Settings tile** "Log Expense" | Every Android phone (7.0+) |
| **Launcher shortcut**: long-press the app icon → Log Expense (can be pinned to the home screen) | Every Android phone |
| **Brand gestures**: Samsung side-button double press, Pixel Quick Tap, and similar settings on other brands | Most phones; set the gesture to open the separate **"Log Expense"** launcher entry |

The "Log Expense" entry is a second launcher icon that opens straight into the pop-up. It exists because brand gesture settings can open an app but not a specific screen. The cost is a second icon in the app drawer, which was accepted.

**The pop-up** is the app's own translucent window drawn over whatever app is on screen, not a full-screen app. It has the same three steps as iOS (On what? → Amount → Category), and **both text steps use the same big, bold style**, which iOS's system prompts could not offer.

Settings → **Set Up Quick Entry** explains all three, with one-tap buttons to add the tile (Android 13+) and to pin a home-screen icon.

## 5. Build plan

| # | Milestone | Status |
|---|---|---|
| A0 | Android Studio project, runs in the emulator | ✅ |
| A1 | Quick-entry pop-up, Room database, tile, launcher shortcut, "Log Expense" entry | ✅ |
| A2 | Rules and calculations ported from iOS, with JVM and on-device tests | ✅ |
| A3 | History: search, category chips, day headings, swipe to delete with Undo, edit sheet | ✅ |
| A4 | Dashboard and bottom tab bar | ✅ |
| A5 | Settings tab and category management | ✅ |
| A6 | CSV/PDF export, quick-entry setup guide, green ₹ icon | ✅ |
| A7 | README and screenshots | ✅ |
| — | Signed release APK for sharing; split into its own repo | ✅ |

**Tests:** 46 JVM tests for the domain layer and 18 on-device tests against an in-memory Room database.
