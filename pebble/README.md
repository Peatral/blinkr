# Blinkr

A Pebble smartwatch application designed to protect your eyes using the **20-20-20 rule** while tracking your screen time.

Blinkr is built using Rust for the watchapp and TypeScript for the phone configuration and companion app integration (via `PebbleKit.ts`).

---

## How It Works

- **20-20-20 Rule**: Reminds you every 20 minutes to look at something 20 feet away for 20 seconds to reduce eye strain.
- **Screen Time Tracking**: Logs and tracks your active screen time intervals.
- **Configurable**: Settings like the notification interval can be configured directly from your phone or the watch itself.

---

## Tech Stack

- **Watchapp**: Written in **Rust** using [pebble-rs](https://github.com/Peatral/pebble-rust).
- **Companion App (Phone-side)**: Written in **TypeScript** using [PebbleKit.ts](https://github.com/jccit/PebbleKit.ts) and [Clay](https://github.com/pebble-dev/clay) for configurations.

---

## Installation & Setup

1. **Install Dependencies**:
   Ensure you have Node.js and npm installed, then run:
   ```bash
   npm install
   ```

2. **Rust Toolchain**:
   You need a Rust toolchain targeted for embedded ARM development (`thumbv7m-none-eabi`).

---

## Building the App

Since this project leverages Rust, do not use the standard `pebble build`. Instead, use `cargo-pebble` from the `pebble-rust` project:

```bash
cargo pebble build
```

> [!IMPORTANT]
> The build command relies on `pebble-cli` from the [pebble-rust](https://github.com/Peatral/pebble-rust) repository. Make sure you follow the installation instructions there to set up the build environment.

During compilation:
- TypeScript code in `src/ts/` will compile into JavaScript inside `src/ts-build/`.
- Rust source files will compile and link with the Pebble SDK resources.

---

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
