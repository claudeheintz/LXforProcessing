/**
 * Copyright (c) 2015-2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
*/

package lx4p;

import java.net.*;

/**
 * LXSACN.java
 * 
 * <p>LXSACN partially implements E1.31,<BR>
 * Lightweight streaming protocol for transport of DMX512 using ACN</p>
 * 
 * <p>sACN E 1.31 is a public standard published by the PLASA technical standards program<BR>
 * http://tsp.plasa.org/tsp/documents/published_docs.php</p>
*/
public class LXSACN extends LXDMXEthernet  {
	
	public static final int SACN_PORT = 0x15C0;
	public static final int SACN_BUFFER_MAX = 638;
	public static final int SACN_CID_LENGTH = 16;
	
	/**
	 * buffer for reading and sending packets
	 */
	byte[] _packet_buffer = new byte[SACN_BUFFER_MAX];
	/**
	 * buffer for dmx data for sending and received from first source
	 * <p>Includes dmx start code.</p>
	 */
	byte[] _dmx_buffer1 = new byte[DMX_UNIVERSE_MAX+1];
	/**
	 * dmx data received from second dmx source
	 * * <p>Includes dmx start code.</p>
	 */
	byte[] _dmx_buffer2 = new byte[DMX_UNIVERSE_MAX+1];
	/**
	 * number of slots aka addresses or channels
	 */
	int _dmx_slots = DMX_UNIVERSE_MAX;
	/**
	 * dmx universe 1-32767
	 */
	byte _universe = 1;
	/**
	 * packet sequence number for sending
	 */
	byte _sequence = 0;
	/**
	 * multicast IPv4 address for sending packets
	 * <p>sending socket must be configured for multicast to use this address</p>
	 */
	InetAddress _multicast_address = null;
	/**
	 * IPv4 address of first received dmx source
	 */
	byte[]  _dmx_source1 = new byte[SACN_CID_LENGTH];
	/**
	 *   priority of first received dmx source, 100=normal
	 */
	byte _priority1 = 100;
	/**
	 * IPv4 address of second received dmx source
	 */
	byte[]  _dmx_source2 = new byte[SACN_CID_LENGTH];
	/**
	 *   priority of second received dmx source, 100=normal
	 */
	byte _priority2 = 0;
	/**
	 *   array representing UUID for sending DMX (not persistent between launches)
	 */
	byte[] my_cid = new byte[SACN_CID_LENGTH];
	
	/**
	 * constructor initializes packet and data buffers
	 */
	public LXSACN() {
		for (int n=0; n<SACN_BUFFER_MAX; n++) {
			_packet_buffer[n] = 0;
			if ( n < SACN_CID_LENGTH ) {
				_dmx_source1[n] = 0;
				_dmx_source2[n] = 0;
				my_cid[n] = 0;
			}
		}
		clearSlots();
	}
	
	/**
	 * constructor initializes data buffers and IPv4 multicast address
	 * <p>optional constructor with multicast address for sending sACN E1.31 packets</p>
	 * @see #sendDMX
	 * @param maddr multicast address
	 */
	public LXSACN(InetAddress maddr) {
		for (int n=0; n<SACN_BUFFER_MAX; n++) {
			_packet_buffer[n] = 0;
			if ( n < SACN_CID_LENGTH ) {
				_dmx_source1[n] = 0;
				_dmx_source2[n] = 0;
				my_cid[n] = 0;
			}
		}
		clearSlots();
		setMulticastAddress(maddr);
	}
	
	/**
	 * port for E1.31 sACN
	 * @return the standard network port for E1.31 sACN
	 */
	public int getPort() {
		return SACN_PORT;
	}
	
	/**
	 * dmx level data in slot
	 * @param slot the address or channel of the data (1-512)
	 * @return the level 0-255 for the slot (aka address or channel)
	 */
	public int getSlot(int slot) {
		// decide which value to return if more than one source
		if ( _priority2 != 0 ) {
			// _dmx_source2 is set when a packet is received with a second, different source string
			if ( _priority2 > _priority1 ) {				// return slot from dmx buffer #2 if _priority2 is greater
				return byte2int( _dmx_buffer2[slot] );
			} else if ( _priority2 == _priority1 ) {	    // merge output HTP if priorities are equal
				int b1 = byte2int( _dmx_buffer1[slot] );
				int b2 = byte2int( _dmx_buffer2[slot] );
				if ( b1 > b2 ) {
					return b1;
				}
				return b2;
			}
		}
		return byte2int( _dmx_buffer1[slot] );		// otherwise return slot from dmx buffer #1
	}

