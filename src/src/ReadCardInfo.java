import AcsCommon.Apdu;

public class ReadCardInfo {
    private static Acr1552U reader;

    public static void main(String[] args) {
        try {
            System.out.println("Initializing NFC Reader...");

            // Initialize the reader
            reader = new Acr1552U();
            reader.setEventHandler(new Pcsc.ReaderEvent());
            reader.establishContext();

            // List available readers
            String[] readerList = reader.listTerminals();
            if (readerList.length == 0) {
                System.out.println("No NFC readers found.");
                return;
            }

            // Connect to the first reader
            System.out.println("\nConnecting to: " + readerList[0]);
            reader.connect(0, "*");

            // Configure reader for ISO14443A cards
            Acr1552U.PICC_POLLING_OPTION pollingOption = new Acr1552U.PICC_POLLING_OPTION();
            pollingOption.enablePolling = true;
            pollingOption.enablePart3 = true;
            pollingOption.enablePart4 = true;
            pollingOption.pollInterval = 0x01;
            reader.setPollingOption(pollingOption);

            // Set polling for ISO14443A cards
            reader.setPollingType(Acr1552U.CONTACTLESS_CARD_TYPE_ISO14443A_3);

            System.out.println("\nPlease place your card on the reader...");

            boolean cardRead = false;
            while (!cardRead) {
                // Check for card presence
                byte status = reader.getPCDPICCStatus();
                if ((status & 0x02) != 0x02) {
                    Thread.sleep(1000);
                    continue;
                }

                System.out.println("\nCard Detected!");

                // Read card UID and data
                AcsCommon.Apdu apdu = new Apdu();
                byte[] command = new byte[] { (byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00 };
                reader.setCommandData(apdu, command, 0x0A);
                reader.sendControlCommand(apdu);
                byte[] response = apdu.getReceiveData();

                if (response != null && response.length > 2) {
                    System.out.println("Card UID: " + bytesToHex(response, response.length - 2));

                    // Read all available data blocks
                    StringBuilder cardData = new StringBuilder();
                    for (int block = 0; block <= 15; block++) {
                        try {
                            command = new byte[] { (byte) 0xFF, (byte) 0xB0, 0x00, (byte) block, 0x10 };
                            reader.setCommandData(apdu, command, 0x12);
                            reader.sendControlCommand(apdu);
                            response = apdu.getReceiveData();

                            if (response != null && response.length > 2) {
                                String blockData = new String(response, 0, response.length - 2)
                                        .replaceAll("[^\\x20-\\x7E]", ""); // Remove non-printable chars
                                cardData.append(blockData);
                            }
                        } catch (Exception e) {
                            // Skip blocks that can't be read
                            continue;
                        }
                    }
                    System.out.println("Card Data: " + cardData.toString().trim());
                }

                cardRead = true;
            }
            System.out.println("\nCard reading complete.");

        } catch (Exception e) {
            if (!e.getMessage().contains("SCARD_E_NO_SMARTCARD")) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            try {
                if (reader != null && reader.isConnectionActive()) {
                    reader.disconnect();
                }
            } catch (Exception e) {
                System.out.println("Error while disconnecting: " + e.getMessage());
            }
        }
    }

    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}