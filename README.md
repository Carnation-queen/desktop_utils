# Desktop Utils

A lightweight cross-platform Swing desktop utility that runs quietly in the system tray and provides six core features:

- **System Info** — displays basic information about the current runtime environment.
- **Scheduled Shutdown** — shuts the computer down automatically on a one-time, daily, or workday-only schedule.
- **Holiday Management** — imports annual holiday data from Excel and uses it to skip shutdowns on holidays.
- **Settings** — toggles auto-start on login and update preferences.
- **Software Update** — checks for new releases and downloads the update.
- **Novel Reader** — an offline TXT novel library with progress-saving paginated reading.

> The UI language is Chinese.

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
- **Auto-start on login** (Windows only) is registered under `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`. On startup the registered command is refreshed so it always points to the current location of the application.

### 5. Software Update

- The **About (关于)** tab shows the current version and a *检查更新* button.
- Update checking reads the latest GitHub Release from the repository's releases API (configurable under **Settings → 更新设置**).
- When a newer version is found, the app shows the release notes and a *下载更新* button; the downloaded installer/asset can be opened right away.
- **Auto-check on startup** is enabled by default and can be toggled in Settings.

To ship updates, publish a GitHub Release with a tag such as `v1.0.2` and attach the installer (e.g. `desktop_utils-1.0.2.exe`) as a release asset. The app compares the tag version against the version baked into the build (`version.properties`, sourced from `pom.xml`).

### 6. Novel Reader（小说阅读器）

- Press **Ctrl+Alt+Shift+F12** anywhere inside the main window to open a standalone **Novel Reader** window (independent of the main frame).
- **Library (书库)**: import one or more `.txt` files, view the book list with reading progress and last-read time; supports **open / rename / delete**, and double-click a book to start (or resume) reading.
- **Reading**: paginated text display with previous/next page buttons, plus keyboard support (`Space` / `PageDown` / `→` next, `PageUp` / `←` previous, mouse wheel flips pages). Font size can be adjusted with `A−` / `A＋`.
- **Chapter navigation**: common chapter headings (第X章/节/回…、楔子、序章、番外、后记…) are auto-detected into a clickable **目录** (table of contents); a progress slider and a chapter indicator are shown in the footer.
- **Progress memory**: the reading position (character offset) is saved automatically on every page turn and when the window closes; reopening the same book resumes at exactly the same spot.
- Text files are copied into `~/.desktop_utils/novels/books/` and decoded adaptively (UTF-8, UTF-16 or GB18030/GBK), so the library stays self-contained after import.

> Tip: pressing Ctrl+Alt+Shift+F12 again while the Novel Reader is focused hides it (progress is saved); calling it from the main window brings it back to the last view.

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
target/desktop_utils-1.0-SNAPSHOT.jar
```

## Run

```bash
java -jar target/desktop_utils-1.0-SNAPSHOT.jar
```

## Packaging (Windows Installer)

The Windows installer is built with **jpackage** (bundled with JDK 17+). On Windows, `jpackage` needs **WiX Toolset 3.x** to generate the `.exe` installer.

### 1. Prepare the application JAR

`jpackage` packages one self-contained executable JAR. Build the Maven shaded JAR, which bundles **all** runtime dependencies (FlatLaf, Apache POI, Gson, …) into a single file:

```bash
mvn clean package
```

The output is written to `target/desktop_utils-<version>.jar` (e.g. `target/desktop_utils-1.0.2.jar`).

> ⚠️ Do **not** feed jpackage the IntelliJ artifact JAR (`out/artifacts/desktop_utils_jar/desktop_utils.jar`): it is a thin JAR whose dependencies are missing or incomplete. Packaging from it produced a build that crashed on “检查更新” with `NoClassDefFoundError: com/google/gson/JsonParser` because Gson was absent from the runtime classpath.

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
  --main-jar desktop_utils-1.0.2.jar `
  --main-class changcun.desktop_utils.Main `
  --dest "installer" `
  --license-file "LICENSE.txt" `
  --icon "icon.ico" `
  --win-dir-chooser `
  --win-shortcut-prompt `
  --win-menu
```

Key options:

| Option                 | Purpose                                                            |
| ---------------------- | ------------------------------------------------------------------ |
| `--win-dir-chooser`    | Let the user choose the install directory during setup.            |
| `--win-shortcut-prompt`| Prompt whether to create a desktop shortcut.                       |
| `--win-menu`           | Add the app to the Windows Start Menu.                             |
| `--license-file`       | Embed `LICENSE.txt` as the installer license (must be `.txt`).     |

On success, the installer is written to `installer/desktop_utils-1.0.exe`.

> To use the multi-resolution `icon.ico` for the installer/executable, add `--icon icon.ico` (the file is provided in the project root).

## Configuration & Data Files

All state is stored under `~/.desktop_utils/`:

| File                    | Purpose                                    |
| ----------------------- | ------------------------------------------ |
| `config.properties`   | Scheduled shutdown settings.               |
| `holidays.properties` | Imported holiday dates and reminder state. |
| `app.properties`      | General settings (auto-start toggle).      |

## Project Structure

```
src/main/java/changcun/desktop_utils/
├── Main.java              # Application entry point
├── model/                 # Data models (ShutdownConfig, HolidayData, AppSettings)
├── service/               # Business logic (scheduler, stores, auto-start, shutdown)
├── tray/                  # System tray management
└── ui/                    # Swing UI panels and theming
src/main/resources/        # Runtime resources (application icon)
```

## Technology Stack

- **Java 17**
- **Swing** with [FlatLaf](https://www.formdev.com/flatlaf/) (modern look & feel)
- **Apache POI** for Excel import
- **Maven Shade Plugin** for packaging

## Icons

- `src/main/resources/icon.png` — runtime icon used by the window, tray, and taskbar (falls back to a programmatically drawn power icon if missing).
- `icon.ico` (project root) — multi-resolution Windows icon (16/24/32/48/64/128/256) for building an `.exe` with `jpackage` or Launch4j.

## License

See the [LICENSE](LICENSE) file.
