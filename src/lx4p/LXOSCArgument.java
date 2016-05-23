/**
 * Copyright (c) 2015-2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
 * 
*/

package lx4p;

/** LXOSCArgument
 * 
 * <p>LXOSCArgument class encapsulates possible arguments to an OSC message
 *  supports OSC 1.1 int, float, double, timestamp, string, blob, True, False, Impulse and Null arguments
 *  numbers are internally stored as a double regardless of original type.</p>
*/

public class LXOSCArgument  {
	/**
	 * dv is number value stored as a double
	 */
	double dv;
	/**
	 * sv is string representation
	 */
	String sv;
	/**
	 * bv is byte array for blob type arguments
	 */
	byte[] bv;
	/**
	 * type is the OSC required type of the argument
	 * 0 int
	 * 1 float
	 * 2 double
	 * 3 timestamp (double)
	 * 4 string
	 * 5 blob (byte[])
	 * 6 True
	 * 7 False
	 * 8 Impulse
	 * 9 Null
	 */
	int type;
	
	
	/**
	 * construct an LXOSCArgument from a double
	 * @param d double
	 */
	public LXOSCArgument(double d) {
		dv = d;
		sv = Double.toString(d);
		type = 2;
	}
	
	/**
	 * construct an LXOSCArgument from a float
	 * @param f float
	 */
	public LXOSCArgument(float f) {
		dv = f;
		sv = Float.toString(f);
		type = 1;
	}
	
	/**
	 * construct an LXOSCArgument from an int
	 * @param i int
	 */
	public LXOSCArgument(int i) {
		dv = i;
		sv = Integer.toString(i);
		type = 0;
	}
	
	/**
	 * construct an LXOSCArgument from a String
	 * @param s String
	 */
	public LXOSCArgument(String s) {
		sv = s;
		try {
			dv = Double.parseDouble(s);
		} catch (Exception e) {
			dv = 0;
		}
		type = 4;
	}
	
	/**
	 * construct an LXOSCArgument from a byte array
	 * @param b byte[]
	 */
	public LXOSCArgument(byte[] b) {
		sv = "";
		dv = 0;
		bv = b;
		type = 5;
	}
	
	/**
	 * construct an LXOSCArgument from a double
	 * @param d double
	 * @param timestamp true if type is timestamp, false if type is double
	 */
	public LXOSCArgument(double d, boolean timestamp) {
		dv = d;
		sv = Double.toString(d);
		if ( timestamp ) {
			type = 3;
		} else {
			type = 2;
		}
	}
	
	/**
	 * construct an LXOSCArgument of one of the "no bytes" types
	 * @param typetag character indicating type tag
	 */
	public LXOSCArgument(char typetag) {
		int value = 0;				//value to use if retrieved as a number most likely 1 for T and I and 0 for F and N
		if ( typetag == 'T' ) {
			type = 6;
			value = 1;
			sv = "true";
		} else if ( typetag == 'F' ) {
			type = 7;
			sv = "false";
		} else if ( typetag == 'I' ) {
			type = 8;
			value = 1;
			sv = "";
		} else if ( typetag == 'N' ) {
			type = 9;
			sv = "null";
		} else {
			type = 0;
		}
		dv = value;
	}
	
	/**
	 * construct an LXOSCArgument from a source byte array
	 * @param bsrc source of bytes IMPORTANT size must be at least start+length
	 * @param start beginning index of bytes to take from bsrc
	 * @param length how many bytes to take from bsrc
	 */
	public LXOSCArgument(byte[] bsrc, int start, int length) {
		bv = new byte[length];
		for(int i=0; i<length; i++) {
			bv[i] = bsrc[start+i];
		}
		type = 5;
	}
	
	/**
	 * getTimestamp
	 * @return the argument as a double
	 */
	public double getTimestamp() {
		return dv;
	}
	
	/**
	 * getDouble
	 * @return the argument as a double
	 */
	public double getDouble() {
		return dv;
	}
	
	/**
	 * getFloat
	 * @return the argument as a float
	 */
	public float getFloat() {
		return (float) dv;
	}
	
	/**
	 * getInt
	 * @return the argument as an int
	 */
	public int getInt() {
		return (int) dv;
	}
	
	/**
	 * getString
	 * @return the argument as a String
	 */
	public String getString() {
		return sv;
	}
	
	/**
	 * getBytes
	 * @return the bytes or an empty array if not a bytes type argument
	 */
	public byte[] getBytes() {
		if ( isBytesType() ) {
			return bv;
		}
		return new byte[0];
	}
	
	/**
	 * isIntType
	 * @return true if argument was originally an nteger
	 */
	public boolean isIntType() {
		return (type == 0);
	}
	
	/**
	 * isFloatType
	 * @return true if argument was originally a float
	 */
	public boolean isFloatType() {
		return (type == 1);
	}
	
	/**
	 * isDoubleType
	 * @return true if argument was originally a double
	 */
	public boolean isDoubleType() {
		return (type == 2);
	}
	
	/**
	 * isTimestampType
	 * @return true if argument was originally a timestamp
	 */
	public boolean isTimestampType() {
		return (type == 3);
	}
	
	/**
	 * isNumberType
	 * @return true if argument was originally a number
	 */
	public boolean isNumberType() {
		return (type < 4);
	}
	
	/**
	 * isStringType
	 * @return true if argument was originally a string
	 */
	public boolean isStringType() {
		return (type == 4);
	}
	
	/**
	 * isBytesType
	 * @return true if argument was originally bytes
	 */
	public boolean isBytesType() {
		return (type == 5);
	}
	
	/**
	 * isTrueType
	 * @return true if argument was originally True
	 */
	public boolean isTrueType() {
		return (type == 6);
	}
	
	/**
	 * isFalseType
	 * @return true if argument was originally False
	 */
	public boolean isFalseType() {
		return (type == 7);
	}
	
	/**
	 * isImpulseType
	 * @return true if argument was originally Impulse
	 */
	public boolean isImpulseType() {
		return (type == 8);
	}
	
	/**
	 * isNullType
	 * @return true if argument was originally Null
	 */
	public boolean isNullType() {
		return (type == 9);
	}

}