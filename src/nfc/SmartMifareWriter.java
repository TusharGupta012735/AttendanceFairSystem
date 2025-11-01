package nfc;

import javax.smartcardio.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Library-style SmartMifareWriter. Blocking methods to be called from a
 * background thread.
 *
 * Usage:
 * SmartMifareWriter.WriteResult r = SmartMifareWriter.writeText("Hello world");
 */
public class SmartMifareWriter {

    private static final byte[][] COMMON_KEYS = new byte[][] {
            hex("FFFFFFFFFFFF"),
            hex("A0A1A2A3A4A5"),
            hex("D3F7D3F7D3F7"),
            hex("000000000000"),
            hex("AABBCCDDEEFF"),
            hex("4D3A99C351DD")
    };
    private static final int KEY_SLOT = 0x00;

    /** Default wait for card present (ms) */
    public static final long DEFAULT_PRESENT_TIMEOUT_MS = 10_000L;
    /** Default wait for card absent after write (ms) */
    public static final long DEFAULT_ABSENT_TIMEOUT_MS = 5_000L;

    // Public result POJO
    public static class WriteResult {
        public final String uid; // UID hex string (uppercase, no spaces)
        public final List<Integer> blocks; // block indices written
        public final String textWritten; // original text written
        public final Instant timestamp; // when write completed

        public WriteResult(String uid, List<Integer> blocks, String textWritten, Instant timestamp) {
            this.uid = uid;
            this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
            this.textWritten = textWritten;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "WriteResult{uid=" + uid + ", blocks=" + blocks + ", textWritten=" + textWritten + ", timestamp="
                    + timestamp + "}";
        }
    }

    /**
     * Write text using default timeouts. Blocking call.
     * 
     * @param text text to write (UTF-8). Must be non-empty.
     * @return WriteResult on success
     * @throws Exception on any failure (use message to show user)
     */
    public static WriteResult writeText(String text) throws Exception {
        return writeText(text, DEFAULT_PRESENT_TIMEOUT_MS, DEFAULT_ABSENT_TIMEOUT_MS);
    }

