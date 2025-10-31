package nfc;

import javax.smartcardio.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class SmartMifareWriter {

    private static final byte[][] COMMON_KEYS = new byte[][] {
            hex("FFFFFFFFFFFF")
    };
    private static final int KEY_SLOT = 0x00;

    public static void writeNextAvailableBlock(String text) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        if (terminals.isEmpty())
            throw new Exception("No NFC reader detected");

        CardTerminal terminal = terminals.get(0);
        terminal.waitForCardPresent(0);
        Card card = terminal.connect("*");
        CardChannel channel = card.getBasicChannel();

        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        List<byte[]> chunks = chunkBytes(payload, 16);

        for (byte[] chunk : chunks) {
            boolean written = false;
            for (int block = 4; block < 64 && !written; block++) {
                if (isTrailerBlock(block))
                    continue;
                for (byte[] key : COMMON_KEYS) {
                    if (!loadKey(channel, KEY_SLOT, key))
                        continue;
                    AuthResult ar = tryAuthAsAorB(channel, block, (byte) KEY_SLOT);
                    if (!ar.success)
                        continue;
                    byte[] data = readBlock(channel, block);
                    boolean empty = data == null || isDataBlockEmpty(data);
                    if (!empty)
                        continue;
                    if (!authWithKeySlot(channel, block, ar.keyType, (byte) KEY_SLOT))
                        continue;
                    writeBlock(channel, block, chunk);
                    written = true;
                    break;
                }
            }
            if (!written)
                throw new Exception("No empty writable block found");
        }

        card.disconnect(false);
        terminal.waitForCardAbsent(0);
    }

    private static class AuthResult {
        boolean success;
        byte keyType;

        AuthResult(boolean s, byte t) {
            success = s;
            keyType = t;
        }
    }

    private static AuthResult tryAuthAsAorB(CardChannel c, int b, byte slot) {
        boolean a = authWithKeySlot(c, b, (byte) 0x60, slot);
        if (a)
            return new AuthResult(true, (byte) 0x60);
        boolean bb = authWithKeySlot(c, b, (byte) 0x61, slot);
        if (bb)
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
            byte[] apdu = new byte[] {
                    (byte) 0xFF, (byte) 0x86, 0x00, 0x00, 0x05,
                    0x01, 0x00, (byte) b, type, slot
            };
            ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
            return r.getSW() == 0x9000;
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeBlock(CardChannel c, int b, byte[] data) throws Exception {
        if (isTrailerBlock(b))
            throw new Exception("Refusing to write to trailer block");
        if (data.length != 16)
            throw new Exception("Invalid data size");
        byte[] apdu = new byte[21];
        apdu[0] = (byte) 0xFF;
        apdu[1] = (byte) 0xD6;
        apdu[2] = 0x00;
        apdu[3] = (byte) b;
        apdu[4] = 0x10;
        System.arraycopy(data, 0, apdu, 5, 16);
        ResponseAPDU r = c.transmit(new CommandAPDU(apdu));
        if (r.getSW() != 0x9000)
            throw new Exception("Write failed SW=" + Integer.toHexString(r.getSW()));
    }

    private static byte[] readBlock(CardChannel c, int b) {
        try {
            byte[] cmd = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) b, 0x10 };
            ResponseAPDU r = c.transmit(new CommandAPDU(cmd));
            if (r.getSW() == 0x9000)
                return r.getData();
        } catch (Exception ignored) {
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
            System.arraycopy(src, i, chunk, 0, len);
            out.add(chunk);
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
