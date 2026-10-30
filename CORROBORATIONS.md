# External corroboration index

C1 | DOCUMENTED / PRIMARY
Claim: Android supports call screening and silent disallow with notification-request flags, subject to role, contact, caller-ID and log limitations.
Detail: `research/corroborations/C1.md`

C2 | UNVERIFIED ON TARGET DEVICE / CRITICAL
Claim: Forwarded calls may expose the originator number, not the forwarding line; this cannot reliably be recovered from ordinary third-party call screening.
Detail: `research/corroborations/C2.md`

C3 | OBSERVED / SIGNING
Claim: The existing Chromecast project loads a local private PKCS#12 release identity via `development/signing.properties`; the new app may reuse the signing identity with a distinct application ID.
Detail: `research/corroborations/C3.md`