	/**
	 * set (byte) dmx level data in slot
	 * <p>The first dmx buffer is used to store dmx levels to be sent.</p>
	 * @param slot aka the address or channel of the level data (1-512)
	 * @param value the level 0-255 for the slot (aka address or channel)
	 */
	public void setSlot(int slot, byte value) {
		_dmx_buffer1[slot] = value;
	}
	
	/**
	 * clears both dmx data buffers
	 */
	public void clearSlots() {
		for(int j=0; j<DMX_UNIVERSE_MAX+1; j++) {
			_dmx_buffer1[j] = 0;
			_dmx_buffer2[j] = 0;
		}
	}
	
	/**
	 * dmx has variable number of addresses ~24min to 512max
	 * @return number of slots slot (aka addresses or channels)
	 */
	public int getNumberOfSlots() {
	   return _dmx_slots - 1;			//sACN includes slot for dmx start code
	}
	
	/**
	 * dmx has variable number of addresses ~24min to 512max
	 * @param slots number of slots (aka addresses or channels)
	 */
	public void setNumberOfSlots(int slots) {
		_dmx_slots =  Math.max(slots, DMX_MIN_SLOTS) + 1;			//sACN includes slot for dmx start code
	}
	
	/**
	 * dmx universe
	 * <p>Each dmx stream of up to 512 slots/addresses/channels is called a universe.<BR>
	 * Universe numbers vary by protocol.  The first universe in zero for Art-Net and one for sACN</p>
	 * @return the dmx universe
	 */
	public int getUniverse() {
		return byte2int(_universe);
	}
	
	/**
	 * set dmx universe
	 * <p>Each dmx stream of up to 512 slots/addresses/channels is called a universe.<BR>
	 * Universe numbers vary by protocol.  The first universe in zero for Art-Net and one for sACN</p>
	 * @param u the dmx universe
	 */
	public void setUniverse(int u) {
		_universe = (byte) u;
	}
	
	/**
	 * DMX Start Code (zero for normal dmx data)
	 * @return dmx start code
	 */
	public int getStartCode() {
		return getSlot(0);
	}
	
	/**
	 * set DMX Start Code, normally zero for regular dmx data
	 * @param c start code
	 */
	public void setStartCode(int c) {
		setSlot(0, (byte) c);
	}
	
	/**
	 * packet priority
	 * @return priority of packet (100=normal)
	 */
	public int packetPriorty() {
		return _priority1;
	}
	
	/**
	 * set packet priority
	 * @param p priority of packet (100=normal)
	 */
	public void setPacketPriority(int p) {
		_priority1 = (byte) p;
	}
	
	/**
	 * sets the CID based on the hardware MAC address
	 * @param mac byte array representing the hardware MAC address
	 */
	public void setCIDwithMACAddress(byte[] mac) {
		my_cid[0] = mac[0];
		my_cid[1] = mac[1];
		my_cid[2] = mac[2];
		my_cid[3] = mac[3];
		my_cid[4] = mac[4];
		my_cid[5] = mac[5];
		my_cid[6] = 'p';
		my_cid[7] = 'r';
		my_cid[8] = 'o';
		my_cid[9] = 'c';
		my_cid[10] = 'e';
		my_cid[11] = 's';
		my_cid[12] = 's';
		my_cid[13] = 'i';
		my_cid[14] = 'n';
		my_cid[15] = 'g';
	}
	
	/**
	 * sets multicast address
	 * <p>Note: does not prepare socket for multicast which must be done outside this class.</p>
	 * @param maddr broadcast InetAddress
	 */
	public void setMulticastAddress(InetAddress maddr) {
		_multicast_address = maddr;
	}
	
	/**
	 * attempt to read an sACN E1.31 packet from socket
	 * @param socket used to read the sACN E1.31 packet
	 * @return true if packet contained dmx output
	 */
	public boolean readPacket(DatagramSocket socket) {
		boolean good_dmx = false;
		// setup a DatagramPacket.  Note that _packet_buffer is the storage for the receivePacket
		DatagramPacket receivePacket = new DatagramPacket(_packet_buffer, _packet_buffer.length);
		try {
			socket.receive(receivePacket);
			if ( processDatagramPacket(socket, receivePacket) ) {
				good_dmx = ( getStartCode() == 0 );
			}
		} catch ( Exception e) {
			//   will catch receive time out exception
			System.out.println("receive exception " + e);
		}
      return good_dmx;
	}
	
	/**
	 * parses DatagramPacket for sACN E1.31 content
	 * @param socket
	 * @param receivePacket UPP packet to process.
	 * @return true if packet contains dmx output
	 */
	public boolean processDatagramPacket(DatagramSocket socket, DatagramPacket receivePacket) {
		boolean good_dmx = false;
		int receivedDataLength = receivePacket.getLength();
		
		if ( receivedDataLength > 0 ) {
			good_dmx = parseRootLayer(receivedDataLength);
		}
	
		return good_dmx;
	}
	
