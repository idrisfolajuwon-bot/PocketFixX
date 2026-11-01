# PocketFix X — Android starter build

A lightweight native Android diagnostic starter for Android 8+ (targeting Android 14 / API 35).

## Included
- On-demand battery level, charging state, battery temperature and Android battery-health flag.
- RAM and internal storage estimates.
- Android version, device model and network connection status.
- A combined diagnostic report with clear limits on what Android exposes.
- Safe Phone Finder placeholder: no covert tracking, password collection or location access.

## Build
This repository includes `.github/workflows/build-apk.yml`. Push to the `main` branch or manually run **Build PocketFix X APK** in GitHub Actions. The workflow builds a debug APK and uploads it as an artifact.

## Important limits
- Battery temperature is not CPU temperature. Ordinary apps cannot read every thermal sensor or reliably identify one exact cause of heating.
- Storage and RAM readings are system estimates.
- No location-sharing service is implemented in this starter. Any future sharing must be opt-in on the device being located.
- This project has not yet been built or tested on a physical Tecno Pop 10C. Treat the APK as unverified until the CI build succeeds and it is installed/tested.
