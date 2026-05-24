# Money Manager UI Redesign Guide

This document summarizes the current app UI so the interface can be redesigned without losing important product behavior.

## Product Summary

Money Manager is a personal finance Android app built with Kotlin and Jetpack Compose. It helps users track accounts, daily transactions, auto-detected SMS transactions, budgets, monthly summaries, and app preferences.

The current UI is a Material 3 Compose app with:

- Bottom navigation across five primary sections.
- Dark/light theme support.
- User-selectable accent colors and surface styles.
- Card-based finance dashboards.
- Bottom sheets for create/edit flows.
- Manual and auto-detected transaction workflows.

## Primary Users

The app is aimed at individual users who want quick daily money tracking, especially users who receive bank SMS alerts and want those messages converted into reviewable transaction drafts.

Design priorities should be:

- Fast transaction entry.
- Clear spending and income visibility.
- Low-friction review of detected transactions.
- Trustworthy account and budget numbers.
- Calm, readable financial reporting.

## Current Navigation

The main app uses a bottom navigation bar with five tabs.

| Tab | Current Label | Purpose |
| --- | --- | --- |
| Dashboard | Today | Daily overview, today transactions, pending detected SMS drafts, shortcut to summary |
| Activity | Activity | Transaction history by date range, SMS scan/update controls |
| Budget | Budget | Monthly budget plans and spending progress |
| Summary | Summary | Monthly financial report, charts, account filters |
| Settings | Settings | Profile, accounts, categories, currency, theme, SMS setup, backup/restore |

Source: `app/src/main/java/com/moneymanager/app/model/FinanceModels.kt`

## App Entry Flow

1. `InitialLoadingScreen`
   - Blank themed loading surface while state initializes.

2. `RegistrationScreen`
   - Captures user name.
   - Lets user optionally create starting bank accounts.
   - Allows account draft rows with name and balance.

3. Main App Shell
   - `Scaffold`
   - Bottom navigation.
   - Floating add button, hidden on Settings.
   - Shared top `BrandHeader`.
   - Tab content rendered inside a vertical `LazyColumn`.

4. Required Prompts
   - If accounts exist but no default account is selected, `DefaultBankPrompt` appears.
   - If a budget limit is crossed, a warning dialog appears.

## Screen Inventory

### Dashboard / Today

Current components:

- `BrandHeader`
- `TodayDonutCard`
- `TodayCategoryBreakdown`
- Pending detected transaction section
- Today's transaction list
- Pagination controls
- `ActionPanel` shortcut to monthly analysis

Main jobs:

- Show today's total movement quickly.
- Show category split for today.
- Let users accept or ignore detected SMS drafts.
- Let users open and edit a transaction.

Redesign notes:

- Make pending SMS drafts visually distinct from confirmed transactions.
- Put the most important daily number above charts.
- Consider grouping income and expense totals separately before showing net.
- Empty state should invite adding or scanning transactions.

### Activity

Current components:

- `ActivityScanPanel`
- `ActivityDateFilterPanel`
- `MessageScanPanel`
- Transaction list
- Load more pagination

Main jobs:

- Review historical transactions.
- Filter by Today, Last 7 Days, This Month, or Custom range.
- Manually scan SMS messages for selected periods.
- Populate sample/demo history for recent months.

Redesign notes:

- Keep date filtering highly visible.
- Separate scanning controls from transaction history so the screen does not feel like a settings page.
- Transaction rows should show date, category, account, name, and signed amount without crowding.

### Budget

Current components:

- Budget list rows.
- Spending progress bars.
- Add budget bottom sheet.
- Delete budget action.

Main jobs:

- Track spending limits per month.
- Link budgets to one or more categories.
- Warn when spending crosses a monthly limit.

Redesign notes:

- Show remaining amount, spent amount, and limit in each budget row.
- Use warning color only when attention is required.
- Consider a budget summary header: total limit, total spent, total remaining.

### Summary

Current components:

- `MonthSelector`
- `SummaryAccountFilterRow`
- `MetricGrid`
- `MonthBalanceStrip`
- `CashFlowOverviewCard`
- `CashFlowGraph`
- `DailyExpenseBarGraph`
- `CategoryPieChart`
- `CategoryHistoryGraph`
- Salary/category behavior settings

Main jobs:

- Explain monthly income, expense, net, opening balance, closing balance, and category mix.
- Filter reporting by account.
- Show cash flow and daily/category breakdowns.

