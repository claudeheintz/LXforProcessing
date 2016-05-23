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

/** LXOSCPacketReader
 * 
 * <p>LXOSCPacketReader extracts LXOSCMessages from a packet of bytes</p>
*/

public class LXOSCPacketReader  {
	public static final int OSC_BUFFER_MAX = 400;
	Vector<LXOSCMessage> _results;
	boolean _inBundle;
	
	/**
	 * construct a LXOSCPacketReader with an empty list of messages
	 */
	LXOSCPacketReader () {
		_results = new Vector<LXOSCMessage>();
	}
	
	/**
	 * results
	 * @return list of messages after parsing the bytes
	 */
	public Vector<LXOSCMessage> results() {
		return _results;
	}
	
	/**
	 * 
	 * @param buffer byte[] containing the packet to be read
	 * @param msglength length of packet (may be smaller that the whole buffer)
	 * @param buffer_max the size of the buffer
	 */
	public void parseBuffer(byte[] buffer, int msglength, int buffer_max) {
		int dataindex = 0;
		while ( (dataindex >= 0 ) && ( dataindex < msglength ) )   {
			dataindex = processMessageAt(buffer, dataindex, msglength, buffer_max);
		}
	}
	
	/**
	 * Process a number of bytes of a message (a bundle can contain more than one message in a single packet)
	 * @param buffer byte[] containing the packet to be read
	 * @param beginindex index of starting byte in buffer to read
	 * @param msglength length of packet (may be smaller that the whole buffer)
	 * @param buffer_max the size of the buffer
	 * @return index of location in byte array at end of processed message
	 */
	public int processMessageAt(byte[] buffer, int beginindex, int msglength, int buffer_max) {
		int outindex = 0;
		int dataloc = 0;
    
    	int start = beginindex; 
		 if ( _inBundle ) {
			  start += 4;             // ignore 4 bytes bundle element size
		 }
		int zeroloc = nextLocationOfChar(buffer, '\0', start, msglength, buffer_max);
	
		if ( zeroloc + 4 < msglength ) {	//insure that cstring will terminate with room for one argument
			String addressPattern = new String(buffer, start, zeroloc-start);
			if ( addressPattern.startsWith("/") ) {
			
				int typeloc = nextIndexForString(addressPattern, start);
				dataloc = nextIndexForIndex( nextLocationOfChar(buffer, '\0', typeloc, msglength, buffer_max));
			
			
				if ( dataloc+4 <= msglength ) {
					if ( buffer[typeloc] == ',' ) {
						typeloc++;
					}
					boolean done = false;
					LXOSCMessage oscmessage = new LXOSCMessage(addressPattern);
				
					while (( dataloc + 4 <=  msglength ) && ( ! done )) {
						if ( buffer[typeloc] == 0 ) {
							done = true;
						} else if ( buffer[typeloc] == 'f' ) {
							float data = (float) decode_ieee_single(buffer, dataloc, true);
							oscmessage.addArgument(data);
							dataloc += 4;
						} else if ( buffer[typeloc] == 'i' ) {
							int data = decode_bytes_to_int(buffer, dataloc, true);
							oscmessage.addArgument(data);
							dataloc += 4;
						} else if ( buffer[typeloc] == 'd' ) {
							double data = decode_ieee_double(buffer, dataloc, true);
							oscmessage.addArgument(data);
							dataloc += 8;
						} else if ( buffer[typeloc] == 't' ) {
							double data = decode_ieee_double(buffer, dataloc, true);
							oscmessage.addArgument(data, true);
							dataloc += 8;
						} else if ( buffer[typeloc] == 's' ) {
							int endofstr = nextLocationOfChar(buffer, '\0', dataloc, msglength, buffer_max);
							if ( endofstr <= msglength ) {
								String data = new String(buffer, dataloc, endofstr-dataloc);
								oscmessage.addArgument(data);
								dataloc = nextIndexForIndex(endofstr);
							} else {
								System.out.println("OSC string argument error.");
								done = true;
								outindex = -1;
							}
						} else if ( buffer[typeloc] == 'b' ) {
							int dlen = decode_bytes_to_int(buffer, dataloc, true);
									dataloc += 4;
							if ( dlen > 0 ) {
								 if ( dlen <= buffer_max-dataloc ) {
									 oscmessage.addArgument(buffer, dataloc, dlen);
									  dataloc += dlen;
									  int rlen = dlen %4;             //check for padding
									  rlen = (rlen == 0) ? 0 : 4-rlen;
									  dataloc += rlen;
								 } else {
									  System.out.println("OSC blob size to large for buffer");
									  done = true;
									  outindex = -1;
								 }
							}
						} else if ( buffer[typeloc] == 'T' ) {
							oscmessage.addArgument(buffer[typeloc]);		
						} else if ( buffer[typeloc] == 'F' ) {
							oscmessage.addArgument(buffer[typeloc]);
						} else if ( buffer[typeloc] == 'I' ) {
							oscmessage.addArgument(buffer[typeloc]);
						} else if ( buffer[typeloc] == 'N' ) {
							oscmessage.addArgument(buffer[typeloc]);
						} else {
							//unknown data and size
							return -1;
						}
						typeloc ++;
					}		//while arguments left
					_results.addElement(oscmessage);
				} else {                //no arguments, just the message
					outindex = -1;
					 if ( dataloc == msglength ) {
						  LXOSCMessage oscmessage = new LXOSCMessage(addressPattern);
						  _results.addElement(oscmessage);
					 } else {
						  System.out.println("OSC message format error. (ignored)\n");
					 }
				}
			  } else {
					if ( addressPattern.equals("#bundle") ) {
						 if ( start== 0 ) {
							  dataloc = 16;            // 8 bytes #bundle\0 + 8 bytes
							  _inBundle = true;
						 } else {
							 System.out.println("OSC Warning: #bundle not at start of packet. (ignored)");
							 outindex = -1;
						 }
					} else {
						 System.out.println("OSC Warning: initial zeroloc error");
						 outindex = -1;
					}
			  }
		} else {
			outindex = -1;
			System.out.println("OSC message format error: at least one argument expected. (ignored)");
		}
	
		if ( outindex != -1 ) {
			outindex = dataloc;
		}
	
		return outindex;
	}
	
