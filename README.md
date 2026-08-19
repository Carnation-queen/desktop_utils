# Desktop Utils

A lightweight cross-platform Swing desktop utility that runs quietly in the system tray and provides four core features:

- **System Info** — displays basic information about the current runtime environment.
- **Scheduled Shutdown** — shuts the computer down automatically on a one-time, daily, or workday-only schedule.
- **Holiday Management** — imports annual holiday data from Excel and uses it to skip shutdowns on holidays.
- **Settings** — toggles auto-start on login (Windows only).

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

## Requirements

- **JDK 17** or newer
- **Maven 3.6+** (for building)

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
