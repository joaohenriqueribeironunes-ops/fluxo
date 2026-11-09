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

rootProject.name = "Lambda"
include(":app")
include(":core-ui")
include(":data-behavior")
include(":data-academic")
include(":ai-engine")
include(":voice")
include(":study")
