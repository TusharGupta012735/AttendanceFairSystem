package AcsCommon;

public class Apdu {
	final static public int APDU_STRUCTURE_CLASS = 0;
	final static public int APDU_STRUCTURE_INSTRUCTION = 1;
	final static public int APDU_STRUCTURE_PARAMETER1 = 2;
	final static public int APDU_STRUCTURE_PARAMETER2 = 3;
	final static public int APDU_STRUCTURE_PARAMETER3 = 4;
	final static public int APDU_STRUCTURE_SIZE = 5;
	final static public int STATUS_WORD_LENGTH = 2;

	private int _operationControlCode = 0;
	private byte _instructionClass;
	private byte _instructionCode;
	private byte _parameter1;
	private byte _parameter2;
	private byte _parameter3;

	private byte[] aCommand_;
	private byte[] aStatusWord_;
	private byte[] aSendData_;
	private byte[] aReceiveData_;

	public int getOperationControlCode() {
		return this._operationControlCode;
	}

	public void setOperationControlCode(int code) {
		this._operationControlCode = code;
	}

	/// <summary>
	/// The T=0 instruction class.
	/// </summary>
	public byte getInstructionClass() {
		return this._instructionClass;
	}

	public void setInstructionClass(byte cls) {
		this._instructionClass = cls;
	}

	/// <summary>
	/// Reference codes that complete the instruction code.
	/// </summary>
	public byte getInstructionCode() {
		return this._instructionCode;
	}

	public void setInstructionCode(byte code) {
		this._instructionCode = code;
	}

	/// <summary>
	/// Reference codes that complete the instruction code.
	/// </summary>
	public byte getParameter1() {
		return this._parameter1;
	}

	public void setParameter1(byte apduP1) {
		this._parameter1 = apduP1;
	}

	/// <summary>
	/// Reference codes that complete the instruction code.
	/// </summary>
	public byte getParameter2() {
		return this._parameter2;
	}

	public void setParameter2(byte apduP2) {
		this._parameter2 = apduP2;
	}

	/// <summary>
	/// Reference codes that complete the instruction code.
	/// </summary>
	public byte getParameter3() {
		return this._parameter3;
	}

	public void setParameter3(byte apduP3) {
		this._parameter3 = apduP3;
	}

	public void setSendData(byte[] sendData) {
		this.aSendData_ = sendData;
	}

	public byte[] getStatusWord() {
		return this.aStatusWord_;
	}

	public void setStatusWord(byte[] statusWord) {
		this.aStatusWord_ = statusWord;
	}

	public byte[] getReceiveData() {
		return this.aReceiveData_;
	}

	public void setReceiveData(byte[] receiveData) {
		this.aReceiveData_ = receiveData;
	}

	/// <summary>
	/// Length of data expected from the card
	/// </summary>
	public int getLengthExpected() {
		return this.aReceiveData_.length;
	}

	public void setLengthExpected(int receiveData) {
		if (receiveData != 0)
			this.aReceiveData_ = new byte[receiveData];
		else
			this.aReceiveData_ = new byte[256];
	}

	public Apdu() {
	}

	public byte[] getCommand() {
		aCommand_ = new byte[APDU_STRUCTURE_SIZE];

		aCommand_[APDU_STRUCTURE_CLASS] = getInstructionClass();
		aCommand_[APDU_STRUCTURE_INSTRUCTION] = getInstructionCode();
		aCommand_[APDU_STRUCTURE_PARAMETER1] = getParameter1();
		aCommand_[APDU_STRUCTURE_PARAMETER2] = getParameter2();
		aCommand_[APDU_STRUCTURE_PARAMETER3] = getParameter3();

		if (!(aSendData_ == null) && (aSendData_.length > 0)) {
			byte[] command = new byte[APDU_STRUCTURE_SIZE + aSendData_.length];
			System.arraycopy(aCommand_, 0, command, 0, APDU_STRUCTURE_SIZE);
			System.arraycopy(aSendData_, 0, command, APDU_STRUCTURE_SIZE, aSendData_.length);
			return command;
		} else
			return aCommand_;
	}

	public void setCommand(byte[] apduCommand) throws Exception {
		if (apduCommand.length < APDU_STRUCTURE_SIZE)
			throw new Exception("Invalid command");

		setInstructionClass(apduCommand[APDU_STRUCTURE_CLASS]);
		setInstructionCode(apduCommand[APDU_STRUCTURE_INSTRUCTION]);
		setParameter1(apduCommand[APDU_STRUCTURE_PARAMETER1]);
		setParameter2(apduCommand[APDU_STRUCTURE_PARAMETER2]);
		setParameter3(apduCommand[APDU_STRUCTURE_PARAMETER3]);
	}

	public boolean swEqualTo(byte[] data) {
		int index = 0;

		if (getStatusWord() == null)
			return false;

		for (index = 0; index < getStatusWord().length; index++)
			if (getStatusWord()[index] != data[index])
				return false;

		return true;
	}
}
