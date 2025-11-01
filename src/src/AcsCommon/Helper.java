package AcsCommon;

import java.util.regex.Pattern;

public class Helper {

	public static final int BYTE_SIZE = 4;
	public static final int LITTLE_ENDIAN_FIRST_SHIFT = 24;
	public static final int LITTLE_ENDIAN_SECOND_SHIFT = 16;
	public static final int LITTLE_ENDIAN_THIRD_SHIFT = 8;

	public static boolean isHexStringValid(String input) {
		return Pattern.matches("^[0-9A-Fa-f]*$", input);
	}

	public static String byteAsString(byte[] data, boolean spaceInBetween) {
		String temporaryString = "";
		int index = 0;

		if (data == null)
			return "";

		for (index = 0; index < data.length; index++)
			temporaryString += String.format((spaceInBetween ? "%02X " : "%02X"), data[index]);

		return temporaryString;
	}

	public static byte[] getBytes(String stringBytes, String delimeter) {
		String[] arrayString = stringBytes.split(delimeter);
		byte[] bytesResult = new byte[arrayString.length];

		for (int index = 0; index < arrayString.length; index++)
			bytesResult[index] = (byte) ((Integer) Integer.parseInt(arrayString[index], 16)).byteValue();

		return bytesResult;
	}

	public static byte[] getBytes(String stringBytes) {
		String formattedString = "";
		int counter = 0;

		if (stringBytes.trim() == "")
			return null;

		for (int i = 0; i < stringBytes.length(); i++) {
			if (stringBytes.charAt(i) == ' ')
				continue;

			if (counter > 0 && counter % 2 == 0)
				formattedString += " ";

			formattedString += stringBytes.charAt(i);

			counter++;
		}

		return getBytes(formattedString, " ");
	}

	public static String byteArrayToString(byte[] b, int startIndx, int len, boolean spaceInBetween) {
		byte[] newByte;

		if (b.length < startIndx + len)
			b = new byte[startIndx + len];

		newByte = new byte[len];

		for (int i = 0; i < len; i++, startIndx++)
			newByte[i] = b[startIndx];

		return byteArrayToString(newByte, spaceInBetween);
	}

	public static String byteArrayToString(byte[] tmpBytes, boolean spaceInBetween) {
		String tmpStr = "", tmpStr2 = "";

		if (tmpBytes == null)
			return "";

		for (int i = 0; i < tmpBytes.length; i++) {
			tmpStr = Integer.toHexString(((Byte) tmpBytes[i]).intValue() & 0xFF).toUpperCase();

			// For single character hex
			if (tmpStr.length() == 1)
				tmpStr = "0" + tmpStr;

			tmpStr2 += " " + tmpStr;
		}

		return tmpStr2;
	}

	public static String byteArrayToString(byte[] data, int length) {
		String string = "";
		int index = 0;

		while ((data[index] & 0xFF) != 0x00) {
			string += (char) (data[index] & 0xFF);
			index++;
			if (index == length)
				break;
		}

		return string;
	}

	public static int byteToInt(byte[] data) {
		byte[] holder = new byte[4];

		if (data == null)
			return -1;

		// Make sure that the array size is 4
		System.arraycopy(data, 0, holder, 4 - data.length, data.length);

		return (((holder[0] & 0xFF) << 24) + ((holder[1] & 0xFF) << 16) + ((holder[2] & 0xFF) << 8)
				+ (holder[3] & 0xFF));
	}

	public static boolean byteArrayIsEqual(byte[] array1, byte[] array2, int length) {
		if (array1.length < length)
			return false;

		if (array2.length < length)
			return false;

		for (int i = 0; i < length; i++) {
			if (array1[i] != array2[i])
				return false;
		}

		return true;
	}

	public static boolean byteArrayIsEqual(byte[] array1, byte[] array2) {
		return byteArrayIsEqual(array1, array2, array2.length);
	}

	public static byte[] appendArrays(byte[] arr1, byte[] arr2) {
		byte[] c = new byte[arr1.length + arr2.length];

		for (int i = 0; i < arr1.length; i++)
			c[i] = arr1[i];

		for (int i = 0; i < arr2.length; i++)
			c[arr1.length + i] = arr2[i];

		return c;

	}
	
	public static byte[] intToByte(int number)
	{
		byte[] data = new byte[4];

		data[0] = (byte)((number >> 24) & 0xFF);
		data[1] = (byte)((number >> 16) & 0xFF);
		data[2] = (byte)((number >> 8) & 0xFF);
		data[3] = (byte)(number & 0xFF);

		return data;
	}
}
