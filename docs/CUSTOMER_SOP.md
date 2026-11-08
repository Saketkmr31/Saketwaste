# Vedic Compass 360
## Customer Operating Procedure (SOP) & User Guide

**Document ID:** VC360-SOP-001  
**Document version:** 1.0  
**Product build:** Android beta – debug distribution  
**Language:** English / Hindi (mixed interface)  
**Owner:** Product & Support Team  
**Review cadence:** Before every public release

---

## 1. Purpose

This SOP explains how a customer installs and operates Vedic Compass 360, selects a city, uses the compass, explores Vastu reference guidance, views the currently supported Panchang calculation, saves a private birth profile, and uses the numerology calculator.

This document also defines the limits of the current beta. A screen being visible in the app does not mean its underlying calculation engine is complete.

## 2. Product overview

Vedic Compass 360 is an Android companion for direction awareness and selected Vedic-calendar utilities. The product is designed around:
- Compass 360: live magnetic heading and sensor accuracy.
- Vastu: 16-sector direction reference and general room-placement guidance.
- Panchang: sunrise-anchored tithi, paksha, sunrise and sunset for supported registered cities.
- Birth profile: optional birth time and coordinates; encrypted local storage after explicit consent.
- Numerology: Mulank, Bhagyank and optional Chaldean name-number arithmetic.
- Kundli, Muhurat and observance calendar: screens are present, but the full calculation capabilities described in Section 8 are not yet production-enabled.

## 3. Installation and first launch

1. Obtain the APK only from the official Vedic Compass 360 GitHub Actions artifact shared by the product owner.
2. Confirm the artifact name and build number before installation.
3. Android may ask permission to install apps from the browser or file manager. Enable this only for the trusted installer you are using.
4. Open the app and confirm the title is **Vedic Compass 360**.
5. Do not install APKs forwarded from unknown sources.
6. This is a debug/test build, not a Play Store-signed production release. Do not distribute it as a final commercial release.

## 4. Select the city

1. Open Home.
2. Tap **Change city**.
3. Enter the city name exactly as supported by the calculation library.
4. Save the city.
5. Open Panchang and confirm the displayed canonical city.
6. If the city is unsupported, the app must show an error and must not silently substitute a different city.

**Important:** City-based Panchang calculations use the selected city and the configured timezone. The current build uses Asia/Kolkata; it is not yet a global timezone-aware location picker.

## 5. Compass 360 operating procedure

1. Open the **Compass** tab.
2. Hold the phone flat and away from metal objects, speakers, magnetic phone covers and vehicle dashboards.
3. Move the phone slowly in a figure-eight pattern if the sensor reports low accuracy or calibrating.
4. Wait for the displayed heading and accuracy indicator to stabilise.
5. Rotate the phone and verify that the heading changes smoothly.
6. If the heading jumps or drifts, move away from magnetic interference and recalibrate.

**Interpretation:** The current display is magnetic heading. It is not corrected to true north using local magnetic declination. Do not use it for surveying, navigation safety, construction layout or legal boundary work.

## 6. Vastu reference procedure

1. Open the **Vastu** tab.
2. Review the current phone bearing as a directional reference only.
3. Select the room type from **Choose room**.
4. Read the commonly encountered traditional direction association and the practical caution shown.
5. Review the 16 direction sectors for reference.

**Customer caution:** Vastu traditions differ. These are general educational associations, not a universal classical ruling. The app is not an architectural, structural, fire-safety, electrical or building-code assessment. Never demolish, relocate utilities or undertake costly construction based solely on this screen. Consult a qualified architect/engineer for building decisions.

## 7. Panchang procedure (currently supported scope)

1. Open the **Panchang** tab.
2. Confirm the selected city.
3. Use **Change date** to select a civil date, or use Previous day / Next day.
4. Review the tithi at local sunrise, paksha, sunrise, sunset and weekday.
5. Review the calculation method and engine label shown on screen.
6. For a second opinion, open the official India Meteorological Department Rashtriya Panchang link.

**Current calculation scope:** sunrise-anchored tithi, paksha/lunar-month display and sunrise/sunset for supported registered cities. This is not yet a complete five-limb Panchang. Nakshatra, Yoga, Karana, exact ending times, Rahu Kaal, Yamaganda, Gulika, Choghadiya and a complete regional festival engine must not be represented as available until separately implemented and validated.

## 8. Birth profile and Kundli procedure

### 8.1 Save a profile
1. Open **Profile**.
2. Enter profile name, date of birth and birth city/place.
3. Birth time is optional. Enter it in 24-hour HH:MM format if known.
4. Latitude and longitude are optional. Enter both together only if known.
5. Timezone is optional and defaults to Asia/Kolkata.
6. Tick the explicit on-device storage consent.
7. Tap **Save private profile**.
8. To correct details, edit and save the profile again.
9. To erase the saved profile, use **Delete saved profile**.

The profile is encrypted with Android Keystore-backed AES-GCM and stored locally in this build. It is not uploaded to a server by this app version. Deleting the profile also removes its local encryption key.

### 8.2 Accuracy guidance
- Date of birth and birthplace are required for a meaningful chart calculation.
- Exact local birth time materially affects Lagna/Ascendant and house cusps.
- If time is unknown, leave it blank. Do not invent a time.
- If coordinates are unknown, use the birthplace text and leave both coordinates blank until a verified place resolver is available.
- Approximate inputs must be labelled approximate. Exact Lagna/house claims must not be made from unknown or approximate time.

