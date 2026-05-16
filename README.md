# Resume_Builder

Android tooling to tailor a resume toward a page target while preserving Word (`.docx`) package styling where possible.

## Quick start

1. Install the Android SDK and set `ANDROID_HOME`, **or** create `local.properties` with `sdk.dir=/path/to/Android/sdk` (this file is intentionally **not** committed).
2. From the repo root:

   `./gradlew :app:assembleDebug`

## Responsible use

You are responsible for the **accuracy** of your resume and for complying with laws, regulations, and any applicable employer or platform rules. This app is a formatting and prioritization aid, not a substitute for honest representation of your experience.

## Security and compliance

- **Never commit** signing keys, API tokens, passwords, or other secrets. `local.properties` and Gradle caches are listed in `.gitignore`.
- Report security issues privately; see [SECURITY.md](SECURITY.md).
- Contributions must follow GitHub's Terms of Service and Community Guidelines; see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This repository is licensed under the MIT License; see [LICENSE](LICENSE).

Third-party libraries (for example AndroidX and PDFBox Android) remain under their respective licenses.
