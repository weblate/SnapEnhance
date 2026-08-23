# v1.0.0 | CI Builds
## CI Build | 18f41fe 
- feat(app): no quick actions hint
- refactor(app): imports
- fix(ci): debug builds
- chore(readme): migrate links
- feat(app): forgejo
- feat(ci): renaming apk
- fix(app): in-app updating
- fix: hiccup
- refactor(app): updating repeater
- fix(app): home update card

## CI Build | 0e294e4
- blank commit, for internal repo!
- Merge remote-tracking branch 'upstream/dev' into dev
- fix(cargo): merge duplicate release profiles
- fix(lang): missing keys
- fix(app): fab icon color
- feat(workflows/github-updates): support multi commits
- chore(readme): capitalisation and grammar (#182)
- chore: add new contributor
- fix(common/wrapper): missing strings
- refactor: languages
- fix(common): side effect
- fix(native): errors
- chore(readme): include downloads badge
- chore(libs): update material3
- Merge branch 'dev' into dev-public

## CI Build | a577169
Redesign | Material 3 Expressive Update!
This build introduces a major UI overhaul.

Dependences:
- SE Extended now targets Android 16 (SDK 36)
- Updated some 3rd party libs

UI/UX:
- Completely redesigned card layouts.
- Replaced dialogs with bottom sheets.
- Added support for Material 3 Expressive (Expressive is still in the alpha stage, so UI bugs may happen).
- Redesigned FABs & Added FAB menus for better access.
- New loaders and progress indicators.
- Changed out tabs rows for group buttons & embedded into the top app bar
- Message preview has a new look
- Redesigned some dropdown menus (Features, logger history)
- Added search placeholders to all searches bars

App Features:
- New "Appearance" option in settings (app themes, blur option, app icons, liquid glass and more)
- New "Updating" option in settings (notifications, update checker and more)
- New "Credits"  & "About" pages
- You can import your exported message logger database via settings (.db only)
- You can now merge multiple message logger databases into a single file (via message logger options)
- New updating process (Users can directly download & Install updates within SE Extended)
- When an update is available users will receive a notification (optional via settings)
- Added translation support to most pages.
- Added an Easter Egg
- Users can now swipe to delete tasks.
- Added built in repos for the catalog on themes (scripts catalog coming next build)
- Some debug settings are now locked via a password to protect users

Snapchat Features:
- in-app Menu Dialog has been disabled (This feature will come back in future builds)

Scripting Engine:
- Script developers can now add custom cover images for their scripts.

Fixes:
- Improved stability by fully disabling features that are incompatible with the user's snapchat version to prevent crashes.
- Hides FAB Menu when scripts folder hasn't been selected
- Fixed default app icon
- Fixed message logger being set to 3 days when app closes

## CI Build | a0b21c8
- feat(debug): log resources
- feat(scripting): how to use
- fix(disclaimer): button font size
- feat(app/ui): translations
- fix(lang): trailing comma
- fix(app/ui): external link icons
- Merge remote-tracking branch 'upstream/dev' into dev

## CI Build | 9b9f096
- fix: translation keys
- refactor: remove Experimental colors
- fix: missing commits from upstream
- fix: mappings
- fix: security feature
- merge upstream/dev into dev (Sync Fork)
- fix: disable inapp popup
- fix: composer loading
- refactor(core): safemode warn
- fix: crash
- chore(readme): update
- Merge branch 'rhunk:dev' into dev
- fix: native build
- refactor(issues): remove theme
- chore(ci): update jdk to 21
- refactor(common/en_us.json): send_override_dialog
- fix(common/en_us.json): spell
- chore(readme): refactor download
- chore(workflows): refactor
- fix(native): duplicate code
- Update CHANGELOGS.md
- chore(readme): contributors
- refactor(ui/scripts): url
- refactor(core): remove sif status
- fix(core/ui_tweaks): infinite loading animation (#116)
- fix(translations): space
- Revert "refactor(core): remove sif status"
- fix(security features): disabled custom error message
- fix(ui/home): git hash link opener
- fix: disable security feat
- Revert "fix: disable security feat"
- refactor(core/security_features): sif composable
- chore(readme): updates
- chore: update dependencies
- fix(build): deprecated function
- fix(build): native ndk version
- fix(build/manager): fix shell command
- fix(lang): gallery media key
- feat(ui/scripting): more info on script card
- feat(ui/scripting): note
- refactor(LSPatchupdater): toasts
- chore(readme): mention not stolen
- refactor(lang): name
- fix(sif): enable sif screen
- feat: disclaimer statement
- chore(readme): add contributor
- refactor(screens/securityscreen): Dialog text
- chore: update some dependencies
- Revert "chore: update some dependencies"
- fix: extra spacing
- refactor(app): name
- refactor(app/updater): repo link
- fix(app/updater): link opener
- Revert "fix(app/updater): link opener"
- fix(ui/home): syntax/conflicts
- feat(ui/scripting): notice
- chore(readme): add contributors
- Revert "fix(core/ui_tweaks): infinite loading animation"
- chore(readme): remove a pull request
- refactor: some translations
- fix(config/messagingtweaks): auto purge
- fix: ci
- chore: translations (#109)
- refactor(core): settings menu
- chore: delete pull request
- Merge remote-tracking branch 'refs/remotes/upstream/dev' into dev

## CI Build | cb8df1b
- chore(readme): refactor contributor link
- Merge branch 'rhunk:dev' into dev ( Sync Repo)
- chore(readme): update feature list
- chore(ci): set pull request builds
- chore(readme): update contributor list
- chore: translations (#102)
- chore(readme): fix translation
- chore(readme): update contributor list
- chore: refactor workflows
- feat(core/ui_tweaks): multiple hide ui components (#106)
- refactor: sync
- fix: sync
- refactor: rhunks changes
- fix: link
- fix: unused code
- chore: translations (#105)

## CI Build | b075ecf
- Revert "refactor(core): init"
- Revert "fix: init"
- Revert "refactor(loader): name change"
- Revert "Merge branch 'refs/heads/sync' into dev"
- Revert "feat(scripting): custom module execution sides"

## CI Build |  8e07e12
- Merge branch 'rhunk:dev' into dev (Sync Fork)
- feat(theme_picker): midnight slate
- fix: export properties
- revert "fix: export properties"
- fix: color class
- chore(readme): fix contributor link
- Translated: (Japanese)
- Merge remote-tracking branch 'weblate/dev' into dev
- refactor: fix translations
- refactor
- Merge branch 'refs/heads/sync' into dev
- refactor(loader): name change
- chore: translations
- fix: init
- refactor(core): init

## CI Build | b5a2b7c
- refactor(readme): changelogs
- chore: translations (rhunk#1094)
- refactor(readme): donate
- refactor(ui/home): donate link
- refactor(readme): contributor name change
- refactor(readme)
- refactor: home/ui
- Revert "[WIP] Update fstat_hook.h "
- chore(readme): remove contributor
- chore: translations
- chore(readme): add contributor
- chore: translations
- Merge remote-tracking branch 'upstream/dev' into dev (Sync Fork)
- refactor(manager): home

## CI Build | 333f102
- Merge branch 'rhunk:dev' into dev (Sync Fork)
- chore(readme): fix spell
- chore: sync translations
- chore: translations
- chore: update contributor list
- chore: translations
- chore: changelogs
- refactor: changelogs
- feat(theme_picker): watercolor wash
- refactor: manifest
- chore(readme): add contributor
- chore: translations
- chore(changelogs): start refactoring
- Revert "chore: translations "
- fix: translations
- chore: fix translations
- chore: translations
- chore: add contributor
- refactor: lang
- feat(home/ui): changelog & Donate icon
- Merge remote-tracking branch 'origin/dev' into dev (Fix Conflics)
- chore: translations
- chore: sync translations from upstream
- fix: translation conflics
- fix
- feat(theme_picker): 23 more themes
- chore: translations
- [WIP] Update fstat_hook.h
- chore(readme): add contributor

## CI Build | 776930b
- refactor: readme
- Merge branch 'rhunk:dev' into dev (Sync Fork)
- refactor: lang
- chore: translations

## CI Build | 1ef90bc
- feat(colors): colors debug
- Merge branch 'rhunk:dev' into dev (Sync Fork)
- refactor: colors debug description
- Merge remote-tracking branch 'origin/dev' into dev (Fix Conflicts)
- feat(experimental): new color options
- Revert "feat(experimental): new color options"
- feat(experimental): new color options
- refactor: lang

## CI Build | 5f4b4dd
- chore(readme): logo size
- refactor(list_background_drawable): description
- chore(readme): update main features
- Merge branch 'rhunk:dev' into dev (Sync Fork)
- refactor: logs output
- chore(readme): add contributor
- chore: translations
- Revert "refactor: logs output"
- fix: logs output

## CI Build | 4a77fce
- Merge branch 'rhunk:dev' into dev (Sync Fork)
- fix(app): pick folder
- chore(readme): add badges, add mention to weblate
- chore(readme): add full transition badge
- refactor(experimentalColors): move colors to custom colors
- chore: translations

## CI Build | 5272d65
- feat(experimental): more color options
- refactor: quick actions
- Merge branch 'rhunk:dev' into dev (Sync Fork)

## CI Build | b368564
- chore(ci): sign debug apks
- refactor: homepage/icons
- refactor(routes): duplicate code
- feat(experimental): experimental color options

## CI Build | 8f25db4
- refactor: snapenhance to se extended