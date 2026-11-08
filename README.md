# Vedic Compass 360

A bilingual Android companion for compass direction, educational Vastu reference, sunrise-tithi utilities, birth-profile storage and numerology.

## Current beta capabilities

### Available in the app
- **Compass 360:** live magnetic heading from device sensors, accuracy indicator and calibration guidance. True-north declination correction is not enabled.
- **Vastu:** 16-sector direction classification, current phone-bearing reference and general room-placement guide. Educational only; not an engineering or building-code assessment.
- **Panchang:** sunrise-anchored tithi, paksha/lunar-month display, sunrise, sunset and weekday for supported registered cities. Date navigation is available. This is not yet a complete five-limb Panchang.
- **Birth profile:** name, date of birth and birthplace required; birth time, coordinates and timezone optional. Profile is encrypted locally using Android Keystore-backed AES-GCM after explicit consent.
- **Numerology:** Mulank, Bhagyank and optional Chaldean name-number arithmetic.
- **Tithi observance finder:** selected sunrise-tithi matches over the next 120 days. These are candidate tithi matches, not final regional festival assignments.
- **Muhurat planner:** stores event/date/city/tradition planning inputs locally. It does not yet calculate auspicious windows.
- **Kundli workspace:** reads and displays the saved birth profile, but does not yet calculate a validated natal chart.

## Not yet production-enabled
- Full five-limb Panchang: Nakshatra, Yoga, Karana and exact transition times.
- Rahu Kaal, Yamaganda, Gulika, Choghadiya and complete regional festival/vrata observance.
- Validated Kundli engine: Rashi, Lagna, Nakshatra/Pada, graha longitudes, houses, Navamsha, Vimshottari Dasha and compatibility.
- Tradition-specific Muhurat windows.
- True-north correction, global city/timezone resolver and production release signing.

These features must not be advertised as live calculations until their calculation engines, licensing, golden-reference validation and regression tests are complete.

## Product engineering principles
- Never fabricate astronomy, Kundli, Panchang, festival or Muhurat values.
- Every astronomical output must disclose engine, version, location, timezone, method and calculation time.
- Unknown birth time/coordinates must reduce precision and must never be silently guessed.
- Birth details stay on-device in this build; no server upload is implemented.
- No GPS permission is needed for the compass; city selection is manual.
- Vastu and numerology outputs are traditional/educational references, not evidence-based predictions or engineering advice.

## Build and test
GitHub Actions builds a debug APK on pushes to main and manual workflow dispatch:
1. Open **Actions** → **Build Vedic Compass 360 APK**.
2. Open the latest successful run.
3. Download the artifact named **vedic-compass-360-debug-apk**.

A debug APK is for testing only and is not a signed Play Store release.

## Customer documentation
- [Customer Operating Procedure (SOP)](docs/CUSTOMER_SOP.md)
- [Product Architecture](ARCHITECTURE.md)

## Calculation references
- Panchang engine: `io.github.misrilibrary:tithi-engine:5.0.0`, Apache License 2.0.
- Engine scope and documented validation: https://github.com/misrilibrary/tithi-engine
- India Meteorological Department Rashtriya Panchang: https://packolkata.imd.gov.in/panchang/en

## Privacy and release gates
Before production distribution, complete the ephemeris licensing review, privacy policy, consent and retention design, Play Store Data Safety declaration, release signing, Hindi/English review, accessibility testing and golden-reference astronomy tests.


## Proprietary commercial Jyotish engine

Selected provider target: TantraGurukul / Tantrakulam Commercial API, subject to a signed production grant and paid key. The vendor currently advertises ₹36,000/year and 250,000 standard monthly credits for Commercial; credit weights vary. Kundali book PDF/mobile pack and multi-day Panchanga range/month routes are Enterprise-only. See docs/COMMERCIAL_ENGINE_INTEGRATION.md.

The Android client calls a first-party authenticated gateway only. The vendor API key must remain in server-side secret storage. Source scaffolds:
- Android gateway client: app/src/main/java/com/surwi/compass360/domain/astronomy/CommercialAstrologyGateway.kt
- Cloudflare Worker proxy: backend/cloudflare-worker/src/index.ts

The gateway URL is supplied using Gradle property COMMERCIAL_API_BASE_URL and defaults to blank. The gateway is not live until the commercial key, JWT identity service, Cloudflare deployment, secrets, rate limits and privacy controls are configured. Do not put a vendor key or static client token in the APK.
