package Acos;

import javax.smartcardio.CardException;

import AcsCommon.*;
import Pcsc.*;

public class Acos3 {

	public enum CODE_TYPE {
		AC1((byte) 0x01), AC2((byte) 0x02), AC3((byte) 0x03), AC4((byte) 0x04), AC5((byte) 0x05), PIN((byte) 0x06),
		IC((byte) 0x07);

		private final int ID;

		CODE_TYPE(int id) {
			this.ID = id;
		}

		public int getCardType() {
			return ID;
		}
	}

	public enum INTERNAL_FILE {
		MCUID_FILE(0), MANUFACTURER_FILE(1), PERSONALIZATION_FILE(2), SECURITY_FILE(3), USER_FILE_MGMT_FILE(4),
		ACCOUNT_FILE(5), ACCOUNT_SECURITY_FILE(6), ATR_FILE(7);

		private final int ID;

		INTERNAL_FILE(int id) {
			this.ID = id;
		}

		public int getInternalFile() {
			return ID;
		}
	}

	public enum CARD_INFO_TYPE {
		CARD_SERIAL(0), EEPROM(5), VERSION_NUMBER(6);

		private final int ID;

		CARD_INFO_TYPE(int id) {
			this.ID = id;
		}

		public int getCardInfoType() {
			return ID;
		}
	}

	private PcscReader pcscReader_;

	public PcscReader getPcscConnection() {
		return this.pcscReader_;
	}

	public void setPcscConnection(PcscReader pcscConnection) {
		this.pcscReader_ = pcscConnection;
	}

	public Acos3() {
		pcscReader_ = null;
	}

	public Acos3(PcscReader pcscReader) {
		setPcscConnection(pcscReader);
	}

