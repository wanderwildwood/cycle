# Cycle — Privacy Policy

Version 0.2.16. Last reviewed 23 October 2026.

## The short version

Cycle collects nothing. It asks for no permissions and has no network access, so no data can leave your phone even in principle. There is no account, no sign-up, no analytics, no advertising and no crash reporting. The developer never receives anything about you or your use of the app, and has no way to.

## Who is responsible

wander wildwood, the author of the app. Contact: email@wanderthe.dev

Because no personal data is transmitted to or held by the developer, there is no data controller relationship for the developer to administer. Where the app processes personal data, that processing happens on your own device, under your control.

## What the app stores, and where

Only what you enter: the days you mark as a period, with how heavy (light, medium or heavy); and, for any day, the moods and symptoms you pick, whether you marked it as intimate, and a note you type. This is health data, and it is treated as such: it is written only to a database in the app's own private storage on this phone. The app's settings are kept there too.

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

## Your rights

Under the GDPR and UK GDPR you have rights of access, rectification, erasure, restriction, portability and objection. Under the CCPA/CPRA and comparable North American laws you have rights to know, delete, correct and opt out of sale or sharing.

The developer holds no data about you, so there is nothing to request from them, and nothing has ever been sold or shared. You can exercise every one of these rights directly: the data is on your phone. Edit or clear a day in the app, save a copy with Back up, clear the app's data in Android's settings, or uninstall to remove all of it at once.

## Children

The app is suitable for all ages and does not knowingly collect anything from anyone, of any age, because it does not collect anything.

## Changes

If a later version changes what the app stores or which hosts it contacts, this policy is updated in the same release and the change is described in the release notes. The source is public, so any claim here can be checked against the code: <https://github.com/wanderwildwood/cycle>. It is released under GPL-3.0-only.
