# Surwi Compass 360

Native Android compass for phones with accelerometer and magnetometer sensors.
- Uses TYPE_ACCELEROMETER and TYPE_MAGNETIC_FIELD.
- Calculates heading using Android SensorManager rotation matrix/orientation APIs.
- Displays eight-point direction, degrees and magnetometer accuracy status.
- Includes traditional Vastu / puja direction reference.

## Build
GitHub Actions builds a debug APK on pushes to main and on manual workflow dispatch.
Open Actions > Build Surwi Compass APK > latest run > Artifacts > surwi-compass-debug-apk.
Install the downloaded APK on the Samsung Galaxy S24. You may need to allow installation from the browser or file manager.

## Sensor notes
No location permission, GPS or internet is required for compass operation. It reports magnetic north, not true north. Hold the phone flat, calibrate with a figure-eight motion and keep away from magnetic cases, speakers and metal objects. Accuracy depends on sensor calibration and local interference.
