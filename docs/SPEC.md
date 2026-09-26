# Koku for Android (formerly BudgetApp) — Specification

> Status: **Koku 2.0 built** (2026-09-26): renamed Koku, with the iPhone app's new minimalist design (Light / Dark /
> System and six highlight colours), a monthly budget with alerts, an Insights tab, a faster Add screen and notes.
> BudgetApp 1.0 for Android was completed on 2026-09-25. The iPhone version lives in its own repo,
> [bughet-app-ios](https://github.com/Madhav0637/bughet-app-ios), whose
> [spec](https://github.com/Madhav0637/bughet-app-ios/blob/main/docs/SPEC.md) holds the full product decisions.

## 1. Overview

A minimal personal expense tracker whose core advantage is **speed of capture**: one gesture opens a pop-up that asks "On what?", the amount and the category, then saves silently. Home and Insights show where the money went, and an optional monthly budget warns at 80% and 100%. The Android app has the same features and rules as the iPhone app; only the way quick entry is opened differs.

**Success metric:** log an expense in **under 5 seconds**, starting from an unlocked phone, without opening the app.

## 2. Shared rules (same as iOS)

| Area | Rule |
|---|---|
| Name | **Koku** from 2.0 (1.0 was called BudgetApp) |
| Currency | INR only, whole rupees (no paise), shown with Indian digit grouping (₹1,23,456) |
| Data | On the phone only. No account, no server, no sync |
| Entry | Merchant, amount and category are all required. A note is optional (2.0) |
| Date | Quick entry: always "now". In the app (2.0): defaults to now; any date and time can be picked when adding or editing |
| Category step | Quick entry: always picked from the list. In the app (2.0): a merchant used before fills in the category it was last logged under, until a category is tapped by hand |
| Categories | Seven defaults (🍔 Food, 🚕 Transport, 🛍️ Shopping, 🧾 Bills, 🎬 Entertainment, 💊 Health, 📦 Other); user-managed with a name and one emoji; names unique ignoring case |
| Category order | Most-used first; ties alphabetical |
| Category deletion | Blocked while in use or if it's the last one; "Move all expenses to…" empties a category |
| Home | Week, Month or Year, current period only, remembered; weeks start Monday, months on the 1st. 1.0 (then called Dashboard) showed lists only; 2.0 adds the comparison, the budget card and a 7-day chart |
| Insights periods (2.0) | Week, Month or Year, current or any earlier period (arrows or swipe); never the future |
| Comparisons (2.0) | A running period is compared with the same stretch of the previous one (1–26 Sep vs 1–26 Aug), never past that period's end (31 Mar is compared with the whole of February); a finished period with the whole previous one |
| Charts (2.0) | Neutral bars with one highlighted bar (the tapped one, else the peak) and a dashed average line |
| Budget (2.0) | One monthly budget in whole rupees. Alerts at 80% and 100%, each at most once a month, shared by the app (banner) and quick entry (notification) |
| Design (2.0) | Neutral canvas, one highlight colour (mint by default; lime, sky, periwinkle, coral or amber). Light / Dark / System, chosen in the app |
| Activity | Called History in 1.0. Search by merchant (ignores case and accents), category chips, grouped by day with each day's total (2.0), edit, delete with Undo |
| Export | CSV (`Date,Merchant,Category,Amount,Note`; the `Note` column is new in 2.0), escaped and formula-safe, and an A4 PDF report. Files are named `Koku-expenses-yyyy-MM-dd.csv` / `.pdf` (1.0: `BudgetApp-expenses-…`) |
| Lock state | Quick entry works only when the phone is unlocked |
| Left out on purpose | Income, per-category budgets, recurring expenses, widgets, app lock, logging streaks, reminders, cloud sync or backup, multiple currencies, paise, receipts, automatic capture |

## 3. Decision log (Android-specific)

| Area | Decision |
|---|---|
| Stack | Kotlin, Jetpack Compose, Room, ViewModels; JUnit for logic and instrumented tests for the database; `android.graphics.pdf.PdfDocument` for PDF; only Google's Jetpack libraries |
| Device support | Android 8.0 (API 26) and newer, about 97% of phones in use; targets API 37 |
| Package | `com.madhav0637.budgetapp` (same as the iOS bundle ID). Unchanged in 2.0, so Koku installs over BudgetApp 1.0 |
| Distribution | Personal use; shared as a release APK signed with the owner's key (kept outside the repo); no Play Store for now |
| Delete | 1.0: swipe to delete with a 10-second Undo bar (the Android convention). 2.0: swipe to delete, or the delete button on the edit screen, with Undo in Koku's own toast |
| Name (2.0) | Koku as the display name. The package, the tile, the "Log Expense" entry and the shortcut id are unchanged, so data, the tile and pinned icons carry over |
| Design (2.0) | The iOS redesign rebuilt in Compose, with Android conventions where they differ: a bottom navigation bar, Back returns to Home before leaving, Material dialogs and date and time pickers drawn in Koku's colours, the system font with tabular figures |
| Charts (2.0) | Drawn with Compose Canvas, no chart library, which keeps the "only Google's Jetpack libraries" rule. Tap or drag to pick a bar; TalkBack reads a summary. Home's 7-day chart uses plain Compose shapes |
| Theme (2.0) | Colours cross-fade when the theme or highlight changes; the status and navigation bars follow. On Android 12+ the choice is also passed to `UiModeManager.setApplicationNightMode`. Both activities handle `uiMode` changes themselves, so switching theme doesn't restart them |
| Settings (2.0) | One `AppSettings` over `SharedPreferences`, each setting a `StateFlow`, shared by the screens and the pop-up (same process). Key names match the iOS `UserDefaults` keys, and the 1.0 key for Home's period (`dashboardPeriod`) is kept so the choice survives the update |
| Budget alerts (2.0) | `BudgetService` compares this month's spending before and after each save. In the app the alert is a banner; after a quick entry it's a notification on a "Budget alerts" channel, one per level, so a repeat replaces rather than stacks. `POST_NOTIFICATIONS` is requested on Android 13+ when alerts are turned on or a budget is set. If notifications are off, Settings says alerts only appear in the app and links to Android's settings |
| Database (2.0) | Version 2 adds a nullable `note` column (`MIGRATION_1_2`: `ALTER TABLE expenses ADD COLUMN note TEXT`). Room must never fall back to wiping the database. Every schema version is exported to `app/schemas/`, and `MigrationTest` builds a real version-1 database from `1.json` and migrates it |
| Sample data (2.0) | Debug builds only (`app/src/debug`): launch extras swap in an in-memory database with about 11 weeks of spending and a separate settings file, and only if the real database isn't open yet. Release builds get an empty stand-in that ignores the extras |
| App icon (2.0) | An adaptive vector icon with the Koku mark (mint circle, amber and periwinkle pills) and a monochrome layer for themed icons, plus a matching notification icon. Replaces the 1.0 green ₹ icon and the Swift script that drew it |
| Version (2.0) | `versionCode` 2, `versionName` 2.0 |

## 4. Quick entry on Android

No hardware gesture exists on every Android phone, and apps can't listen to the power button. So quick entry is reachable in layers:

| Entry point | Availability |
|---|---|
| **Quick Settings tile** "Log Expense" | Every Android phone (7.0+) |
| **Launcher shortcut**: long-press the app icon → Log Expense (can be pinned to the home screen) | Every Android phone |
| **Brand gestures**: Samsung side-button double press, Pixel Quick Tap, and similar settings on other brands | Most phones; set the gesture to open the separate **"Log Expense"** launcher entry |

The "Log Expense" entry is a second launcher icon that opens straight into the pop-up. It exists because brand gesture settings can open an app but not a specific screen. The cost is a second icon in the app drawer, which was accepted.

**The pop-up** is the app's own translucent window drawn over whatever app is on screen, not a full-screen app. It has the same three steps as iOS (On what? → Amount → Category, shown as "1 of 3" and so on), and **both text steps use the same big, bold style**, which iOS's system prompts could not offer. From 2.0 it follows Koku's theme and highlight colour, and if the saved expense takes the month past 80% or 100% of the budget for the first time, a notification says so; otherwise the save stays silent.

Settings → **Set up quick entry** explains all three, with one-tap buttons to add the tile (Android 13+) and to pin a home-screen icon.

## 5. Koku 2.0 on Android

### Data model

One change: each expense gains an optional note.

| Field | Type | Rules |
|---|---|---|
| `Expense.note` | `String?` (nullable `TEXT` column) | Optional; trimmed, and a blank note is stored as null. Rows from 1.0 get null. Shown in italics in expense rows and exported in the CSV's `Note` column |

### Tab 1: Home
- Koku logo and a period menu (This week / This month / This year), remembered
- Hero total for the period, and "↓ 12% vs same time last month" when there was earlier spending
- Budget card for the current month: progress, what's left (or how much over), roughly what can be spent per day; tapping it edits the budget. Without a budget: "Set a monthly budget"
- Last 7 days: seven bars, today's in the highlight colour with its amount, and the daily average
- Where it went: the top 3 categories with share bars, and a link to Insights
- Recent: the 5 latest expenses, "See all" opens Activity; tap one to edit
- Empty state: "Nothing spent this month"
- A floating **+** button opens Add Expense; toasts (Undo, budget alerts) appear beside it

### Tab 2: Activity
- Search by merchant (case- and accent-insensitive, all dates)
- Category chips ("All" plus every category, most used first); combine with search
- List grouped by day, newest first, with each day's total
- Swipe to delete, then Undo in a toast; tap to open Edit Expense
- A floating **+** button opens Add Expense

### Tab 3: Insights
- Week / Month / Year picker (remembered); arrows or a swipe on the total move to earlier periods and back, never past now
- Period title ("21 – 27 Sep", "September 2026", "2026"), total, comparison pill ("vs same time last month" while running, "vs July" once finished), number of expenses and average per day
- Bar chart by day (by month for a year), drawn on a Canvas; tap or drag for a bar's total, otherwise the peak is highlighted; dashed average line
- Categories: one split bar (biggest in the highlight colour) and each category's amount, count and share
- Tiles: biggest spend, most visited merchant, average per day, no-spend days
- Top 5 merchants by amount

### Tab 4: Settings
- **Appearance:** Theme (System / Light / Dark) and Highlight (six colours)
- **Budget:** monthly budget, and budget alerts (on by default, available once a budget is set; asks for notification permission on Android 13+; notes when notifications are off)
- **Data:** Categories (usage order with expense counts; edit name and emoji, "Move all expenses to…", delete only when empty), Set up quick entry, Export (CSV or PDF, opened in the share sheet)

### Add / Edit Expense
- Amount first on a custom keypad (1–9, 00, 0, delete; hold delete to clear), and "₹X left this month after this" when there's a budget and the expense is in this month
- "On what?" with suggestions from merchants used before; choosing one fills in its last category ("picked from Zomato")
- Category chips, optional note, and a date pill (Today / Yesterday / a date) that opens a date and time dialog
- The save button says what's missing ("Enter an amount", "Add what it was for", "Pick a category") until it reads "Save ₹420"; saving shows a checkmark
- **Edit:** the same screen, pre-filled, with a delete button (and Undo afterwards)

### Monthly budget sheet
- Keypad with presets from ₹5,000 to ₹50,000, last month's total as a guide, "Set budget · ₹X", and Remove

## 6. Build plan

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
| A8 | **Koku 2.0**: Koku design and themes, monthly budget and alerts, Insights, faster Add screen with suggestions, notes and dates, Activity day totals, the 1 → 2 database migration, README and screenshots | ✅ 2026-09-26 |

**Tests:** 1.0 shipped with 46 JVM tests for the domain layer and 18 on-device tests against an in-memory Room database. 2.0 adds JVM suites for insights and comparisons, budget pace and alerts, merchant suggestions, the keypad, notes in the CSV and the Insights titles, and on-device tests for notes, restore, the budget check around a save and the 1 → 2 migration. The README lists what each suite covers.
