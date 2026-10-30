# Permanent agent governance

This file owns permanent rules. Product behavior belongs solely in `docs/PRODUCT_SPEC.md`; project transitions in `ROADMAP.md`; prior-art discovery in `CORROBORATIONS.md`.

## Authority and canonical protection

- The user owns project intent, release decisions and permission-sensitive actions.
- Once initialized, **`START.md`, `AGENTS.md`, `ROADMAP.md` and `CORROBORATIONS.md` are read-only to all agents**. Only the user or an agent with specific approval in the current conversation may modify them.
- To propose otherwise stop with `HUMAN_REQUIRED`, `GATE=CANONICAL_FILE_CHANGE_APPROVAL`, `FILES`, `REASON`, `PROPOSED_CHANGE` and `IMPACT`.
- State and index JSON are non-authoritative, repairable caches. Actual Git history and evidence win.

## Lean orchestration

- AI PM coordinates investigators/executors and an independent reviewer when useful; subagents inherit all limits and need no separate authorization if within existing scope.
- Use platform Android APIs, local storage, minimum dependencies and reversible changes. Avoid architecture or documentation for its own sake.
- Load only task-relevant context, reuse verified evidence, and keep one owner for each concept.
- Label findings `OBSERVED`, `INFERRED`, `PROVEN`, `ASSUMED`. For material failures report `EVIDENCE_FOR`, `EVIDENCE_AGAINST`, `ALTERNATIVE_EXPLANATIONS`, `CONFIDENCE` and `MINIMAL_DISCRIMINATING_EXPERIMENT`.

## User safety and data

- **Fail open** on missing/ambiguous caller ID, configuration or permission: never block an unspecified caller. Keep screening disabled on first install.
- Never modify phone, call forwarding, Android roles, permissions or live device settings without explicit human approval at a targeted gate. Avoid replacing system apps or bypassing platform restrictions.
- Do not commit real phone numbers, call logs, screenshots revealing contact data, signing material, credentials or private keys; no unnecessary collection, telemetry or upload.
- Real-device tests—not API documentation, screenshots or emulators—are required to verify notification and caller behavior. Record OEM, OS-version and compatibility caveats.
- Preserve easy disable/uninstall paths and user-owned data. Minimize changes to stock Android.

## Signing identity — mandatory

- **Reuse the existing release signing key from `sorguido/chromecast-killswitch-app`**. Do not generate, rotate, replace, overwrite or commit another signing key.
- Keep `it.sorguido.unconnected` as a distinct Android `applicationId`. The public application name is **unconnected**.
- The existing PKCS#12 keystore and passwords remain outside the repository. Refer to `docs/SIGNING.md` for safe local configuration and signature verification.
- If the original signing material is unavailable, do not substitute debug/test signing for an official release. Report a concrete `HUMAN_REQUIRED` gate.

## Human gates and terminal outcomes

Reserve gates for permissions, real telephone tests, signature access, publication, destructive changes, changed technical direction and canonical edits. Specify `PURPOSE`, `EXPECTED_RESULT`, `COMMANDS`, `RISK`, `ROLLBACK`, `EVIDENCE_TO_RETURN`.
- `MILESTONE_COMPLETE`: verified exit condition met.
- `HUMAN_REQUIRED`: unavoidable concrete human action.
- `BLOCKED`: documented actionable external impediment.

**Public-facing documentation is English.** App strings default to English with Italian resources when the system locale is Italian.
