package AcsCommon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Chain3DES {

	final static public int KEY_LENGTH_3DES = 24;
	final static public int RANDOM_DATA_LENGTH = 8;
	public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";

	private static Cipher _cipher;
	private static KeySpec _myKeySpec;
	private static SecretKeyFactory _mySecretKeyFactory;
	private static String _myEncryptionScheme;
	private static SecretKey _key;
	private static String transformation = "DESede/CBC/NoPadding";

	// MAC as defined in ACOS manual
	// receives 8-byte Key and 16-byte Data
	// result is stored in Data
	public static void singleMac(byte[] data, byte[] key) {
		int index;

		des(data, key);
		for (index = 0; index < 8; index++)
			data[index] = data[index] ^= data[index + 8];

		des(data, key);
	}

	public static void des(byte data[], byte key[]) {
		DESKeySpec desKeySpec;
		SecretKeyFactory keyFactory;
		SecretKey secretKey;
		Cipher encryptCipher;
		byte[] temporaryKey = new byte[8];
		byte encryptedContents[];
		int index;

		for (index = 0; index < 8; index++) {
			temporaryKey[index] = key[index];
		}
		try {
			desKeySpec = new DESKeySpec(temporaryKey);
			keyFactory = SecretKeyFactory.getInstance("DES");
			secretKey = keyFactory.generateSecret(desKeySpec);
			encryptCipher = Cipher.getInstance("DES");

			encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

			encryptedContents = process(data, encryptCipher);

			for (index = 0; index < 8; index++) {
				data[index] = encryptedContents[index];
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	private static byte[] process(byte processMe[], Cipher cipher) throws Exception {
		// Create the input stream to be used for encryption
		ByteArrayInputStream in = new ByteArrayInputStream(processMe);

		// Now actually encrypt the data and put it into a
		// ByteArrayOutputStream so we can pull it out easily.
		CipherInputStream processStream = new CipherInputStream(in, cipher);
		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
		int whatWasRead = 0;

		while ((whatWasRead = processStream.read()) != -1) {
			resultStream.write(whatWasRead);
		}

		return resultStream.toByteArray();
	}

	// Triple MAC as defined in ACOS manual
	// receives 16-byte Key and 16-byte Data
	// result is stored in Data
	public static byte[] tripleMac(byte[] data, byte[] key) {
		int index;
		byte[] testByte = new byte[data.length];

		System.arraycopy(data, 0, testByte, 0, 16);

		data = tripleDes(data, key);

		System.arraycopy(testByte, 8, data, 8, data.length - 8);

		for (index = 0; index < 8; index++)
			data[index] = data[index] ^= data[index + 8];

		data = tripleDes(data, key);
		System.arraycopy(testByte, 8, data, 8, data.length - 8);
		return data;
	}

	public static byte[] tripleDes(byte[] data, byte[] key) {
		Cipher cipher;
		SecretKeySpec myKey;
		byte[] result = null;
		byte[] temporaryKey = new byte[24];

		try {
			System.arraycopy(key, 0, temporaryKey, 0, 16);
			System.arraycopy(key, 0, temporaryKey, 16, 8);

			cipher = Cipher.getInstance("DESede/ECB/NoPadding");
			myKey = new SecretKeySpec(temporaryKey, "DESede");

			cipher.init(Cipher.ENCRYPT_MODE, myKey);

			try {

				result = cipher.doFinal(data);
			} catch (IllegalBlockSizeException | BadPaddingException exception) {
				exception.printStackTrace();
			}

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	public static SecretKey createKey(byte[] myEncryptionKey) throws Exception {
		byte[] temporaryKey = new byte[KEY_LENGTH_3DES];

		try {
			System.arraycopy(myEncryptionKey, 0, temporaryKey, 0, 16);
			System.arraycopy(myEncryptionKey, 0, temporaryKey, 16, 8);

			_myEncryptionScheme = DESEDE_ENCRYPTION_SCHEME;
			_myKeySpec = new DESedeKeySpec(temporaryKey, 0);
			_mySecretKeyFactory = SecretKeyFactory.getInstance(_myEncryptionScheme);
			_key = _mySecretKeyFactory.generateSecret(_myKeySpec);
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return _key;
	}

	public static byte[] encrypt(byte[] plainText, IvParameterSpec iv, SecretKey key) {
		byte[] encryptedText = new byte[RANDOM_DATA_LENGTH];
		byte[] data = new byte[RANDOM_DATA_LENGTH];

		try {
			_cipher = Cipher.getInstance(transformation);
			_cipher.init(Cipher.ENCRYPT_MODE, key, iv);

			encryptedText = _cipher.doFinal(plainText);
			for (int index = 0; index < RANDOM_DATA_LENGTH; index++) {
				data[index] = encryptedText[index];
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return data;
	}

	public static byte[] decrypt(byte[] encryptedData, IvParameterSpec iv, SecretKey key) {
		byte[] plainText = null;
		try {
			_cipher = Cipher.getInstance(transformation);
			_cipher.init(Cipher.DECRYPT_MODE, key, iv);

			plainText = _cipher.doFinal(encryptedData);
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return plainText;
	}
}
