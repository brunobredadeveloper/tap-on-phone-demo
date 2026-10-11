pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TapOnPhoneDemo"

include(":payment-sdk", ":core")

if (providers.gradleProperty("includeAndroid").orNull == "true") include(":app")