    /**
     * Write text blocking call with explicit timeouts.
     * 
     * @param text             text to write (UTF-8)
     * @param presentTimeoutMs timeout waiting for card present
     * @param absentTimeoutMs  timeout waiting for card absent after write
     * @return WriteResult on success
     * @throws Exception on failure
     */
    public static WriteResult writeText(String text, long presentTimeoutMs, long absentTimeoutMs) throws Exception {
        if (text == null)
            throw new IllegalArgumentException("text is null");
        String trimmed = text.trim();
        if (trimmed.isEmpty())
            throw new IllegalArgumentException("text is empty");

        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        if (terminals == null || terminals.isEmpty()) {
            throw new Exception("No NFC reader detected");
        }
        CardTerminal terminal = terminals.get(0);

        // Wait for card present (with chunked polling to be resilient)
        final long chunkMs = 500L;
        long deadline = System.currentTimeMillis() + presentTimeoutMs;
        boolean present = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                present = terminal.waitForCardPresent((int) chunkMs);
            } catch (CardException ce) {
                // ignore and continue until deadline
            }
            if (present)
                break;
        }
        if (!present)
            throw new Exception("Timed out waiting for card (ms=" + presentTimeoutMs + ")");

        Card card = null;
        List<Integer> writtenBlocks = new ArrayList<>();
        String uid = "";
        try {
            card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            // read UID
            CommandAPDU uidCmd = new CommandAPDU(new byte[] { (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00 });
            ResponseAPDU rUid = channel.transmit(uidCmd);
            uid = bytesToHex(rUid.getData()).replace(" ", "");

            // prepare chunks (16 bytes each)
            byte[] payload = trimmed.getBytes(StandardCharsets.UTF_8);
            List<byte[]> chunks = chunkBytes(payload, 16);

            // For each chunk, find an empty writable block and write.
            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                byte[] chunk = chunks.get(chunkIndex);
                boolean written = false;

                // search candidate blocks on MIFARE Classic 1K (blocks 4..63)
                for (int block = 4; block < 64 && !written; block++) {
                    if (isTrailerBlock(block))
                        continue;

                    // try keys for this block
                    for (byte[] key : COMMON_KEYS) {
                        boolean loaded = loadKey(channel, KEY_SLOT, key);
                        if (!loaded)
                            continue;

                        AuthResult probe = tryAuthAsAorB(channel, block, (byte) KEY_SLOT);
                        if (!probe.success)
                            continue;

                        // read block to see if it's empty
                        byte[] existing = readBlock(channel, block);
                        boolean empty = existing == null || isDataBlockEmpty(existing);
                        if (!empty)
                            continue;

                        // final auth using discovered key type
                        boolean finalAuth = authWithKeySlot(channel, block, probe.keyType, (byte) KEY_SLOT);
                        if (!finalAuth)
                            continue;

                        // attempt write
                        writeBlock(channel, block, chunk); // throws on verification failure
                        writtenBlocks.add(block);
                        written = true;
                        break; // chunk -> next chunk
                    } // keys
                } // blocks

                if (!written) {
                    throw new Exception("No empty writable block found for chunk " + (chunkIndex + 1));
                }
            } // chunks

            // success
            return new WriteResult(uid, writtenBlocks, trimmed, Instant.now());

        } catch (Exception e) {
            // bubble up with context
            throw new Exception("Write failed: " + e.getMessage(), e);
        } finally {
            // disconnect
            if (card != null) {
                try {
                    card.disconnect(false);
                } catch (Exception ignored) {
                }
            }
            // wait for absent (best-effort)
            long absentDeadline = System.currentTimeMillis() + absentTimeoutMs;
            while (System.currentTimeMillis() < absentDeadline) {
                try {
                    if (terminal.waitForCardAbsent(500))
                        break;
                } catch (CardException ignored) {
                }
            }
        }
    }

    // --- Internal helper classes & methods (ported/kept from your code) ---
    private static class AuthResult {
        boolean success;
        byte keyType;

        AuthResult(boolean s, byte t) {
            success = s;
            keyType = t;
        }
    }

    private static AuthResult tryAuthAsAorB(CardChannel c, int b, byte slot) {
        boolean aOk = authWithKeySlot(c, b, (byte) 0x60, slot);
        if (aOk)
            return new AuthResult(true, (byte) 0x60);
        boolean bOk = authWithKeySlot(c, b, (byte) 0x61, slot);
        if (bOk)
            return new AuthResult(true, (byte) 0x61);
        return new AuthResult(false, (byte) 0x00);
    }

    private static boolean loadKey(CardChannel c, int slot, byte[] key) {
        try {
            byte[] apdu = new byte[11];
            apdu[0] = (byte) 0xFF;
            apdu[1] = (byte) 0x82;
            apdu[2] = 0x00;
            apdu[3] = (byte) slot;
            apdu[4] = 0x06;
            System.arraycopy(key, 0, apdu, 5, 6);
            ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
            return r.getSW() == 0x9000;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean authWithKeySlot(CardChannel c, int b, byte type, byte slot) {
        try {
            byte[] apdu = new byte[] { (byte) 0xFF, (byte) 0x86, 0x00, 0x00, 0x05, 0x01, 0x00, (byte) b, type, slot };
            ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
            return r.getSW() == 0x9000;
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeBlock(CardChannel c, int b, byte[] data) throws Exception {
        if (isTrailerBlock(b))
            throw new Exception("Refusing to write to trailer block " + b);
        if (data.length != 16)
            throw new Exception("Invalid data size: " + data.length + " (expected 16)");

        byte[] apdu = new byte[21];
        apdu[0] = (byte) 0xFF;
        apdu[1] = (byte) 0xD6;
        apdu[2] = 0x00;
        apdu[3] = (byte) b;
        apdu[4] = 0x10;
        System.arraycopy(data, 0, apdu, 5, 16);

        ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
        if (r.getSW() != 0x9000) {
            throw new Exception("Write failed SW=" + Integer.toHexString(r.getSW()));
        }

        byte[] verify = readBlock(c, b);
        if (verify == null)
            throw new Exception("Write verification failed - couldn't read back block " + b);
        if (!Arrays.equals(data, verify))
            throw new Exception("Write verification failed - data mismatch in block " + b);
    }

    private static byte[] readBlock(CardChannel c, int b) {
        try {
            byte[] cmd = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) b, 0x10 };
            ResponseAPDU r = c.transmit(new CommandAPDU(cmd));
            if (r.getSW() == 0x9000) {
                return r.getData();
            }
        } catch (Exception e) {
            // ignore / return null
        }
        return null;
    }

    private static boolean isTrailerBlock(int b) {
        return (b % 4) == 3;
    }

    private static byte[] hex(String s) {
        s = s.replaceAll("[^0-9A-Fa-f]", "");
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++)
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    private static List<byte[]> chunkBytes(byte[] src, int size) {
        List<byte[]> out = new ArrayList<>();
        for (int i = 0; i < src.length; i += size) {
            int len = Math.min(size, src.length - i);
            byte[] chunk = new byte[size];
            Arrays.fill(chunk, (byte) 0x00);
            System.arraycopy(src, i, chunk, 0, len);
            out.add(chunk);
        }
        if (out.isEmpty()) {
            byte[] empty = new byte[size];
            Arrays.fill(empty, (byte) 0x00);
            out.add(empty);
        }
        return out;
    }

    private static boolean isDataBlockEmpty(byte[] d) {
        if (d == null || d.length == 0)
            return true;
        for (byte b : d)
            if (b != 0x00 && b != (byte) 0xFF)
                return false;
        return true;
    }

    private static String bytesToHex(byte[] b) {
        if (b == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte v : b)
            sb.append(String.format("%02X:", v));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
}
