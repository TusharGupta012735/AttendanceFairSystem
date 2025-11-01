package nfc;

import javax.smartcardio.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * SmartMifareReader - compatible with JavaFX frontend.
 * Works with MIFARE 1K/4K cards using ACR1552U or similar readers.
 * Returns structured data in a Map for UI display (instead of console prints).
 */
public class SmartMifareReader {

    /**
     * Reads full card data (UID + readable text from sectors).
     * Returns a Map containing: { "uid": "...", "data": "...", "error": "..." }
     */
    public static Map<String, String> readCardData() {
        Map<String, String> result = new HashMap<>();
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals;

            try {
                terminals = factory.terminals().list();
            } catch (CardException e) {
                result.put("status", "error");
                result.put("error", "No NFC reader detected or PC/SC service not running.");
                return result;
            }

            if (terminals.isEmpty()) {
                result.put("error", "No NFC reader detected.");
                return result;
            }

            CardTerminal terminal = terminals.get(0);
            result.put("status", "waiting");
            result.put("message", "Place your card on the reader...");

            terminal.waitForCardPresent(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            // === READ UID ===
            CommandAPDU getUidCmd = new CommandAPDU(new byte[] {
                    (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00
            });
            ResponseAPDU uidResp = channel.transmit(getUidCmd);
            String uid = bytesToHex(uidResp.getData()).replace(" ", "");
            result.put("uid", uid);

            // === Try Reading Data Using Common Keys ===
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
            StringBuilder readableData = new StringBuilder();

            for (int sector = 0; sector < 16; sector++) {
                int firstBlockOfSector = sector * 4;
                int probeBlock = (sector == 0) ? 1 : firstBlockOfSector;
                AuthResult successfulAuth = null;

                for (byte[] key : commonKeys) {
                    if (!loadKey(channel, keySlot, key))
                        continue;
                    AuthResult ar = tryAuthAsAorB(channel, probeBlock, (byte) keySlot);
                    if (ar.success) {
                        successfulAuth = ar;
                        anyAuth = true;
                        break;
                    }
                }

                if (successfulAuth == null)
                    continue;

                for (int b = firstBlockOfSector; b < firstBlockOfSector + 4; b++) {
                    if (sector == 0 && b == 0)
                        continue;
                    if ((b % 4) == 3)
                        continue; // skip trailer block

                    byte[] data = readBlock(channel, b);
                    if (data != null) {
                        String text = tryUtf8Trim(data);
                        if (!text.isEmpty())
                            readableData.append(text).append(" ");
                    }
                }
            }

            card.disconnect(false);
            terminal.waitForCardAbsent(0);

            if (!anyAuth) {
                result.put("error", "No known default key worked for any sector.");
            } else {
                result.put("data", readableData.toString().trim());
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Reads only UID (fast mode).
     */
    public static String readUID() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty())
                return null;

            CardTerminal terminal = terminals.get(0);
            terminal.waitForCardPresent(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            CommandAPDU getUidCmd = new CommandAPDU(new byte[] {
                    (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00
            });
            ResponseAPDU uidResp = channel.transmit(getUidCmd);
            String uid = bytesToHex(uidResp.getData()).replace(" ", "");

            card.disconnect(false);
            terminal.waitForCardAbsent(0);

            return uid;
        } catch (Exception e) {
            return null;
        }
    }

    // === Internal helpers ===

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
            byte[] apdu = new byte[11];
            apdu[0] = (byte) 0xFF;
            apdu[1] = (byte) 0x82;
            apdu[2] = 0x00;
            apdu[3] = (byte) keySlot;
            apdu[4] = 0x06;
            System.arraycopy(key, 0, apdu, 5, 6);
            ResponseAPDU resp = channel.transmit(new CommandAPDU(apdu));
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
            byte[] apdu = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) blockNumber, 0x10 };
            ResponseAPDU resp = channel.transmit(new CommandAPDU(apdu));
            if (resp.getSW() == 0x9000)
                return resp.getData();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] hex(String s) {
        s = s.replaceAll("[^0-9A-Fa-f]", "");
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
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