Redesign notes:

- This is the most analytics-heavy screen. Use strong information hierarchy.
- Put month and account filters at the top.
- Keep charts readable and avoid overloading one card with too many ideas.
- Distinguish report income from calendar-month account movement when salary shifting is enabled.

### Settings

Current components:

- `ProfileHeader`
- Account settings
- Default account selection
- Category management
- Currency selector
- Theme selector
- Accent selector
- Surface selector
- SMS setup
- Salary summary behavior
- Backup/restore panel
- Delete all data panel

Main jobs:

- Manage app identity and preferences.
- Manage accounts and categories.
- Configure theme appearance.
- Export/import backup data.

Redesign notes:

- Settings should be grouped into clear sections: Profile, Money Setup, Appearance, Automation, Data.
- Destructive actions should be isolated at the bottom.
- Theme controls can be more compact because they are secondary to finance tasks.

## Core Create/Edit Flows

### Add Transaction Sheet

Supports:

- Income
- Expense
- Transfer
- Name
- Amount
- Category
- Account
- Date
- Summary exclusion
- Credit card transaction flag

Redesign goal:

Make the fastest common path very short: type amount, choose type, choose category/account, save.

### Transaction Detail Sheet

Supports:

- Editing transaction details.
- Changing category.
- Creating a custom category from the edit flow.
- Deleting transaction.

Redesign goal:

Use a detail layout that makes destructive actions visibly separate from normal edits.

### Add Budget Sheet

Supports:

- Budget name.
- Limit amount.
- Month.
- Linked categories.

Redesign goal:

Show selected categories as clear chips and preview how much has already been spent in those categories.

### Add Category Sheet

Supports:

- Category name.
- Icon selection.
- Color swatches.

Redesign goal:

Make icon and color choices scannable in a grid, with strong selected states.

## Current Design System

### Theme Tokens

Defined in:

- `app/src/main/java/com/moneymanager/app/ui/theme/Theme.kt`
- `app/src/main/java/com/moneymanager/app/ui/theme/Type.kt`

Core semantic colors:

| Token | Current Use |
| --- | --- |
| `Navy950` | App background |
| `Navy900` | Card/input lower surface |
| `Navy850` | Panel/card surface |
| `Navy800` | Chip/high surface |
| `TextPrimary` | Main readable text |
| `TextMuted` | Secondary text |
| `TextDim` | Tertiary/disabled text |
| `PrimaryBlue` | Primary action/accent |
| `PrimarySoft` | Soft selected/accent text |
| `MoneyGreen` | Income/success |
| `LossRed` | Expense/destructive |
| `WarningAmber` | Budget warning |

The app currently supports:

- `ThemeMode.Dark`
- `ThemeMode.Light`
- 12 accent presets: Sky, Mint, Rose, Amber, Violet, Cyan, Coral, Pink, Emerald, Indigo, Teal, Slate
- 6 surface presets: Midnight, Graphite, Ocean, Plum, Forest, Warm

### Typography

Current type scale:

| Style | Size | Weight |
| --- | --- | --- |
| `headlineLarge` | 36sp / 42sp | ExtraBold |
| `headlineMedium` | 26sp / 32sp | Bold |
| `titleLarge` | 21sp / 28sp | Bold |
| `titleMedium` | 16sp / 24sp | SemiBold |
| `bodyLarge` | 18sp / 28sp | Regular |
| `bodyMedium` | 16sp / 24sp | Regular |
| `labelMedium` | 14sp / 20sp | Bold |
| `labelSmall` | 12sp / 16sp | Bold |

Redesign recommendation:

Keep the app readable and operational. Avoid oversized marketing-style text inside dashboards. Finance screens need dense but calm hierarchy.

### Shapes

Current Material 3 shapes are expressive:

| Shape | Radius |
| --- | --- |
| `extraSmall` | 12dp |
| `small` | 16dp |
| `medium` | 22dp |
| `large` | 28dp |
| `extraLarge` | 36dp |

Main cards use rounded panels, often around 22dp. Bottom sheets and major controls also use rounded corners.

### Spacing

Current shell layout:

- Screen horizontal padding: 20dp.
- Screen top padding: 18dp.
- Main vertical item spacing: 16dp.
- Bottom content padding: 104dp to clear navigation/FAB.
- Most cards use 16dp to 20dp internal padding.