### 8.3 Current Kundli status
The saved profile is connected to the Kundli workspace and its details are displayed there. However, this beta does not yet calculate a validated natal chart. Rashi, Lagna, Nakshatra/Pada, planetary longitudes, houses, Navamsha, Dasha and compatibility must not be shown as calculated results until the ephemeris engine and golden-reference tests are released.

## 9. Numerology procedure

1. Open Home → **Numerology**.
2. Confirm the prefilled name and date of birth from the saved profile, or enter them manually.
3. Tap **Calculate**.
4. Review Mulank, Bhagyank and optional Chaldean name number.
5. Read the arithmetic method displayed with the result.

Numerology is a traditional symbolic system and is not a scientifically validated method for predicting personality, health, relationships, career or financial outcomes. Do not use it as a substitute for professional advice or evidence-based decisions.

## 10. Tithi and Vrat calendar

The current screen finds selected sunrise-tithi matches (for example Ekadashi, Purnima, Amavasya and selected other tithis) over a forward date window. These are tithi matches, not final regional festival declarations. Smarta, Vaishnava and regional observance rules can assign different dates. Verify festival and fasting decisions with the relevant regional Panchang or religious authority.

## 11. Muhurat planner

The current Muhurat screen can store an event, preferred date, city and tradition as a local planning request. It does not yet calculate or rank auspicious windows. No Muhurat should be promised to a customer from this beta.

## 12. Privacy and data handling

- Compass operation does not require GPS permission.
- City selection is manual in this build.
- Birth profile storage requires explicit consent.
- Birth profile data is encrypted locally using Android Keystore-backed AES-GCM.
- The app does not upload birth-profile data in this build.
- Users can delete the stored profile from the Profile screen.
- Do not add analytics, advertising, cloud sync or remote astrology computation without a separate privacy review and clear customer consent.
- Customers should not enter another person's birth details without appropriate permission.

## 13. Troubleshooting

| Issue | Recommended action |
|---|---|
| Compass shows Calibrating | Move phone in a figure-eight; avoid metal/magnets |
| Compass direction jumps | Remove magnetic case; move away from speakers/vehicles |
| Panchang says city unsupported | Enter a city supported by the engine; do not guess a nearby city |
| Birth profile not visible | Reopen Profile, check save confirmation and consent |
| Kundli shows engine unavailable | Expected in this beta; do not interpret as a generated chart |
| Numerology validation error | Use a valid past DOB in YYYY-MM-DD format |
| App install blocked | Verify the APK source and enable installation only for the trusted installer |

## 14. Customer support information to capture

When reporting a problem, provide:
- App version and build/run number.
- Android version and device model.
- Screen name and exact steps performed.
- Selected city and date (do not send birth details unless absolutely necessary).
- Screenshot with personal details hidden.
- Error message and approximate time of occurrence.

Never ask a customer to send passwords, account credentials or unnecessary personal birth information.

## 15. Production release acceptance checklist

A production release must not be approved until all applicable checks pass:

- [ ] Release APK signed with protected production key.
- [ ] Full five-limb Panchang and timing calculations validated against authoritative references.
- [ ] Festival/vrata observance rules documented by region and tradition.
- [ ] Kundli engine selected with licensing review completed.
- [ ] Planetary longitudes, Lahiri ayanamsha, Lagna, houses, Navamsha and Vimshottari Dasha validated against golden test charts.
- [ ] Unknown birth time/coordinates correctly trigger approximate-result behaviour.
- [ ] Muhurat rules documented, tested and attributed to a defined tradition.
- [ ] City and timezone resolver validated, including daylight-saving transitions where applicable.
- [ ] Hindi and English translations reviewed.
- [ ] Accessibility, font scaling, TalkBack and device-size tests passed.
- [ ] Privacy policy, consent, retention and deletion behaviour reviewed.
- [ ] Play Store Data Safety declarations completed.
- [ ] Crash, regression and performance tests passed on representative Android devices.
- [ ] Customer-facing support and escalation process published.

## 16. Reference sources

- India Meteorological Department, Rashtriya Panchang: https://packolkata.imd.gov.in/panchang/en
- Tithi engine documentation and licence: https://github.com/misrilibrary/tithi-engine
- Android Keystore: https://developer.android.com/privacy-and-security/keystore
- Android app signing: https://developer.android.com/studio/publish/app-signing

---

**Customer-facing statement:** Vedic Compass 360 provides direction, selected Vastu reference, sunrise-tithi and numerology tools in its current beta. Advanced astrology and auspicious-time calculations are being validated and are not represented as live calculations until their calculation engines and verification gates are complete.


## 17. Commercial astrology engine status

The product has selected TantraGurukul / Tantrakulam's Commercial API as the intended proprietary Jyotish provider, subject to a signed commercial grant and paid production key. The provider's published Commercial plan currently lists Panchanga, Muhurta, Kundali, divisional charts, Dasha and matching; some PDF/mobile-pack and bulk calendar features are Enterprise-only.

**This integration is not live in the current APK.** The app must not display vendor-derived Kundli, full Panchang or Muhurat results until the commercial licence is active, the secure first-party gateway and authentication are deployed, privacy consent is implemented, and calculation regression tests pass. Existing local Tithi output remains a limited feature, not a complete Panchang.

Commercial engine details and release gates: see the product's Commercial Engine Integration document in the source repository.
