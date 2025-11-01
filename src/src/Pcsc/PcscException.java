package Pcsc;

import javax.smartcardio.CardException;

public class PcscException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PcscProvider.CODES _errorCode;
	private String _message;

	public PcscProvider.CODES getReaderResponse() {
		return this._errorCode;
	}

	public String getMessage() {
		return this._message;
	}

	public PcscException(PcscProvider.CODES errorCode) {
		_errorCode = errorCode;
	}

	public PcscException(CardException cardException) {
		_errorCode = PcscProvider.CODES.getKeyNameErrorCode(cardException.getCause().getMessage());
		_message = "[" + _errorCode.getErrorCode() + "]" + PcscProvider.getScardErrorMessage(cardException);
	}

	public PcscException(String errorMessage) {
		_message = errorMessage;
	}
}
