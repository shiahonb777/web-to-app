
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register<Exec>("checkUiDesignSystem") {
    group = "verification"
    description = "Checks Compose UI files against the WebToApp design-system debt baseline."
    workingDir = rootDir
    commandLine(
        "python3",
        "tools/audit_ui_design_system.py",
        "--enforce-baseline",
        "--allowlist",
        "tools/ui_design_allowlist.txt",
        "--top",
        "12"
    )
}
