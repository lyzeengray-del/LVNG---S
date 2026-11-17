# LVNG -X Android

Dark-crimson Android Game Space foundation. Package: `com.lvngx.app`.

## Build with GitHub Actions

1. Upload the contents of this folder to the root of a GitHub repository.
2. Push to `main`/`master`, or run **Actions → Android Build → Run workflow**.
3. Download the `LVNG-X-debug-apk` artifact.

The workflow uses JDK 17 and Gradle 8.9.

## Current implementation

- Game library based on installed launchable applications; game list is stored locally.
- Overlay bubble using `WindowManager`, with drag, edge docking and persisted normalized position.
- Crimson overlay panel and macro trigger overlay.
- Shizuku permission state detection and a shell-input tap repeat prototype.
- Hold/release stop behavior, emergency stop path, screen-off and service teardown stop.
- CPU sampling from `/proc/stat` where readable; RAM from Android `ActivityManager`.
- Backup/restore of local app preferences.
- No billing, paid token, credit, or premium entitlement code.
- No AccessibilityService declaration or dependency.

## Important compatibility notes

This is an implementation foundation, not a claim of universal game compatibility. Shizuku shell `input tap` may be blocked, ignored, or behave differently by OEM, Android release, game anti-cheat, protected surfaces, or input policy. It sends discrete taps, not a true multi-touch finger stream. The macro must be tested in the intended game/device before relying on it.

Android does not expose another app's exact FPS universally. FPS remains unavailable unless a supported system/vendor interface is present. CPU sampling may be restricted on some devices. RAM is device-wide memory information, not per-game RAM usage.

Overlay permission is an Android special permission and must be granted once by the user. Shizuku server must be started and LVNG -X must be authorized in Shizuku. Shizuku does not silently grant overlay permission or bypass Android security dialogs.

The current overlay's module shortcuts may open the corresponding LVNG -X module Activity; full in-overlay editing screens for every module are not yet implemented. Controller remapping/keymapping across arbitrary third-party games is not universally possible through public Android APIs.

## Design references

The `docs/` folder contains concept images for the agreed dark-red home and in-game panel direction. These are design references, not runtime screenshots.

## Permissions

- `QUERY_ALL_PACKAGES`: discover installed launchable apps for the game picker.
- `SYSTEM_ALERT_WINDOW`: floating bubble, macro trigger and panel.
- Shizuku provider: communicate with the user-started Shizuku service.

No Accessibility permission is requested.
