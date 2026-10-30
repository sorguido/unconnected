# unconnected

**Choose who can reach you, and when.**

unconnected lets you pick specific phone numbers and decide the days and hours they are allowed to call you. Outside those hours their calls are filtered quietly. A number you have not listed is not affected by the app.

## What it does

- **Several numbers.** Keep as many numbers as you need in one list.
- **From your contacts, or typed in.** Choose a number straight from your Android contacts, or enter it manually with its country code.
- **A weekly schedule that fits your routine.** Set the days and hours when listed callers may reach you, including windows that cross midnight.
- **On and off in one tap.** One button turns the filter on. The same button turns it off straight away and keeps every setting you made.
- **Works offline.** The app has no Internet permission at all, so it cannot send anything anywhere.
- **No account.** Nothing to sign up for and nothing to remember.

## Installing

1. Download the APK from the [latest release](https://github.com/sorguido/unconnected/releases).
2. Open the file on your phone and allow the installation if Android asks you to.
3. Open **unconnected**.
4. Grant the two things it needs: the call-screening role, and the Contacts permission. The setup card in the app walks you through both.
5. Add your numbers, set your hours, and turn the filter on.

**Requires Android 10 or later.**

## Good to know

- **Forwarded calls.** When a call is forwarded to you, your phone may show the original caller rather than the number that forwarded it. unconnected can only act on the number your phone actually reports.
- **Hidden or incomplete numbers.** If a caller cannot be identified — because the number is withheld, restricted or incomplete — that call is allowed through. Whenever unconnected is unsure, it deliberately lets the call pass rather than risk blocking the wrong person.
- **Your call log.** Whether a filtered call still appears in your call history depends on your phone and its phone app. unconnected does not remove entries from your call log.
- **Phones differ.** Filtering behaviour varies between manufacturers and carriers, so results may not be identical on every device.

## Privacy

There is no account, no advertising, no analytics and no online tracking. Your numbers and your schedule stay on your phone, and they are excluded from cloud backup and transfer to a new device. unconnected uses the Contacts permission only to read the name and phone numbers of the one contact you choose yourself; it does not scan, copy or upload your address book.

## Turning it off

Turn the filter off inside the app whenever you like, or remove unconnected as your call-screening app in Android settings. Uninstalling the app through Android settings deletes everything it stored. unconnected never changes your call forwarding, Do Not Disturb, default phone app or any other phone setting on its own.

## License

unconnected is free software under the [BSD 2-Clause License](LICENSE).

---

Building from source, installation over adb, signing and maintenance are documented in [docs/BUILD.md](docs/BUILD.md). Third-party components are listed in [NOTICE.md](NOTICE.md).
