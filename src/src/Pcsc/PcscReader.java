package Pcsc;

import javax.smartcardio.*;

import AcsCommon.*;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;

public class PcscReader {

	public enum CARD_STATUS {
		UNKNOWN, FOUND, NOT_FOUND, ERROR
	};

	private int returnCode;

	protected TerminalFactory _terminalFactory;

	public TerminalFactory getTerminalFactory() {
		return this._terminalFactory;
	}

	public void setTerminalFactory(TerminalFactory terminalFactory) {
		this._terminalFactory = terminalFactory;
	}

	protected List<CardTerminal> _cardTerminalList;

	public List<CardTerminal> getCardTerminalList() {
		return this._cardTerminalList;
	}

	public void setCardTerminalList(List<CardTerminal> cardTerminalList) {
		this._cardTerminalList = cardTerminalList;
	}

	protected String _preferredProtocol;

	public String getPreferredProtocol() {
		return this._preferredProtocol;
	}

	public void setPreferredProtocol(String preferredProtocol) {
		this._preferredProtocol = preferredProtocol;
	}

	protected boolean _connectionActive;

	public boolean isConnectionActive() {
		return this._connectionActive;
	}

	public void setConnectionActive(boolean connectionActive) {
		this._connectionActive = connectionActive;
	}

	protected CardTerminal _activeTerminal;

	public CardTerminal getActiveTerminal() {
		return this._activeTerminal;
	}

	public void setActiveTerminal(CardTerminal activeTerminal) {
		this._activeTerminal = activeTerminal;
	}

	protected Card _card;

	public Card getCard() {
		return this._card;
	}

	public void setCard(Card card) {
		this._card = card;
	}

	protected CardChannel _cardChannel;

	public CardChannel getCardChannel() {
		return this._cardChannel;
	}

	public void setCardChannel(CardChannel cardChannel) {
		this._cardChannel = cardChannel;
	}

	protected String _activeProtocol;

	public String getActiveProtocol() {
		return this._activeProtocol;
	}

	public void setActiveProtocol(String activeProtocol) {
		this._activeProtocol = activeProtocol;
	}

	protected ReaderEvent _eventHandler;

	public ReaderEvent getEventHandler() {
		return this._eventHandler;
	}

	public void setEventHandler(ReaderEvent eventHandler) {
		this._eventHandler = eventHandler;
	}

	protected byte[] _controlResponse;

	public byte[] getControlResponse() {
		return this._controlResponse;
	}

	public void setControlResponse(byte[] controlResponse) {
		this._controlResponse = controlResponse;
	}

	protected boolean _beginExclusive;

	public boolean isBeginExclusive() {
		return this._beginExclusive;
	}

	public void setBeginExclusive(boolean beginExclusive) {
		this._beginExclusive = beginExclusive;
	}

	public PcscReader() {
		setTerminalFactory(TerminalFactory.getDefault());
		setPreferredProtocol("*");
		setConnectionActive(false);
	}

	public void establishContext() throws Exception {
		Class<?> pcscTerminal = null;
		Field contextId = null;
		Class<?> pcsc = null;
		Method SCardEstablishContext = null;
		Field SCARD_SCOPE_USER = null;
		TerminalFactory factory = null;
		CardTerminals terminals = null;
		Field fieldTerminals = null;
		Class<?> classMap = null;
		Method clearMap = null;
		long newId;

		pcscTerminal = Class.forName("sun.security.smartcardio.PCSCTerminals");
		contextId = pcscTerminal.getDeclaredField("contextId");
		contextId.setAccessible(true);

		if (contextId.getLong(pcscTerminal) != 0L) {
			// First get a new context value
			pcsc = Class.forName("sun.security.smartcardio.PCSC");
			SCardEstablishContext = pcsc.getDeclaredMethod("SCardEstablishContext", new Class[] { Integer.TYPE });
			SCardEstablishContext.setAccessible(true);

			SCARD_SCOPE_USER = pcsc.getDeclaredField("SCARD_SCOPE_USER");
			SCARD_SCOPE_USER.setAccessible(true);

			newId = ((Long) SCardEstablishContext.invoke(pcsc, new Object[] { SCARD_SCOPE_USER.getInt(pcsc) }));
			contextId.setLong(pcscTerminal, newId);

			// Then clear the terminals in cache
			factory = TerminalFactory.getDefault();
			terminals = factory.terminals();
			fieldTerminals = pcscTerminal.getDeclaredField("terminals");
			fieldTerminals.setAccessible(true);
			classMap = Class.forName("java.util.Map");
			clearMap = classMap.getDeclaredMethod("clear");

			clearMap.invoke(fieldTerminals.get(terminals));
		}
	}

