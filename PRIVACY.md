# Cycle — Privacy Policy

Version 0.2.17. Last reviewed 4 November 2026.

## The short version

Cycle collects nothing. It asks for no permissions and has no network access, so no data can leave your phone even in principle. There is no account, no sign-up, no analytics, no advertising and no crash reporting. The developer never receives anything about you or your use of the app, and has no way to.

<!-- shared-about privacy section -->
## Who we are

Cycle is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Cycle keeps what you put in it on your phone and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Cycle is sold or shared.

Since Cycle keeps what it stores only on your phone, you can exercise most of these rights yourself: edit or clear a day in the app, save a copy with Back up, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->

## What the app stores, and where

Only what you enter: the days you mark as a period, with how heavy (light, medium or heavy); and, for any day, the moods and symptoms you pick, cervical mucus and an ovulation test result if you log them, whether you marked it as intimate, and a note you type. This is health data, and it is treated as such: it is written only to a database in the app's own private storage on this phone. The app's settings are kept there too.

The manifest sets `allowBackup="false"`, so Android does not copy any of it into a device backup or a cloud backup. Uninstalling the app deletes it. There is no cloud copy and no copy held by the developer.

Two things leave the app only when you press them, and only to where you point them:

- **Back up** writes one plain-text file of the whole record to a place you choose in Android's file picker. Nothing is written until you choose one.
- **Send to…** hands one line (when the next period is expected, as the app works it out) to Android's share sheet. Whatever app you pick does the sending; the app itself has no way to.

**Import** reads a text file you pick, either one of this app's own backups or a Period Tracker export. It reads only that file.

## Permissions, and what each is for

None. The manifest has no `uses-permission` line. The app cannot reach the network, the camera, the microphone, your location, your contacts or your files.

The store lists `com.wanderwildwood.cycle.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` because it reads the APK. AndroidX declares it for its own internal receivers; it is signature-level and namespaced to this app, so nothing else can hold it and it grants no access to anything on the phone.

## Who else receives anything

Nobody. The app sends nothing to anyone: no analytics, advertising or crash-reporting service is contacted, and the developer runs no server.

Hosts contacted: none. The app has no internet permission.

In About, "feed the llamas" opens a donation page (square.link) and wanderthe.dev opens the author's website, in your browser and only when you press them. The app does not fetch either page and learns nothing about whether you went.

No data is sold, rented, or shared for advertising. There is no profiling, no personalization, and no automated decision-making about you.

## Legal basis (EU / EEA and UK)

No personal data is collected, transmitted or processed by the developer, so no legal basis under Article 6 GDPR is engaged on the developer's part. Processing that happens wholly on your own device, for your own purposes, is outside the scope of the Regulation (Article 2(2)(c)).

## Children

The app is suitable for all ages and does not knowingly collect anything from anyone, of any age, because it does not collect anything.

## Terms

This app is free software under the GNU General Public License v3. It comes with no warranty: use it at your own risk. The same text is in the app, under the small i on the Today screen, with the licence and a link to the source.

## Not medical advice

This app is a record-keeping aid, not medical advice. It does not diagnose, treat or replace a doctor or pharmacist. Its dates are estimates worked out from what you recorded. It is not contraception, and it is not a fertility or pregnancy test: do not rely on it to prevent or to plan a pregnancy.

## Changes

If a later version changes what the app stores or which hosts it contacts, this policy is updated in the same release and the change is described in the release notes. The source is public, so any claim here can be checked against the code: <https://github.com/wanderwildwood/cycle>. It is released under GPL-3.0-only.
