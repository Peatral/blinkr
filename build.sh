#@IgnoreInspection BashAddShebang

target="thumbv7m-none-eabi"

export RUSTFLAGS="-C relocation-model=pie -C codegen-units=1 -C link-arg=--gc-sections -C link-arg=--build-id=sha1 -C link-arg=--emit-relocs -C debuginfo=2"

C_FILE="build/src/message_keys.auto.c"

if [ ! -f "$C_FILE" ] || [ "package.json" -nt "$C_FILE" ]; then
    echo "Pebble keys are missing or outdated. Re-generating headers..."
    pebble clean
    pebble build || true
fi

# Build the project through Cargo
cargo --version
cargo build --target $target --release || exit 1

cd target/$target/release/deps

# Extract the archive
ar x *.a

# Remove all the mess produced by Rust (shouldn't be a problem if you use the 'compiler-builtins' crate).
find . -type f ! -name '*.rcgu.o' -delete

cd -

# Build through waf
pebble build
