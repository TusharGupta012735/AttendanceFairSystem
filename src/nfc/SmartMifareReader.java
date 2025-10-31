package nfc;

import javax.smartcardio.*;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class SmartMifareReader {

    /**
     * Reads the UID of the card and optionally dumps data.
     * Called by UI to get card ID.
     */
    public static String readUID() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty()) {
                System.out.println("No NFC reader detected.");
                return null;
            }
            CardTerminal terminal = terminals.get(0);
            System.out.println("Using reader: " + terminal.getName());
            System.out.println("Place the card...");
            terminal.waitForCardPresent(0);
            Card card = terminal.connect("*");
            ATR atr = card.getATR();
            System.out.println("Card connected. ATR: " + bytesToHex(atr.getBytes()));
            CardChannel channel = card.getBasicChannel();

            // === READ UID ===
            CommandAPDU getUidCmd = new CommandAPDU(new byte[] {
                    (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00
            });
            ResponseAPDU uidResp = channel.transmit(getUidCmd);
            String uid = bytesToHex(uidResp.getData());
            System.out.println("Card UID: " + uid);

            // === Try Authentication (Optional, only for your cards) ===
            System.out.println("\nAttempting authentication on your card...");

            byte[][] commonKeys = new byte[][] {
                    hex("FFFFFFFFFFFF"),
                    hex("A0A1A2A3A4A5"),
                    hex("D3F7D3F7D3F7"),
                    hex("000000000000"),
                    hex("AABBCCDDEEFF"),
                    hex("4D3A99C351DD")
            };

            int keySlot = 0x00;
            boolean anyAuth = false;

            for (int sector = 0; sector < 16; sector++) {
                int firstBlockOfSector = sector * 4;
                int probeBlock = (sector == 0) ? 1 : firstBlockOfSector;

                AuthResult successfulAuth = null;
                byte[] successfulKey = null;

                for (byte[] key : commonKeys) {
                    boolean loaded = loadKey(channel, keySlot, key);
                    if (!loaded)
                        continue;

                    AuthResult ar = tryAuthAsAorB(channel, probeBlock, (byte) keySlot);
                    if (ar.success) {
                        successfulAuth = ar;
                        successfulKey = key;
                        anyAuth = true;
                        break;
                    }
                }

                if (successfulAuth == null)
                    continue;

                // Dump accessible blocks
                System.out.printf("=== Sector %d unlocked ===\n", sector);
                for (int b = firstBlockOfSector; b < firstBlockOfSector + 4; b++) {
                    if (sector == 0 && b == 0)
                        continue;
                    if ((b % 4) == 3)
                        continue;
                    byte[] data = readBlock(channel, b);
                    if (data != null) {
                        System.out.printf("Block %02d: %s | Text: \"%s\"\n",
                                b, bytesToHex(data), tryUtf8Trim(data));
                    }
                }
            }

            if (!anyAuth) {
                System.out.println("No known default key worked for any sector.");
            }

            card.disconnect(false);
            terminal.waitForCardAbsent(0);

            return uid; // ✅ Return UID for app use

        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    private static AuthResult tryAuthAsAorB(CardChannel channel, int blockNumber, byte keySlot) {
        boolean aOk = authWithKeySlot(channel, blockNumber, (byte) 0x60, keySlot);
        if (aOk)
            return new AuthResult(true, (byte) 0x60);
        boolean bOk = authWithKeySlot(channel, blockNumber, (byte) 0x61, keySlot);
        if (bOk)
            return new AuthResult(true, (byte) 0x61);
        return new AuthResult(false, (byte) 0x00);
    }

    private static boolean loadKey(CardChannel channel, int keySlot, byte[] key) {
        try {
            byte[] apdu = new byte[5 + 6];
            apdu[0] = (byte) 0xFF;
            apdu[1] = (byte) 0x82;
            apdu[2] = 0x00;
            apdu[3] = (byte) keySlot;
            apdu[4] = 0x06;
            System.arraycopy(key, 0, apdu, 5, 6);

            CommandAPDU loadKeyCmd = new CommandAPDU(apdu);
            ResponseAPDU resp = channel.transmit(loadKeyCmd);
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
            byte[] apdu = new byte[] {
                    (byte) 0xFF, (byte) 0xB0, 0x00, (byte) blockNumber, 0x10
            };
            ResponseAPDU resp = channel.transmit(new CommandAPDU(apdu));
            if (resp.getSW() == 0x9000) {
                return resp.getData();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

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

    private static String tryUtf8Trim(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return "";
        int len = bytes.length;
        while (len > 0 && bytes[len - 1] == 0x00)
            len--;
        if (len == 0)
            return "";
        try {
            String s = new String(bytes, 0, len, StandardCharsets.UTF_8);
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