	public byte[] getCardInfo(CARD_INFO_TYPE cardInfoType) throws Exception {
		Apdu apdu = new Apdu();

		if (cardInfoType == CARD_INFO_TYPE.CARD_SERIAL) {
			apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x08 });
			apdu.setLengthExpected(10);
		} else if (cardInfoType == CARD_INFO_TYPE.VERSION_NUMBER) {
			apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x14, (byte) 0x06, (byte) 0x00, (byte) 0x08 });
			apdu.setLengthExpected(2);
		} else if (cardInfoType == CARD_INFO_TYPE.EEPROM) {
			apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x14, (byte) 0x05, (byte) 0x00, (byte) 0x00 });
			apdu.setLengthExpected(10);
		}

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x67 && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("Wrong P3");
		else if (apdu.getStatusWord()[0] == (byte) 0x67 && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("P1 or P2 is invalid.");
		else if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public void submitCard(CODE_TYPE codeType, byte[] code) throws Exception {
		Apdu apdu = new Apdu();
		byte[] aCommand = { (byte) 0x80, 0x20, (byte) codeType.ID, 0x00, 0x08 };

		if (pcscReader_ == null)
			throw new CardException("Connection is not yet established");
		if (code.length != 8)
			throw new CardException("Code has invalid length. Code should be 8 bytes long");

		apdu.setCommand(aCommand);
		apdu.setSendData(code);
		apdu.setLengthExpected(2);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x63) {
			throw new CardException("Wrong Code." + (apdu.getStatusWord()[1] - (byte) 0xC0) + "retries left.");
		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new CardException("The specified Code is locked.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new CardException(
					"Mutual Authentication not successfully completed prior to the Submit Code command.");
		else
			throw new CardException(getErrorMessage(apdu.getStatusWord()));
	}

	public void clearCard() throws Exception {
		Apdu apdu = new Apdu();
		byte[] aCommand = { (byte) 0x80, 0x30, 0x00, 0x00, 0x00 };

		if (pcscReader_ == null)
			throw new CardException("Connection is not yet established");

		apdu.setCommand(aCommand);
		apdu.setLengthExpected(2);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new CardException("IC code not satisfied or card is in user state.");
		else
			throw new CardException(getErrorMessage(apdu.getStatusWord()));
	}
	
	public void changePin(byte[] newPin) throws Exception
	{
		Apdu apdu = new Apdu();
		apdu.setCommand(new byte[] {(byte)0x80, (byte)0x24, 0x00, (byte)0x00, (byte)0x08});
		apdu.setSendData(newPin);
		
		getPcscConnection().sendApduCommand(apdu);
				
		if (apdu.getStatusWord()[0] != (byte)0x90)
			throw new Exception ("Change PIN Failed [" + Helper.byteAsString(apdu.getStatusWord(), true) + "]");
	}

	public void selectFile(INTERNAL_FILE internalFile) throws Exception {
		byte[] fileID;

		if (internalFile == INTERNAL_FILE.MCUID_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x00 };
		else if (internalFile == INTERNAL_FILE.MANUFACTURER_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x01 };
		else if (internalFile == INTERNAL_FILE.PERSONALIZATION_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x02 };
		else if (internalFile == INTERNAL_FILE.SECURITY_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x03 };
		else if (internalFile == INTERNAL_FILE.USER_FILE_MGMT_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x04 };
		else if (internalFile == INTERNAL_FILE.ACCOUNT_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x05 };
		else if (internalFile == INTERNAL_FILE.ACCOUNT_SECURITY_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x06 };
		else if (internalFile == INTERNAL_FILE.ATR_FILE)
			fileID = new byte[] { (byte) 0xFF, (byte) 0x07 };
		else
			throw new Exception("Invalid internal file");

		this.selectFile(fileID);
	}

	public void selectFile(byte[] fileID) throws Exception {
		Apdu apdu = new Apdu();
		if (fileID == null || fileID.length != 2)
			throw new Exception("File ID length should be 2 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x02 });
		apdu.setSendData(fileID);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90 || apdu.getStatusWord()[0] == (byte) 0x91)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("File does not exist.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	// Acos3ReadWrite start region
	public void writeRecord(byte recordNumber, byte offset, byte[] dataToWrite) throws Exception {
		Apdu apdu = new Apdu();

		if (dataToWrite == null || dataToWrite.length < 1)
			throw new Exception("Data to write is not specified");

		if (dataToWrite.length > 255)
			throw new Exception("Data to write is too long");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xD2, recordNumber, offset, (byte) dataToWrite.length });
		apdu.setSendData(dataToWrite);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x67 && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("Specified Length plus Offset is larger than the record length.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x81)
			throw new Exception("Command incompatible with file structure.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - Secret code not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("No file selected.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("Record not found - file too short.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("I/O error; data to be accessed resides in invalid address.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public byte[] readRecord(byte recordNumber, byte offset, byte lengthToRead) throws Exception {
		Apdu apdu = new Apdu();
		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xB2, recordNumber, offset, lengthToRead });

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();

		if (apdu.getStatusWord()[0] == (byte) 0x67 && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("Specified Length plus Offset is larger than the record length.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x81)
			throw new Exception("Command incompatible with file structure.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - Secret code not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("No file selected.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("Record not found - file too short.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("I/O error; data to be accessed resides in invalid address.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}
	// Acos3ReadWrite end region

	// Acos3BinaryFiles start region
	public byte[] readBinary(byte highOffset, byte lowOffset, byte lengthToRead) throws Exception {
		Apdu apdu = new Apdu();
		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xB0, highOffset, lowOffset, lengthToRead });

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();

		if (apdu.getStatusWord()[0] == (byte) 0x67 && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("Specified Length plus Offset is larger than file length.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x81)
			throw new Exception("Command incompatible with file structure.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - Secret code not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("No file selected.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("File too short - Offset is larger than the file length.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("I/O error; data to be accessed resides in invalid address.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public void writeBinary(byte highOffset, byte lowOffset, byte[] dataToWrite) throws Exception {
		Apdu apdu = new Apdu();

		if (dataToWrite == null || dataToWrite.length < 1)
			throw new Exception("Data to write is not specified");

		if (dataToWrite.length > 255)
			throw new Exception("Data to write is too long");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xD0, highOffset, lowOffset, (byte) dataToWrite.length });
		apdu.setSendData(dataToWrite);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x67 && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("Specified Length plus Offset is larger than file length.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x81)
			throw new Exception("Command incompatible with file structure.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - Secret code not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("No file selected.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("File too short - Offset is larger than the file length.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x00)
			throw new Exception("I/O error; data to be accessed resides in invalid address.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}
	// Acos3BinaryFiles end region

	// Acos3Account start region
	public byte[] inquireAccount(byte keyNumber, byte[] randomNumber) throws Exception {
		Apdu apdu = new Apdu();

		if (randomNumber == null || randomNumber.length != 4)
			throw new Exception("Random data length should be 4 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xE4, keyNumber, (byte) 0x00, (byte) 0x04 });
		apdu.setSendData(randomNumber);
		apdu.setLengthExpected(25 + 2);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else if (apdu.getStatusWord()[0] == (byte) 0x61)
			return getResponse(apdu.getStatusWord()[1]);
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception(
					"No data available; the Inquire Account command was not executed immediately prior to the Get Response command.");
		else if (apdu.getStatusWord()[0] == (byte) 0x62 && apdu.getStatusWord()[1] == (byte) 0x81)
			throw new Exception("Account data may be corrupted.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public void credit(byte[] mac, byte[] amount, byte[] ttref) throws Exception {
		Apdu apdu = new Apdu();
		;
		byte retriesLeft;

		if (mac == null || mac.length != 4)
			throw new Exception("MAC length should be 4 bytes");

		if (amount == null || amount.length != 3)
			throw new Exception("Amount length should be 3 bytes");

		if (ttref == null || ttref.length != 4)
			throw new Exception("Terminal transaction reference should be 4 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xE2, (byte) 0x00, (byte) 0x00, (byte) 0x0B });

		byte[] apduSendData = new byte[11];
		System.arraycopy(mac, 0, apduSendData, 0, 4);
		System.arraycopy(amount, 0, apduSendData, 4, 3);
		System.arraycopy(ttref, 0, apduSendData, 7, 4);
		apdu.setSendData(apduSendData);
		apdu.setLengthExpected(2);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x63) {
			retriesLeft = (byte) (apdu.getStatusWord()[1] & (byte) 0x0F);
			throw new Exception(
					"MAC cryptographic checksum is wrong; " + Integer.toString(retriesLeft) + " retries left");
		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0xF0)
			throw new Exception("Data in account is inconsistent - no access unless in Issuer Mode.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Account does not exist.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x10)
			throw new Exception("Account Transaction Counter at maximum - no more transaction is possible.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - PIN not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6B && apdu.getStatusWord()[1] == (byte) 0x20)
			throw new Exception("Amount is too large.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("Credit Key is locked.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("Mutual Authentication has not been completed.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public byte[] debit(byte[] mac, byte[] amount, byte[] ttref, byte requireDebit) throws Exception {
		Apdu apdu = new Apdu();
		byte retriesLeft;

		if (mac == null || mac.length != 4)
			throw new Exception("MAC length should be 4 bytes");

		if (amount == null || amount.length != 3)
			throw new Exception("Amount length should be 3 bytes");

		if (ttref == null || ttref.length != 4)
			throw new Exception("Terminal transaction reference should be 4 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xE6, requireDebit, (byte) 0x00, (byte) 0x0B });

		byte[] apduSendData = new byte[11];
		System.arraycopy(mac, 0, apduSendData, 0, 4);
		System.arraycopy(amount, 0, apduSendData, 4, 3);
		System.arraycopy(ttref, 0, apduSendData, 7, 4);
		apdu.setSendData(apduSendData);
		if (requireDebit == 0x00)
			apdu.setLengthExpected(2);
		else if (requireDebit == 0x01)
			apdu.setLengthExpected(6);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else if (apdu.getStatusWord()[0] == (byte) 0x61)
			return getResponse(apdu.getStatusWord()[1]);
		else if (apdu.getStatusWord()[0] == (byte) 0x63) {
			retriesLeft = (byte) (apdu.getStatusWord()[1] & (byte) 0x0F);
			throw new Exception(
					"MAC cryptographic checksum is wrong; " + Integer.toString(retriesLeft) + " retries left");
		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0xF0)
			throw new Exception("Data in account is inconsistent - no access unless in Issuer Mode.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Account does not exist.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x10)
			throw new Exception("Account Transaction Counter at maximum - no more transaction is possible.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - PIN not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6B && apdu.getStatusWord()[1] == (byte) 0x20)
			throw new Exception("Amount is too large.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("Credit Key is locked.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("Mutual Authentication has not been completed.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public void revokeDebit(byte[] mac) throws Exception {
		Apdu apdu = new Apdu();
		byte retriesLeft;

		if (mac == null || mac.length != 4)
			throw new Exception("MAC length should be 4 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0xE8, (byte) 0x00, (byte) 0x00, (byte) 0x04 });
		apdu.setSendData(mac);
		apdu.setLengthExpected(2);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90)
			return;
		else if (apdu.getStatusWord()[0] == (byte) 0x63) {
			retriesLeft = (byte) (apdu.getStatusWord()[1] & (byte) 0x0F);
			throw new Exception(
					"MAC cryptographic checksum is wrong; " + Integer.toString(retriesLeft) + " retries left");
		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0xF0)
			throw new Exception("Data in account is inconsistent - no access unless in Issuer Mode.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6A && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Account does not exist.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6F && apdu.getStatusWord()[1] == (byte) 0x10)
			throw new Exception("Account Transaction Counter at maximum - no more transaction is possible.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x82)
			throw new Exception("Security status not satisfied - PIN not submitted.");
		else if (apdu.getStatusWord()[0] == (byte) 0x6B && apdu.getStatusWord()[1] == (byte) 0x20)
			throw new Exception("Amount is too large.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("Revoke Debit Key is locked.");
		else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("Mutual Authentication has not been completed.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}
	// Acos3Account end region

	// Acos3CreateFile start region
	public void configurePersonalizationFile(HelperClass.OptionRegister optionRegister,
			HelperClass.SecurityOptionRegister securityRegister, byte NumberOfFiles) throws Exception {
		try {
			byte[] data = new byte[] { optionRegister.getRawValue(), securityRegister.getRawValue(), NumberOfFiles,
					(byte) 0x00 };
			selectFile(INTERNAL_FILE.PERSONALIZATION_FILE);
			writeRecord((byte) 0x00, (byte) 0x00, data);

		} catch (Exception ex) {
			throw new Exception(ex.getMessage());
		}
	}

	public void createRecordFile(byte recordNumber, byte[] fileID, byte numberOfRecords, byte recordLength,
			HelperClass.SecurityAttribute writeSecurityAttribute, HelperClass.SecurityAttribute readSecurityAttribute,
			boolean readRequireSecureMessaging, boolean writeRequireSecureMessaging) throws Exception {
		byte[] buffer;

		this.selectFile(INTERNAL_FILE.USER_FILE_MGMT_FILE);

		buffer = new byte[7];
		buffer[0] = recordLength;
		buffer[1] = numberOfRecords;
		buffer[2] = readSecurityAttribute.getRawValue();
		buffer[3] = writeSecurityAttribute.getRawValue();
		buffer[4] = fileID[0];
		buffer[5] = fileID[1];

		if (readRequireSecureMessaging)
			buffer[6] |= (byte) 0x40;

		if (writeRequireSecureMessaging)
			buffer[6] |= (byte) 0x20;

		writeRecord(recordNumber, (byte) 0x00, buffer);
	}
	// Acos3CreateFile end region

	// Acos3MutualAuthentication start region
	public byte[] startSession() throws Exception {
		byte[] randomNumber = new byte[8];
		Apdu apdu = new Apdu();

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x84, (byte) 0x00, (byte) 0x00, (byte) 0x08 });
		apdu.setLengthExpected(10);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x90 && apdu.getStatusWord()[1] == (byte) 0x00) {
			System.arraycopy(apdu.getReceiveData(), 0, randomNumber, 0, 8);

			return randomNumber;
		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x83)
			throw new Exception("Terminal Authentication Key KT is locked, authentication process cannot be executed.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public byte[] authenticate(byte[] encryptedData, byte[] terminalRandomNumber) throws Exception {
		Apdu apdu = new Apdu();
		;
		byte retriesLeft;

		if (encryptedData == null || encryptedData.length != 8)
			throw new Exception("Encrypted data length should be 8 bytes");

		if (terminalRandomNumber == null || terminalRandomNumber.length != 8)
			throw new Exception("Terminal random number length should be 8 bytes");

		apdu.setCommand(new byte[] { (byte) 0x80, (byte) 0x82, (byte) 0x00, (byte) 0x00, (byte) 0x10 });
		byte[] apduSendData = new byte[16];

		System.arraycopy(encryptedData, 0, apduSendData, 0, 8);
		System.arraycopy(terminalRandomNumber, 0, apduSendData, 8, 8);
		apdu.setSendData(apduSendData);

		getPcscConnection().sendApduCommand(apdu);

		if (apdu.getStatusWord()[0] == (byte) 0x61)
			return getResponse(apdu.getStatusWord()[1]);
		else if (apdu.getStatusWord()[0] == (byte) 0x90)
			return apdu.getReceiveData();
		else if (apdu.getStatusWord()[0] == (byte) 0x63) {
			retriesLeft = (byte) (apdu.getStatusWord()[0] & (byte) 0x0F);
			throw new Exception("Key KT not correct; " + Integer.toString(retriesLeft) + " retries left");

		} else if (apdu.getStatusWord()[0] == (byte) 0x69 && apdu.getStatusWord()[1] == (byte) 0x85)
			throw new Exception("Start Session not executed immediately before Authenticate command.");
		else
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}
	// Acos3MutualAuthentication end region

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
			throw new Exception(getErrorMessage(apdu.getStatusWord()));
	}

	public String getErrorMessage(byte[] statusWord) {
		if (statusWord.length == 0)
			return "Unknow Status Word (statusWord)";
		else if (statusWord[0] == 0x62 && statusWord[1] == 0x81)
			return "Data returned in response to the Inquire Account command may be incorrect due to corrupted data in the Account Data Structure.";
		else if (statusWord[0] == 0x63)
			return "Security related command failed - External Authentication failed; incorrect Secret Code submiited; incorrect key used in Credit MAC generation. "
					+ (statusWord[1] - (byte) 0xC0F) + " retries left.";
		else if (statusWord[0] == 0x67 && statusWord[1] == 0x00)
			return "Wrong P3.";
		else if (statusWord[0] == 0x68 && statusWord[1] == 0x82)
			return "Secure Messaging not allowed.";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0x66)
			return "Command not available.";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0x81)
			return "Command incompatible with file structure.";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0x83)
			return "Key or Secret Code is locked - no more verification or submission is possible.\"";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0x85)
			return "Conditions of use not satisfied.";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0x87)
			return "Expected Secure Messaging Data Objects missing.";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0x88)
			return "The Secure Messaging MAC does not match the data.";
		else if (statusWord[0] == 0x69 && statusWord[1] == 0xF0)
			return "Account data inconsistent/transaction interrupted - access to account only in priviledged mode possible.";
		else if (statusWord[0] == 0x6A && statusWord[1] == 0x82)
			return "File does not exist; account not available.";
		else if (statusWord[0] == 0x6A && statusWord[1] == 0x83)
			return "Record not found - file too short.";
		else if (statusWord[0] == 0x6A && statusWord[1] == 0x86)
			return "P1-P2 is incorrect.";
		else if (statusWord[0] == 0x6B && statusWord[1] == 0x20)
			return "Invalid amount in Credit/Debit command.";
		else if (statusWord[0] == 0x6D && statusWord[1] == 0x00)
			return "Unknown INS";
		else if (statusWord[0] == 0x6E && statusWord[1] == 0x00)
			return "Invalid CLA";
		else if (statusWord[0] == 0x6F && statusWord[1] == 0x00)
			return "I/O error; data to be accessed resides in invalid address";
		else
			return "Unknown Status Word";
	}
}
