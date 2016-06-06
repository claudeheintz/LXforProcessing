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

import java.util.*;

/** LXOSCMessage
 * 
 * <p>Encapsulates an OSC message including its address pattern and arguments
 * supports OSC 1.1 int, float, double, timestamp, string, blob, True, False, Impulse and Null arguments.</p>
*/

public class LXOSCMessage  {
	
	Vector<String> _address_pattern;
	Vector<LXOSCArgument> _arguments;
	boolean _inBundle;

	/**
	 * construct empty OSC message
	 */
	public LXOSCMessage() {
		_address_pattern = new Vector<String>();
		_arguments = new Vector<LXOSCArgument>();
	}
	
	/**
	 * construct OSC message with list of address pattern parts
	 */
	public LXOSCMessage(Vector<String> addressPattern) {
		_address_pattern = addressPattern;
		_arguments = new Vector<LXOSCArgument>();
	}
	
	/**
	 * construct OSC message with list of address pattern parts
	 */
	public LXOSCMessage(String pstr) {
		setAddressPattern(pstr);
		_arguments = new Vector<LXOSCArgument>();
	}
	
	/**
	 * Retrieve the address pattern
	 * @return vector of strings composing address pattern
	 */
	public Vector<String> addressPattern() {
		return _address_pattern;
	}
	
	/**
	 * Set the address pattern list
	 * @param addressPattern list of address pattern parts
	 */
	public void setAddressPattern(Vector<String> addressPattern) {
		_address_pattern = addressPattern;
	}
	
	/**
	 * Set the address pattern from a String
	 * @param pstr address pattern string with parts separated by forward slashes
	 */
	public void setAddressPattern(String pstr) {
		setAddressPattern(addressPatternStringToParts(pstr));
	}
	
	/**
	 * utility for creating an address pattern list from a string
	 * @param pstr
	 * @return Vector of strings derived from separating the input string into elements separated by forward slashes
	 */
	public static Vector<String> addressPatternStringToParts(String pstr) {
		Vector<String> rv = new Vector<String>();
		String[] ta = pstr.split("/");
		String estr;
		for (int j=0; j<ta.length; j++) {
			estr = ta[j];
			if ( estr.length() > 0 ) {
				rv.addElement(estr);
			}
		}
		return rv;
	}
	
	/**
	 * adds a string to the address pattern
	 * @param astr
	 */
	public void addAddressPart(String astr) {
		_address_pattern.addElement(astr);
	}
	
	/**
	 * get the nth element of the address pattern
	 * @param n index of element eg /0.../1.../2.../3...
	 * @return String element of address pattern
	 */
	public String addressPartAt(int n) {
		if ( n < _address_pattern.size() ) {
			return _address_pattern.elementAt(n);
		}
		return "";	//return null string so equals does not fail
	}
	
