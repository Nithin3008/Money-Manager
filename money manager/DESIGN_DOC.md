# Money Manager — Current Application Design Doc

Purpose of this document: a complete, accurate snapshot of the app's current UI (screens, flows, components, and design tokens) plus the target direction, so new screen designs can be produced against **Material 3 Expressive** without losing product behavior.

Last updated: 2026-07-09. Source of truth is the code — file paths are given per section.

---

## 1. Product Overview

Money Manager is a personal finance Android app (Kotlin + Jetpack Compose, single-activity). Core value: it reads Indian bank SMS alerts and converts them into transactions automatically, alongside manual entry.

Key capabilities:

- Multiple bank accounts with live balances (balance tracking starts from the moment the user onboards; older imported history never moves balances).
- Auto-detected SMS transactions: high-confidence ones import silently, ambiguous ones become **drafts** the user must accept/ignore.
- Manual income / expense / transfer entry.
- Monthly budgets linked to categories, with overspend warnings.
- Monthly reports: cash flow, category mix, daily spend.
- Credit-card transactions tracked but excluded from bank-balance math.
- Investment category treated as expense-like but excluded from summaries.
- Backup/restore to JSON.

Design priorities: fast transaction entry, glanceable daily/monthly status, clearly separated pending-vs-confirmed data, trustworthy numbers, calm density (finance utility, not a landing page).

## 2. Tech Constraints

| Item | Value |
| --- | --- |
| UI framework | Jetpack Compose, Material 3 (`androidx.compose.material3`) |
| Compose BOM | 2025.02.00 (needs bump to a BOM with material3 1.4.x for Expressive APIs) |
| SDK | compileSdk 35, minSdk 26 |
| Navigation | No nav library — one Scaffold, tab content switched inside a shared `LazyColumn` |
| Theming | Custom mutable color tokens (see §6), not dynamic color; both dark and light modes |

## 3. App Entry Flow

1. `InitialLoadingScreen` — blank themed surface while Room state loads.
2. `RegistrationScreen` — user name + optional starting bank accounts (name + current balance rows, pick default). On completion the app stamps the onboarding time, then auto-scans the last 3 months of SMS to backfill history (history only — balances stay as entered).
3. `BankSmsSetupScreen` — maps discovered SMS bank labels to accounts (can create accounts from labels).
4. Main shell — `Scaffold` with `BrandHeader` (logo, greeting, settings shortcut), bottom navigation (5 tabs), FAB `+` for add-transaction (hidden on Profile tab), content in a `LazyColumn`.
5. Conditional prompts: `DefaultBankPrompt` (accounts exist but no default), budget-overspend warning dialog.

## 4. Navigation

Bottom navigation, 5 tabs (`ScreenTab` in `app/src/main/java/com/moneymanager/app/model/FinanceModels.kt`):

| Tab | Label | Icon | Purpose |
| --- | --- | --- | --- |
| Dashboard | Home | GridView | Balance hero, quick stats, pending drafts, today's transactions |
| Activity | Transactions | ReceiptLong | History with date filters + manual SMS scan controls |
| Budget | Budget | PieChart | Budget hero + budget list with progress |
| Summary | Reports | BarChart | Monthly analytics and charts |
| Settings | Profile | Settings | Profile, accounts, categories, appearance, SMS, backup, danger zone |

## 5. Screen Inventory (current composition)

### 5.1 Home (Dashboard) — `ui/screens/dashboard/DashboardContent.kt`

Top to bottom:

1. **Balance hero card** (`FintrackBalanceHero`) — filled accent-colored card: "Current bank balance" label, large balance figure, default account name, wallet icon tile, decorative mini line/bar chart. (A "Bank balance / Monthly spend" pill row was recently removed.)
2. **Quick actions strip** (`FintrackQuickActions`) — counts of accounts, categories, budgets, transactions + current-month investment total.
3. **Review required** section — only when SMS drafts exist today: draft rows (`DetectedDraftRow`) with accept/ignore, paginated.
4. **Today segments** (`FintrackTodaySegments`) — today's income vs expense split.
5. **Transactions** — today's transaction rows (`TransactionRow`), paginated; empty state: "No transactions today. Tap + to add one."

### 5.2 Transactions (Activity) — `ui/screens/activity/ActivityContent.kt`

- Scan panel + date filter panel (Today / Last 7 Days / This Month / Custom with date pickers).
- `MessageScanPanel` — manual SMS scan for Today / Yesterday / Last 7 Days / Custom range.
- Transaction list for the filtered range with "load more" pagination.

### 5.3 Budget — in `ui/MoneyManagerApp.kt`

- **Budget hero** (`FintrackBudgetHero`) — total limit vs spent progress, "Spent" / "Left" mini stats.
- Budget rows (`BudgetRow`) — name, month, linked categories, spent vs limit, progress bar, over-limit state in red, delete action.
- Add budget via bottom sheet.

