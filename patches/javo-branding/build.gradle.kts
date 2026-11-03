plugins { kotlin("jvm") version "2.3.21" }
group = "app.javo"
version = "0.1.0-alpha.1"
dependencies { compileOnly("app.revanced:patcher-jvm:22.0.2-dev.1") }
kotlin { jvmToolchain(17); compilerOptions { freeCompilerArgs.add("-Xcontext-parameters") } }
tasks.jar {
    manifest.attributes(mapOf("Name" to "Javo resource branding", "Version" to project.version, "Description" to "Opt-in Javo Video/Music name and icon resources; no playback changes", "Author" to "Javo project", "License" to "GPL-3.0-only"))
}
