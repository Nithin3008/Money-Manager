# Money Manager

Money Manager is a personal finance Android app built with Kotlin and Jetpack Compose.

## Current MVP

- Name-based registration.
- Optional bank account setup with starting balances.
- Default categories: Grocery, Food, Shopping, Fuel, and Rent.
- Custom category creation.
- Manual transaction entry with name, amount, income/expense type, category, and optional account.
- Monthly budgets linked to one or more categories.
- Warning dialog when spending crosses a monthly budget limit.
- Monthly summary with income, expenses, net total, graph, and category pie chart.
- SMS transaction parser and today-SMS scanner scaffold for NEFT, RTGS, debited, credited, and UPI style alerts.
- Notification listener scaffold for bank app transaction notifications.

## Next Development Steps

1. Add Room persistence for accounts, categories, transactions, budgets, and detected transaction drafts.
2. Wire notification detections into the same pending review list as SMS detections.
3. Add proper runtime permission education screens for SMS and notification access.
4. Replace the in-memory ViewModel with repositories and database-backed monthly aggregates.
5. Add edit/delete flows for transactions, budgets, accounts, and categories.
