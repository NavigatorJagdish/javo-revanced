pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
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
        mavenCentral(); google()
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            content { includeGroup("app.revanced") }
            val hasUsername=providers.gradleProperty("githubPackagesUsername").isPresent
            val hasPassword=providers.gradleProperty("githubPackagesPassword").isPresent
            check(hasUsername==hasPassword) { "Configure both GitHub Packages environment properties" }
            if(hasUsername) credentials(PasswordCredentials::class)
        }
    }
}
rootProject.name = "javo-branding"
