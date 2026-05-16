# Contributing

Thank you for helping improve this project.

## GitHub rules and acceptable use

Contributions must comply with:

- [GitHub Terms of Service](https://docs.github.com/en/site-policy/github-terms/github-terms-of-service)
- [GitHub Community Guidelines](https://docs.github.com/en/site-policy/github-terms/github-community-guidelines)

In practical terms: no malware, no attempts to compromise accounts or services, no harassment, no spam (including mass-automated issues or pull requests), and no content that violates applicable law.

## Pull requests

- Keep changes focused and described clearly in the PR text.
- Do not include secrets, personal data, or copyrighted material you do not have rights to share.
- Prefer dependencies from **Google Maven**, **Maven Central**, or other widely trusted sources. Avoid arbitrary script/plugin URLs in Gradle unless there is a strong justification and clear review.

## Development notes

- Android SDK location: set `ANDROID_HOME` or add `sdk.dir=...` to `local.properties` (this file is gitignored).
- Build: `./gradlew :app:assembleDebug` (add `--no-daemon` on CI or constrained shells). Requires **JDK 17+** and Android SDK **Platform 36** (see README).

For security-sensitive reports, see [SECURITY.md](SECURITY.md).
