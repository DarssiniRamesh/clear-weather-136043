androidApplication {
    namespace = "org.example.app"

    dependencies {
        // Removed unused sample dependency
        // Note: Keeping utilities module to satisfy existing project structure, not used by weather app
        implementation(project(":utilities"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }
}
