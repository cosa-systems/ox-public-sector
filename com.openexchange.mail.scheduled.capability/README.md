# com.openexchange.mail.scheduled.capability

Declares and grants the **`scheduled_mail`** capability so the App Suite
composer's **"Send later"** action is exposed.

## Problem

In the public-sector build, the "Send later" composer button never appears,
even with the scheduled-mail scheduler enabled and the UI feature toggle on.

## Root cause

The feature is split across three parts, and the middleware is missing the
capability that links them:

1. **Scheduler backend** — `com.openexchange.mail.scheduled.*` bundles ship and
   run (they send due scheduled mails when `com.openexchange.mail.scheduled.enabled=true`). ✔
2. **UI** — `core-ui` ships `io.ox/mail/compose/schedule/{view,model}.js`. The
   button is gated by `hasFeature('scheduleSend')`, which `core-ui` defines as
   `capabilities.has('scheduled_mail')`, and is *enabled* only when the
   `/mail/compose` response's `capabilities` array contains `scheduled_mail`. ✔
3. **The `scheduled_mail` capability itself** — **no middleware bundle declares
   or grants it.** Verified: no bundle's bytecode references `scheduled_mail`,
   the capability is never advertised in the server config, and the
   `com.openexchange.mail.compose.*` bundles have no scheduled-mail awareness. ✘

Because the capability is undeclared, the middleware never advertises it (so the
button can't draw) and never returns it in the per-compose capabilities (so the
button stays greyed). Granting it via config-cascade
(`com.openexchange.capability.scheduled_mail=true`) does **not** work — OX does
not advertise an *undeclared* capability of that name, and a per-user
`changeuser --capabilities-to-add scheduled_mail` is likewise ignored by the
compose backend. It must be **declared** by a bundle.

## The fix (this bundle)

`osgi/Activator.java`:
- `CapabilityService.declareCapability("scheduled_mail")` — makes it a known,
  advertised capability (so `hasFeature('scheduleSend')` can be true and the
  button draws).
- Registers a `CapabilityChecker` for `scheduled_mail` that returns `true` for a
  session iff `com.openexchange.mail.scheduled.enabled` is `true` for that user
  (config-cascade aware) — so it is part of the user's effective capabilities and
  appears in the `/mail/compose` response (button enabled), gated on the same
  switch that drives the scheduler. No other behaviour changes.

## Build the image — GitHub Action overlay (primary route)

No OX SDK or Debian pipeline needed. The OpenDesk
`middleware-public-sector` runtime image already ships **JDK 21** and the OX API
jars this bundle imports (`com.openexchange.capabilities.jar`,
`com.openexchange.config.cascade.jar`, `com.openexchange.osgi.jar`, slf4j, the
Equinox OSGi framework). So we compile the bundle *in-image* and layer it back
on, registering it exactly like a Debian package install:

- `Dockerfile` (this dir) — multi-stage: stage 1 `javac`s + jars the bundle
  against `/opt/open-xchange/bundles/**/*.jar`; stage 2 copies the jar into
  `/opt/open-xchange/bundles/` and writes
  `osgi/bundle.d/open-xchange-mail-scheduled-capability/com.openexchange.mail.scheduled.capability.ini`
  = `…/com.openexchange.mail.scheduled.capability.jar@start`. At container start,
  `open-xchange-cloud` runs
  `ox_update_cloud_config_ini config.ini config.ini.template bundle.d`, which
  regenerates `osgi.bundles` to include it — no manual config.ini edit.
- `.github/workflows/build-scheduled-mail-capability.yml` — builds the overlay
  and pushes `ghcr.io/<owner>/middleware-public-sector-scheduled-mail:<base-tag>`.
  Triggers on changes under this dir, or `workflow_dispatch` (override the base
  image). **Base image pull**: if `registry.opencode.de/.../middleware-public-sector`
  is not publicly pullable, set repo secrets `OPENCODE_REGISTRY_USER` +
  `OPENCODE_REGISTRY_TOKEN` (the workflow logs in only when both are present).

Then **repoint** OpenDesk's OX middleware image (the
`open-xchange-core-mw-groupware` workload, today
`images-mirror/middleware-public-sector:8.48.91`) at the published GHCR tag, in
the cosa opendesk fork's OX values/charts image override.

> Pin the GHCR tag to the same `:<base-tag>` so it tracks the upstream OX
> version; rebuild when the base image bumps.

## Alternative — proper Debian package (upstream contribution)

1. Add a Debian package `open-xchange-mail-scheduled-capability` (copy
   `open-xchange-conference-element/debian/`), referencing this bundle the same
   way conference-element references `com.openexchange.conference.element`.
2. Register it in `public-sector-packages/public-sector-packages.psf` + add the
   bundle's `.psf`.
3. Build via the public-sector pipeline → bundle jar lands in a
   `middleware-public-sector` image (no separate overlay needed).

## Verify against the SDK when building

The OX API used here is stable but unverified against this repo's SDK from the
runtime image. Confirm at build time:
- `com.openexchange.capabilities.CapabilityService#declareCapability(String)`
- `com.openexchange.capabilities.CapabilityChecker#isEnabled(String, Session)` and
  `CapabilityChecker.PROPERTY_CAPABILITIES`
- `com.openexchange.config.cascade.ConfigViewFactory#getView(int,int)` /
  `ConfigView#opt(String, Class, T)`

Adjust `META-INF/MANIFEST.MF` `Import-Package` if package names differ.

## Downstream (cloud repo) — already in place / cleanup

The OpenDesk-side prerequisites are committed in the cosa opendesk fork and stay:
- `io.ox/core//features/scheduleSend: "true"` (UI toggle — required)
- `com.openexchange.mail.scheduled.enabled: "true"` (scheduler/feature — required)

Redundant once this bundle ships (can be removed): the
`com.openexchange.capability.scheduled_mail` config-cascade grant and any
per-user `changeuser` grant — the bundle's declare+checker supersedes them.
