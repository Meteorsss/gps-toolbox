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