	/**
	 * Sends sACN E1.31 DMX packet to address using socket.
	 * <p>Assumes that the socket is already setup for the type of address unicast/multicast.</p>
	 * @param socket Open and configured socket used to send the packet.
	 * @param to_ip address to which packet is sent.
	 */
	public void sendDMX ( DatagramSocket socket, InetAddress to_ip ) {
		for (int n=0; n<126; n++) {
			_packet_buffer[n] = 0;		// zero outside layers & start code
		 }
		// ----- root layer -----
		_packet_buffer[1] = (byte) 0x10;
		_packet_buffer[4] = 'A';
		_packet_buffer[5] = 'S';
		_packet_buffer[6] = 'C';
		_packet_buffer[7] = '-';
		_packet_buffer[8] = 'E';
		_packet_buffer[9] = '1';
		_packet_buffer[10] = '.';
		_packet_buffer[11] = '1';
		_packet_buffer[12] = '7';
		int fplusl = _dmx_slots + 109 + 0x7000;
		_packet_buffer[16] = (byte)(fplusl >> 8);
		_packet_buffer[17] = (byte)(fplusl & 0xff);
		_packet_buffer[21] = (byte)(0x04);			// ACN vector
		for(int i =0; i<my_cid.length; i++) {
			_packet_buffer[22+i] = my_cid[i];
		}
		// ----- framing layer -----
		fplusl = _dmx_slots + 87 + 0x7000;
		_packet_buffer[38] = (byte)(fplusl >> 8);
		_packet_buffer[39] = (byte)(fplusl & 0xff);
		_packet_buffer[43] = (byte)(0x02);			// ACN vector
		_packet_buffer[44] = 'L';
		_packet_buffer[45] = 'X';
		_packet_buffer[46] = 'S';
		_packet_buffer[47] = 'A';
		_packet_buffer[48] = 'C';
		_packet_buffer[49] = 'N';
		_packet_buffer[108] = _priority1;
		if ( _sequence == 0 ) {
			_sequence = 1;
		} else {
			_sequence++;
		}
		_packet_buffer[111] = _sequence;
		_packet_buffer[113] = 0;	//_universe >> 8
		_packet_buffer[114] = _universe;
		// ----- dmp layer -----
		fplusl = _dmx_slots + 10 + 0x7000;
		_packet_buffer[115] = (byte)(fplusl >> 8);
		_packet_buffer[116] = (byte)(fplusl & 0xff);
		_packet_buffer[117] = (byte)0x02;			// ACN vector
		_packet_buffer[118] = (byte)0xa1;			// address and data type
		_packet_buffer[122] = (byte)0x01;			// address increment
		fplusl = _dmx_slots;                        // _dmx_slots includes 1 for start code 1-513
		_packet_buffer[123] = (byte)(fplusl >> 8);
		_packet_buffer[124] = (byte)(fplusl & 0xFF);

		// write from dmx buffer #1 to packet buffer for output
		// (setSlot puts values into dmx buffer #1)
		for(int j=0; j<_dmx_slots; j++)  {
			_packet_buffer[125+j] = _dmx_buffer1[j];
  		}
		
  		DatagramPacket sendPacket = new DatagramPacket(_packet_buffer, 125+_dmx_slots, to_ip, SACN_PORT);
		try {
			socket.send(sendPacket);
		} catch ( Exception e) {
			System.out.println("send dmx exception " + e);
		}
	}
	
	/**
	 * Sends sACN E1.31 DMX packet to member variable multicast address using socket.
	 * <p>Assumes that the socket is already setup for the type of address (unicast or multicast).</p>
	 * @see #_multicast_address
	 * @param socket Open and configured socket used to send the packet.
	 */
	public void sendDMX ( DatagramSocket socket ) {
		if ( _multicast_address != null ) {
			sendDMX( socket, _multicast_address );
		}
	}
	
	/**
	 * parses packet and checks root layer data
	 * @param size received data length
	 * @return true if packet contains dmx output
	 */
	public boolean parseRootLayer( int size ) {
		if  ( byte2int(_packet_buffer[1]) == 0x10 ) {									//preamble size
			String header = new String( _packet_buffer, 4, 9 );
			if ( header.equals("ASC-E1.17") ) {
				int tsize = size - 16;
				if ( checkFlagsAndLength(16, tsize) ) { // root pdu length
				  if ( byte2int(_packet_buffer[21]) == 0x04 ) {							// vector RLP is 1.31 data
					 return parseFramingLayer( tsize );
				  }
				}
			}       // ACN packet identifier
		}			// preamble size
		return false;
	}
	
