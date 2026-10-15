# Records & Achievements: local implementation

This implementation covers requirements 3.1–3.7 and integrates with the Campaign, Settings, and audio code from team baseline `1eac70c`. The acceptance criteria are defined in [`teams/Chinese_can_fly.md`](../teams/Chinese_can_fly.md).

## Usage

- On the title screen, use Up/Down to select **Records & Achievements**, then press Space to open it.
- Use Left/Right to switch between **Run Records** and **Achievements**. Press Esc or Space to return to the menu.
- New achievements appear at the top of the HUD for about three seconds. The game-over summary lists all achievements unlocked during that run.
- The profile is stored at `.space-invaders/records-achievements.properties` under the user's home directory. Run records and achievement unlock times are saved together; personal bests are calculated from the full record history.

## Local data protection

The profile uses a versioned UTF-8 Properties file. A background thread writes a temporary file, reads it back for validation, preserves the previous valid profile as a backup, and replaces the main file. If loading fails, the application attempts to retain a copy of the damaged file and restore the backup. If neither file can be loaded, it reports the failure and blocks automatic writes to protect existing files.

Exiting during a run does not create a completed-run record. Achievements already unlocked are queued for saving immediately. Completed runs are saved asynchronously so the game-over screen can continue. Exiting through the menu, closing the window, or pressing Alt+F4 waits up to ten seconds for queued saves; failures and timeouts are reported. Forced termination or power loss can discard progress that has not yet been written.

After backup recovery, the records page and log warn that newer progress may be unavailable. Backup updates also use a validated temporary file. If a backup update fails, the save stops and retains the existing valid main profile.

## Gameplay integration

The baseline's regular and special enemies connect to requirement 3.5 through `EnemyDefeatedEvent` and the public `GameplayEventPublisher`. Each defeated enemy has a stable event ID. `RecordsService.enemyDefeated(runId, eventId)` ignores duplicate IDs within the active run. The local collision handler skips processing after a stage has ended. Additional defeat paths, including new enemy types, bosses, and lasers, must publish equivalent events or call this interface with stable IDs.

The local title menu opens the records page and handles returning to the menu. The menu entry and HUD notifications still require coordination with the Main Menu and Gameplay HUD teams for shared integration. Octopus's proposed first-time Campaign stage-clear achievement is outside the five registered achievements and is not included here.

## Build and validation

Run these commands from the game repository root on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\build.ps1
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\test-records.ps1
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\run-game.ps1
```

All three scripts accept `-JdkRoot` to select a JDK explicitly, for example:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\build.ps1 -JdkRoot 'C:\path\to\jdk'
```

Automatic discovery checks `JAVA_HOME`, portable JDK directories, and `PATH`. The current portable-JDK search supports `.tools\jdk-21\<jdk-directory>\bin` under the repository or an ancestor directory. It does not recognize `.tools\jdk-21\bin` directly; pass that JDK's root with `-JdkRoot` or set `JAVA_HOME`. Build output and personal high scores are excluded from commits.

The smoke suite covers field validation, active timing, duplicate completions, five achievement conditions, ranking order and the ten-entry limit, persistence, backup recovery warnings, protection of unreadable profiles, backup-write failures, and events received after service closure.

Verification on 2026-10-15 compiled 50 Java source files and passed all 104 smoke-suite checks in headless mode with isolated Java preferences. The suite exercises the records page's Esc/Space return loop and launches a separate Java process to verify that the production shutdown hook flushes 80 queued run records. Completion-time checks cover the 60-second achievement boundary. Gameplay adapter checks verify duplicate enemy suppression and delivery of the twentieth distinct defeat.

The suite also uses production drawing code to generate images of a ten-entry ranking, five unlocked achievements, and five achievements in the game-over summary under the ignored `out/records-tests/render/` directory. These images use isolated test data. Manual gameplay, full Campaign regression, and visual acceptance were not performed in this verification.

## Compatibility and merge dependencies

The implementation retains the baseline's Campaign clear/failure rules, difficulty settings, custom controls, audio, assets, and Settings menu. Records uses menu code 8. The `1eac70c` baseline has no boss defeat or pause integration; new defeat paths must publish the same events, and any pause feature must stop and resume active timing.

Team `main` at `6ebc2c4` has since added item and laser code. Those changes have not been integrated with this PR stack. Merge conflicts must be resolved, the new defeat paths connected, and the combined behavior tested before acceptance.

The seven PRs depend on one another in requirement order, 3.1 through 3.7. Later PRs include their predecessors' commits until those dependencies are merged. Update each remaining PR and recheck its changes and validation after merging a dependency. Integration with other teams' branches remains unverified.
