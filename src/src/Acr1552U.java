import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import AcsCommon.*;

public class Acr1552U extends Pcsc.PcscReader
{
    public static final int FILE_DEVICE_SMARTCARD = 0x310000; // Reader action IOCTLs
	public static final int IOCTL_SMARTCARD_ACR1281UC1_ESCAPE_COMMAND = FILE_DEVICE_SMARTCARD + 3500 * 4;
	protected int _ctrlCode;
	
	public static final int CONTACTLESS_CARD_TYPE_ISO14443A_3 = 0x0001;
    public static final int CONTACTLESS_CARD_TYPE_ISO14443B_3 = 0x0002;
    public static final int CONTACTLESS_CARD_TYPE_APPLE_ECP_VAS = 0x0004;
    public static final int CONTACTLESS_CARD_TYPE_FELICA = 0x0008;
    public static final int CONTACTLESS_CARD_TYPE_SRI_ISO14443B_2 = 0x0010;
    public static final int CONTACTLESS_CARD_TYPE_PICOPASS_ISO14443B_2 = 0x0020;
    public static final int CONTACTLESS_CARD_TYPE_ISO15693_3 = 0x0040;
    public static final int CONTACTLESS_CARD_TYPE_PICOPASS_ISO15693_2 = 0x0080;
    public static final int CONTACTLESS_CARD_TYPE_INNOVATRON = 0x0400;
    public static final int CONTACTLESS_CARD_TYPE_CTS = 0x0800;
    
    public static final byte PPS_106K_BPS = 0x00;
    public static final byte PPS_212K_BPS = 0x01;
    public static final byte PPS_424K_BPS = 0x02;
    public static final byte PPS_848K_BPS = 0x03;
    
    public static class AUTO_PPS_SETTING
    {
    	public byte maxSpeed;
        public byte currentSpeed;
    }
    
    public static class PICC_POLLING_OPTION
    { 	
    	public boolean enablePolling;
        public boolean enableRFOff;
        public boolean enablePart3;
        public boolean enablePart4;
        public byte pollInterval;
    }
	
	public Acr1552U() 
	{
		super();
		this._ctrlCode = IOCTL_SMARTCARD_ACR1281UC1_ESCAPE_COMMAND;
	}
	
	public void setCommandData(AcsCommon.Apdu cApdu, byte[] cmdData, int receiveLen) throws Exception
    {
        cApdu.setCommand(cmdData);
        if (cmdData.length > AcsCommon.Apdu.APDU_STRUCTURE_SIZE)
        {
            byte[] sendData = new byte[cmdData.length - AcsCommon.Apdu.APDU_STRUCTURE_SIZE];
            System.arraycopy(cmdData, AcsCommon.Apdu.APDU_STRUCTURE_SIZE, sendData, 0, cmdData.length - AcsCommon.Apdu.APDU_STRUCTURE_SIZE);
            cApdu.setSendData(sendData);
        }
        else
        {
        	byte[] emptyArray = {};
            cApdu.setSendData(emptyArray);
        }
        cApdu.setLengthExpected(receiveLen);
        cApdu.setOperationControlCode(_ctrlCode);
    }
	
//////////////////////////////////////////////////////////////////////
// Escape command for Peripheral Control and Other
//////////////////////////////////////////////////////////////////////
	
	public String getFirmwareVersion() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x18, 0x00};
		setCommandData(cApdu, aCommand, 32);
		
		sendControlCommand(cApdu);
		
		String firmwareVersion = new String(Arrays.copyOfRange(cApdu.getReceiveData(), 5, cApdu.getReceiveData().length), StandardCharsets.UTF_8); 
		
		return firmwareVersion;
	}
	
	public String getSerialNumber() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x33, 0x00};
		setCommandData(cApdu, aCommand, 32);
		
		sendControlCommand(cApdu);
		
		String serialNo = new String(Arrays.copyOfRange(cApdu.getReceiveData(), 5, cApdu.getReceiveData().length), StandardCharsets.UTF_8); 
		
		return serialNo;
	}
	
	public int setBuzzerState(byte cDuration) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x28, 0x01, cDuration};
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public int setBuzzerState(byte cOnTime, byte cOffTime, byte cRepeatTime) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x28, 0x03, cOnTime, cOffTime, cRepeatTime};
		setCommandData(cApdu, aCommand, 8);
		
		return sendControlCommand(cApdu);
	}
	
	public int setLEDState(boolean LED1, boolean LED2) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x29, 0x01, 0x00};
		
		if (LED1)
            aCommand[5] |= 0x01;

        if (LED2)
            aCommand[5] |= 0x02;
        
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public byte getLEDState() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x29, 0x00};
        
		setCommandData(cApdu, aCommand, 6);
		
		sendControlCommand(cApdu);
		
		return cApdu.getReceiveData()[5];
	}
	
	public int setUIBehaviour(byte flag) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x21, 0x01, flag};
        
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public byte getUIBehaviour() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x21, 0x00};
        
		setCommandData(cApdu, aCommand, 6);
		
		sendControlCommand(cApdu);
		
		return cApdu.getReceiveData()[5];
	}

