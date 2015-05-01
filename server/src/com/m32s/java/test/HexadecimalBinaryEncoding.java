package com.m32s.java.test;

import ie.omk.smpp.util.AlphabetEncoding;

public class HexadecimalBinaryEncoding extends AlphabetEncoding
{
	private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	protected HexadecimalBinaryEncoding()
	{
		super(-1);
	}
	
	@Override
	public String decodeString(byte[] b)
	{
		return binaryArrayToHexadecimalText(b);
	}
	
	@Override
	public byte[] encodeString(String s)
	{
		String raw = s.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "").replaceAll("\t", "");
		byte[] arr = new byte[raw.length()/2];
		int p = 0;
		for(int i = 0, n = raw.length(); i < n; i += 2)
		{
			String byteStr = raw.substring(i, i + 2);
			// need to parse via Integer because values greater than 0x7F are possible
			byte byteVal = (byte)Integer.parseInt(byteStr, 16);
			arr[p++] = byteVal;
		}
		return arr;
	}

	public static final String binaryArrayToHexadecimalText(byte data[])
	{
		return binaryArrayToHexadecimalText(data, 0, data.length);
	}

	public static final String binaryArrayToHexadecimalText(byte data[], int off)
	{
		return binaryArrayToHexadecimalText(data, off, data.length - off);
	}
	
	/**
	 * Returns a string representation of the byte argument as an
     * unsigned integer in base&nbsp;16.
     * 
	 * @param b byte to convert
	 * @return the string representation of the byte value represented by the argument in hexadecimal
	 */
	public static final String byteToHex(byte b)
	{
		char digits[] = new char[2];
		int idx = ((int)(b) & 0xF0) >> 4;
		digits[0] = HEX_DIGITS[idx];
		idx = (int)(b) & 0x0F;
		digits[1] = HEX_DIGITS[idx];
		return new String(digits);
	}

	/**
	 * Converts specified byte subarray into string hexadecimal representation.
	 * 
	 * @param data an array to be converted to a string
	 * @param off the index of the first element to convert
	 * @param count the number of element to convert
	 * @return the string representation of byte array in base 16
	 */
	public static final String binaryArrayToHexadecimalText(byte data[], int off, int count)
	{
		/* binary */
		StringBuffer bb = new StringBuffer(count * 2);
		int toIdx = off + count;
		for (int i = off; i < toIdx; i++)
		{
			bb.append(byteToHex(data[i]));
		}
		return bb.toString();
	}
}