	public String[] listTerminals() throws Exception {
		String[] terminals = null;
		int index = 0;

		try {
			setCardTerminalList(getTerminalFactory().terminals().list());
		} catch (CardException exception) {
			if (exception.getCause().getMessage().equals(PcscProvider.CODES.SCARD_E_SERVICE_STOPPED.toString())) {
				establishContext();
				setCardTerminalList(getTerminalFactory().terminals().list());
			} else {
				throw exception;
			}
		}

		terminals = new String[getCardTerminalList().size()];

		for (index = 0; index < getCardTerminalList().size(); index++)
			terminals[index] = getCardTerminalList().get(index).getName();

		return terminals;
	}

	// Connect to the smart card through the specified smart card reader (overloaded
	// function)
	public int connect(int terminalNumber, String preferredProtocol) throws Exception {
		setActiveTerminal(getCardTerminalList().get(terminalNumber));
		setPreferredProtocol(preferredProtocol);

		return connect();
	}

	// Connect to the smart card through the specified smart card reader
	public int connect() throws Exception {
		try {
			setCard(getActiveTerminal().connect(getPreferredProtocol()));
			setCardChannel(getCard().getBasicChannel());
			setActiveProtocol(getCard().getProtocol());
		} catch (CardException cardException) {
			if (cardException.getCause().getMessage().equals("SCARD_E_SERVICE_STOPPED")) {
				establishContext();

				String previousSelectedTerminal = getActiveTerminal().getName();
				List<CardTerminal> previousCardTerminals = getCardTerminalList();

				String[] newTerminals = listTerminals();

				int terminalIndex = -1;
				for (int index = 0; index < newTerminals.length; index++) {
					if (newTerminals[index].equals(previousSelectedTerminal)) {
						terminalIndex = index;
						break;
					}
				}

				if (terminalIndex == -1) {
					setCardTerminalList(previousCardTerminals);
					throw new PcscException("SCARD_E_UNKNOWN_READER");
				}

				setActiveTerminal(getCardTerminalList().get(terminalIndex));
				setCard(getActiveTerminal().connect(getPreferredProtocol()));
				setCardChannel(getCard().getBasicChannel());
			} else {
				throw cardException;
			}
		}

		setConnectionActive(true);

		return 0;
	}

	// Connect directly to the smart card reader
	public int connectDirect(int terminalNumber, boolean isSetTerminalNumber) throws Exception {
		String previousSelectedTerminal = null;
		List<CardTerminal> previousCardTerminals = null;
		String[] newTerminals = null;
		int terminalIndex = -1;
		int index = 0;

		try {
			if (isSetTerminalNumber)
				setActiveTerminal(getCardTerminalList().get(terminalNumber));

			setCard(getActiveTerminal().connect("direct"));
			setConnectionActive(true);
		} catch (CardException exception) {
			if (exception.getCause().getMessage().equals(PcscProvider.CODES.SCARD_E_SERVICE_STOPPED.toString())) {
				establishContext();

				previousSelectedTerminal = getActiveTerminal().getName();
				previousCardTerminals = getCardTerminalList();
				newTerminals = listTerminals();

				if (isSetTerminalNumber) {
					for (index = 0; index < newTerminals.length; index++) {
						if (newTerminals[index].equals(previousSelectedTerminal)) {
							terminalIndex = index;
							break;
						}
					}
				}
				if (terminalIndex == -1) {
					setCardTerminalList(previousCardTerminals);
					throw new PcscException(PcscProvider.CODES.SCARD_E_UNKNOWN_READER);
				}

				setActiveTerminal(getCardTerminalList().get(terminalIndex));
				setCard(getActiveTerminal().connect("direct"));
				setConnectionActive(true);
			} else {
				throw exception;
			}
		}

		return 0;
	}

