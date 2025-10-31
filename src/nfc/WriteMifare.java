package nfc;

import javax.smartcardio.*;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class WriteMifare {

    // Candidate keys to try (only use on cards you own)
    private static final byte[][] COMMON_KEYS = new byte[][] {
            hex("FFFFFFFFFFFF"),
            hex("A0A1A2A3A4A5"),
            hex("D3F7D3F7D3F7"),
            hex("000000000000"),
            hex("AABBCCDDEEFF"),
            hex("4D3A99C351DD")
    };

    public static void main(String[] args) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty()) {
                System.out.println("No NFC reader detected.");
                return;
            }

            CardTerminal terminal = terminals.get(0);
            System.out.println("Using reader: " + terminal.getName());
            System.out.println("Place the card...");

            terminal.waitForCardPresent(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            // UID
            CommandAPDU getUid = new CommandAPDU(new byte[] { (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00 });
            ResponseAPDU uidResp = channel.transmit(getUid);
            System.out.println("Card UID: " + bytesToHex(uidResp.getData()));

            // Text to write (you can change this)
            String text = "Software Engineer"; // default
            Integer requestedBlock = null;
            if (args != null && args.length > 0) {
                text = args[0];
            }
            if (args != null && args.length > 1) {
                try {
                    requestedBlock = Integer.parseInt(args[1]);
                } catch (Exception ignored) {
                }
            }

            // Validate requested block if provided
            if (requestedBlock != null) {
                if (requestedBlock < 0 || requestedBlock > 63) {
                    System.out.println("Invalid block number requested. Must be 0-63.");
                    return;
                }
                if (isTrailerBlock(requestedBlock)) {
                    System.out.println("Refusing to write to sector trailer block " + requestedBlock);
                    return;
                }
                if (requestedBlock == 0) {
                    System.out.println("Refusing to write to manufacturer block 0.");
                    return;
                }
            }

            // Find a writable block (skip sector 0 manufacturer block and trailers), or use
            // requestedBlock
            int targetBlock = -1;
            byte[] foundKey = null;
            byte foundKeyType = 0x00; // 0x60 = Key A, 0x61 = Key B
            int foundKeySlot = 0x00;

            if (requestedBlock != null) {
                // Try the user-specified block first (authenticate with known keys)
                int blk = requestedBlock;
                for (int i = 0; i < COMMON_KEYS.length && foundKey == null; i++) {
                    byte[] candidate = COMMON_KEYS[i];
                    if (!loadKey(channel, 0x00, candidate))
                        continue;
                    AuthResult ar = tryAuthAsAorB(channel, blk, (byte) 0x00);
                    if (ar.success) {
                        byte[] blockData = readBlock(channel, blk);
                        // allow write even if not empty (user asked for specific block) but prefer
                        // empty
                        foundKey = candidate;
                        foundKeyType = ar.keyType;
                        foundKeySlot = 0x00;
                        targetBlock = blk;
                        break;
                    }
                }
                if (foundKey == null) {
                    // best-effort try default key to give write a chance (warn user)
                    loadKey(channel, 0x00, hex("FFFFFFFFFFFF"));
                    if (authWithKeySlot(channel, blk, (byte) 0x60, (byte) 0x00)) {
                        foundKey = hex("FFFFFFFFFFFF");
                        foundKeyType = 0x60;
                        foundKeySlot = 0x00;
                        targetBlock = blk;
                        System.out
                                .println("Warning: authenticated the requested block with default key (best-effort).");
                    } else {
                        System.out.println(
                                "Warning: could not authenticate the requested block with known keys. Proceeding may fail.");
                        targetBlock = blk; // still attempt write (user asked for it)
                    }
                }
            } else {
                // Scan for an empty writable block
                outer: for (int blk = 4; blk < 64; blk++) { // start at 4; skip manufacturer block0
                    if (isTrailerBlock(blk))
                        continue; // never touch trailer
                    for (int i = 0; i < COMMON_KEYS.length; i++) {
                        byte[] candidate = COMMON_KEYS[i];
                        if (!loadKey(channel, 0x00, candidate))
                            continue;
                        AuthResult ar = tryAuthAsAorB(channel, blk, (byte) 0x00);
                        if (ar.success) {
                            byte[] blockData = readBlock(channel, blk);
                            if (blockData == null)
                                continue; // can't read even after auth
                            boolean isEmpty = true;
                            for (byte b : blockData) {
                                if (b != 0x00 && b != (byte) 0xFF) {
                                    isEmpty = false;
                                    break;
                                }
                            }
                            if (isEmpty) {
                                targetBlock = blk;
                                foundKey = candidate;
                                foundKeyType = ar.keyType;
                                foundKeySlot = 0x00;
                                break outer;
                            }
                        }
                    }
                }
                if (targetBlock == -1) {
                    System.out.println("No empty writable block found; will attempt block 4 (may overwrite).");
                    targetBlock = 4;
                } else {
                    System.out.println(
                            "Found writable empty block: " + targetBlock + " (auth with " + bytesToHex(foundKey) + ")");
                }
            }

            // SAFETY: prevent writing trailers / block0 (double-check)
            if (isTrailerBlock(targetBlock)) {
                System.out.println("Refusing to write to sector trailer block " + targetBlock);
                return;
            }
            if (targetBlock == 0) {
                System.out.println("Refusing to write to manufacturer block 0.");
                return;
            }

            // If we have a working key, reload and re-authenticate before write
            if (foundKey != null) {
                if (!loadKey(channel, foundKeySlot, foundKey))
                    throw new Exception("Failed to reload key");
                if (!authWithKeySlot(channel, targetBlock, foundKeyType, (byte) foundKeySlot))
                    throw new Exception("Auth failed before write");
            } else {
                // best-effort: load default key so write has a chance (warn)
                boolean ok = loadKey(channel, 0x00, hex("FFFFFFFFFFFF"));
                if (!ok)
                    System.out.println("Warning: couldn't load default key; write may fail.");
            }

            // Prepare data to write
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            if (textBytes.length > 16) {
                System.out.println("Warning: text length (" + textBytes.length
                        + " bytes) exceeds one block (16 bytes). It will be truncated.");
            }
            byte[] writeData = new byte[16];
            System.arraycopy(textBytes, 0, writeData, 0, Math.min(textBytes.length, 16));
            // remainder already 0x00

            // Write
            writeBlock(channel, targetBlock, writeData);
            System.out.println("Wrote to block " + targetBlock + ": \"" + readableFromBytes(writeData) + "\"");

            // Verify
            byte[] verify = readBlock(channel, targetBlock);
            if (verify != null) {
                String v = readableFromBytes(verify);
                System.out.println("Verify read: \"" + v + "\"");
            } else {
                System.out.println("Could not verify readback (read failed).");
            }

            card.disconnect(false);
            terminal.waitForCardAbsent(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // small container to return auth result
    private static class AuthResult {
        boolean success;
        byte keyType;

        AuthResult(boolean s, byte t) {
            success = s;
            keyType = t;
        }
    }

    private static AuthResult tryAuthAsAorB(CardChannel channel, int blockNumber, byte keySlot) {
        boolean aOk = authWithKeySlot(channel, blockNumber, (byte) 0x60, keySlot);
        if (aOk)
            return new AuthResult(true, (byte) 0x60);
        boolean bOk = authWithKeySlot(channel, blockNumber, (byte) 0x61, keySlot);
        if (bOk)
            return new AuthResult(true, (byte) 0x61);
        return new AuthResult(false, (byte) 0x00);
    }

    // Load key into reader key slot (FF 82)
    private static boolean loadKey(CardChannel channel, int keySlot, byte[] key) {
        try {
            byte[] apdu = new byte[11];
            apdu[0] = (byte) 0xFF;
            apdu[1] = (byte) 0x82;
            apdu[2] = 0x00;
            apdu[3] = (byte) keySlot;
            apdu[4] = 0x06;
            System.arraycopy(key, 0, apdu, 5, 6);
            ResponseAPDU resp = channel.transmit(new CommandAPDU(apdu));
            // minimal logging only
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean authWithKeySlot(CardChannel channel, int blockNumber, byte keyType, byte keySlot) {
        try {
            byte[] apdu = new byte[] {
                    (byte) 0xFF, (byte) 0x86, 0x00, 0x00, 0x05,
                    0x01, 0x00, (byte) blockNumber, keyType, keySlot
            };
            ResponseAPDU resp = channel.transmit(new CommandAPDU(apdu));
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] readBlock(CardChannel channel, int blockNumber) {
        try {
            byte[] cmd = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) blockNumber, 0x10 };
            ResponseAPDU resp = channel.transmit(new CommandAPDU(cmd));
            if (resp.getSW() == 0x9000)
                return resp.getData();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void writeBlock(CardChannel channel, int blockNumber, byte[] data) throws Exception {
        if (isTrailerBlock(blockNumber))
            throw new Exception("Refusing to write trailer block");
        byte[] apdu = new byte[5 + 16];
        apdu[0] = (byte) 0xFF;
        apdu[1] = (byte) 0xD6;
        apdu[2] = 0x00;
        apdu[3] = (byte) blockNumber;
        apdu[4] = 0x10;
        System.arraycopy(data, 0, apdu, 5, 16);
        ResponseAPDU resp = channel.transmit(new CommandAPDU(apdu));
        if (resp.getSW() != 0x9000)
            throw new Exception("Write failed SW=" + Integer.toHexString(resp.getSW()));
    }

    private static boolean isTrailerBlock(int block) {
        return (block % 4) == 3;
    }

    // Utility hex -> bytes
    private static byte[] hex(String s) {
        s = s.replaceAll("[^0-9A-Fa-f]", "");
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++)
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X:", b));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    private static String readableFromBytes(byte[] data) {
        if (data == null)
            return "";
        int len = data.length;
        // strip trailing 0x00
        while (len > 0 && data[len - 1] == 0x00)
            len--;
        if (len == 0)
            return "";
        String s = new String(data, 0, len, StandardCharsets.UTF_8);
        // turn non-printable to dot (rare now because we trimmed)
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 0x20 && c <= 0x7E)
                sb.append(c);
            else
                sb.append('.');
        }
        return sb.toString();
    }
}
