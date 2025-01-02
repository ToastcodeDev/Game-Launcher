import org.gradle.api.initialization.resolve.RepositoriesMode.*

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
    this.repositoriesMode.set(FAIL_ON_PROJECT_REPOS)
    this.repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Game Launcher"
include(":app")
