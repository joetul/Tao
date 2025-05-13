pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Use a different approach for repository management
// This avoids the incubating API warnings
dependencyResolutionManagement {
    // Instead of using repositoriesMode.set() with the incubating enum
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tao"
include(":app")