# Desktop Utils

A lightweight cross-platform Swing desktop utility that runs quietly in the system tray and provides eight core features:

- **System Info** — displays basic information about the current runtime environment.
- **Scheduled Shutdown** — shuts the computer down automatically on a one-time, daily, or workday-only schedule.
- **Holiday Management** — imports annual holiday data from Excel and uses it to skip shutdowns on holidays.
- **Settings** — manages auto-start on login, update preferences, and the interface language.
- **Software Update** — checks for new releases and downloads the update.
- **Novel Reader** — an offline TXT novel library with progress-saving paginated reading.
- **Shutdown Audit Log** — a hidden record of when/how the scheduler fired (visible via **F12** only).
- **Interface Language** — a bilingual (Chinese & English) UI, switchable live from Settings.

> The UI is bilingual. By default it follows the OS language (Chinese OS → Chinese, anything else → English); you can switch between **跟随系统 / 简体中文 / English (Follow System / Simplified Chinese / English)** anytime in **Settings → Interface Language** — no restart required.

## Features

### 1. System Info

Shows the OS name, OS version, architecture, Java version, Java vendor, and the current user name.

### 2. Scheduled Shutdown

Four shutdown modes:

| Mode        | Behavior                                               |
| ----------- | ------------------------------------------------------ |
| `Off`     | No automatic shutdown.                                 |
| `Once`    | Shut down once at a specific date and time.            |
| `Daily`   | Shut down every day at a specific time.                |
| `Workday` | Shut down at a specific time only on non-holiday days. |

The scheduler checks every second in the background and executes a full shutdown when the trigger time is reached. The shutdown command is adapted per platform:

- **Windows**: `shutdown /s /f /t 0`
- **macOS**: `osascript` (falls back to `shutdown -h now`)
- **Linux**: `shutdown -h now` (falls back to `systemctl poweroff`)

### 3. Holiday Management

- Import holiday dates from an Excel (`.xlsx` / `.xls`) file — the first column of the first sheet is parsed as dates.
- View imported holidays in a table (index, date, day of week).
- Holiday data is used by the *Workday* shutdown mode to skip holidays.
- Automatic reminders prompt you to re-import the current year's holidays on **Jan 1** and the next year's holidays on **Dec 31**.

### 4. System Tray & Settings

- Closing the main window minimizes the app to the system tray instead of exiting.
- The tray icon supports **double-click to show the main window** and a right-click menu with *Show Main Window* and *Exit*.
- **Auto-start on login** is supported cross-platform:
  - **Windows** — a Run entry under `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`;
  - **macOS** — a LaunchAgent under `~/Library/LaunchAgents/`;
  - **Linux** — a `.desktop` file under `~/.config/autostart/`.
  On startup the registered command is refreshed so it always points to the current location of the application.
