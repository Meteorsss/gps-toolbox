# Map and track insights: manual acceptance

- This feature remains in an unmerged PR. Repository stays public; no user coordinates or screenshots committed.
- Upgrade signed preview over 0.1.2; verify old tracks and places remain. Export important data before upgrade. Do not downgrade database v2 to v1.
- Decline online consent: local map shows track and location without roads. Other pages continue functioning.
- Allow online: pan/zoom on mobile data and Wi-Fi; check fonts, attribution, loaded streets, and coordinate alignment in your actual area. WGS84 positions are never transformed to GCJ-02 for this basemap.
- Walk in an open area; start map recording, drag to stop following, recenter, pause, resume, finish, inspect full track and gaps. Leave map/app and turn screen off; recording must continue via foreground service.
- Enable airplane mode mid-recording: GPS and recording should continue; map may use cached tiles or show load error. Restore network and retry.
- Check old track altitude metrics show unknown when vertical accuracy unavailable. Flat-ground walk should not create large climb; hill ascent/descent should be treated as filtered GNSS estimates, not ground truth.
- Check MSL vs ellipsoid label, current speed vs overall/moving averages, highest speed and raw point validity. Unknown time must not be silently counted as stationary.
- Verify location permission revocation, orientation change, background/foreground and closing the map do not crash or restart recording.

- While recording, leave map detailed statistics open; verify values refresh without reopening, including after returning from background and finishing recording.
- From a historical track, recenter to current location; rotate or change basemap, and verify live-follow mode remains selected without auto-fitting the old track again.

## Cloud verification and preview delivery (2026-11-21)

- Signed preview build #12: https://github.com/Meteorsss/gps-toolbox/actions/runs/44849840678
- APK source commit: `cfe21c2a6cd1550f76aa4d90f60e301f77b1edf1`.
- 71 unit tests, 13 Android 16 instrumentation tests, release lint and APK signature/permission verification passed. No tests failed or were skipped.
- Regressions include live map statistics updating while open, pause/background/resume and final persisted statistics, recentered live mode across recreation, and retained history-map camera zoom.
- Artifact: `StarTrace-PR-preview-apk`; install `StarTrace-0.2.0-pr-build-12.apk`, not a PR debug APK. Existing release signing certificate is retained.
- APK SHA-256: `5a1cfe3ec5a3fd7199e16642c4e924ebbc0f82efe2507df7e2c010a017a15780`.
- All fixtures and screenshots use synthetic data. Cloud emulator results do not replace Redmi K70 / HyperOS 3 field acceptance, mobile-network availability or battery testing.
- PR stays open and unmerged, main unchanged, repository public, no new Release.

## ARM64 Pre-release requested by owner (2026-11-21)

- The owner explicitly requested publishing this version as a GitHub Pre-release, like the preceding three releases. The earlier no-new-Release restriction is superseded for this delivery only.
- Release APK includes only `arm64-v8a`; cloud verification checks every bundled native library is ELF64 AArch64, with matching APK badging and fixed signing certificate.
- Debug builds retain emulator-compatible ABIs for Android 16 tests and are not uploaded as Release assets.
- Publishing targets the verified feature-branch commit; the PR remains unmerged, main unchanged and repository public.

## KML export and centered speed (0.2.1-pr)

- Confirm the dashboard speed title and value/unit row are centered in both themes and after recreation.
- Finished-track actions and details include KML, GPX and CSV. Recording must be stopped before exporting.
- Save a KML through DocumentsUI, rotate while the picker is open, and verify `.kml`/MIME/selected track are retained. Cancel without writing or changing the source.
- Import a synthetic KML in Google Earth / My Maps and verify WGS84 alignment, separate sections, singleton points and no invented altitude. Do not upload real user tracks for testing.
- Google Earth and My Maps import limitations are documented in KML-IMPORT.md; no direct Google Maps app import or automatic cloud upload is claimed.
