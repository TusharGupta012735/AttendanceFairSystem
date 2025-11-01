package Pcsc;

import java.util.Arrays;

import javax.smartcardio.CardException;

import Acos.*;
import AcsCommon.*;

public class CardSelector {
	private PcscReader _pcscReader;

	public PcscReader getPcscReader() {
		return _pcscReader;
	}

	public void setPcscReader(PcscReader _pcscReader) {
		this._pcscReader = _pcscReader;
	}

	public CardSelector(PcscReader pcscReader) {
		this.setPcscReader(pcscReader);
	}

	public String getCardNameOnRid(byte[] atr) throws Exception {
		String cardName = "Unknown";
		byte[] rid = { (byte) 0xA0, 0x00, 0x00, 0x03, 0x06 };
		byte[] tempRid = new byte[5];
		byte[] cardIdentifier = new byte[2];
		tempRid = Arrays.copyOfRange(atr, 7, 12);

		if (atr[4] == (byte) 0x80 && atr[5] == 0x4F) {
			if (Arrays.equals(tempRid, rid)) {
				cardIdentifier = Arrays.copyOfRange(atr, 13, 15);
				switch (cardIdentifier[1]) {
				case 0:
					cardName = "Unknown";
					break;
				case 1:
					cardName = "Mifare Standard 1K";
					break;
				case 2:
					cardName = "Mifare Standard 4K";
					break;
				case 3:
					cardName = "Mifare Ultralight";
					break;
				case 4:
					cardName = "SLE55R_XXXX";
					break;
				case 6:
					cardName = "SR176";
					break;
				case 7:
					cardName = "SRI X4K";
					break;
				case 8:
					cardName = "AT88RF020";
					break;
				case 9:
					cardName = "AT88SC0204CRF";
					break;
				case 10:
					cardName = "AT88SC0808CRF";
					break;
				case 11:
					cardName = "AT88SC1616CRF";
					break;
				case 12:
					cardName = "AT88SC3216CRF";
					break;
				case 13:
					cardName = "AT88SC6416CRF";
					break;
				case 14:
					cardName = "SRF55V10P";
					break;
				case 15:
					cardName = "SRF55V02P";
					break;
				case 16:
					cardName = "SRF55V10S";
					break;
				case 17:
					cardName = "SRF55V02S";
					break;
				case 18:
					cardName = "TAG IT";
					break;
				case 19:
					cardName = "LR1512";
					break;
				case 20:
					cardName = "ICODESLI";
					break;
				case 21:
					cardName = "TEMPSENS";
					break;
				case 22:
					cardName = "I.CODE1";
					break;
				case 23:
					cardName = "PicoPass 2K";
					break;
				case 24:
					cardName = "PicoPass 2KS";
					break;
				case 25:
					cardName = "PicoPass 16K";
					break;
				case 26:
					cardName = "PicoPass 16Ks";
					break;
				case 27:
					cardName = "PicoPass 16K(8x2)";
					break;
				case 28:
					cardName = "PicoPass 16Ks(8x2)";
					break;
				case 29:
					cardName = "PicoPass 32KS(16+16)";
					break;
				case 30:
					cardName = "PicoPass 32KS(16+8x2)";
					break;
				case 31:
					cardName = "PicoPass 32KS(8x2+16)";
					break;
				case 32:
					cardName = "PicoPass 32KS(8x2+8x2)";
					break;
				case 33:
					cardName = "LRI64";
					break;
				case 34:
					cardName = "I.CODE UID";
					break;
				case 35:
					cardName = "I.CODE EPC";
					break;
				case 36:
					cardName = "LRI12";
					break;
				case 37:
					cardName = "LRI128";
					break;
				case 38:
					cardName = "Mifare Mini";
					break;
				case 39:
					cardName = "my-d move (SLE 66R01P)";
					break;
				case 40:
					cardName = "my-d NFC (SLE 66RxxP)";
					break;
				case 41:
					cardName = "my-d proximity 2 (SLE 66RxxS)";
					break;
				case 42:
					cardName = "my-d proximity enhanced (SLE 55RxxE)";
					break;
				case 43:
					cardName = "my-d light (SRF 55V01P))";
					break;
				case 44:
					cardName = "PJM Stack Tag (SRF 66V10ST)";
					break;
				case 45:
					cardName = "PJM Item Tag (SRF 66V10IT)";
					break;
				case 46:
					cardName = "PJM Light (SRF 66V01ST)";
					break;
				case 47:
					cardName = "Jewel Tag";
					break;
				case 48:
					cardName = "Topaz NFC Tag";
					break;
				case 49:
					cardName = "AT88SC0104CRF";
					break;
				case 50:
					cardName = "AT88SC0404CRF";
					break;
				case 51:
					cardName = "AT88RF01C";
					break;
				case 52:
					cardName = "AT88RF04C";
					break;
				case 53:
					cardName = "i-Code SL2";
					break;
				case 54:
					cardName = "Mifare Plus SL1_2K";
					break;
				case 55:
					cardName = "Mifare Plus SL1_4K";
					break;
				case 56:
					cardName = "Mifare Plus SL2_2K";
					break;
				case 57:
					cardName = "Mifare Plus SL2_4K";
					break;
				case 58:
					cardName = "Mifare Ultralight C";
					break;
				case 59:
					cardName = "FeliCa";
					break;
				case 60:
					cardName = "Melexis Sensor Tag (MLX90129)";
					break;
				case 61:
					cardName = "Mifare Ultralight EV1";
					break;
				default:
					cardName = "Unknown";
					break;
				}
			}
		} else {
			if (atr[4] == 0x10) {
				cardName = "Memory Card";
			} else if (atr[4] == 0x00) {
				cardName = getCardVersion();
			} else {
				cardName = getUid();
			}
		}

		return cardName;
	}