- The **Settings** page additionally lets you toggle *auto-check for updates* on startup, edit the *update source URL*, and pick the *interface language* (see [Interface Language (i18n)](#8-interface-language-i18n)).

### 5. Software Update

- The **About** tab shows the current version and a *Check for Updates* button.
- Update checking reads the latest GitHub Release from the repository's releases API (configurable under **Settings → Updates**).
- When a newer version is found, the app shows the release notes and a *Download Update* button; the downloaded installer/asset can be opened right away.
- **Auto-check on startup** is enabled by default and can be toggled in Settings.

To ship updates, publish a GitHub Release with a tag such as `v1.0.4` and attach the installer (e.g. `desktop_utils-1.0.4.exe`) as a release asset. The app compares the tag version against the version baked into the build (`version.properties`, sourced from `pom.xml`).

### 6. Novel Reader（小说阅读器）

- Press **Ctrl+Alt+Shift+F12** anywhere inside the main window to open a standalone **Novel Reader** window (independent of the main frame).
- **Library (书库)**: import one or more `.txt` files, view the book list with reading progress and last-read time; supports **open / rename / delete**, and double-click a book to start (or resume) reading.
- **Reading**: paginated text display with previous/next page buttons, plus keyboard support (`Space` / `PageDown` / `→` next, `PageUp` / `←` previous, mouse wheel flips pages). Font size can be adjusted with `A−` / `A＋`.
- **Chapter navigation**: common chapter headings (第X章/节/回…、楔子、序章、番外、后记…) are auto-detected into a clickable **Contents (目录)**; a progress slider and a chapter indicator are shown in the footer.
- **Progress memory**: the reading position (character offset) is saved automatically on every page turn and when the window closes; reopening the same book resumes at exactly the same spot.
- Text files are copied into `~/.desktop_utils/novels/books/` and decoded adaptively (UTF-8, UTF-16 or GB18030/GBK), so the library stays self-contained after import.

> Tip: pressing Ctrl+Alt+Shift+F12 again while the Novel Reader is focused hides it (progress is saved); calling it from the main window brings it back to the last view.

### 7. Shutdown Audit Log（关机日志）

- **Hidden entry**: press **F12** anywhere inside the main window to open a standalone **Shutdown Log** window. There is **no** public menu/button/tab for it — only F12 reveals it.
- Records an **audit trail** of scheduler activity into `~/.desktop_utils/logs/shutdown.log`:
  - program startup / exit (with app version, OS and Java version);
  - scheduler start/stop and every config change (set / cancel);
  - each **actually fired** shutdown, with mode, the originally scheduled instant and the real trigger instant;
  - one-shot tasks that were **missed** and auto-cancelled.
- **Millisecond precision**: every line starts with a `yyyy-MM-dd HH:mm:ss.SSS` timestamp, and trigger events also embed the raw epoch-millisecond values.
- Implemented with **SLF4J + Logback** (rolling file, 30-day history). While the Shutdown Log window is focused, pressing F12 (or ESC) hides it again; the window provides **Refresh (刷新)** and **Clear Log (清空日志)** buttons.

> Tip: the audit file is a plain text file under the user home directory — treat it as developer/maintenance information, not as a security boundary.

### 8. Interface Language (i18n)

- The UI ships with **Chinese** and **English**; the default is **Follow System** (a Chinese OS shows Chinese, anything else shows English).
- Switch it live in **Settings → Interface Language** — the whole UI (panels, tabs, dialogs, tray menu, novel reader and log window) rebuilds immediately, no restart needed.
- The selection is persisted in `app.properties` under the `app.language` key (`system` / `zh` / `en`).
- All strings are centralized in UTF-8 resource bundles under `src/main/resources/i18n/`:
  - `messages.properties` — Chinese (also the fallback bundle);
  - `messages_en.properties` — English (falls back to Chinese for any missing key).
- Strings are resolved through `changcun.desktop_utils.i18n.Messages` (`Messages.tr("key", args…)`). To add a language, create a `messages_<lang>.properties` with the same key set and map it in `Messages.resolveLocale(...)`; the `MessagesTest` unit test keeps the Chinese and English key sets in sync.

## Requirements

- **JDK 17** or newer (includes `jpackage`)
- **Maven 3.6+** (for building)
- **WiX Toolset 3.x** — required only for building the Windows installer with `jpackage` (see [Packaging](#packaging-windows-installer))

## Build

```bash
mvn clean package
```

The Maven Shade plugin produces a self-contained executable JAR:

```
target/desktop_utils-1.0.4.jar
```

## Run

```bash
java -jar target/desktop_utils-1.0.4.jar
```

## Packaging (Windows Installer)

The Windows installer is built with **jpackage** (bundled with JDK 17+). On Windows, `jpackage` needs **WiX Toolset 3.x** to generate the `.exe` installer.

### 1. Prepare the application JAR

`jpackage` packages one self-contained executable JAR. Build the Maven shaded JAR, which bundles **all** runtime dependencies (FlatLaf, Apache POI, Gson, …) into a single file:

```bash
mvn clean package
```

The output is written to `target/desktop_utils-<version>.jar` (e.g. `target/desktop_utils-1.0.4.jar`).

> ⚠️ Do **not** feed jpackage the IntelliJ artifact JAR (`out/artifacts/desktop_utils_jar/desktop_utils.jar`): it is a thin JAR whose dependencies are missing or incomplete. Packaging from it produced a build that crashed when checking for updates with `NoClassDefFoundError: com/google/gson/JsonParser` because Gson was absent from the runtime classpath.

### 2. Configure WiX 3.x (one-time setup)

`jpackage` looks for WiX 3.x in the directory named by the `WIX` environment variable, or falls back to the default path `C:\Program Files (x86)\WiX Toolset v3.11`.

1. Download the WiX 3.11.2 binaries from the [wix3 releases](https://github.com/wixtoolset/wix3/releases) page (`wix311-binaries.zip`).
2. Extract the archive, for example to `C:\wix311`, so that `C:\wix311\bin\candle.exe` and `C:\wix311\bin\light.exe` exist.
3. Create a system or user environment variable `WIX` that points to that directory:

   ```powershell
   setx WIX "C:\wix311"
   ```

   (Or set it via *System Properties → Environment Variables*. Open a new terminal afterwards so the change takes effect.)
4. Verify the setup:

   ```powershell
   Test-Path "$env:WIX\bin\candle.exe"
   ```

### 3. Run jpackage

Run the command from the project root (so that `LICENSE.txt` resolves):

```powershell
jpackage --name desktop_utils `
  --input "target" `
  --main-jar desktop_utils-1.0.4.jar `
  --main-class changcun.desktop_utils.Main `
  --dest "installer" `
  --license-file "LICENSE.txt" `
  --icon "icon.ico" `
  --win-dir-chooser `
  --win-shortcut-prompt `
  --win-menu `
  --app-version "1.0.4"
```

Key options:

| Option                 | Purpose                                                            |
| ---------------------- | ------------------------------------------------------------------ |
| `--win-dir-chooser`    | Let the user choose the install directory during setup.            |
| `--win-shortcut-prompt`| Prompt whether to create a desktop shortcut.                       |
| `--win-menu`           | Add the app to the Windows Start Menu.                             |
| `--license-file`       | Embed `LICENSE.txt` as the installer license (must be `.txt`).     |

On success, the installer is written to `installer/desktop_utils-1.0.4.exe`.

> To use the multi-resolution `icon.ico` for the installer/executable, add `--icon icon.ico` (the file is provided in the project root).

## Configuration & Data Files

All state is stored under `~/.desktop_utils/`:

| File / Folder         | Purpose                                                     |
| --------------------- | ----------------------------------------------------------- |
| `config.properties`   | Scheduled shutdown settings.                                |
| `holidays.properties` | Imported holiday dates and reminder state.                  |
| `app.properties`      | General settings: auto-start, auto-update, update source URL, interface language. |
| `novels/`             | Novel reader library (`library.json` + `books/*.txt`).      |
| `logs/shutdown.log`   | Shutdown audit log (rolling, 30-day history).               |

## Project Structure

```
src/main/java/changcun/desktop_utils/
├── Main.java              # Application entry point
├── i18n/                  # Internationalization: Messages loader + message keys
├── model/                 # Data models (ShutdownConfig, HolidayData, AppSettings, novel…)
├── service/               # Business logic (scheduler, stores, auto-start, update checker, audit log)
├── tray/                  # System tray management
└── ui/                    # Swing UI panels, theming, and ui/novel reader views
src/main/resources/        # Runtime resources: icon, logback.xml, i18n/*.properties
src/test/java/             # JUnit 5 tests (models, stores, scheduler, update checker, i18n)
```

## Technology Stack

- **Java 17**
- **Swing** with [FlatLaf](https://www.formdev.com/flatlaf/) (modern look & feel)
- **Apache POI** for Excel import (holidays)
- **Gson** for parsing the GitHub Releases update API
- **SLF4J + Logback** for the shutdown audit log
- **Maven Shade Plugin** for packaging a self-contained JAR
- **JUnit 5** for unit tests

## Icons

- `src/main/resources/icon.png` — runtime icon used by the window, tray, and taskbar (falls back to a programmatically drawn power icon if missing).
- `icon.ico` (project root) — multi-resolution Windows icon (16/24/32/48/64/128/256) for building an `.exe` with `jpackage` or Launch4j.

## License

See the [LICENSE](LICENSE) file.