	// Disconnect from the smart card
	public int disconnect() throws Exception {
		getCard().disconnect(true);
		setConnectionActive(false);

		return returnCode;
	}

	// Get the ATR of the smart card
	public byte[] getAtr() throws Exception {
		return getCard().getATR().getBytes();
	}

	public String getCardProtocol() throws Exception {
		return getCard().getProtocol();
	}

	public int beginExclusive() throws Exception {

		getCard().beginExclusive();

		setConnectionActive(true);
		setBeginExclusive(true);

		return 0;
	}

	public int endExclusive() throws Exception {

		getCard().endExclusive();

		setConnectionActive(false);
		setBeginExclusive(false);

		return 0;
	}

	// Send direct control commands to the smart card reader
	public int sendControlCommand(AcsCommon.Apdu cApdu) throws Exception {
		for (int x = 0; x < 2; x++) {
			try {
				getEventHandler().sendCommandData(cApdu.getCommand());
				cApdu.setReceiveData(
						getCard().transmitControlCommand(cApdu.getOperationControlCode(), cApdu.getCommand()));
				getEventHandler().receiveCommandData(cApdu.getReceiveData());
				break;
			} catch (CardException exception) {
				if (x < 1 && exception.getCause().getMessage()
						.equals(PcscProvider.CODES.SCARD_E_SERVICE_STOPPED.toString())) {
					establishContext();
					connect();
					if (isBeginExclusive())
						beginExclusive();
				} else
					throw exception;

			}
		}
		return 0;
	}

	// Send APDU commands to the smart card
	public int sendApduCommand(byte[] cApdu) throws Exception {
		byte[] response = null;
		ByteBuffer responseBuffer = ByteBuffer.allocate(65535);

		for (int x = 0; x < 2; x++) {
			try {
				getEventHandler().sendCommandData(cApdu);
				int responseLen = getCardChannel().transmit(ByteBuffer.wrap(cApdu), responseBuffer);
				response = Arrays.copyOfRange(responseBuffer.array(), 0, responseLen);
				getEventHandler().receiveCommandData(response);

				break;
			} catch (CardException exception) {
				if (x < 1 && exception.getCause().getMessage()
						.equals(PcscProvider.CODES.SCARD_E_SERVICE_STOPPED.toString())) {
					establishContext();
					connect();
					if (isBeginExclusive())
						beginExclusive();
				} else
					throw exception;
			}
		}

		return 0;
	}

	// Send APDU commands to the smart card (overloaded function)
	public int sendApduCommand(Apdu cApdu) throws Exception {
		byte[] response;
		ByteBuffer responseBuffer = ByteBuffer.allocate(65535);

		for (int x = 0; x < 2; x++) {
			try {
				getEventHandler().sendCommandData(cApdu.getCommand());
				int responseLen = getCardChannel().transmit(ByteBuffer.wrap(cApdu.getCommand()), responseBuffer);
				response = Arrays.copyOfRange(responseBuffer.array(), 0, responseLen);
				cApdu.setReceiveData(response);
				getEventHandler().receiveCommandData(cApdu.getReceiveData());

				if (responseLen >= Apdu.STATUS_WORD_LENGTH) {
					byte[] statusWord = new byte[Apdu.STATUS_WORD_LENGTH];
					System.arraycopy(response, responseLen - Apdu.STATUS_WORD_LENGTH, statusWord, 0,
							Apdu.STATUS_WORD_LENGTH);
					cApdu.setStatusWord(statusWord);
					cApdu.setReceiveData(Arrays.copyOfRange(response, 0, responseLen - Apdu.STATUS_WORD_LENGTH));
				} else if (responseLen == 1) {
					byte[] statusWord = new byte[] { response[0], 0x00 };
					cApdu.setStatusWord(statusWord);
				}
				break;
			} catch (CardException exception) {
				if (x < 1 && exception.getCause().getMessage()
						.equals(PcscProvider.CODES.SCARD_E_SERVICE_STOPPED.toString())) {
					establishContext();
					if (isBeginExclusive())
						beginExclusive();
				} else
					throw exception;
			}
		}

		return 0;
	}
}
