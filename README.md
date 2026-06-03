# Iskandert's Utilities

| Version | Support Status |
|--------|---------------|
| 26.1.2 | Supported |
| 1.21.1 | Supported |
| 1.20.1 | Not Supported |
| Older Versions | Not Supported |

## 1.21.1 development

Requires **Iskandert's Library** (`iska_lib`) as a separate mod. There is no Curse pin yet — use a local JAR.

1. Build the library: `cd ../Iskandert-Library-1.21.1 && ./gradlew jar`
2. Copy `build/libs/iska_lib-1.7.0.0.0.jar` to `libs/iska_lib-1.7.0.0.0-neoforge-1.21.1.jar` in this project
3. Copy the same file to `run/mods/` (dev client loads mods from there)
4. Build utilities: `./gradlew build` (`iskaLibUseLocal=true` in `gradle.properties` enables compile against the JAR)

After changing the library sources, repeat steps 1–3 before `runClient`.

### Pre-release smoke test (1.21.1, on a **copy** of a real world)

1. Backup the world folder.
2. Start with `iska_lib` and `iska_utils` in `run/mods/`.
3. Login: player/world/team stages still present.
4. Shop: team balance and members unchanged.
5. Structure placer: structure in list → place → undo.
6. Datapack reload: structures still listed.
7. Reopen world: repeat (3) and (4).
8. `/iska_utils_stage list all` shows world, player, and team stages for the player.
