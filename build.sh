#!/bin/bash
set -e

target="thumbv7m-none-eabi"
export RUSTFLAGS="-C relocation-model=pie -C codegen-units=1 -C link-arg=--gc-sections -C link-arg=--build-id=sha1 -C link-arg=--emit-relocs -C debuginfo=2"

C_FILE="build/src/message_keys.auto.c"
if [ ! -f "$C_FILE" ] || [ "package.json" -nt "$C_FILE" ]; then
    echo "[RUST-BUILD] Pebble keys are missing or outdated. Pre-building C stubs..."
    pebble clean
    pebble build || true
fi

if [ -f "package.json" ]; then
    PLATFORMS=($(node -p "require('./package.json').pebble.targetPlatforms.join(' ')"))
elif [ -f "appinfo.json" ]; then
    PLATFORMS=($(node -p "require('./appinfo.json').targetPlatforms.join(' ')"))
else
    echo "[RUST-BUILD] ERROR: Neither package.json nor appinfo.json found!"
    exit 1
fi

echo "[RUST-BUILD] Detected target platforms: ${PLATFORMS[*]}"
echo "[RUST-BUILD] Starting multi-platform Rust compilation loop..."

for platform in "${PLATFORMS[@]}"; do
    echo "--------------------------------------------------"
    echo "[RUST-BUILD] Compiling Rust binaries for platform: ${platform^^}"
    echo "--------------------------------------------------"

    rm -rf target/$target/release/deps/*.o

    cargo build --target $target --release --no-default-features --features "$platform"

    PLATFORM_OBJ_DIR="build/rust_out/$platform"
    mkdir -p "$PLATFORM_OBJ_DIR"
    rm -rf "$PLATFORM_OBJ_DIR"/*

    cd target/$target/release/deps
    ar x *.a

    find . -type f ! -name '*.rcgu.o' -delete

    arm-none-eabi-ld -r *.o -o "../../../../$PLATFORM_OBJ_DIR/rust_app.o"

    cd - > /dev/null
done

echo "[RUST-BUILD] All Rust platforms compiled successfully! Handing over to Pebble Waf Linker..."

pebble build