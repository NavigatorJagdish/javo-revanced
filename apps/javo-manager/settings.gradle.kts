pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        providers.environmentVariable("JAVO_MAVEN_CACHE").orNull?.let { cache ->
            maven {
                name = "verifiedLocalReVanced"
                url = uri(file(cache))
                content { includeGroup("app.revanced") }
                metadataSources { gradleMetadata(); mavenPom() }
            }
        }
        mavenLocal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
        maven {
            name = "githubPackages"
            // A repository must be specified for some reason. "registry" is a dummy.
            url = uri("https://maven.pkg.github.com/revanced/registry")
            content { includeGroup("app.revanced") }
            // Resolve public build plugins before requiring protected ReVanced artifacts.
            // The actual credentials still come from local environment/CI, never source.
            val hasUsername = providers.gradleProperty("githubPackagesUsername").isPresent
            val hasPassword = providers.gradleProperty("githubPackagesPassword").isPresent
            check(hasUsername == hasPassword) {
                "Configure both GitHub Packages username and password through environment variables."
            }
            if (hasUsername) credentials(PasswordCredentials::class)
        }
    }
}

rootProject.name = "javo-manager"
include(":app", ":api")
