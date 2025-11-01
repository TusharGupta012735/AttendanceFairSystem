package Acos;

import AcsCommon.*;
import Pcsc.*;

public class Acos6 {

	public enum RECORD_REFERENCE {
		FIRST(0x00), LAST(0x01), NEXT(0x02), PREVIOUS(0x03), RECORD_INDEX(0x04);

		private final int _id;

		RECORD_REFERENCE(int id) {
			this._id = id;
		}
	}

	public enum GENERATE_KEY_SIZE {
		EIGHT_BYTE(0x00), SIXTEEN_BYTE(0x01), TWENTY_FOUR_BYTE(0x02);

		private final int _id;

		GENERATE_KEY_SIZE(int id) {
			this._id = id;
		}
	}

	public enum DIVERSIFY_KEY_OPERATION {
		SECRET_CODE(0x01), ACCOUNT_KEY(0x02), TERMINAL_KEY(0x03), CARD_KEY(0x04), BULK_ENCRYPTION(0x05),
		INITIAL_VECTOR(0x06);

		private final int _id;

		DIVERSIFY_KEY_OPERATION(int id) {
			this._id = id;
		}
	}

	public enum PIN_KEY_SCOPE {
		GLOBAL(0x00), LOCAL(0x80);

		PIN_KEY_SCOPE(int id) {
		}
	}

	public enum PREPARE_AUTHENTICATION_ENCRYPTION {
		TRIPLE_DES(0x00), SINGLE_DES(0x01), THREE_KEY_DES_DESFIRE(0x02), AES(0x03), TRIPLE_DES_DESFIRE(0x80),
		SINGLE_DES_DESFIRE(0x81);

		private final int _id;

		PREPARE_AUTHENTICATION_ENCRYPTION(int id) {
			this._id = id;
		}
	}

	public enum PREPARE_AUTHENTICATION_CLIENT_CARD {
		VERIFY_ACOS3_6_7_10_AUTH_RETURN(0x00), MIFARE_ULTRALIGHTC_TERMINAL_KEY(0x01),
		MIFARE_ULTRALIGHT_DESFIRE_AUTH_RETURN(0x02), MIFARE_ULTRALIGHTC_BULK_ENCRYPTION(0x05);

		private final int _id;

		PREPARE_AUTHENTICATION_CLIENT_CARD(int id) {
			this._id = id;
		}
	}

	public enum VERIFY_ENCRYPTION {
		TRIPLE_DES(0x00), SINGLE_DES(0x01), THREE_KEY_DES_DESFIRE(0x02), AES(0x03);

		private final int _id;

		VERIFY_ENCRYPTION(int id) {
			this._id = id;
		}
	}

	public enum VERIFY_CLIENT_CARD {
		ACOS(0x00), MIFARE(0x01);

		private final int _id;

		VERIFY_CLIENT_CARD(int id) {
			this._id = id;
		}
	}

	private PcscReader pcscReader_;

	public PcscReader getPcscConnection() {
		return this.pcscReader_;
	}

	public void setPcscConnection(PcscReader pcscConnection) {
		this.pcscReader_ = pcscConnection;
	}

	public Acos6() {
		pcscReader_ = null;
	}

	public Acos6(PcscReader pcscReader) {
		setPcscConnection(pcscReader);
	}

	public void clearCard() throws Exception {
		Apdu apdu;

		apdu = new Apdu();

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90)
			throw new Exception("Clear Card Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
	}