	/**
	 * convert 8 bytes from a buffer into a double (No length checking!)
	 * @param data the byte[] buffer
	 * @param start index in byte[] of first byte
	 * @param natural_order	true if bigendian
	 * @return double value of bytes
	 */
	double decode_ieee_double(byte[] data, int start, boolean natural_order) {
		long l = decode_bytes_to_long(data, start, natural_order);
		return Double.longBitsToDouble(l);
	}
	
	/**
	 * convert 4 bytes from a buffer into a float (No length checking!)
	 * @param data the byte[] buffer
	 * @param start index in byte[] of first byte
	 * @param natural_order	true if bigendian
	 * @return float value of bytes
	 */
	float decode_ieee_single(byte[] data, int start, boolean natural_order) {
		int asInt = decode_bytes_to_int (data, start, natural_order);
		return Float.intBitsToFloat(asInt);
	}

	/**
	 * convert 4 bytes from a buffer into an int (No length checking!)
	 * @param data the byte[] buffer
	 * @param start index in byte[] of first byte
	 * @param natural_order	true if bigendian
	 * @return int value of bytes
	 */
	int decode_bytes_to_int (byte[] data, int start, boolean natural_order) {
		int i;
		if ( natural_order ) {
			i = ((data[start] & 0xff) << 24) |
			((data[start+1] & 0xff) << 16) |
			((data[start+2] & 0xff) <<  8) |
			(data[start+3] & 0xff);
		} else {
			i = ((data[start+3] & 0xff) << 24) |
			((data[start+2] & 0xff) << 16) |
			((data[start+1] & 0xff) << 8)  |
			(data[start] & 0xff);
		}
		return i;
	}
	
	/**
	 * convert 8 bytes from a buffer into an long (No length checking!)
	 * @param data the byte[] buffer
	 * @param start index in byte[] of first byte
	 * @param natural_order	true if bigendian
	 * @return long value of bytes
	 */
	long decode_bytes_to_long (byte[] data, int start, boolean natural_order) {
		long i;
		if ( natural_order ) {
			i = ((data[start] & 0xff) << 56) |
			((data[start+1] & 0xff) << 48) |
			((data[start+2] & 0xff) << 40) |
			((data[start+3] & 0xff) << 32) |
			((data[start+4] & 0xff) << 24) |
			((data[start+5] & 0xff) << 16) |
			((data[start+6] & 0xff) <<  8) |
			(data[start+7] & 0xff);
		} else {
			i = ((data[start+7] & 0xff) << 56) |
			((data[start+6] & 0xff) << 48) |
			((data[start+5] & 0xff) << 40) |
			((data[start+4] & 0xff) << 32) |
			((data[start+3] & 0xff) << 24) |
			((data[start+2] & 0xff) << 16) |
			((data[start+1] & 0xff) <<  8) |
			(data[start] & 0xff);
		}
		return i;
	}

	/**
	 * find the next location of a character in a byte[]
	 * @param buffer the byte[]
	 * @param test the character you are looking for
	 * @param start the starting index
	 * @param msglength	the length of the message withing the byte[]
	 * @param buffer_max the maximum length of a message
	 * @return index of char test
	 */
	public int nextLocationOfChar(byte[] buffer, char test, int start, int msglength, int buffer_max) { 
		int nn;
		int zeroloc = buffer_max + 10;	//message size must be less than LXOSC_BUFFER_MAX
		for ( nn=start; nn<msglength; nn++) {
			if ( buffer[nn] == test ) {
				zeroloc = nn;
				break;
			}
		}
		return zeroloc;
	}
	
	/**
	 * find the index for a location following a string, padded to 4 byte increments
	 * @param s the string
	 * @param start the starting index
	 * @return the index following the 4 byte padded string
	 */
	int nextIndexForString(String s, int start) {
		int l = s.length();
		int ml = l / 4 + 1;
		return start + (ml*4);
	}
	
	/**
	 * find the index for a location in 4 byte increments
	 * @param index
	 * @return the next index following the index with 4 byte padding
	 */
	int nextIndexForIndex(int index) {
		int ml = index / 4 + 1;
		return (ml*4);
	}

}