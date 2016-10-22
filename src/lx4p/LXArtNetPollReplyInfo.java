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
import java.util.*;

/** LXArtNetPollReplyInfo
 * 
 * <p>LXArtNetPollReplyInfo is a container class for info contained in an Art-Net poll reply.
 * 
 * <p>Art-Net(TM) Designed by and Copyright Artistic Licence Holdings Ltd.</p>
*/


public class LXArtNetPollReplyInfo  {

	/**
	 * short name of Art-Net node
	 */
	String nodeNameShort;
	/**
	 * long name of Art-Net node
	 */
	String nodeNameLong;
	/**
	 * IP Address of Art-Net node
	 */
	InetAddress nodeAddress;
	/**
	 * number of ports
	 */
	int ports;
	/**
	 * universe Port 1
	 */
	int port1Universe;
	/**
	 * can output DMX from port 1
	 */
	boolean port1CanOutput;
	
	/**
	 * construct LXArtNetPollReplyInfo using ArtPollReply packet bytes
	 * @param packet_buffer containing contents of ArtPollReply packet
	 * @param sender	InetAddress of sender of poll reply
	 */
	public LXArtNetPollReplyInfo( byte[] packet_buffer , InetAddress sender ) {
		nodeAddress = sender;
		
		// locate end of short name string
		int z = 26;
		while ( (z<packet_buffer.length) && (packet_buffer[z]!=0) ) {
			z++;
		}
		nodeNameShort = new String(Arrays.copyOfRange(packet_buffer, 33, z));
		
		// locate end of short name string
		z = 44;
		while ( (z<packet_buffer.length) && (packet_buffer[z]!=0) ) {
			z++;
		}
		nodeNameLong = new String(Arrays.copyOfRange(packet_buffer, 33, z));
		ports = packet_buffer[173];
		port1Universe = packet_buffer[190];
		port1CanOutput = ( ( (packet_buffer[174]&0x80) == 0x80 ) && ( (packet_buffer[174]&0x3f) == 0 ) );
	}
	
	/**
	 * @return shortNodeName String
	 */
	public String shortNodeName() {
		return nodeNameShort;
	}
	
	/**
	 * @return longNodeName String
	 */
	public String longNodeName() {
		return nodeNameLong;
	}

	/**
	 * @return nodeAddress InetAddress
	 */
	public InetAddress nodeAddress() {
		return nodeAddress;
	}
	
	public int numberOfPorts() {
		return ports;
	}

	/**
	 * @return subnet/universe of port one
	 */
	public int portOneUniverse() {
		return port1Universe;
	}
	
	/**
	 * @return port one can output DMX
	 */
	public boolean canOutputDMXFromPortOne() {
		return port1CanOutput;
	}
}