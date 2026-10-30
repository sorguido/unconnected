# unconnected

**Quiet, scheduled call filtering for Android.**

> **Development status:** Android release candidate implemented. Build, tests, signature receipts and remaining limitations are tracked in [verification](evidence/runs/contacts-1.0.0-vc3/verification.md). Physical-phone acceptance and explicit release approval are still required; this is not a verified production release.

unconnected lets you add multiple phone numbers manually or select a specific number from Android contacts, then define the weekly days and hours when those callers may reach you. Outside the allowed windows, the app aims to block only listed callers silently, without ringing or an ordinary missed-call notification where Android permits. A prominent green **ACTIVATE** button enables the rule and changes to a red **DEACTIVATE** button, which suspends all filtering. The list, days, and time windows are editable.

The app is local-first, offline, requires no account or root access, and does not replace the Phone app. Android's call-screening role is required. English is the fallback language and Italian is selected automatically for Italian system locales. The user interface must be polished, accessible and functional—not a mock-up.

## Important platform limitations

Only the caller number presented to Android can be matched. Forwarded calls may display the **original caller**, rather than the forwarding office number. Call logs may still contain blocked calls, and manufacturer behavior differs. Saved-contact calls may require the Contacts permission. The product must clearly communicate these limits and allow all calls if caller identity or rules are uncertain.

## Development

- [Build, install, disable and uninstall](docs/BUILD.md)
- [Physical-device acceptance gate](docs/DEVICE_TEST_GATE.md)
- [Independent review](evidence/runs/contacts-1.0.0-vc3/review.md)

- [Agent bootstrap](START.md)
- [Product requirements](docs/PRODUCT_SPEC.md)
- [Goal-mode delegation for AI PM](docs/AI_PM_GOAL.md)
- [Build/signing policy](docs/SIGNING.md)
- [Technical evidence](CORROBORATIONS.md)

No user phone numbers or signing secrets belong in this repository.

Copyright © 2026 sorguido. See [LICENSE](LICENSE).
