# AGENTS.md

## Project
- Architectury multiloader Minecraft mod: **Fabric + NeoForge**, MC **1.21.1**, Java **21**, Gradle **8.8** wrapper, **Mojang official mappings** (not Yarn).
- `common/` holds essentially all code; `fabric/` and `neoforge/` are thin lifecycle entrypoints. `quilt/` is legacy and not included in `settings.gradle` — ignore it.
- Hard deps: BankSystem and ModUtilities (same `1.21.1-2.2.0` line), Architectury, Fabric API. JEI is optional and dev-runtime only.
- Public surface is `net.kroia.stockmarket.api` + `StockMarketMod.getAPI()`; engine, plugin system and networking live under the sibling packages.

## Build & test
- `./gradlew build` — full build; `./gradlew :fabric:build` / `:neoforge:build` — one loader; `./gradlew :common:compileJava` — fast compile check.
- Dev client/server: `./gradlew :fabric:runClient` / `:neoforge:runClient` (and `runServer`).
- **No JUnit/test source set, CI, formatter or linter.** `build` + in-game tests are the verification gate.
- Dev features: add `-Penable_dev_features=true` to build/run commands. Dev builds need BankSystem/ModUtilities jars built the same way. Keep `enable_dev_features=false` in commits.
- Dev-only code lives in `net.kroia.stockmarket.testing`; release code must reach it only through reflective `DevBootstrap`, never a direct import.
- In-game tests (dev build, permission level 2): `/stockmarket test`, `/stockmarket test list`, `/stockmarket test <category>`. They mutate the live world — use a throwaway world. A StockMarket run also executes the BankSystem/ModUtilities suites. New suites register in `StockMarketTestRegistration` + `StockMarketTestCategories` and allocate resources via `TestFixture`.
- Full Fabric/NeoForge + master/slave matrix: ModUtilities repo `tools/autotest`.

## Architecture
- Server side is **master/slave**: the master owns SQLite, market engine, plugins and `settings.json`; slaves forward via async requests. Guard master-only work with `getSync() == null` / `isSlave()`.
- `StockMarketModBackend` is the composition root: `ServerInstances` is created from BankSystem's setup-complete signal; `ClientInstances` on player join. Platform entrypoints only forward lifecycle events.
- Markets are keyed by BankSystem **`ItemID`** (component-aware), not raw `Item`/`ResourceLocation`. BankSystem owns accounts/money; use its API.
- ModUtilities supplies the GUI framework, settings/persistence bases, request/stream networking and the in-game test harness — follow those abstractions, not vanilla/standalone equivalents.
- Management data/operations are gated **server-side on the master** (StockMarket admin flag + trusted slave), not by hiding client UI.
- SQL goes through `DatabaseManager`'s single `db-worker` (`CompletableFuture` + `getDatabaseThread()` + `commitTransaction()`); schema files are `common/src/main/resources/sql/*.sql`, applied idempotently.
- Persisted server data is under `world/data/StockMarket/` (NBT via `DataManager`/`DataPersistence`, `settings.json`, SQLite in `Database/`); news config is `config/StockMarket/news/`.
- Common code compiles for both sides. Keep `Minecraft`/GUI classes out of server paths; client setup is `onClientSetup`, plugin GUI factories go in `Plugins.clientSetup()`.

## Conventions / gotchas
- **Do not remove commented version-gated code** (`// mc<=1.21.10`, `// mc>=1.21.11`, `// mc>=26.1`, …) in build files or Java sources. It is the multi-version support mechanism; this branch's active lines target 1.21.1.
- Version/dependency bumps touch `gradle.properties` (`mod_version`, `banksystem_version`, `modutilities_version`), `StockMarketMod.VERSION`, and the BankSystem ranges in `fabric.mod.json` + `neoforge.mods.toml`.
- UI strings: `assets/stockmarket/lang/{en_us,de_de,de_ch,zh_cn}.json`. `en_us`, `de_de`, `de_ch` are kept key-identical; `zh_cn` is partial and falls back to `en_us`. Add matching keys.
- New mod setting: add it to `StockMarketModSettings`; add label/tooltip lang keys; mark startup-cached fields in `ModSettingsScreen.RESTART_REQUIRED_SETTINGS`; decide slave propagation in `ModSettingsRequest.handleOnMasterServer`.
- Changelog: add an entry to `changelog/v2.2.0.md` using its badge tags and section order (see the header comment). Update `documentation/README.md` when adding docs.
- Shipped default news events are JSON strings in `news/DefaultNewsEvents.java` (written to `config/StockMarket/news/default_events.json` at runtime), not a JSON resource; default pictures are `common/src/main/resources/news_pictures/<eventId>.png`.
- Sources contain raw UTF-8 (German/Chinese/umlauts); javac is configured for UTF-8. Do not ASCII-escape or re-encode.

## Docs
- `README.md` is the user/admin/feature reference and dev-test overview; `documentation/README.md` indexes the user and developer guides (plugin API, market-creation API, news system).
