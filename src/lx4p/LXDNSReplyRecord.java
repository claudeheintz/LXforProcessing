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
import java.net.*;

/**
 *  LXmDNSReplyRecord holds a Reply Record Data structure
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXUPnPDelegate
 */

public class LXDNSReplyRecord {
	/**
	 * QNAME or NAME extracted from a series of labels
	 * 
	 * in a DNS record, me.mycomputer.com is encoded as
	 * 	0x02, 'm', 'e', 0x0A, 'm', 'y', 'c', 'o', 'm', 'p', 'u', 't', 'e', 'r', 0x03, 'c', 'o', 'm'
	 *  each "label" has a length byte, followed by that number of characters
	 *  when written or extracted as a string, the domain labels are concatenated with dots
	 * 
	 */
	String name;
	
	/**
	 * packetid is assigned to all reply records in a single DNS message
	 * 
	 * Reply records may belong to the question, answer, authority, or additional sections of the message.
	 * Each reply record is linked to being part of the same message by this number which is just a simple
	 * sequence count of packets.
	 * 
	 */
	int packetid;
	
	/**
	 * QTYPE of the record (see constants defined below for common types)
	 * 0xFF is special for question records meaning All Classes
	 */
	int qtype;
	
	/**
	 * QCLASS of the record (most likely 0x01 = IN or internet class)
	 * includes a cache flag in the MSB (0x8000)
	 */
	int qclass;
	
	/**
	 * TTL how long this record is valid
	 * TTL = 0 means that the service is no longer available
	 */
	long timeToLive;
	
	/**
	 * index[] of first byte of the next record in message bytes
	 */
	int pointer;
	
	/**
	 * for answer records, this is the InetAddress of the sender of the DNS message
	 * address is null for other records
	 */
	InetAddress address = null;
	
	/**
	 * data copied from packet
	 * data is null for query records
	 */
	byte[] data;
	
	public static final int A_RECORD_QTYPE = 1;
	public static final int NS_RECORD_QTYPE = 2;
	public static final int CNAME_RECORD_QTYPE = 5;
	public static final int MX_RECORD_QTYPE = 15;
	public static final int TXT_RECORD_QTYPE = 16;
	public static final int SRV_RECORD_QTYPE = 33;
	public static final int ALL_QTYPES = 255;
	
	public static int IN_QCLASS = 1;
	
	/**
	 * construct a LXDNSReplyRecord
	 * @param n NAME or QNAME represented as dot separated labels: webpage._http.local
	 * @param qt QTYPE
	 * @param qc QCLASS
	 * @param ttl TimeToLive
	 * @param p pointer to next record
	 * @param d data array
	 */
	public LXDNSReplyRecord(String n, int qt, int qc, long ttl, int p, byte[] d ) {
		name = n;
		timeToLive = ttl;
		qtype = qt;
		qclass = qc;
		pointer = p;
		data = d;
	}
	
	/**
	 * in a DNS record, me.mycomputer.com is encoded as
	 * 	0x02, 'm', 'e', 0x0A, 'm', 'y', 'c', 'o', 'm', 'p', 'u', 't', 'e', 'r', 0x03, 'c', 'o', 'm'
	 *  each "label" has a length byte, followed by that number of characters
	 *  when written or extracted as a string, the domain labels are concatenated with dots
	 *
	 *  @return the NAME or QNAME of the record
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Reply records may belong to the question, answer, authority, or additional sections of the message.
	 * Each reply record is linked to being part of the same message by this number which is just a simple
	 * sequence count of packets.
	 * 
	 * @return id of the packet to which this record belongs
	 */
	public int packetID() {
		return packetid;
	}
	
	/**
	 * Reply records may belong to the question, answer, authority, or additional sections of the message.
	 * Each reply record is linked to being part of the same message by this number which is just a simple
	 * sequence count of packets.
	 *  
	 * @param i the sequence number of the packet to which this message belongs
	 */
	public void setPacketID(int i) {
		packetid = i;
	}
	
	/**
	 * QTYPE of the record eg. A, CNAME, MX, etc.
	 * @return int code giving the QTYPE of the record
	 */
	public int getQType() {
		return qtype;
	}
	
	/**
	 * QCLASS of the record, most likely 0x01, or IN, internet class
	 * @return ode giving the QCLASS of the record
	 */
	public int getQClass() {
		return qclass & 0x7FFF;
	}
	
	/**
	 * @return true if cache flag is set
	 */
	public boolean getCacheFlag() {
		return (qclass & 0x8000) != 0;
	}
	
	/**
	 * @return long time for the record to remain valid
	 */
	public long getTimeToLive() {
		return timeToLive;
	}
	
	/**
	 * @return int index of next record in the bytes of the message packet
	 */
	public int getNextLocation() {
		if ( data == null ) {
			return pointer;
		}
		return pointer + data.length;
	}
	
	/**
	 *  sender's IP address should be provided for answer messages
	 * 	null for query messages
	 *  optional for authority and additional messages
	 *  
	 * @return INetAddress of the sender of the packet to which this record belongs
	 */
	public InetAddress getAddress() {
		return address;
	}
	
	/**
	 * @param addr the INetAddress of the sender of the packet to which this record belongs
	 */
	public void setAddress(InetAddress addr) {
		address = addr;
	}
	
	/**
	 * 
	 * @return array of bytes representing the data attached to this record
	 */
	public byte[] getData() {
		return data;
	}
	
	
	public static String qtypeString(int qt) {
		switch ( qt ) {
		case A_RECORD_QTYPE:
			return "A";
		case NS_RECORD_QTYPE:
			return "NS";
		case CNAME_RECORD_QTYPE:
			return "CNAME";
		case MX_RECORD_QTYPE:
			return "MX";
		case TXT_RECORD_QTYPE:
			return "TXT";
		case SRV_RECORD_QTYPE:
			return "SRV";
		case ALL_QTYPES:
			return "*ALL*";
		}
		return "?";
	}
}