	/**
	 * parses packet and checks framing layer data
	 * <p>Checks to see if unverse matches and ignore packets sent to other universes.<BR>
	 * Extracts source string.</p>
	 * @param size received data length
	 * @return true if packet contains dmx output
	 */
	public boolean parseFramingLayer( int size ) {
		int tsize = size - 22;
		if ( checkFlagsAndLength(38, tsize) ) {     // framing pdu length
			if ( byte2int(_packet_buffer[43]) == 0x02 ) {                        // vector dmp is 1.31
				if ( byte2int(_packet_buffer[114]) == _universe ) {// implementation has 255 universe limit
					return parseDMPLayer( tsize, _packet_buffer[108] );       
        		}
			}
		}
		return false;
	}


	/**
	 * parses and checks DMP portion of packet and extracts dmx from DMP layer
	 * @param size received data length
	 * @param priority
	 * @return true if packet contains dmx output
	 */
	public boolean parseDMPLayer( int size, byte priority ) {
		int tsize = size - 77;
		boolean good_dmx = false;
		if ( checkFlagsAndLength(115, tsize) ) {
			if ( byte2int(_packet_buffer[117]) == 0x02 ) {		// dmp vector
				if ( byte2int(_packet_buffer[118]) == 0xa1 ) { 	// address and data type
					int slots = byte2int(_packet_buffer[124]);
	        		slots += byte2int(_packet_buffer[123]) << 8;
	   
	        		if ( isEmptyUUIDBytes(_dmx_source1) ) {
	        			for(int k=0; k<SACN_CID_LENGTH; k++) {
	        				_dmx_source1[k] = _packet_buffer[22+k];
	        			}
	        		}
	        		if ( isEqualUUIDBytes(_dmx_source1, _packet_buffer, 22) ) {
		        		if ( slots > _dmx_slots ) {
		        			_dmx_slots = slots;
		        		}
		        		_priority1 = priority;
		        		for(int j=0; j<_dmx_slots; j++) {
		        			if ( j < slots ) {
		        				_dmx_buffer1[j] = _packet_buffer[j+125];
		        			} else {
		        				_dmx_buffer1[j] = 0;
		        			}
						}
		        		good_dmx = true;
	        		} else {
	        			if ( isEmptyUUIDBytes(_dmx_source2) ) {
	        				for(int k=0; k<SACN_CID_LENGTH; k++) {
		        				_dmx_source2[k] = _packet_buffer[22+k];
		        			}
		        		}
	        			if ( isEqualUUIDBytes(_dmx_source2, _packet_buffer, 22) ) {
			        		if ( slots > _dmx_slots ) {
			        			_dmx_slots = slots;
			        		}
			        		_priority2 = priority;
			        		for(int j=0; j<_dmx_slots; j++) {
			        			if ( j < slots ) {
			        				_dmx_buffer2[j] = _packet_buffer[j+125];
			        			} else {
			        				_dmx_buffer2[j] = 0;
			        			}
							}
			        		good_dmx = true;
		        		}
	        		}
				}			//address type
			}				//type
		}					//flags & length
		return good_dmx;
	}
	
//  ***** checkFlagsAndLength(index, size) *****
//  flags | length is 2 bytes starting at index
//  highest 4 nibble is the flags and should be 0x70
//  lowest 12 bits are length
	
	/**
	 * Utility for checking flags + length bytes
	 * <p>flags | length is 2 bytes starting at index<BR>
	 * highest nibble is the flags and should be 0x7<BR>
	 * lowest 12 bits are length</p>
	 * @param index starting index of 2byte flags + length field
	 * @param size to compare with expected size
	 * @return true if flags and size are good
	 */
	public boolean checkFlagsAndLength( int index, int size ) {
		if ( ( byte2int(_packet_buffer[index]) & 0xF0 ) == 0x70 ) {
		   int pdu_length = byte2int(_packet_buffer[index+1]);
			pdu_length += ((byte2int(_packet_buffer[index]) & 0x0f) << 8);
			if ( ( pdu_length != 0 ) && ( size >= pdu_length ) ) {
			   return true;
			}
		}
		return false;
	}
	
	/**
	 * Utility to check if UUID byte[] is empty (all zeros)
	 * @param ba byte array
	 * @return true if all zeros
	 */
	public boolean isEmptyUUIDBytes(byte[] ba) {
		for(int i =0; i<ba.length; i++) {
			if ( ba[i] != 0 ) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Utility to compare UUID byte[] to series of bytes in another array starting at index
	 * @param ba byte array
	 * @param ca byte array
	 * @param ci starting index in ca
	 * @return true if identical series of bytes
	 */
	public boolean isEqualUUIDBytes(byte[] ba, byte[] ca, int ci) {
		for(int i =0; i<ba.length; i++) {
			if ( ba[i] != ca[ci+i]) {
				return false;
			}
		}
		return true;
	}

}