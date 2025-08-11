// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.googleFirebaseCrashlytics) apply false
    alias(libs.plugins.googleFirebasePerf) apply false
    alias(libs.plugins.googleFirebaseAppdistribution) apply false
}