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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // SpatialReal's Maven repository serves 1.0.0-beta9 until it is also on Maven Central
        maven("https://maven.spatialreal.cloud")
    }
}

rootProject.name = "PipecatClientExample"
include(":app")
