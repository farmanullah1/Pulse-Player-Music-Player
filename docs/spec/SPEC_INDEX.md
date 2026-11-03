# Spec Index

## Original Phases (0–8) — IMPLEMENTED IN SOURCE; CURRENT BUILD VERIFICATION BLOCKED

| Phase | Goal | Required files | Status |
|---|---|---|---|
| 0 | Audit and plan | `SPEC_INDEX.md`, 00, 12 | ✓ AUDITED |
| 1 | Foundation: schema, scan, permissions | 03, 14 | ✓ DONE |
| 2 | Playback engine: service, fades, queue, sessions | 04, 13, 14 §6–§7, 15 §1 | ✓ DONE |
| 3 | Theme, shell, main screens | 02, 05, 14 §8–§10 | ✓ DONE |
| 4 | Player themes and skins (9 themes) | 06, 16 | ✓ DONE |
| 5 | Classic favourites backup | 07 | ✓ DONE |
| 6 | Full backup (reference format) | 10, 11, 15 §2–§4 | ✓ DONE |
| 7 | Widgets, recap, icon, README | 08, 15 §1.4 | ✓ DONE |
| 8 | Hardening, tests, audit | 17 + all earlier phase files | ✓ DONE |

---

## Merge Phases (M0–M14) — Unified Master Merge Specification

| Phase | Goal | Main Scope | Status |
|---|---|---|---|
| M0 | Audit and merge plan | Inventory, preservation matrix, gap matrix, parity inventory, decisions, licensing | ✓ AUDITED |
| M1 | Stability baseline + Schema v4 | Schema migration 3→4, new tables/columns, migration test, flavor groundwork | PARTIAL / UNVERIFIED |
| M2 | Theme architecture | Token system, colorSource axis, Material You, corner radius, global color audit | PARTIAL / UNVERIFIED |
| M3 | Player layout registry | Widescreen layout, layout previews, responsive player host, Atmospheric Queue polish | PENDING |
| M4 | Live button arrangements | 12+ presets, `ButtonPreset` system, persistence, animation, accessibility | PARTIAL / UNVERIFIED |
| M5 | Library / indexing | Artist parsing, genre, tags, moods, ratings, sort extensions, folder cover, `.info.txt` | PARTIAL |
| M6 | Metadata and playlists | Tag editor, smart playlists, M3U, saved queue sessions, track audio profiles | PARTIAL |
| M7 | Search and lyrics | Typo-tolerant search, adjacent .lrc, lyrics editor, sync offset | PARTIAL |
| M8 | Audio enhancements | EQ profiles, ReplayGain, per-track sound, pitch, crossfade (experimental), audio path | PENDING |
| M9 | History and discovery | Custom range stats, Your Year, Smart Tracks, Daily Mix, Lost Memories, HistoryWriter gate | PENDING |
| M10 | Visual and interaction polish | Visualizer, waveform seekbar pipeline, gestures, quick tiles, widget improvements | PENDING |
| M11 | Optional integrations | Product flavors (offline/full), NetworkGateway, LRCLIB, Deezer art, AI, remote libs, Cast | PENDING |
| M12 | Backup hardening | Classic v2 audio copy, backup progress/cancel, free-space check, schema v4 compatibility | PENDING |
| M13 | Security, accessibility, performance | Permission onboarding, Capabilities registry, Privacy section, Settings IA, contrast audit | PENDING |
| M14 | Final acceptance | Full regression, documentation, README, architecture overview, release readiness | PENDING |

## Layer 4 Phases (9–17) — File 18

| Phase | Goal | Required files | Status |
|---|---|---|---|
| 9 | Merge groundwork: inventory, licences, capability registry, schema v4, settings IA, error framework | 18 §1–§6, §13; 03, 14 §1–§2 | AUDITED; CODE PARTIAL |
| 10 | Tokens, theme fixes, colour sources, corner radius, navigation styles, micro-interactions | 18 §5.1, §10, §6.6; 02, 05 | PARTIAL |
| 11 | Player layouts, Atmospheric Queue, live button presets, waveform seekbar, visualizer, particles | 18 §7, §8, §6.2; 05, 06, 16 | PARTIAL |
| 12 | Library intelligence, new pages, tag editor, search, smart playlists, stats, queues, gestures | 18 §6.1, §6.4, §15.1–§15.2; 14, 15 | PARTIAL |
| 13 | Sound engine and transitions | 18 §6.3, §15.3, §15.4; 04, 13 | PENDING |
| 14 | Classic backup v2, permission onboarding, error-handling sweep | 18 §9, §13, §14; 07, 14 §8, 15 Part C | PENDING |
| 15 | Online layer: LRCLIB, Deezer, AI, history imports, Android Auto, Cast, widgets | 18 §15.5–§15.8, §3 C1; 08, 13 | PENDING |
| 16 | Sources, downloads, LAN party/sync, gated streaming | 18 §5.4, §6.5, §15.9; 14 | PENDING |
| 17 | Responsiveness, performance, README, final traceability audit | 18 §11, §12, §16, §18; 17 | PENDING |

---

## Key Documents

| Document | Purpose |
|---|---|
| `docs/spec/CURRENT_INVENTORY.md` | Snapshot of all existing features with WORKING/PARTIAL/BROKEN/MISSING/UNVERIFIED status |
| `docs/spec/PRESERVATION_MATRIX.md` | Every existing capability mapped to post-merge destination |
| `docs/spec/FEATURE_GAP_MATRIX.md` | Every new requirement with phase, classification, priority, dependencies, status |
| `docs/spec/PARITY_INVENTORY.md` | Per-feature mapping from Namida/PixelPlayer to Pulse Player |
| `docs/DECISIONS.md` | Architecture and product decisions (D01–D13) |
| `docs/THIRD_PARTY_NOTICES.md` | License inventory for all dependencies and reference projects |
| `docs/TEMP_CODE.md` | Temporary code tracking (must be empty before release) |
| `brain.md` | Development log updated after each phase |
