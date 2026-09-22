# Fix Navigation and Tab Redirection

The navigation system currently fails because the tab routes (`home`, `search`, `profile`) are not registered in the `NavHost`, and the `main` route doesn't handle its own initial state correctly.

## Proposed Changes

### [app]

#### [MODIFY] [AppNavHost.kt](file:///C:/Users/Student/AndroidStudioProjects/cinePulse/app/src/main/java/com/cinepulse/app/ui/AppNavHost.kt)
- Move the `Scaffold` to wrap the entire `NavHost`.
- Register `home`, `search`, and `profile` as separate `composable` destinations.
- Update the `startDestination` to `"home"` when authenticated.
- Add a helper to determine if the bottom bar should be visible based on the current route.
- Remove the redundant `"main"` route.

## Verification Plan

### Manual Verification
- Deploy the app to BlueStacks.
- Verify that the Home screen appears immediately after login (or if already authed).
- Click the "Search" and "Profile" tabs and verify they display the correct screens.
- Navigate to a "Detail" screen and verify that the bottom navigation bar disappears (standard behavior for detail views).
- Use the back button to return to the tabs.
