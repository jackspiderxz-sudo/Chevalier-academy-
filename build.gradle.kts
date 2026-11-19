// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Unified Cross-Platform Web Tasks (reuses Android brand drawables & fonts into web/dist/)
tasks.register<Exec>("buildWeb") {
  group = "web"
  description = "Bundles the Chevalier Academy Responsive Website into web/dist/ using shared Android assets."
  workingDir = rootDir
  commandLine("node", "web/build.mjs")
}

tasks.register<Exec>("testWeb") {
  group = "verification"
  description = "Runs the Chevalier Academy Responsive Website automated test suite."
  workingDir = rootDir
  commandLine("node", "--test", "web/test/web.test.mjs")
}

