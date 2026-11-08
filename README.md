# Surwi

Surwi is being developed as a public-facing bilingual (Hindi + English), hybrid Android Vedic lifestyle companion.

## Product direction
- Compass 360 and calibration
- Vastu Shastra: 16 direction sectors and room/entrance guidance
- Numerology: Mulank, Bhagyank and name-number calculations
- Vedic astrology: Janma Kundli, Rashi, Lagna, Nakshatra, graha positions and Dasha
- Hindu Panchang: Tithi, Nakshatra, Yoga, Karana, sunrise/sunset and regional festivals
- Muhurat and observance calendar

## Engineering principles
- Offline-first for compass, saved profiles, Vastu reference content and deterministic calculators.
- Hybrid sync for location-specific Panchang, calendar updates and remote content.
- Explicit user-selected birthplace/city; no silent precise-location collection.
- Every astronomical result must identify calculation engine, convention, timezone, source and calculation timestamp.
- Never display guessed Kundli, Panchang, festival or Muhurat values.

See ARCHITECTURE.md for module boundaries, conventions and release gates.

## Current implementation
- Existing compass UI and sensor pipeline.
- Domain foundation for 16 Vastu direction sectors.
- Numerology arithmetic for Mulank, Bhagyank and optional Chaldean name number.
- Birth profile validation.
- Panchang and Vedic chart provider contracts; a validated/licensed calculation engine is not configured yet.
- Local language and city preference storage.

## Build and test
GitHub Actions builds a debug APK on pushes to main and manual workflow dispatch. Open Actions → Build Surwi Compass APK → latest run → Artifacts → surwi-compass-debug-apk.

A debug APK is for device testing only. It is not a signed Play Store release.

## Privacy and permissions
The compass does not need GPS. The product is designed to use a city/place explicitly selected by the user for local calendar calculations. Birth details are sensitive personal data: they must not be sent to a server or persisted until explicit consent and privacy design are implemented. Do not add analytics or advertising SDKs without a documented data review.

## References
- India Meteorological Department, Rashtriya Panchang: https://mausam.imd.gov.in/responsive/rashtriyPanchang.php
- Android app architecture: https://developer.android.com/topic/architecture
- Android offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