//////////////////////////////////////////////////////////////////////
// Escape command for NFC
//////////////////////////////////////////////////////////////////////
	
	public int setAutoPPS(byte maxSpeed) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x24, 0x01, maxSpeed};
        
		setCommandData(cApdu, aCommand, 7);
		
		return sendControlCommand(cApdu);
	}
	
	public AUTO_PPS_SETTING getAutoPPS() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x24, 0x00};
        
		setCommandData(cApdu, aCommand, 7);
		
		sendControlCommand(cApdu);
		
		AUTO_PPS_SETTING speed = new AUTO_PPS_SETTING();
		speed.maxSpeed = cApdu.getReceiveData()[5];
		speed.currentSpeed = cApdu.getReceiveData()[6];
		
		return speed;
	}
	
	public int setPollingOption(PICC_POLLING_OPTION pollingOption) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x23, 0x01, 0x00};
		
		if (pollingOption.enablePolling)
            aCommand[5] |= 0x01;

        if (pollingOption.enableRFOff)
            aCommand[5] |= 0x02;

        if (pollingOption.enablePart3)
            aCommand[5] |= 0x08;

        if (pollingOption.enablePart4)
            aCommand[5] |= 0x80;

        switch (pollingOption.pollInterval)
        {
            case 0x00:
                aCommand[5] |= 0x00;
                break;
            case 0x01:
                aCommand[5] |= 0x10;
                break;
            case 0x02:
                aCommand[5] |= 0x20;
                break;
            case 0x03:
                aCommand[5] |= 0x30;
                break;
        }
        
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public PICC_POLLING_OPTION getPollingOption() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x23, 0x00};
        
		setCommandData(cApdu, aCommand, 6);
		
		sendControlCommand(cApdu);
		
		PICC_POLLING_OPTION pollingOption = new PICC_POLLING_OPTION();
		pollingOption.enablePolling = ((cApdu.getReceiveData()[5] & 0x01) == 0x01);
		pollingOption.enableRFOff = ((cApdu.getReceiveData()[5] & 0x02) == 0x02);
		pollingOption.enablePart3 = ((cApdu.getReceiveData()[5] & 0x08) == 0x08);
		pollingOption.enablePart4 = ((cApdu.getReceiveData()[5] & 0x80) == 0x80);
		pollingOption.pollInterval = (byte)((cApdu.getReceiveData()[5] & 0x30) >> 4);
		
		return pollingOption;
	}
	
	public int setRFControl(byte RFStatus) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x25, 0x01, RFStatus};
        
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public byte getPCDPICCStatus() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x25, 0x00};
        
		setCommandData(cApdu, aCommand, 6);
		
		sendControlCommand(cApdu);
		
		return cApdu.getReceiveData()[5];
	}
	
	public int setPollingType(int cardType) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x01, 0x20, 0x02, (byte)cardType, (byte)(cardType >> 8)};
        
		setCommandData(cApdu, aCommand, 7);
		
		return sendControlCommand(cApdu);
	}
	
	public int getPollingType() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x01, 0x20, 0x00};
        
		setCommandData(cApdu, aCommand, 7);
		
		sendControlCommand(cApdu);
		
		int cardType = cApdu.getReceiveData()[6] << 8;
        cardType |= cApdu.getReceiveData()[5];
		
		return cardType;
	}
	
	public int getPICCType() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x35, 0x00};
        
		setCommandData(cApdu, aCommand, 7);
		
		sendControlCommand(cApdu);
		
		int PICCType = (cApdu.getReceiveData()[6] & 0xFF) << 8;
		PICCType |= cApdu.getReceiveData()[5];
		
		return PICCType;
	}
	
//////////////////////////////////////////////////////////////////////
// Escape command for ICC/SAM
//////////////////////////////////////////////////////////////////////
	
	public int setMode(byte mode) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x2B, 0x01, mode};
        
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public byte getMode() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x2B, 0x00};
        
		setCommandData(cApdu, aCommand, 6);
		
		sendControlCommand(cApdu);
		
		return cApdu.getReceiveData()[5];
	}
	
	public int setCardPowerConfig(byte cardPower) throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x0B, 0x01, cardPower};
        
		setCommandData(cApdu, aCommand, 6);
		
		return sendControlCommand(cApdu);
	}
	
	public byte getCardPowerConfig() throws Exception
	{
		AcsCommon.Apdu cApdu = new Apdu();
		
		byte[] aCommand = new byte[] {(byte) 0xE0, 0x00, 0x00, 0x0B, 0x00};
        
		setCommandData(cApdu, aCommand, 6);
		
		sendControlCommand(cApdu);
		
		return cApdu.getReceiveData()[5];
	}
}
