# Surwi — Product Architecture (Public / Hindi + English / Hybrid)

## Product decisions
- Distribution target: public Android release; Play Store readiness is a separate release gate.
- Languages: Hindi and English. User can change language in settings.
- Connectivity: hybrid. Compass, saved profiles, Vastu reference content and basic calculators must remain usable offline. Network data is cached locally and refreshed when available.
- Location: ask the user to choose a city/place. Do not infer or silently collect precise location.
- Birth profile: collect date, optional exact local birth time, place, coordinates and IANA time zone only after explicit user action. Explain why each field is needed.
- Trust: show calculation method, source, timezone and last-updated time for astronomical/calendar outputs. Never invent missing results.

## Layering
Presentation (screens, navigation, Hindi/English, accessibility) → Domain (use cases and pure calculations) → Repositories (single source of truth) → Local cache + remote sources + calculation providers.

### Current packages
- domain/numerology: deterministic Mulank, Bhagyank and Chaldean name-number arithmetic.
- domain/vastu: 16-sector bearing classification and qualified traditional associations.
- domain/profile: birth-profile validation; distinguishes exact from approximate birth time.
- domain/astronomy: explicit provider contracts and metadata. No fabricated Panchang or Kundli output.
- data/preferences: local language and user-selected city preferences.

## Feature modules and acceptance gates
### Compass (offline)
Magnetic heading, sensor accuracy and interference guidance. True-north display only after validated declination and explicit location selection. Test cardinal/intercardinal bearings, 0/360 wrap, portrait/landscape, tilt and magnetic interference.

### Vastu (offline)
16-sector compass classification; room/entrance assessment with source and school labels; separate classical-text references from modern popular conventions. No fear-based claims, forced demolition advice or guaranteed outcomes.

### Numerology (offline)
Mulank (day-of-month root), Bhagyank (full DOB digit root), optional Chaldean name number. Every result states its arithmetic convention. Alternative master-number conventions are explicit. Interpretations are traditional beliefs, not evidence-based predictions.

### Janma Kundli (hybrid)
Requires date, local birth time, selected birthplace, coordinates and timezone. Calculate Lagna, Moon sign, Nakshatra/Pada, graha longitudes, houses, divisional charts and Dasha only through a validated ephemeris. Proposed default: sidereal zodiac with Lahiri ayanamsha; expose convention and allow later override. Unknown/approximate birth time disables exact Lagna and house claims.

### Panchang and Hindu calendar (hybrid)
Tithi, Paksha, Vara, Nakshatra, Yoga, Karana, sunrise/sunset and observance windows. Location and timezone are mandatory for local day boundaries and festival observance. Cross-check against IMD Rashtriya Panchang and regional observance sources. Preserve region/tradition differences; never hardcode annual festival dates. Cache last successful results with source and timestamp.

### Muhurat (hybrid)
Explain rule set, location, timezone and tradition used. Never present one universal auspicious time when traditions or regional rules differ.

## Public-release requirements (not yet complete)
- Production signing key and protected release workflow; never commit signing secrets.
- Privacy policy, in-app consent, profile deletion and data retention rules.
- Data safety declaration and Play Console disclosures.
- Network API security, rate limits, caching, error states and source attribution.
- Accessibility, screen-size testing, Hindi typography and TalkBack.
- Automated unit tests for pure calculations and golden-reference astronomy tests.
- Legal review of ephemeris/library licences before distribution.

## Current implementation status
Implemented foundations are not finished modules. Astronomy interfaces are contracts only; no production Panchang or Kundli engine is configured. Current GitHub workflow produces a debug APK for testing, not a Play Store release.
