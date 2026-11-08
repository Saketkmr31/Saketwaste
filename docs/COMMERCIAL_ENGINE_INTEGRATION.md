# Commercial Jyotish Engine — Integration Decision

Product: Vedic Compass 360 (proprietary Android application)
Selected target: TantraGurukul / Tantrakulam Commercial API
Decision status: Provider selected; commercial order, key issuance and production gateway activation pending.

## Why this provider

The vendor's published commercial product describes one REST JSON API for daily Panchanga, daily Muhurta/Hora/Choghadiya, Kundali, divisional charts, Vimshottari and Yogini Dasha, yogas/doshas, gochara and Ashtakoota matching. It advertises both Drik/Lahiri and Sūrya-siddhānta calculation engines and Hindi display fields.

Published Commercial price: ₹36,000 per year, 250,000 standard credits per month. Credit weights vary by endpoint and calculation. Kundali book PDF, mobile Kundali pack and multi-day Panchanga range/kala/month endpoints are Enterprise-only; Enterprise is published from ₹1,20,000 per year. Confirm the current quote, taxes, quota, endpoint entitlements and contract before payment.

## License requirements

- Purchase Commercial or Enterprise in the product owner's/legal entity's name.
- The written grant must explicitly permit production use in Vedic Compass 360 Android app, package ID com.vediccompass360.app, and the planned customer-facing outputs.
- Confirm API proxy use, white-label terms, end-user display, data processing/retention, deletion, subprocessors, support/SLA, quota/credit weights, renewal and cancellation.
- Do not resell, sublicense, mirror or expose the upstream API as a standalone service.
- Do not bulk-extract results to create a competing ephemeris/Panchanga database.
- Free, Developer and Pro tiers do not authorise commercial production use.
- Do not claim Enterprise-only PDF/mobile-pack features under the Commercial plan.

## Architecture

Android app -> first-party authenticated HTTPS gateway -> JWT verification and schema validation -> vendor API using server-side X-API-Key -> JSON response -> app renders results and calculation provenance.

The vendor key must never be included in Android resources, BuildConfig, APK assets, source control or client logs.

## Source-controlled implementation

- Android client: app/src/main/java/com/surwi/compass360/domain/astronomy/CommercialAstrologyGateway.kt
- Cloudflare Worker proxy scaffold: backend/cloudflare-worker/src/index.ts
- Gateway base URL is supplied through Gradle property COMMERCIAL_API_BASE_URL. It defaults to blank and fails closed.

The Worker scaffold validates a short-lived HS256 JWT, allowlists upstream routes and fields, keeps the vendor key in a Worker secret, avoids logging request bodies and returns no-store responses.

Not yet provisioned: commercial vendor key, executed license, JWT issuer/user authentication, Cloudflare Worker deployment, secrets, rate limiting/abuse protection, privacy notice and app UI integration. Do not expose the gateway publicly until all are complete. A static token compiled into an APK is not secure.

## Initial route map

| First-party gateway | Vendor route | Required input |
|---|---|---|
| /api/panchanga | /v1/vedic/panchanga | date, plus place or coordinates and timezone as required |
| /api/kundali | /v1/vedic/kundali | birth date, local birth time, coordinates, timezone |
| /api/muhurta | /v1/vedic/muhurta | date, coordinates, timezone, event code |
| /api/dashas | /v1/vedic/dashas | birth date/time, coordinates, timezone |
| /api/divisional | /v1/vedic/divisionals | birth date/time, coordinates, timezone, chart |
| /api/matching | /v1/vedic/match/ashtakoot | two complete birth profiles |

Confirm exact parameters and response schemas against the current vendor API documentation and a paid test key before rendering results. Do not silently guess coordinates or birth time.

## Personal data controls

Birth date, time and place are personal data. Before the first remote calculation:
- show separate consent that birth details are sent to the calculation provider through Vedic Compass's gateway;
- identify the provider, purpose, retention/deletion policy and any cross-border processing;
- send only fields needed for the chosen calculation;
- exclude birth details from analytics, crash logs and request logs;
- provide profile deletion and a vendor-side deletion workflow if the provider retains payloads;
- obtain legal review of the privacy notice and applicable Indian data-protection obligations.

## Validation and release gates

- Contract tests for every route, required fields, errors, quota exhaustion, 401/403/429/5xx and timeouts.
- Golden natal-chart fixtures validating planetary longitudes, Lahiri ayanamsha, Lagna, Moon nakshatra/pada, D1/D9, house system and Dasha dates.
- Panchanga comparison across multiple Indian cities, boundary days, leap days and historical timezone offsets against a named reference and declared convention.
- Muhurta validation per event and named tradition; no generic auspiciousness score without documented rules.
- Privacy tests proving no vendor key in APK, no PII in logs and explicit remote-processing consent.
- Credit budgets, quota alerts, retry policy (avoid blind retries for billable calls), and caching only as allowed by contract.
- Offline/degraded-state, performance, accessibility, Hindi/English, signing and Play Store release tests.

## Truthful current status

The provider is selected as the target. The Android gateway client and server proxy scaffold are committed. The paid commercial licence/key, JWT identity service and deployed gateway are not available yet. Therefore no live vendor Kundali, Muhurta or complete Panchanga calculation is active in the app. Existing local Tithi calculation remains a limited fallback and must be labelled as such.