	private String getCardVersion() throws Exception {
		byte[] cardVersion;
		byte[] CV = new byte[5];
		byte[] version = new byte[] { (byte) 0x41, (byte) 0x43, (byte) 0x4F, (byte) 0x53, (byte) 0x03 };
		Acos3 acos3 = new Acos3(getPcscReader());

		try {
			cardVersion = acos3.getCardInfo(Acos3.CARD_INFO_TYPE.VERSION_NUMBER);
			CV = Arrays.copyOfRange(cardVersion, 0, 5);

		} catch (PcscException cException) {
			throw cException;
		} catch (CardException cException) {
			if (cException.getMessage().contains("Unknown Status Word")
					|| cException.getMessage().contains("Error Status Word"))
				return "Unknown";
			else
				throw cException;
		}

		if (Arrays.equals(CV, version))
			return "ACOS3";
		else
			return "Unknown";
	}

	public String getUid() throws Exception {
		byte[] command = { (byte) 0xFF, (byte) 0xCA, 0x01, 0x00, 0x00 };
		byte[] reponse = new byte[100];
		Apdu cApdu = new Apdu();

		try {
			cApdu.setCommand(command);
			cApdu.setReceiveData(reponse);

			int result = getPcscReader().sendApduCommand(cApdu);

			if (result == PcscProvider.SCARD_S_SUCCESS) {
				if (cApdu.getStatusWord()[0] == (byte) 0x6A) // Type B
				{
					return "ISO 14443 Part 4 Type B";
				} else if (cApdu.getStatusWord()[0] == (byte) 0x90 && cApdu.getStatusWord()[1] == (byte) 0x00) // ISO
																												// 14443
																												// Part
																												// 4
																												// Type
																												// A;
																												// includes
																												// ACOS
																												// cards
				{
					return getCardVersion();
				} else {
					return "Unkown";
				}
			}
		} catch (Exception ex) {
			return "Unkown";
		}
		return "Unkown";
	}

}
