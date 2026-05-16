# Security policy

## Supported versions

Security updates are applied on a best-effort basis for the default branch.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for undisclosed security vulnerabilities.

Instead, use one of these options:

1. Open a **private security advisory** for this repository (GitHub: **Security** tab → **Report a vulnerability**), if the repository owner has advisories enabled.
2. Otherwise, contact the repository owner through a **private** channel they publish in their GitHub profile or organization documentation.

Include enough detail to reproduce the issue (affected component, version, steps, impact) without including secrets, customer data, or unrelated personal information.

## Secrets and credentials

Contributors must **never** commit API keys, passwords, personal access tokens, OAuth client secrets, signing keys, or other credentials. Use environment variables, local `local.properties` (which should stay untracked), or your platform’s secret storage.

If you believe a secret was exposed in git history, rotate the credential immediately and follow GitHub’s guidance on removing sensitive data from a repository.
