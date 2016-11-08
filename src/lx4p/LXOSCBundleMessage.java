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

/** LXOSCBundleMessage
 * 
 * <p>Encapsulates an OSC message including its address pattern and arguments
 * supports OSC 1.1 int, float, double, timestamp, string, blob, True, False, Impulse and Null arguments.</p>
*/

public class LXOSCBundleMessage extends LXOSCMessage {
	
	Vector<LXOSCMessage> msgs;
	
	public LXOSCBundleMessage(Vector<LXOSCMessage> v) {
		msgs = v;
	}
	
	public int addOSCMessageToBytes(byte[] buffer, int si) {
		buffer[si] = '#';
		buffer[si+1] = 'b';
		buffer[si+2] = 'u';
		buffer[si+3] = 'n';
		buffer[si+4] = 'd';
		buffer[si+5] = 'l';
		buffer[si+6] = 'e';
		buffer[si+7] = 0;
	
		// handle timestamp here
		//long mtime = Calendar.getInstance().getTimeInMillis() - 220898880L;	//convert from Epoch to 1900 still milliseconds
		//long time = Double.doubleToLongBits((System.currentTimeMillis()/1000.0) + 2208988800.0);						//64 bit float seconds to bytes
		long time = LXOSC.toNtpTime(System.currentTimeMillis());
		buffer[si+8]  = (byte)((time >> 56) & 0xff);
		buffer[si+9]  = (byte)((time >> 48) & 0xff);
		buffer[si+10]  = (byte)((time >> 40) & 0xff);
		buffer[si+11]  = (byte)((time >> 32) & 0xff);
		buffer[si+12]  = (byte)((time >> 24) & 0xff);
		buffer[si+13]  = (byte)((time >> 16) & 0xff);
		buffer[si+14]  = (byte)((time >> 8) & 0xff);
		buffer[si+15]  = (byte)(time & 0xff);
	
		int s = si+20;
		Enumeration<LXOSCMessage> en = msgs.elements();
		LXOSCMessage msg;
		int msgsize;
		int t = s;
		while ( en.hasMoreElements() ) {
			msg = en.nextElement();
			t = msg.addOSCMessageToBytes(buffer, s);
			msgsize = t-s;
			if ( msgsize <= 0 ) {
				return msgsize; // addOSCMessageToBytes returned error
			}
			buffer[s-4] = (byte) ((msgsize>>24) & 0xFF);
			buffer[s-3] = (byte) ((msgsize>>16) & 0xFF);
			buffer[s-2] = (byte) ((msgsize>>8) & 0xFF);
			buffer[s-1] = (byte) (msgsize & 0xFF);
			s = t + 4;	//leave room for next size bytes
		}
	
		return t;
	}
}