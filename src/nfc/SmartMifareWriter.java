package nfc;

import javax.smartcardio.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Improved SmartMifareWriter:
 * - timeout-based waiting for card present / absent
 * - detailed console logging
 * - always disconnect card in finally
 * - clearer exception messages
 *
 * NOTE: This method is blocking and SHOULD be called from a background thread
 * (not JavaFX UI thread).
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

    /**
     * Write text to next available writable block(s). Blocks are written in 16-byte
     * chunks.
     *
     * @param text the UTF-8 text to write
     * @throws Exception on failure (with helpful message)
     */
    public static void writeNextAvailableBlock(String text) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        if (terminals.isEmpty()) {
            System.out.println("[Writer] No NFC reader detected.");
            throw new Exception("No NFC reader detected");
        }

        CardTerminal terminal = terminals.get(0);
        System.out.println("[Writer] Using reader: " + terminal.getName());

        // Wait for card present with timeout (10 seconds)
        final long totalWaitMs = 10_000L;
        final long chunkMs = 500L;
        long deadline = System.currentTimeMillis() + totalWaitMs;
        boolean present = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                present = terminal.waitForCardPresent((int) chunkMs);
            } catch (CardException ce) {
                System.out.println("[Writer] waitForCardPresent threw: " + ce.getMessage());
                // continue and try again until timeout
            }
            if (present)
                break;
        }
        if (!present) {
            System.out.println("[Writer] Timed out waiting for card ("
                    + totalWaitMs + " ms).");
            throw new Exception("Timed out waiting for card");
        }

        Card card = null;
        try {
            card = terminal.connect("*");
            System.out.println("[Writer] Card connected. ATR: " + Arrays.toString(card.getATR().getBytes()));
            CardChannel channel = card.getBasicChannel();

            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            List<byte[]> chunks = chunkBytes(payload, 16);
            System.out.println("[Writer] Need to write " + chunks.size() + " chunk(s) for text \"" + text + "\"");

            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                byte[] chunk = chunks.get(chunkIndex);
                boolean written = false;
                // search blocks 4..63 (MIFARE Classic 1K)
                for (int block = 4; block < 64 && !written; block++) {
                    if (isTrailerBlock(block))
                        continue;

                    // Try each known key
                    for (byte[] key : COMMON_KEYS) {
                        boolean loaded = false;
                        try {
                            loaded = loadKey(channel, KEY_SLOT, key);
                        } catch (Exception le) {
                            System.out.println("[Writer] loadKey exception: " + le.getMessage());
                        }
                        if (!loaded) {
                            // can't use this key on this reader or slot
                            // continue trying other keys
                            continue;
                        }

                        AuthResult authProbe = tryAuthAsAorB(channel, block, (byte) KEY_SLOT);
                        if (!authProbe.success) {
                            // this key didn't authorize this block (as A or B) - try next key
                            continue;
                        }

                        byte[] existing = readBlock(channel, block);
                        boolean empty = existing == null || isDataBlockEmpty(existing);
                        if (!empty) {
                            // block already contains data — skip it
                            continue;
                        }

                        // Final auth step before write
                        boolean finalAuth = authWithKeySlot(channel, block, authProbe.keyType, (byte) KEY_SLOT);
                        if (!finalAuth) {
                            System.out.println("[Writer] Final auth failed for block " + block);
                            continue;
                        }

                        // prepare 16-byte chunk (chunkBytes already returns 16 bytes)
                        try {
                            writeBlock(channel, block, chunk);
                            System.out.println("[Writer] Wrote chunk " + (chunkIndex + 1) + "/" + chunks.size()
                                    + " to block " + block);
                            written = true;
                            break;
                        } catch (Exception we) {
                            System.out
                                    .println("[Writer] writeBlock failed for block " + block + " : " + we.getMessage());
                            // try next candidate block or key
                        }
                    } // for keys
                } // for block

                if (!written) {
                    throw new Exception("No empty writable block found for chunk " + (chunkIndex + 1));
                }
            } // for chunks

            System.out.println("[Writer] All chunks written successfully.");

        } catch (Exception e) {
            System.out.println("[Writer] Exception: " + e.getMessage());
            throw e;
        } finally {
            if (card != null) {
                try {
                    card.disconnect(false);
                    System.out.println("[Writer] Card disconnected.");
                } catch (Exception ex) {
                    System.out.println("[Writer] Error while disconnecting card: " + ex.getMessage());
                }
            }
            // wait for card absent (timeout 5s)
            final long absentDeadline = System.currentTimeMillis() + 5_000L;
            boolean absent = false;
            while (System.currentTimeMillis() < absentDeadline) {
                try {
                    absent = terminal.waitForCardAbsent(500);
                } catch (CardException ce) {
                    // ignore and continue until timeout
                }
                if (absent)
                    break;
            }
            if (!absent) {
                System.out.println(
                        "[Writer] Warning: card may still be present after write (timed out waiting for absent).");
            } else {
                System.out.println("[Writer] Card removed.");
            }
        }
    }

    // === Helper inner class ===
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
            boolean ok = r.getSW() == 0x9000;
            System.out.println("[Writer] loadKey slot=" + slot + " key=" + bytesToHex(key) + " -> "
                    + (ok ? "OK" : "FAIL SW=" + Integer.toHexString(r.getSW())));
            return ok;
        } catch (Exception e) {
            System.out.println("[Writer] loadKey threw: " + e.getMessage());
            return false;
        }
    }

    private static boolean authWithKeySlot(CardChannel c, int b, byte type, byte slot) {
        try {
            byte[] apdu = new byte[] {
                    (byte) 0xFF, (byte) 0x86, 0x00, 0x00, 0x05,
                    0x01, 0x00, (byte) b, type, slot
            };
            ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
            boolean ok = r.getSW() == 0x9000;
            System.out.println("[Writer] auth block=" + b + " type=" + String.format("0x%02X", type) + " slot=" + slot
                    + " -> " + (ok ? "OK" : "FAIL SW=" + Integer.toHexString(r.getSW())));
            return ok;
        } catch (Exception e) {
            System.out.println("[Writer] authWithKeySlot threw: " + e.getMessage());
            return false;
        }
    }

    private static void writeBlock(CardChannel c, int b, byte[] data) throws Exception {
        if (isTrailerBlock(b)) {
            System.out.println("[Writer] Refusing to write to trailer block " + b);
            throw new Exception("Refusing to write to trailer block " + b);
        }
        if (data.length != 16) {
            System.out.println("[Writer] Invalid data size: " + data.length);
            throw new Exception("Invalid data size: " + data.length + " (expected 16)");
        }

        System.out.println("[Writer] Writing to block " + b + ": " + bytesToHex(data));

        byte[] apdu = new byte[21];
        apdu[0] = (byte) 0xFF;
        apdu[1] = (byte) 0xD6;
        apdu[2] = 0x00;
        apdu[3] = (byte) b;
        apdu[4] = 0x10;
        System.arraycopy(data, 0, apdu, 5, 16);

        ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
        if (r.getSW() != 0x9000) {
            String error = "Write failed SW=" + Integer.toHexString(r.getSW());
            System.out.println("[Writer] " + error);
            throw new Exception(error);
        }

        // Verify write by reading back
        byte[] verify = readBlock(c, b);
        if (verify == null) {
            String error = "Write verification failed - couldn't read back block " + b;
            System.out.println("[Writer] " + error);
            throw new Exception(error);
        }

        if (!Arrays.equals(data, verify)) {
            String error = "Write verification failed - data mismatch in block " + b;
            System.out.println("[Writer] " + error);
            System.out.println("[Writer] Written: " + bytesToHex(data));
            System.out.println("[Writer] Read   : " + bytesToHex(verify));
            throw new Exception(error);
        }

        System.out.println("[Writer] Successfully wrote and verified block " + b);
    }

    private static byte[] readBlock(CardChannel c, int b) {
        try {
            byte[] cmd = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) b, 0x10 };
            ResponseAPDU r = c.transmit(new CommandAPDU(cmd));
            if (r.getSW() == 0x9000) {
                byte[] data = r.getData();
                System.out.println("[Writer] readBlock " + b + " -> " + bytesToHex(data));
                return data;
            } else {
                System.out.println("[Writer] readBlock " + b + " failed SW=" + Integer.toHexString(r.getSW()));
            }
        } catch (Exception e) {
            System.out.println("[Writer] readBlock exception for block " + b + " : " + e.getMessage());
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
        // If src is empty, still produce one empty chunk (16 zero bytes) to write
        // nothing? We skip empty text earlier.
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