### 5.4 Reports (Summary) — `ui/screens/summary/SummaryContent.kt`

- Month selector (last 6 months) + per-account filter chips.
- Metric grid: total spent, income, net, opening/closing balance for the month.
- `CashFlowOverviewCard` with per-month bars, `DailyExpenseBarGraph`, `CategoryPieChart` (donut, starts at -90°), `CategoryHistoryGraph`.
- Salary behavior settings (salary-shift window, salary category) — candidates to move to Profile in a redesign.

### 5.5 Profile (Settings) — in `ui/MoneyManagerApp.kt`

`ProfileHeader`, account management (add/edit balance, SMS match keys, default account), category management, currency selector (INR/USD/EUR/GBP), theme mode tiles, accent selector (12 presets), surface selector (6 presets), bank SMS setup, salary summary behavior, backup/restore panel, delete-all-data panel (destructive, isolated at bottom).

### 5.6 Bottom Sheets

| Sheet | Contents |
| --- | --- |
| `AddTransactionSheet` | Big amount card (`FintrackAmountCard`), mode picker Income/Expense/Transfer (`FintrackModePicker`), name, category picker, account picker (from/to for transfers), date, exclude-from-summary toggle, credit-card flag |
| `TransactionDetailSheet` | Edit all fields, type tiles, category grid (can create custom category inline), delete |
| `AddBudgetSheet` | Name, limit, month, category grid with live preview card of already-spent amount |
| `AddCategorySheet` | Name, icon grid, color swatches, live preview card |

## 6. Current Design Tokens

Defined in `ui/theme/Theme.kt` and `ui/theme/Type.kt`. The app uses **mutable global tokens** reassigned per theme/accent/surface selection (a known weakness — a redesign should move these into `MaterialTheme` color roles or a `CompositionLocal`).

### 6.1 Color

Semantic tokens (defaults; accent/surface presets override):

| Token | Dark default | Light default | Use |
| --- | --- | --- | --- |
| `Navy950` | `#141414` | `#F7FAF2` | App background |
| `Navy900` | `#1A1A1A` | `#FFFFFF` | Card / lower surface |
| `Navy850` | `#202020` | `#EFF6E8` | Panel surface |
| `Navy800` | `#2A2A2A` | `#E1ECD8` | Chip / high surface |
| `TextPrimary` | `#F1F1F1` | `#141414` | Primary text |
| `TextMuted` | `#C9C9C9` | `#515151` | Secondary text |
| `TextDim` | `#8C8C8C` | `#737373` | Tertiary text / outline |
| `PrimaryBlue` (accent) | `#B4F077` lime | `#4E7D1C` | Primary action, hero card fill |
| `MoneyGreen` | `#B4F077` | `#4E7D1C` | Income / success |
| `LossRed` | `#FF6B4A` | `#D94A2B` | Expense / destructive |
| `WarningAmber` | `#FFD166` | `#C87400` | Budget warnings |

Note: despite the names, the default look is **neutral charcoal surfaces + lime-green accent** (fintech "Fintrack" style), not navy/blue.

User-selectable presets (`FinanceModels.kt`):

- **12 accents** (dark/light + soft variants each): Metallic Blue `#7EA2FF`, Cool Teal `#3FE0C4`, Raspberry `#FF6FAE`, Goldenrod `#F5C542`, Amethyst `#B794F6`, Aqua Lapis `#35D6E7`, Terracotta `#FF8A65`, Salmon Pop `#FF7A90`, Modern Myrtle `#48D6A5`, Electric Blue `#5BC0FF`, Jade Energy `#54D17A`, Ruby Slate `#F0627D`.
- **6 surfaces** (4 dark + 4 light tones each): Midnight (neutral), Graphite (cool grey), Ocean (deep blue), Plum (purple), Forest (green), Warm (brown).

Color-scheme mapping: `primary`/`primaryContainer` = accent, `secondary` = MoneyGreen, `tertiary` = WarningAmber, `error` = LossRed, surface container ramp = the four surface tones.

### 6.2 Typography (system sans-serif, no custom font)

| Style | Size / line | Weight |
| --- | --- | --- |
| headlineLarge | 28 / 34 | SemiBold |
| headlineMedium | 24 / 30 | SemiBold |
| titleLarge | 20 / 26 | SemiBold |
| titleMedium | 16 / 22 | Medium |
| bodyLarge | 16 / 22 | Regular |
| bodyMedium | 14 / 20 | Regular |
| labelMedium | 13 / 18 | Medium |
| labelSmall | 12 / 16 | Medium |

### 6.3 Shape

| Token | Radius |
| --- | --- |
| extraSmall | 8dp |
| small | 12dp |
| medium | 16dp |
| large | 20dp |
| extraLarge | 24dp |

