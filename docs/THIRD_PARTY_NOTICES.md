# Third-Party Notices — Pulse Player
> Updated: 2026-11-03 Phase 9 audit  
> Reference repositories were inspected remotely; no reference source or asset was copied into Pulse Player.

---

## Reference Projects

### Namida
- **Repository:** https://github.com/namidaco/namida
- **License:** The repository `LICENSE` is a Namidaco EULA, not GPL-3.0. It restricts redistribution, rebranding, commercial use, and reuse of code except personal use/contribution; some components may have separate licenses. Do not copy Namida source code, assets, branding, or screenshots. Re-implement behavior and design independently.
- **Verified commit/date:** `10123d91e2527682efc72d877603a121d85d8fbe`, 2026-11-01
- **Assets policy:** No Namida icons, logos, screenshots, or branding assets included.

### PixelPlayer
- **Repository:** https://github.com/PixelPlayerHQ/PixelPlayer
- **License:** The repository `LICENSE` is a proprietary personal/non-commercial license. Its `THIRD_PARTY_NOTICES.md` preserves MIT terms for contributions before 2026-05-12, but the repository as a whole is not an MIT-licensed dependency. Do not copy PixelPlayer source code, assets, logo, or screenshots.
- **Verified commit/date:** `63cb59b97aff1d60c5aa46afdf5ce489e632b267`, 2026-09-17
- **Assets policy:** No PixelPlayer icons, logos, screenshots, or branding assets included.

---

## App Dependencies (from app/build.gradle.kts)

| Library | Version | License | Notes |
|---|---|---|---|
| AndroidX Compose BOM | 2024.10.01 | Apache 2.0 | Google Android Jetpack |
| AndroidX Activity Compose | 1.9.3 | Apache 2.0 | |
| AndroidX Lifecycle | 2.8.7 | Apache 2.0 | |
| AndroidX Core KTX | 1.15.0 | Apache 2.0 | |
| AndroidX DocumentFile | 1.0.1 | Apache 2.0 | |
| AndroidX Palette KTX | 1.0.0 | Apache 2.0 | |
| AndroidX DataStore Preferences | 1.1.1 | Apache 2.0 | |
| AndroidX Media3 (ExoPlayer, Session, UI) | 1.5.1 | Apache 2.0 | |
| AndroidX Room (Runtime, KTX, Compiler) | 2.6.1 | Apache 2.0 | |
| AndroidX Glance AppWidget | 1.1.1 | Apache 2.0 | |
| Kotlin Coroutines Android | 1.9.0 | Apache 2.0 | |
| Kotlin Coroutines Guava | 1.9.0 | Apache 2.0 | |
| OkHttp | 4.12.0 | Apache 2.0 | Full flavor only; used behind `com.pulse.player.net.NetworkGateway` |
| JUnit | 4.13.2 | EPL 1.0 | Test-only dependency |
| org.json | 20240303 | Public Domain | Test-only dependency |

### Future Dependencies (to be evaluated)

| Library | Intended Use | License Status | Decision |
|---|---|---|---|
| TagLib Android binding | Tag editing (NAM-L1) | To verify — multiple Android bindings exist; check each one | PENDING |
| ExoPlayer FFmpeg extension | Extended format support | Apache 2.0 (ExoPlayer), ffmpeg itself LGPL/GPL — requires careful ABI and linking review | PENDING |

---

## Audio and Media Assets
- No bundled audio files in production build.
- No bundled third-party icons beyond Material Icons Extended (Apache 2.0).
- No scraped images.
- All procedural backgrounds are generated in-app code.

---

## AI Services
- Gemini API: Available only in the `full` flavor through the network gateway and subject to Google's Terms of Service. The API key is user-supplied; it is currently held in the existing DataStore preference and must be moved to Keystore-backed storage before release.

---

## To-Do Before Release
- [x] Verify and record Namida license text and inspected commit hash.
- [x] Verify and record PixelPlayer license text and inspected commit hash.
- [ ] Evaluate and record TagLib Android binding license.
- [ ] Evaluate ExoPlayer FFmpeg extension licensing implications.
- [ ] Add complete license texts for all dependencies in an `about/licenses` screen.
- [ ] Confirm all Material Icons used are from the Apache 2.0 set.