### Common Components

| Component | Role |
| --- | --- |
| `ElevatedPanel` | Shared card/panel wrapper |
| `SectionHeader` | Section title plus right-side action/status |
| `IconTile` | Icon badge for rows/cards |
| `MoneyChip` | General selectable chip |
| `TransactionTypeChip` | Income/expense chip |
| `AddModeChip` | Income/expense/transfer mode chip |
| `BottomNavigation` | Main app navigation |
| `EmptyPanel` | Empty list/card message |
| `LabelText` | Uppercase section label |
| `ChipRow` | Horizontally scrolling chip row |

## Data Elements The UI Must Represent

Important models:

- `BankAccount`: name, balance, optional SMS match key.
- `CategoryItem`: name, icon, default/custom flag, color.
- `LedgerTransaction`: name, amount, type, category, account, date, auto-detected flag, raw SMS, summary exclusion, credit card flag.
- `BudgetPlan`: name, limit, linked categories, month.
- `DetectedTransactionDraft`: bank, name, amount, type, counterparty, raw message, suggested category, detected date.
- `FinanceUiState`: full UI state for selected tab, filters, theme, accounts, categories, transactions, budgets, sheets, warnings.

## Redesign Principles

1. Prioritize scan speed.
   Users should understand today's spending, monthly status, and budget risk in a few seconds.

2. Separate confirmed and pending data.
   SMS-detected drafts should never look exactly like saved transactions.

3. Make money states obvious.
   Income, expense, balance, remaining budget, and warning states need consistent color and sign treatment.

4. Keep charts secondary to decisions.
   Charts should answer a specific question: where did money go, how did cash flow change, or what category is increasing.

5. Use calm density.
   This is a finance utility, not a landing page. Avoid huge decorative hero sections and excessive empty space.

6. Preserve theme flexibility.
   Any redesigned component should still work in dark mode, light mode, custom accent, and custom surface styles.

## Suggested Redesign Structure

### Home / Today

- Header: greeting, selected/default account balance.
- Primary card: today spent, today income, today net.
- Pending review strip: count of detected drafts with quick review.
- Category breakdown: compact horizontal or donut view.
- Recent transactions: clear rows with amount aligned right.

### Activity

- Sticky or top filter area for date range.
- Transaction timeline grouped by date.
- Manual update action placed below filters or behind a secondary action.
- Search can be added later if transaction volume grows.

### Budget

- Month selector.
- Total budget summary.
- Budget cards with progress, remaining amount, linked categories, and risk state.
- Add budget action.

### Summary

- Month and account filters.
- Metrics: opening balance, income, expense, net, closing balance.
- Cash flow chart.
- Category chart.
- Daily spend chart.
- Reconciliation/exception note only when needed.

### Settings

- Profile.
- Accounts.
- Categories.
- Appearance.
- SMS/automation.
- Backup and restore.
- Danger zone.

## Implementation Map

Primary UI files:

- `app/src/main/java/com/moneymanager/app/ui/MoneyManagerApp.kt`
- `app/src/main/java/com/moneymanager/app/ui/MessageScanPanel.kt`
- `app/src/main/java/com/moneymanager/app/ui/theme/Theme.kt`
- `app/src/main/java/com/moneymanager/app/ui/theme/Type.kt`
- `app/src/main/java/com/moneymanager/app/model/FinanceModels.kt`

Best redesign starting points:

1. Update theme tokens and typography in `Theme.kt` and `Type.kt`.
2. Redesign shared components at the bottom of `MoneyManagerApp.kt`, especially `ElevatedPanel`, `SectionHeader`, chips, transaction rows, and navigation.
3. Redesign one tab at a time in this order:
   - Dashboard
   - Activity
   - Budget
   - Summary
   - Settings
4. Then redesign bottom sheets.
5. Finally review empty, warning, destructive, and loading states.

## Acceptance Checklist For A UI Redesign

- App works in both light and dark modes.
- Text does not overflow in bottom navigation, chips, buttons, cards, or transaction rows.
- Income and expense colors are consistent everywhere.
- Pending SMS drafts are visually different from saved transactions.
- Add transaction can be completed quickly with one hand.
- Budget warning state is clear but not visually noisy.
- Summary charts are readable on small Android screens.
- Destructive settings actions are separated and confirmed.
- Empty states are useful and do not look broken.
- All five tabs still support their current behavior.

