import clayConfig from './config';
import Clay from '@rebble/clay';

// Initialize Clay configuration
const clay = new Clay(clayConfig);

interface TimePair {
  start: number;
  end: number;
}

let historyBuffer: TimePair[] = [];
let expectedChunks = 0;
let receivedChunks = 0;

Pebble.addEventListener('ready', () => {
    console.log('PebbleKit JS is ready!');
});

Pebble.addEventListener('appmessage', (e) => {
    const dict = e.payload;
    if (!dict) return;

    if ('SYNC_TOTAL_CHUNKS' in dict) {
        expectedChunks = dict.SYNC_TOTAL_CHUNKS as number;
        receivedChunks = 0;
        historyBuffer = [];
        console.log(`Starting sync. Expecting ${expectedChunks} chunks.`);
    }

    if ('SYNC_DATA_CHUNK' in dict) {
        const rawBytes = dict.SYNC_DATA_CHUNK as number[];

        const parsedPairs = parseHistoryBytes(rawBytes);
        historyBuffer.push(...parsedPairs);
        receivedChunks++;

        console.log(`Received chunk ${receivedChunks}/${expectedChunks} (${parsedPairs.length} pairs)`);

        if (receivedChunks >= expectedChunks) {
            finalizeSync();
        }
    }
});

/**
 * Parses an array of bytes from the Pebble into an array of TimePair objects.
 * The Rust TimePair is two time_t (i32) fields -> 8 bytes total per pair.
 * Pebble uses Little-Endian byte order.
 */
function parseHistoryBytes(bytesArray: number[]): TimePair[] {
    const pairs: TimePair[] = [];

    const uint8 = new Uint8Array(bytesArray);
    const dataView = new DataView(uint8.buffer);

    for (let i = 0; i < uint8.length; i += 8) {
        const start = dataView.getInt32(i, true);
        const end = dataView.getInt32(i + 4, true);

        pairs.push({ start, end });
    }

    return pairs;
}

/**
 * Saves the completed history buffer to the phone's persistent local storage
 */
function finalizeSync(): void {
    try {
        historyBuffer.sort((a, b) => a.start - b.start);

        const backupData = {
            last_sync: Date.now(),
            data: historyBuffer
        };

        localStorage.setItem('pebble_history_backup', JSON.stringify(backupData));
        console.log('Sync complete! Backup successfully saved to phone storage.');
    } catch (err) {
        console.error('Failed to save backup to localStorage:', err);
    }
}