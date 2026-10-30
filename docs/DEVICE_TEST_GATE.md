# HUMAN_REQUIRED — physical Android acceptance

**GATE:** `PHYSICAL_DEVICE_ACCEPTANCE`  
**PURPOSE:** Establish real caller, notification, persistence and UI behavior before approving unconnected v1.  
**EXPECTED_RESULT:** T01–T16 recorded on an identified physical phone; no unexpected filtering; silent-disallow behavior and OEM/carrier limitations documented honestly.

The candidate and source are prepared locally. See [build/install instructions](BUILD.md) and [verification](../evidence/current/verification.md). No device was connected in the observed ADB enumeration. No phone was installed, called, reconfigured, or granted permissions by the agent.

**COMMANDS** (only after the owner authorizes the identified test device):

```bash
adb devices -l
adb -s DEVICE_SERIAL install -r development/deliverables/unconnected-1.0.0-candidate.apk
```

Open the app manually; grant the call-screening role and Contacts permission only through the displayed Android prompts. For permission-revocation trials, use Android's app settings. Reboot and place calls manually as agreed with the owner. Do not automate real calls, forwarding, role changes or permission grants.

**RISK:** Selected test callers may not ring outside the configured window; permitted calls could reveal OEM defects. A screening role change may displace another screening app. Call logs may remain. Use consenting test callers and a noncritical test period; never test against emergency calling or a relied-upon business line.

**ROLLBACK:** Touch DEACTIVATE; remove the screening role; restore the previous screening app if applicable. Uninstall through Android settings to remove local rules (rule loss is irreversible). Do not alter forwarding to perform this test without a separate explicit gate.

**EVIDENCE_TO_RETURN:** Device manufacturer/model, Android version/security patch, build identifier, carrier and app version; anonymized pass/fail results below. Do not return phone numbers, contacts, raw call logs, device serials or unredacted screenshots.

| Trial | Record |
|---|---|
| Clean install / T01, T07, T16 | OFF initially; add/edit/delete/disable three fictional-labelled test entries; update retains rules; disable/uninstall restore ordinary behavior |
| Boundary / T02–T05 | In-hours, exactly opening/closing, weekend, split gap and overnight rollover results; device timezone |
| Locale / T06, T14 | Italian, English and French-primary/Italian-secondary fallback; 12/24h; light/dark; large font; TalkBack focus and control descriptions; portrait/landscape editing |
| Role/permission / T08, T12 | Role refusal/revocation and Contacts denial/revocation permit calls; saved-contact and unsaved-contact cases with both grants |
| Identity / T09, T11 | Listed, disabled, unlisted and hidden caller outcomes; only anonymized identity format, never number |
| Outside hours / T10 | Handset rings/vibrates? Incoming UI? Regular missed-call notification? Blocked-call log? What caller hears? Record each separately |
| Forwarding / T13 | Existing approved forwarded route, two consenting origins: whether presented identity changes with origin; never infer forwarding identity |
| Lifecycle / T07, T15 | App backgrounded, swiped away, OS process recreation and reboot; distinguish force-stop restrictions; role and rules retained? |
| Response latency / R8 | Device-observed screening timing under normal and cold-start conditions; whether Android falls back before a response |

If silent disallow fails, return exact anonymized observations before considering explicit-reject fallback. The code currently never requests explicit rejection.

A separate `PUBLIC_RELEASE_APPROVAL` gate remains after device acceptance. Publication is not authorized by the implementation request, and no upload or public release has been performed.