Hero cards use ~22dp rounded corners; icon tiles ~14dp; pills/chips 12dp.

### 6.4 Spacing & layout

- Screen horizontal padding 20dp, top 18dp, vertical item spacing 16dp.
- Bottom content padding ~104dp to clear nav bar + FAB.
- Card internal padding 16–20dp.

### 6.5 Shared components — `ui/components/`

`ElevatedPanel` (card wrapper), `SectionHeader` (title + right status), `IconTile`, `MoneyChip`, `TransactionTypeChip`, `AddModeChip`, `ChipRow`, `EmptyPanel`, `LabelText` (uppercase label), `TransactionRow`, `DetectedDraftRow`, `DashboardPagination`, `BottomNavigation`, `SheetHeader`/`SheetContent`, `FintrackMiniStat`, `HeroMetricCard`, `FintrackLogoMark`.

## 7. Data the UI Must Represent

- `BankAccount` — name, balance, SMS match key.
- `CategoryItem` — name, icon key, color hex, default/custom.
- `LedgerTransaction` — name, amount, Income/Expense type, category, account, timestamp, auto-detected flag, raw SMS text, `excludeFromSummary`, `isCreditCardTransaction`, optional description. Transfers are stored as paired expense+income transactions.
- `BudgetPlan` — name, limit, linked category ids, month.
- `DetectedTransactionDraft` — bank label, counterparty, amount, type, raw SMS, suggested category, detected/transaction timestamps.
- `BudgetWarning` — budget name, limit, spent.
- Currency display via `state.money()` — INR default, symbol "Rs".

States every design must cover: empty (no transactions/budgets/accounts), loading, pending-review drafts, over-budget warning, no-SMS-permission, and both theme modes across all accent/surface presets.

---

## 8. Target: Material 3 Expressive Redesign

Goal: rebuild the visual layer on **Material 3 Expressive** (material3 1.4+, `MaterialExpressiveTheme`) while keeping every behavior in §3–§5.

### 8.1 Foundation changes

- Replace mutable global color vals with proper `ColorScheme` roles (+ `CompositionLocal` for income/expense/warning semantic colors). Accent presets become seed colors; surface presets map to the `surfaceContainer` ramp.
- Adopt `MaterialExpressiveTheme` with `expressiveLightColorScheme()` / dark equivalent and the Expressive **motion scheme** (spatial springs for navigation/sheets, standard for color/fade).
- Adopt the Expressive **type scale** including emphasized styles — use `displaySmall`/`headlineLarge` emphasized for money figures, keep body/label calm.
- Wider **shape variety**: full-round pills for chips/buttons, 28dp+ for hero cards and sheets, shape-morph on press for primary actions.

### 8.2 Expressive components to adopt per surface

| Current | Expressive replacement |
| --- | --- |
| Custom `BottomNavigation` | Navigation bar with pill indicator (short flexible bar) |
| Lone FAB `+` | **FAB menu** — expands to "Add expense / Add income / Transfer / Scan SMS" |
| Chip rows for date/scan ranges | **Button groups / connected split buttons** |
| Draft accept/ignore text buttons | Filled + tonal button pair, or swipe actions with shape morph |
| Pagination rows | Prefer infinite list with `LoadingIndicator` (Expressive squiggle) |
| Budget/donut progress | Expressive **wavy progress indicators** (linear for budgets, circular for today's split) |
| Bottom sheets | Keep, with drag handle, expressive corner (28dp) and spring motion |
| Section headers | Emphasized-label typography with optional trailing icon-button toolbar |

### 8.3 Screen-level design direction

- **Home**: balance hero stays the anchor (large emphasized figure on `primaryContainer`), quick stats as a 2×2 tonal tile grid, drafts as a visually distinct "needs review" tonal strip (never identical to confirmed rows), today's split as a segmented wavy bar, then transactions.
- **Transactions**: sticky date-range button group on top; scan controls demoted to a secondary toolbar action; rows grouped by day with signed, colored, right-aligned amounts.
- **Budget**: month context + total summary hero (limit/spent/left), budget cards with wavy linear progress, warning color only when ≥ limit.
- **Reports**: filters (month + account) pinned first; one question per card; charts recolored from the categorical accent ramp; keep readable at 360dp width.
- **Profile**: group into Profile / Money setup / Appearance / Automation / Data; danger zone last with confirm dialog.

### 8.4 Acceptance checklist

- Works in dark + light, all 12 accents × 6 surfaces (or their seed-color successors).
- Income/expense/warning colors consistent on every screen and chart.
- Drafts always visually distinct from saved transactions.
- Add-transaction completable one-handed in under ~5 taps.
- No text overflow in nav, chips, rows, or money figures at 360dp / large font scale.
- Empty, loading, over-budget, and no-permission states designed, not accidental.
- Motion: springs on nav transitions, sheet entry, FAB menu; no motion on pure data refresh.