	/**
	 * compare address pattern vector to message address pattern
	 * @param testPattern vector of pattern matching elements
	 * @return true if all address elements match pattern elements
	 */
	public boolean matchesAddressPattern(Vector<String> testPattern) {
		if ( _address_pattern.size() < testPattern.size() ) {
			return false;									// testPattern larger can't possibly match
		}
		for ( int i=0; i<testPattern.size(); i++ ) {
			if ( ! addressPartMatchesPatternPart(_address_pattern.elementAt(i), testPattern.elementAt(i)) ) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * compare osc address vector to message address pattern
	 * @param testAddress vector of osc address
	 * @return true if all address pattern parts match pattern elements
	 */
	public boolean matchesOSCAddress(Vector<String> testAddress) {
		if ( _address_pattern.size() != testAddress.size() ) {
			return false;									  // sizes must match
		}
		for ( int i=0; i<testAddress.size(); i++ ) {
			if ( ! addressPartMatchesPatternPart(testAddress.elementAt(i), _address_pattern.elementAt(i)) ) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * checks to see if test pattern part matches part of address pattern
	 * @param apart	element of address pattern
	 * @param ppart element of test pattern
	 * @return true if the part of an address 
	 */
	public static boolean addressPartMatchesPatternPart(String apart, String ppart) {
		if ( apart.equals(ppart) ) {
			return true;
		}
		int ai = 0;
		int pj = 0;
		while ( (ai<apart.length()) && (pj<ppart.length())) {
			if ( ppart.charAt(pj) ==  '*' ) {
				pj++;
				if ( pj == ppart.length() ) {
					return true;				// wildcard matches to end
				}
				char nextpchar = ppart.charAt(pj);	
				while ( apart.charAt(ai) != nextpchar ) {	//match until next pattern char encountered
					ai++;
					if ( ai >= apart.length() ) {
						return false;	//ran out of characters
					}
				}
				ai++;
				pj++;
			} else {		// pattern char not *
				if ( ppart.charAt(pj) == apart.charAt(ai) ) {
					ai++;
					pj++;
				} else if ( ppart.charAt(pj) == '?' ) { // single char wildcard always matches
					ai++;
					pj++;
				} else if ( ppart.charAt(pj) == '[' ) {
					pj++;
					int closeBracketIndex = ppart.indexOf(']', pj);
					if ( closeBracketIndex < 0 ) {
						return false;	//invalid list;
					}
					if ( ! addressCharMatchesBracketPattern(apart.charAt(ai) , ppart.substring(pj, closeBracketIndex)) ) {
						return false;
					}
					ai++;
					pj = closeBracketIndex+1;
				} else if ( ppart.charAt(pj) == '{' ) {
					return addressPartMatchesBracePatternPart(apart.substring(ai) , ppart.substring(pj+1));
				} else {			// single character not matched
					return false;
				}
			}
		}
		
		if ( ai==apart.length() ) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * matches a set of characters from an address pattern to a character from an address
	 * @param achar character to match
	 * @param blist set of characters to match
	 * @return true if set matches character (or does not match if negated with leading !)
	 */
	public static boolean addressCharMatchesBracketPattern(char achar, String blist) {
		String mblist = blist;
		boolean negate = false;
		if ( blist.startsWith("!")) {
			negate = true;
			mblist = blist.substring(1);
		}
		int cindex = mblist.indexOf(achar);
		if ( cindex >= 0 ) {				//can be true if start or end of range
			if ( negate ) {
				return false;
			}
			return true;
		}
		
		// handle ranges (eg. a-f) here
		int dashIndex = 1;
		int nDashIndex = 0;
		boolean done = mblist.length() < 3;
		while ( ! done ) {
			nDashIndex = mblist.indexOf("-", dashIndex);
			if ( nDashIndex > 0 ) {
				if ( mblist.length() == dashIndex + 1 ) {
					return false;	//no character after dash, bad pattern is false despite negate
				}
				if (( mblist.charAt(nDashIndex-1) < achar ) && ( achar <  mblist.charAt(nDashIndex+1) )) {
					if ( negate ) {
						return false;
					}
					return true;
				}
				dashIndex = nDashIndex+1;	//check for more dashes
				if ( dashIndex+1 >= mblist.length() ) {
					done = true;
				}
			} else {
				done = true;	//no dash, end loop
			}
		}
		
		if ( negate ) {
			return true;
		}
		return false;
	}
	
	/**
	 * matches substring of address pattern after brace { to an address string
	 * @param apart	substring of part of an OSC address
	 * @param ppart substring of part of an OSC address pattern following a left brace {
	 * @return true if one of the comma separated strings inside the braces and remainder of both strings are matched
	 */
	public static boolean addressPartMatchesBracePatternPart(String apart, String ppart) {
		int closeBraceIndex = ppart.indexOf('}');
		if ( closeBraceIndex < 0 ) {
			return false;	//invalid list;
		}
		// compare apart to see if it starts with any of the strings inside the braces
		String[] sa = ppart.substring(0,closeBraceIndex).split(",");
		for(int i=0; i<sa.length; i++) {
			if (apart.startsWith(sa[i])) {
				if ( apart.length() == sa[i].length() ) {	//complete match
					return true;
				} else if (ppart.length() > closeBraceIndex + 1  ) {	//both parts have more characters, compare the rest as if they are whole parts
					return addressPartMatchesPatternPart( apart.substring(sa[i].length()), ppart.substring(closeBraceIndex+1) );
				}
			}
		}
		return false;	//didn't match
	}
	/**
	 * compare address pattern string to message OSC address
	 * @param testString
	 * @return true if all address parts match
	 */
	public boolean matchesAddressPattern(String testString) {
		return  matchesAddressPattern( addressPatternStringToParts(testString) );
	}
	
	/**
	 * compare specific part of address pattern against part of an address string
	 * @param p index of part to match
	 * @param s string to match
	 * @return true if part of message's address pattern matches string
	 */
	public boolean partOfPatternMatchesAddressString(int p, String s) {
		return addressPartMatchesPatternPart(s, _address_pattern.elementAt(p));
	}
	
	/**
	 * compare OSC address string to message address pattern 
	 * @param testString
	 * @return true if all address parts match
	 */
	public boolean matchesOSCAddress(String testString) {
		return  matchesOSCAddress( addressPatternStringToParts(testString) );
	}
	
	/**
	 * Add a pre-made argument
	 * @param arg
	 */
	public void addArgument(LXOSCArgument arg) {
		_arguments.addElement(arg);
	}
	
	/**
	 * Add an integer argument
	 * @param arg int value to add
	 */
	public void addArgument(int arg) {
		_arguments.addElement(new LXOSCArgument(arg));
	}
	
	/**
	 * Add a float argument
	 * @param arg float value to add
	 */
	public void addArgument(float arg) {
		_arguments.addElement(new LXOSCArgument(arg));
	}
	
	/**
	 * Add a double argument
	 * @param arg double value to add
	 */
	public void addArgument(double arg) {
		_arguments.addElement(new LXOSCArgument(arg));
	}
	
	/**
	 * Add a double argument optionally marked as a timestamp type
	 * @param arg double value to add
	 * @param timestamp true if type is timestamp
	 */
	public void addArgument(double arg, boolean timestamp) {
		_arguments.addElement(new LXOSCArgument(arg, timestamp));
	}
	
	/**
	 * Add a String argument
	 * @param arg string to add
	 */
	public void addArgument(String arg) {
		_arguments.addElement(new LXOSCArgument(arg));
	}
	
	/**
	 * Add a bytes type argument
	 * @param bsrc source of bytes
	 * @param start starting index in source
	 * @param length length of bytes to include from source
	 */
	public void addArgument(byte[] bsrc, int start, int length) {
		_arguments.addElement(new LXOSCArgument(bsrc, start, length));
	}
	
	/**
	 * add a "no bytes" typetag argument  The
	 * @param typetag 'T', 'F', 'I', or 'N'
	 */
	public void addArgument(char typetag) {
		_arguments.addElement(new LXOSCArgument(typetag));
	}
	
	/**
	 * retrieve an argument
	 * @param index of the argument
	 * @return the argument or null if the index is out of range
	 */
	public LXOSCArgument argumentAt(int index) {
		if ( argumentExistsAt(index) ) {
			return _arguments.elementAt(index);
		}
		return null;
	}
	
	/**
	 * checks to see if an argument exists
	 * @param index of the argument
	 * @return true if index is valid
	 */
	public boolean argumentExistsAt(int index) {
		return ( (index >= 0 ) && ( index < _arguments.size()) );
	}
	
	/**
	 * @return the number of arguments
	 */
	public int argumentCount() {
		return _arguments.size();
	}
	
	/**
	 * Gets the argument as an int 
	 * @param index of the argument
	 * @return int value of the argument or 0
	 */
	public int intAt(int index) {
		int rv = 0;
		if ( argumentExistsAt(index) ) {
			rv = _arguments.elementAt(index).getInt();
		}
		return rv;
	}
	
	/**
	 * Gets the value of the argument as a double
	 * @param index of the argument
	 * @return double value of the argument or 0
	 */
	public double doubleAt(int index) {
		double rv = 0;
		if ( argumentExistsAt(index) ) {
			rv = _arguments.elementAt(index).getDouble();
		}
		return rv;
	}
	
	/**
	 * Gets the value of the argument as an float
	 * @param index of the argument
	 * @return float value of the argument or zero
	 */
	public float floatAt(int index) {
		float rv = 0;
		if ( argumentExistsAt(index) ) {
			rv = _arguments.elementAt(index).getFloat();
		}
		return rv;
	}
	
	/**
	 * Gets the the value of the argument as an String
	 * @param index of the argument
	 * @return String value of the argument or empty string
	 */
	public String stringAt(int index) {
		String rv = "";
		if ( argumentExistsAt(index) ) {
			rv = _arguments.elementAt(index).getString();
		}
		return rv;
	}
	
	/**
	 * Gets the the value of the argument as an byte[]
	 * @param index of the argument
	 * @return byte[] contents of the argument in list or an empty array if there is no  argument or the argument is not a byte[] type
	 */
	public byte[] byteArrayAt(int index) {
		byte[] rv;
		if ( argumentExistsAt(index) ) {
			rv = _arguments.elementAt(index).getBytes();
		} else {
			rv = new byte[0];
		}	
		return rv;
	}
	
	/**
	 * Adds the complete message to a byte[] using OSC protocol
	 * @param buffer byte array to write into.  Must be large enough to hold address pattern, type list and arguments
	 * @return length of complete OSC packet or -1 if there's an error
	 */
	public int addOSCMessageToBytes(byte[] buffer) {
		// add the address pattern
		int ci = addAddressPatternToBytes(buffer);
		if ( ci < buffer.length ) {
			
			//add the argument types
			ci = addArgumentTypeTagsToBytes(buffer, ci);
			if ( ci < buffer.length ) {
				
				//add the arguments themselves
				int argscount = _arguments.size();
				LXOSCArgument carg;
				for (int k=0; k<argscount; k++ ) {
					carg = _arguments.elementAt(k);
					if ( carg.isIntType() ) {
						ci = addIntArgumentToBytes(buffer, ci, _arguments.elementAt(k));
					} else if ( carg.isDoubleType() ) {
						ci = addDoubleArgumentToBytes(buffer, ci, _arguments.elementAt(k));
					} else if ( carg.isTimestampType() ) {
						ci = addDoubleArgumentToBytes(buffer, ci, _arguments.elementAt(k));
					} else if ( carg.isStringType() ) {
						ci = addStringArgumentToBytes(buffer, ci, _arguments.elementAt(k));
					} else if ( carg.isBytesType() ) {
						ci = addBytesArgumentToBytes(buffer, ci, _arguments.elementAt(k));
					} else if ( carg.isTrueType() ) {
						//no bytes for this type tag
					} else if ( carg.isFalseType() ) {
						//no bytes for this type tag
					} else if ( carg.isImpulseType() ) {
						//no bytes for this type tag
					} else if ( carg.isNullType() ) {
						//no bytes for this type tag
					} else {
						ci = addFloatArgumentToBytes(buffer, ci, _arguments.elementAt(k));	//float is default
					}
					if ( ci >= buffer.length ) {
						ci = -1;
						break;
					}
				}
			} else {
				ci = -1;
			}
		} else {
			ci = -1;
		}
		return ci;
	}
	
	/**
	 * Adds the address pattern to beginning of byte[]
	 * @param buffer byte[] to hold address pattern
	 * @return length of address pattern plus padding if needed
	 */
	public int addAddressPatternToBytes(byte[] buffer) {
		int ci = 0;
		int ce;
		int k;
		String estr;
		Enumeration<String> en = addressPattern().elements();
		while ( en.hasMoreElements() ) {
			estr = en.nextElement();
			ce = estr.length();
			k = 0;
			if ( ci + ce + 8 > buffer.length ) {
				return buffer.length; // address pattern cannot be sent, its too long
			}
			
			buffer[ci] = (byte) '/';	//add slash before address pattern element
			ci++;
			
			while (k<ce) {
				buffer[ci] = (byte) estr.charAt(k);
				ci++;
				k++;
			}
		}
		buffer[ci] = 0;	//zero terminate string
		ci++;
		
		int pad = (ci % 4);
		if ( pad != 0 ) {
			pad = 4 - pad;
			pad = ci + pad;
			for ( ; ci<pad; ci++ ) {
				buffer[ci] = 0;	//pad to even 4 bytes
			}			
		}
		return ci;
	}
	
	/**
	 * Adds the argument type tags to byte[] starting at specified index (following address pattern)
	 * @param buffer byte[] to hold type list
	 * @param start index for ',' starting type tags
	 * @return index of byte in buffer following type tags including padding if necessary
	 */
	public int addArgumentTypeTagsToBytes(byte[] buffer, int start) {
		int argscount = _arguments.size();
		if ( start + argscount + 6 > buffer.length ) {
			return buffer.length;    // cannot be sent, its too long
		}
		int ci = start;
		
		buffer[ci] = ',';
		ci++;
		
		LXOSCArgument carg;
		for (int k=0; k<argscount; k++ ) {
			carg = _arguments.elementAt(k);
			if ( carg.isIntType() ) {
				buffer[ci] = 'i';
			} else if ( carg.isDoubleType() ) {
				buffer[ci] = 'd';
			} else if ( carg.isTimestampType() ) {
				buffer[ci] = 't';
			} else if ( carg.isStringType() ) {
				buffer[ci] = 's';
			} else if ( carg.isBytesType() ) {
				buffer[ci] = 'b';
			} else if ( carg.isTrueType() ) {
				buffer[ci] = 'T';
			} else if ( carg.isFalseType() ) {
				buffer[ci] = 'F';
			} else if ( carg.isImpulseType() ) {
				buffer[ci] = 'I';
			} else if ( carg.isNullType() ) {
				buffer[ci] = 'N';
			} else {
				buffer[ci] = 'f';	//float is default
			}
			ci++;
		}
		
		buffer[ci] = 0;		//zero terminate string
		ci++;
		
		int pad = (ci % 4);
		if ( pad != 0 ) {
			pad = 4 - pad;
			pad = ci + pad;
			for ( ; ci<pad; ci++ ) {
				buffer[ci] = 0;	//pad to even 4 bytes
			}			
		}
		
		return ci;
	}
	
	/**
	 * * Adds an integer argument from arguments Vector to byte[] starting at specified byte index
	 * @param buffer byte[] to hold argument
	 * @param start index in buffer to start writing argument
	 * @param arg argument to be added
	 * @return index of byte in buffer following the 4 bytes for the argument
	 */
	public int addIntArgumentToBytes(byte[] buffer, int start, LXOSCArgument arg) {
		if ( start + 4 > buffer.length ) {
			return buffer.length;    // cannot be sent, not enough room left
		}
		int ci = start;
		
		int bits = arg.getInt();
		buffer[ci]  = (byte)((bits >> 24) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 16) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 8) & 0xff);
		ci++;
		buffer[ci]  = (byte)(bits & 0xff);
		ci++;
		
		return ci;
	}
	
	/**
	 * * Adds a float argument from arguments Vector to byte[] starting at specified byte index
	 * @param buffer byte[] to hold argument
	 * @param start index in buffer to start writing argument
	 * @param arg argument to be added
	 * @return index of byte in buffer following the 4 bytes for the argument
	 */
	public int addFloatArgumentToBytes(byte[] buffer, int start, LXOSCArgument arg) {
		if ( start + 4 > buffer.length ) {
			return buffer.length;    // cannot be sent, not enough room left
		}
		int ci = start;
		
		int bits = Float.floatToIntBits(arg.getFloat());
		buffer[ci]  = (byte)((bits >> 24) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 16) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 8) & 0xff);
		ci++;
		buffer[ci]  = (byte)(bits & 0xff);
		ci++;
		
		return ci;
	}
	
	/**
	 * * Adds a double argument from arguments Vector to byte[] starting at specified byte index
	 * @param buffer byte[] to hold argument
	 * @param start index in buffer to start writing argument
	 * @param arg argument to be added
	 * @return index of byte in buffer following the 8 bytes for the argument
	 */
	public int addDoubleArgumentToBytes(byte[] buffer, int start, LXOSCArgument arg) {
		if ( start + 4 > buffer.length ) {
			return buffer.length;    // cannot be sent, not enough room left
		}
		int ci = start;
		
		long bits = Double.doubleToLongBits(arg.getDouble());
		buffer[ci]  = (byte)((bits >> 56) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 48) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 40) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 32) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 24) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 16) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 8) & 0xff);
		ci++;
		buffer[ci]  = (byte)(bits & 0xff);
		ci++;
		
		return ci;
	}
	
	/**
	 * * Adds an String argument from arguments Vector to byte[] starting at specified byte index
	 * @param buffer byte[] to hold argument
	 * @param start index in buffer to start writing argument
	 * @param arg argument to be added
	 * @return index of byte in buffer following string argument including padding if necessary
	 */
	public int addStringArgumentToBytes(byte[] buffer, int start, LXOSCArgument arg) {
		String astr = arg.getString();
		if ( start + astr.length() + 4 > buffer.length ) {
			return buffer.length;    // cannot be sent, not enough room left
		}
		
		int ci = start;
		for (int k=0; k<astr.length(); k++ ) {
			buffer[ci] = (byte) astr.charAt(k);
			ci++;
		}
		
		int pad = (ci % 4);
		if ( pad != 0 ) {
			pad = 4 - pad;
			pad = ci + pad;
			for ( ; ci<pad; ci++ ) {
				buffer[ci] = 0;	//pad to even 4 bytes
			}			
		}
		
		return ci;
	}
	
	/**
	 * * Adds an Bytes argument from arguments Vector to byte[] starting at specified byte index
	 * @param buffer byte[] to hold argument
	 * @param start index in buffer to start writing argument
	 * @param arg argument to be added
	 * @return index of byte in buffer following string argument including padding if necessary
	 */
	public int addBytesArgumentToBytes(byte[] buffer, int start, LXOSCArgument arg) {
		byte[] bytes = arg.getBytes();
		if ( start + bytes.length + 8 > buffer.length ) {
			return buffer.length;    // cannot be sent, not enough room left
		}
		
		int ci = start;
		int bits = bytes.length;
		buffer[ci]  = (byte)((bits >> 24) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 16) & 0xff);
		ci++;
		buffer[ci]  = (byte)((bits >> 8) & 0xff);
		ci++;
		buffer[ci]  = (byte)(bits & 0xff);
		ci++;
		
		for (int k=0; k<bytes.length; k++ ) {
			buffer[ci] = bytes[k];
			ci++;
		}
		
		int pad = (ci % 4);
		if ( pad != 0 ) {
			pad = 4 - pad;
			pad = ci + pad;
			for ( ; ci<pad; ci++ ) {
				buffer[ci] = 0;	//pad to even 4 bytes
			}			
		}
		
		return ci;
	}
}