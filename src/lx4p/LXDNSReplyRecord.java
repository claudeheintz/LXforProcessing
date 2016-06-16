/**
 * Copyright (c) 2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
 * 
*/

package lx4p;

/**
 *  LXmDNSReplyRecord holds a Reply Record Data structure
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXUPnPDelegate
 */

public class LXDNSReplyRecord {
	String name;
	int packetid;
	int qtype;
	int qclass;
	long timeToLive;
	int pointer;
	byte[] data;
	
	public LXDNSReplyRecord(String n, int qt, int qc, long ttl, int p, byte[] d ) {
		name = n;
		timeToLive = ttl;
		qtype = qt;
		qclass = qc;
		pointer = p;
		data = d;
	}
	
	public String getName() {
		return name;
	}
	
	public int packetID() {
		return packetid;
	}
	
	public void setPacketID(int i) {
		packetid = i;
	}
	
	public int getQType() {
		return qtype;
	}
	
	public int getQClass() {
		return qclass & 0x7FFF;
	}
	
	public boolean getCacheFlag() {
		return (qclass & 0x8000) != 0;
	}
	
	public long getTimeToLive() {
		return timeToLive;
	}
	
	public int getNextLocation() {
		if ( data == null ) {
			return pointer;
		}
		return pointer + data.length;
	}
	
	public byte[] getData() {
		return data;
	}
}