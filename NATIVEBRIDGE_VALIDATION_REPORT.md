# NativeBridge Validation Report (Community QA)

Date:
- 2026-05-18

Reporter:
- @yasinULLAH

Validation app used:
- https://github.com/yasinULLAH/phpAndJSnativeAndriodAPKBridgeAccesYasin

## Summary

Built APK was tested on a real Android device and NativeBridge APIs were validated end-to-end.

Result:
- Security APIs working
- Notification APIs working
- Runtime/background APIs working

## Build Info

- Build source: GitHub Actions cloud build
- Workflow: `Build APK`
- Status: Success
- Duration: 12m 44s
- Tested build type: Debug APK

## Device Info

- Model: Infinix X6831
- Manufacturer: INFINIX
- Android: 13 (SDK 33)
- App package: `com.webtoapp`
- App version: 1.9.6 (33)

## Observed Working APIs

Security:
- `isDeveloperOptionsEnabled`
- `isAdbEnabled`
- `isDebuggable`
- `getSecurityInfo`

Notification/runtime:
- `requestNotificationPermission`
- `areNotificationsEnabled`
- `openNotificationSettings`
- `createNotificationChannel`
- `showNotification`
- `scheduleNotification`
- `cancelNotification`
- `cancelAllNotifications`
- `startForegroundService`
- `stopForegroundService`
- `scheduleWorker`
- `scheduleExactAlarm`
- `canScheduleExactAlarms`
- `isDozeMode`
- `isIgnoringBatteryOptimizations`
- `openBatteryOptimizationSettings`
- `getAppState`
- `isAppInForeground`

## Test Outcomes

- Bridge detected in APK mode
- Bridge method count observed: 56
- Foreground notification: triggered
- Sound notification: triggered
- Action/deep-link notification: triggered
- Scheduled notification (15s): triggered
- Worker/alarm wakeup (~30s): requested successfully
- Runtime state checks: returned successfully

## Notes

- Compiler log warnings were observed during cloud build but were non-fatal.
- This report is intended as QA validation feedback for maintainers and contributors.
