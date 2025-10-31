package nfc;

import javax.smartcardio.*;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class ReadMifare {

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
            System.out.println("Present a card...");
            terminal.waitForCardPresent(0);
            Card card = terminal.connect("*");
            ATR atr = card.getATR();
            System.out.println("Card connected. ATR: " + bytesToHex(atr.getBytes()));
            CardChannel channel = card.getBasicChannel();

            // UID
            CommandAPDU getUidCmd = new CommandAPDU(new byte[] {
                    (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00
            });
            ResponseAPDU uidResp = channel.transmit(getUidCmd);
            System.out.println("UID: " + bytesToHex(uidResp.getData()));

            System.out.println("\nAttempting authentication (only on cards you own)...");

            // Common keys to try (only on your own card)
            byte[][] commonKeys = new byte[][] {
                    hex("FFFFFFFFFFFF"),
                    hex("A0A1A2A3A4A5"),
                    hex("D3F7D3F7D3F7"),
                    hex("000000000000"),
                    hex("AABBCCDDEEFF"),
                    hex("4D3A99C351DD")
            };

            int keySlot = 0x00; // using slot 0 for loadKey
            boolean anyAuth = false;

            // Iterate all sectors and attempt to authenticate each sector with known keys.
            for (int sector = 0; sector < 16; sector++) {
                int firstBlockOfSector = sector * 4;
                int probeBlock = (sector == 0) ? 1 : firstBlockOfSector; // avoid manufacturer block 0 for probe

                AuthResult successfulAuth = null;
                byte[] successfulKey = null;

                for (int i = 0; i < commonKeys.length; i++) {
                    boolean loaded = loadKey(channel, keySlot, commonKeys[i]);
                    if (!loaded) {
                        // don't spam - skip
                        continue;
                    }
                    AuthResult ar = tryAuthAsAorB(channel, probeBlock, (byte) keySlot);
                    System.out.printf("Tried key %s on sector %d (probe block %d) -> %s\n",
                            bytesToHex(commonKeys[i]), sector, probeBlock,
                            (ar.success ? (ar.keyType == 0x60 ? "KeyA" : "KeyB") + " SUCCESS" : "FAIL"));
                    if (ar.success) {
                        successfulAuth = ar;
                        successfulKey = commonKeys[i];
                        anyAuth = true;
                        break;
                    }
                }

                if (successfulAuth == null) {
                    System.out.printf("Sector %2d: no known key worked (skipping)\n", sector);
                    continue;
                }

                // Dump user blocks for this sector (skip manufacturer block 0 and skip
                // trailers)
                System.out.printf("=== Dumping sector %d using %s (%s) ===\n", sector,
                        (successfulAuth.keyType == 0x60 ? "Key A" : "Key B"),
                        bytesToHex(successfulKey));

                for (int b = firstBlockOfSector; b < firstBlockOfSector + 4; b++) {
                    if (sector == 0 && b == 0)
                        continue; // skip manufacturer block
                    if ((b % 4) == 3)
                        continue; // skip sector trailer
                    byte[] data = readBlock(channel, b);
                    if (data == null) {
                        System.out.printf("Block %02d: read error or not permitted\n", b);
                    } else {
                        String hex = bytesToHex(data);
                        String ascii = bytesToPrintableAscii(data);
                        String utf8 = tryUtf8Trim(data);
                        System.out.printf("Block %02d: %s  |  text: \"%s\"  |  utf8: \"%s\"\n",
                                b, hex, ascii, utf8);
                    }
                }
                System.out.println("=== End sector dump ===\n");
            }

            if (!anyAuth) {
                System.out.println("No known default key worked for any sector. You may need the real keys.");
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

    // Try Key A (0x60) then Key B (0x61) using the key already loaded into keySlot.
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
            byte p1 = 0x00;
            byte p2 = (byte) keySlot;
            byte lc = 0x06;
            byte[] apdu = new byte[5 + 6];
            apdu[0] = (byte) 0xFF;
            apdu[1] = (byte) 0x82;
            apdu[2] = p1;
            apdu[3] = p2;
            apdu[4] = lc;
            System.arraycopy(key, 0, apdu, 5, 6);

            CommandAPDU loadKeyCmd = new CommandAPDU(apdu);
            ResponseAPDU resp = channel.transmit(loadKeyCmd);
            // minimal log: only print on error (uncomment for debug)
            // System.out.printf("LoadKey SW: %02X %02X\n", resp.getSW1(), resp.getSW2());
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            System.out.println("loadKey exception: " + e.getMessage());
            return false;
        }
    }

    // Authenticate using key in a slot (FF 86 referencing key slot)
    // keyType: 0x60 = Key A, 0x61 = Key B
    private static boolean authWithKeySlot(CardChannel channel, int blockNumber, byte keyType, byte keySlot) {
        try {
            byte[] apdu = new byte[] {
                    (byte) 0xFF, (byte) 0x86, 0x00, 0x00, 0x05,
                    0x01, 0x00, (byte) blockNumber, keyType, keySlot
            };
            CommandAPDU authCmd = new CommandAPDU(apdu);
            ResponseAPDU authResp = channel.transmit(authCmd);
            System.out.printf("Auth SW for block %d: %02X %02X\n", blockNumber, authResp.getSW1(), authResp.getSW2());
            return authResp.getSW() == 0x9000;
        } catch (Exception e) {
            System.out.println("authWithKeySlot exception: " + e.getMessage());
            return false;
        }
    }

    // Read a 16-byte block using FF B0 (standard read command for many PC/SC
    // readers)
    private static byte[] readBlock(CardChannel channel, int blockNumber) {
        try {
            byte[] apdu = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) blockNumber, 0x10 };
            CommandAPDU readCmd = new CommandAPDU(apdu);
            ResponseAPDU resp = channel.transmit(readCmd);
            if (resp.getSW() == 0x9000) {
                return resp.getData();
            } else {
                // minimal log for read failure
                // System.out.printf("Read block %d failed SW=%02X %02X\n", blockNumber,
                // resp.getSW1(), resp.getSW2());
                return null;
            }
        } catch (Exception e) {
            System.out.println("readBlock exception: " + e.getMessage());
            return null;
        }
    }

    // Utility hex -> bytes
    private static byte[] hex(String s) {
        s = s.replaceAll("[^0-9A-Fa-f]", "");
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    // Convert bytes to printable ASCII (0x20 - 0x7E), replace others with '.'
    private static String bytesToPrintableAscii(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int ub = b & 0xFF;
            if (ub >= 0x20 && ub <= 0x7E) {
                sb.append((char) ub);
            } else {
                sb.append('.');
            }
        }
        // Trim trailing '.' that correspond to 0x00 padding for nicer output
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '.')
            end--;
        return sb.substring(0, end);
    }

    // Attempt to interpret as UTF-8 string (trim trailing NULs). If decoded string
    // is
    // mostly replacement characters or empty, returns empty string.
    private static String tryUtf8Trim(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return "";
        // strip trailing zeros
        int len = bytes.length;
        while (len > 0 && bytes[len - 1] == 0x00)
            len--;
        if (len == 0)
            return "";
        try {
            String s = new String(bytes, 0, len, StandardCharsets.UTF_8);
            // if the string is mostly unprintable, return empty (we want readable)
            int printable = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c >= 0x20 && c <= 0x7E)
                    printable++;
            }
            if (printable == 0)
                return "";
            return s;
        } catch (Exception e) {
            return "";
        }
    }
}
