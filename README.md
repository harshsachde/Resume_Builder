# Resume_Builder

Android tooling to tailor a resume toward a page target while preserving Word (`.docx`) package styling where possible.

## Quick start

This project uses **Android Gradle Plugin 8.11.x** and **Gradle 8.13**, which require a **JDK 17+** toolchain (Java 8 is not supported).

1. **JDK 17**
   - **Android Studio**: set *Gradle JDK* to **17** (or newer LTS): *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK*.
   - **Command line**: point `JAVA_HOME` to a JDK 17+ install, or rely on the [Foojay toolchain resolver](https://github.com/gradle/foojay-resolver) convention plugin enabled in `settings.gradle.kts`, which can download a JDK for Gradle when your default Java is older.
2. **Android SDK**: install **Android SDK Platform 36** (and Build-Tools **35.0.0+**) via SDK Manager. Set `ANDROID_HOME` **or** add `sdk.dir=...` to `local.properties` (gitignored).
3. From the repo root:

   `.\gradlew.bat --no-daemon :app:assembleDebug` (Windows) or `./gradlew --no-daemon :app:assembleDebug` (macOS/Linux)

Use a recent **Android Studio** version that supports AGP **8.11** (upgrade via *Help → Check for Updates* if Gradle sync fails).

## Responsible use

You are responsible for the **accuracy** of your resume and for complying with laws, regulations, and any applicable employer or platform rules. This app is a formatting and prioritization aid, not a substitute for honest representation of your experience.

## Security and compliance

- **Never commit** signing keys, API tokens, passwords, or other secrets. `local.properties` and Gradle caches are listed in `.gitignore`.
- Report security issues privately; see [SECURITY.md](SECURITY.md).
- Contributions must follow GitHub's Terms of Service and Community Guidelines; see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This repository is licensed under the MIT License; see [LICENSE](LICENSE).

Third-party libraries (for example AndroidX and PDFBox Android) remain under their respective licenses.
