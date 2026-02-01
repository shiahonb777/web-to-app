# APK Template Notes

This directory stores the precompiled WebView Shell APK template.

## Template Requirements

The template APK (`webview_shell.apk`) must meet the following requirements:

1. Include the `assets/app_config.json` config entry
2. Read the config on startup and load the target URL
3. Support the following config fields:
   - appName: app name
   - targetUrl: target URL
   - activationEnabled: whether activation code is enabled
   - adBlockEnabled: whether ad blocking is enabled
   - and more...

## Dynamic Build Mode

If there is no precompiled template, the system uses the current app as the base for dynamic building.
In this mode, the config is injected into assets/app_config.json.

## Notes

- The template APK must be debug-signed or unsigned
- Icons are replaced and the APK is re-signed during build
- Package name changes require extra ARSC processing (not supported in current version)
