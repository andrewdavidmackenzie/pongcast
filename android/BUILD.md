
# IntelliJ Config
You need to add modules (Importing from existing sources) for each of these Google/Android libraries:
- mediarouter
- appcompat
- google play services
- CastCompanionLibrary-android

The first three will be installed in subfolders of your Android SDK folder:
- mediarouter
- appcompat

Those can be found in $ANDROID_SDK_HOME/extras/android/support/v7/

- google play services

That can be found in $ANDROID_SDK_HOME/extras/google/google_play_services/lib_project/google-play-services_lib/

Importing those should detect the Android Facet of them.
They should all end up as "Library Projects" in IntelliJ, you can see that by looking at the Android Facet tab inside each of the modules in Project Structure dialog.

Each one needs to include the jar files in it's /lib/ subdirectory as dependencies for the module.

I have included CastCompanionLibrary-android as a module in this project.
It has chanegd in the past, and the latest version can be found here on github.
https://github.com/googlecast/CastCompanionLibrary-android.git

## CastCompanionLibrary-android Dependencies
* google-play-services_lib library from the Android SDK (at least version 4.2)
* android-support-v7-appcompat (version 19.0.1 or above)
* android-support-v7-mediarouter (version 19.0.1 or above)


Then remember to add those modules as dependencies on the main project "android" (/pongcast/android).

## Dependencies Summary

Android (App) Module
  +-> CastCompanionLibrary-android Module
  +-> AppCompat Module
  +-> Google Play Services Lib Module
  +-> MediaRouter Module

CastCompanionLibrary-android Module
  +-> Google Play Services Lib Module
  +-> Media Router Module
  +-> AppCompat Module

Google Play Services Lib Module
  +-> google-play-services JAR
  +-> AppCompat Module

MediaRouter Module
  +-> Media Router JAR
  +-> AppCompat Module

AppCompat Module
  +-> android-support-v7-appcompat JAR
  +-> android-support-v4 JAT