	public void createFile(byte[] fileData) throws Exception {
		Apdu apdu = new Apdu();

		apdu.setCommand(new byte[] { (byte) 0x00, (byte) 0xE0, (byte) 0x00, (byte) 0x00, (byte) fileData.length });
		apdu.setSendData(fileData);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception("Create File Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}

	}

	public void selectFile(byte[] fileID) throws Exception {
		Apdu apdu = new Apdu();
		if (fileID == null || fileID.length != 2)
			throw new Exception("File ID length should be 2 bytes");

		apdu.setCommand(new byte[] { (byte) 0x00, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x02 });
		apdu.setSendData(fileID);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			if (apdu.getStatusWord()[0] != (byte) 0x61)
				throw new Exception("Select File Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}
	}

	public void updateRecord(byte recordNumber, byte[] shortFileIdentifier, RECORD_REFERENCE refMethod, byte[] fileData)
			throws Exception {
		Apdu apdu = new Apdu();

		byte[] aCommand = new byte[] { (byte) 0x00, (byte) 0xDC, recordNumber, (byte) refMethod._id,
				(byte) fileData.length };

		if (fileData.length == 0)
			throw new Exception("Data to write is not specified");

		if (shortFileIdentifier.length != 0) {
			if (shortFileIdentifier[0] > 31)
				throw new Exception("Invalid short file identifier. Valid value is from 0 to 31");
			aCommand[3] |= (byte) (shortFileIdentifier[0] << 3);
		}

		apdu.setCommand(aCommand);
		apdu.setSendData(fileData);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception("Update Record Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}

	}

	public void appendRecord(byte[] fileData) throws Exception {
		Apdu apdu = new Apdu();

		if (fileData.length == 0)
			throw new Exception("Data to write is not specified");

		apdu.setCommand(new byte[] { (byte) 0x00, (byte) 0xE2, (byte) 0x00, (byte) 0x00, (byte) fileData.length });
		apdu.setSendData(fileData);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception("Append Record Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}

	}

	public void verify(byte pinID, byte[] pin) throws Exception {
		Apdu apdu = new Apdu();

		if (pin.length < 1)
			throw new Exception("PIN length is invalid");

		if (pinID > 0x0E)
			throw new Exception("Invalid Pin ID. Valid value: 00-0E");

		apdu.setCommand(new byte[] { (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) pinID, (byte) pin.length });
		apdu.setSendData(pin);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception("Verify Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}
	}

	public byte[] generateKey(GENERATE_KEY_SIZE keySize, byte keyIndex, byte[] inputData) throws Exception {
		byte[] data;
		Apdu apdu = new Apdu();

		if (inputData == null || inputData.length != 8)
			throw new Exception("Invalid data length. Valid length is 8 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x88, (byte) keySize._id, (byte) keyIndex, (byte) 0x08 });
		apdu.setSendData(inputData);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		if (apdu.getStatusWord()[0] != (byte) 0x61)
			throw new Exception("Generate Key Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");

		data = getResponse(apdu.getStatusWord()[1]);

		return data;
	}

	public byte[] getResponse(byte lengthToReceive) throws Exception {
		Apdu apdu = new Apdu();
		byte length;

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xC0, (byte) 0x00, (byte) 0x00, lengthToReceive });

		apdu.setLengthExpected(lengthToReceive + 2);
		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();

		if (apdu.getStatusWord()[0] == (byte) 0x6C) {
			length = (byte) (apdu.getStatusWord()[1] & (byte) 0x0F);
			throw new Exception(
					"Wrong expected data length - issue command again with P3 = " + Integer.toString(length) + ".");
		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("No data available.");
		else if (apdu.getStatusWord()[0] == (byte) 0x62 && apdu.getStatusWord()[1] == (byte) 0x81)
			throw new Exception("Part of the data may be corrupted.");
		else
			throw new Exception("Get Response Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
	}

	public void loadDiversifyKey(DIVERSIFY_KEY_OPERATION diversifyOperation, PIN_KEY_SCOPE pinToUse,
			byte masterKeyIndex, byte[] data) throws Exception {
		Apdu apdu = new Apdu();
		apdu.setCommand(
				new byte[] { (byte) 0x80, (byte) 0x72, (byte) diversifyOperation._id, 0x00, (byte) data.length });

		if (diversifyOperation != DIVERSIFY_KEY_OPERATION.BULK_ENCRYPTION) {

			if (diversifyOperation == DIVERSIFY_KEY_OPERATION.INITIAL_VECTOR && data.length != 8 && data.length != 16)
				throw new Exception("Data parameter has invalid length. Valid length are 8 and 16 bytes");

			apdu.setSendData(data);
		}

		if (pinToUse == PIN_KEY_SCOPE.LOCAL)
			masterKeyIndex |= 0x80;

		apdu.setParameter2(masterKeyIndex);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90)
			throw new Exception("Load Diversify Key failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");

	}

	public byte[] prepareAuthentication(PREPARE_AUTHENTICATION_ENCRYPTION enType,
			PREPARE_AUTHENTICATION_CLIENT_CARD authType, byte[] cardChallengeData) throws Exception {
		Apdu apdu = new Apdu();
		byte[] aCommand = { (byte) 0x80, 0x78, (byte) enType._id, (byte) authType._id,
				(byte) cardChallengeData.length };

		switch (enType) {
		case TRIPLE_DES:
		case SINGLE_DES:
		case THREE_KEY_DES_DESFIRE:
		case TRIPLE_DES_DESFIRE:
		case SINGLE_DES_DESFIRE:
			aCommand[4] = 0x08;
			break;
		case AES:
			aCommand[4] = 0x10;
			break;
		}

		if (aCommand[4] != cardChallengeData.length)
			throw new Exception("Data length mismatch.");

		apdu.setCommand(aCommand);
		apdu.setSendData(cardChallengeData);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else if (apdu.getStatusWord()[0] == (byte) 0x61)
			return getResponse(apdu.getStatusWord()[1]);
		else
			throw new Exception("Load Diversify Key failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
	}

	public void verifyAuthentication(VERIFY_ENCRYPTION enc, VERIFY_CLIENT_CARD clientCard, byte[] responseFromCard)
			throws Exception {
		Apdu apdu = new Apdu();

		if (responseFromCard == null)
			throw new Exception("Card challenge is invalid");

		if ((enc == VERIFY_ENCRYPTION.SINGLE_DES || enc == VERIFY_ENCRYPTION.TRIPLE_DES)
				&& responseFromCard.length != 8)
			throw new Exception("Card response is invalid. Valid card response is 8 bytes");

		if ((enc == VERIFY_ENCRYPTION.THREE_KEY_DES_DESFIRE || enc == VERIFY_ENCRYPTION.AES)
				&& responseFromCard.length != 16)
			throw new Exception("Card response is invalid. Valid card response is 16 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x7A, (byte) enc._id, (byte) clientCard._id,
				(byte) responseFromCard.length });
		apdu.setSendData(responseFromCard);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception("Verify Authenticate Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}
	}

	public byte[] encrypt(byte DesMode, byte[] pin) throws Exception {
		byte[] data;
		Apdu apdu = new Apdu();
		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x74, DesMode, (byte) 0x01, (byte) 0x08 });
		apdu.setSendData(pin);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else if (apdu.getStatusWord()[0] != 0x61)
			throw new Exception(
					"Prepare Authentication failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");

		data = getResponse(apdu.getStatusWord()[1]);

		return data;
	}

	public byte[] decrypt(byte DesMode, byte[] pin) throws Exception {
		byte[] data;
		Apdu apdu = new Apdu();
		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x76, DesMode, (byte) 0x01, (byte) 0x08 });
		apdu.setSendData(pin);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else if (apdu.getStatusWord()[0] != 0x61)
			throw new Exception(
					"Prepare Authentication failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");

		data = getResponse(apdu.getStatusWord()[1]);

		return data;
	}

	public void verifyInquireAccount(byte DesMode, byte[] data) throws Exception {
		Apdu apdu = new Apdu();

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x7C, DesMode, (byte) 0x00, (byte) 0x1D });
		apdu.setSendData(data);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception(
					"Verify Inquire Account Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}
	}

	public byte[] prepareAccountTransaction(byte desMode, byte[] data, byte operationMode) throws Exception {
		Apdu apdu = new Apdu();
		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x7E, desMode, operationMode, (byte) 0x0D });
		apdu.setSendData(data);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		if (apdu.getStatusWord()[0] != 0x61)
			throw new Exception(
					"Prepare Authentication failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");

		return getResponse(apdu.getStatusWord()[1]);
	}

	public void verifyDebitCertificate(byte DesMode, byte[] data) throws Exception {
		Apdu apdu = new Apdu();

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x70, DesMode, (byte) 0x00, (byte) 0x14 });
		apdu.setSendData(data);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] != (byte) 0x90) {
			throw new Exception(
					"Verify Debit Certificate Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
		}

	}
}
