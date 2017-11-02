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

import java.net.*;

/** LXArtNet
 * 
 * <p>LXArtNet partially implements the Art-Net Ethernet Communication Standard.<BR>
 * http://www.artisticlicence.com</p>
 * 
 * <p>LXArtNet is primarily a server (controller) implementation.  It is, however,
 * capable of receiving Art-Net packets from the network and responding to ArtPoll
 * requests.  When in receiving mode, LXArtNet supports merge of up to two Art-Net
 * packet streams from unique IP addresses.</p>
 * 
 * <p>Art-Net(TM) Designed by and Copyright Artistic Licence Holdings Ltd.</p>
*/


public class LXArtNet extends LXDMXEthernet  {

	public static final int ARTNET_PORT = 0x1936;
	public static final int ARTNET_BUFFER_MAX = 530;
	public static final int ARTNET_REPLY_SIZE = 239;
	public static final int ARTNET_POLL_SIZE = 14;
	public static final int ARTNET_ART_ADDRESS_SIZE = 108;

	public static final int ARTNET_ART_POLL = 0x2000;
	public static final int ARTNET_ART_POLL_REPLY = 0x2100;
	public static final int ARTNET_ART_DMX = 0x5000;
	public static final int ARTNET_ART_ADDRESS = 0x6000;
	public static final int ARTNET_NOP = 0;

	/**
	 * buffer for reading and sending packets
	 */
	byte[] _packet_buffer = new byte[ARTNET_BUFFER_MAX];
	/**
	 * buffer for dmx data for sending and received from first source
	 */
	byte[] _dmx_buffer1 = new byte[DMX_UNIVERSE_MAX];
	/**
	 * dmx data received from second dmx source
	 */
	byte[] _dmx_buffer2 = new byte[DMX_UNIVERSE_MAX];
	/**
	 * number of slots aka addresses or channels
	 */
	int _dmx_slots = 512;
	/**
	 * high nibble subnet, low nibble universe
	 */
	byte _universe = 0;
	/**
	 * net portion of Port-Address, net+subnet+universe, 15bits (7+4+4)
	 */
	byte _net = 0;
	/**
	 * packet sequence number for sending
	 */
	byte _sequence = 0;
	/**
	 * IPv4 address advertised by ArtPollReply
	 */
	InetAddress _my_address;
	/**
	 * broadcast IPv4 address for packets/poll replies (can be null)
	 */
	public InetAddress _broadcast_address = null;
	/**
	 * IPv4 address of first received dmx source
	 */
	InetAddress _dmx_source1 = null;
	/**
	 * IPv4 address of second received dmx source
	 */
	InetAddress _dmx_source2 = null;
	/**
	 * Address of node to send output
	 */
	InetAddress _output_node_address = null;
	/**
	 *  controls if ArtDMX is broadcast if sendDMX is called without a specific destination address
	 */
	boolean _broadcast_dmx_enabled = false;
	
	/**
	 * Object interested in received ArtPoll replies
	 */
	LXArtNetPollReplyListener _reply_Listener = null;

	/**
	 * constructor initializes data buffers and local IP address
	 * @param myaddress advertised in ArtPollReply
	 */
	public LXArtNet(InetAddress myaddress) {
	   _my_address = myaddress;
	   _dmx_slots = DMX_MIN_SLOTS;
	   clearSlots();
	}
	/**
	 * constructor initializes data buffers and IPv4 addresses
	 * <p>optional constructor with broadcast address for sending ArtDMX and ArtPollReply packets</p>
	 * @see #sendArtPollReply
	 * @see #sendDMX
	 * @param myaddress advertised in ArtPollReply
	 * @param baddr broadcast address
	 */
	public LXArtNet(InetAddress myaddress, InetAddress baddr) {
		_my_address = myaddress;
		setBroadcastAddress(baddr);
		_dmx_slots = DMX_MIN_SLOTS;
	   clearSlots();
	}
	
	/**
	 * port for Art-Net
	 * @return the standard network port for Art-Net
	 */
	public int getPort() {
		return ARTNET_PORT;
	}
	
	/**
	 * dmx level data in slot
	 * @param slot the address or channel of the data (1-512)
	 * @return the level 0-255 for the slot (aka address or channel)
	 */
	public int getSlot(int slot) {
		if ( _dmx_source2 != null ) {					// merge output HTP if necessary
			int b1 = byte2int( _dmx_buffer1[slot-1] );  // _dmx_source2 is set when a packet is received
			int b2 = byte2int( _dmx_buffer2[slot-1] );	// from a second, different, InetAddress
			if ( b1 > b2 ) {
				return b1;
			}
			return b2;
		}
		return byte2int( _dmx_buffer1[slot-1] );		// otherwise return slot from dmx buffer #1
	}

	/**
	 * set (byte) dmx level data in slot
	 * <p>The first dmx buffer is used to store dmx levels to be sent.</p>
	 * @param slot aka the address or channel of the level data (1-512)
	 * @param value the level 0-255 for the slot (aka address or channel)
	 */
	public void setSlot(int slot, byte value) {
		_dmx_buffer1[slot-1] = value;
	}
	
	/**
	 * clears both dmx data buffers
	 */
	public void clearSlots() {
		for(int j=0; j<DMX_UNIVERSE_MAX; j++) {
			_dmx_buffer1[j] = 0;
			_dmx_buffer2[j] = 0;
			_packet_buffer[18+j] = 0;	//clear output in case less than full number of slots are sent...
		}
	}
	
	/**
	 * dmx has variable number of addresses ~24min to 512max
	 * @return number of slots slot (aka addresses or channels)
	 */
	public int getNumberOfSlots() {
	   return _dmx_slots;
	}
	
	/**
	 * dmx has variable number of addresses ~24min to 512max
	 * @param slots number of slots (aka addresses or channels)
	 */
	public void setNumberOfSlots(int slots) {
		_dmx_slots = Math.max(slots, DMX_MIN_SLOTS);
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
	 * sets high nibble as subnet and low nibble as universe
	 * @see #_universe
	 * @param s subnet
	 * @param u universe
	 */
	public void setSubnetUniverse(byte s, byte u) {
		_universe = (byte) ( ((s & 0x07) << 4 ) | u );
	}
	

	/**
	 * sets low nibble as universe
	 * @see #_universe
	 * @param u universe with bit 7 set to indicate change, bits 0-3 = universe 0-15
	 */
	public void setUniverseAddress(byte u) {
		if ( byte2int(u) != 0x7f ) {
			if ( (u & 0x80) != 0 ) {
				_universe = (byte) ( ( _universe & 0xf0 ) | (u & 0x07) );
			}
		}
	}
	
	/**
	 * sets high nibble as subnet
	 * @see #_universe
	 * @param s subnet with bit 7 set to indicate change, bits 0-3 = subnet 0-15
	 */
	public void setSubnetAddress(byte s) {
		if ( byte2int(s) != 0x7f ) {
			if ( (s & 0x80) != 0 ) {
				_universe = (byte) ( ( _universe & 0x0f ) | ((s & 0x07) << 4 ) );
			}
		}
	}
	
	/**
	 * sets net portion of Port-Address
	 * @param s net with bit 7 set to indicate change, bits 0-6 = net 0-127
	 */
	public void setNetAddress(byte s) {
		if ( (s & 0x80) != 0 ) {
			_net = (byte) (s & 0x07);
		}
	}
	
	/**
	 * sets broadcast address
	 * <p>Note: does not prepare socket for broadcast which must be done outside this class.</p>
	 * @param baddr broadcast InetAddress
	 */
	public void setBroadcastAddress(InetAddress baddr) {
		_broadcast_address = baddr;
	}
	
	/**
	 * allows broadcast address to be used to send ArtDMX when a specific address is not specified
	 * @param en enables broadcastDMX
	 */
	public void setBroadcastDMXEnabled(boolean en) {
		_broadcast_dmx_enabled = en;
	}
	
	/**
	 * sets dmx output address address
	 * @param outaddr address to send ArtDMX when no specific address is specified
	 */
	public void setOutputAddress(InetAddress outaddr) {
		_output_node_address = outaddr;
	}

	/**
	 * attempt to read an Art-Net DMX packet from socket
	 * @param socket Open and configured socket used to receive the packet.
	 * @return true if packet contained dmx output
	 */
	public boolean readPacket(DatagramSocket socket) {
		boolean good_dmx = false;
		// setup a DatagramPacket.  Note that _packet_buffer is the storage for the receivePacket, zero the buffer
		for ( int i = 0; i < _packet_buffer.length; i++ ) {
			_packet_buffer[i] = 0;
  		}
		DatagramPacket receivePacket = new DatagramPacket(_packet_buffer, _packet_buffer.length);
		try {
			socket.receive(receivePacket);
			good_dmx = (processDatagramPacket(socket, receivePacket) == ARTNET_ART_DMX);
		} catch ( Exception e) {
			//   will catch receive time out exception
			//System.out.println("readPacket exception " + e);
		}
      return good_dmx;
	}
	
	/**
	 * attempt to read an Art-Net packet from socket
	 * @param socket Open and configured socket used to receive the packet.
	 * @return opcode of packet or -1 if exception
	 */
	public int readArtNetPacket(DatagramSocket socket) {
		  int rv = -1;
		  // setup a DatagramPacket.  Note that _packet_buffer is the storage for the receivePacket, zero the buffer
		  for ( int i = 0; i < _packet_buffer.length; i++ ) {
		    _packet_buffer[i] = 0;
		    }
		  DatagramPacket receivePacket = new DatagramPacket(_packet_buffer, _packet_buffer.length);
		  try {
		    socket.receive(receivePacket);
		    rv = processDatagramPacket(socket, receivePacket);
		  } catch ( Exception e) {
		    //   will catch receive time out exception
		    //System.out.println("readPacket exception " + e);
		  }
		    return rv;
	}
	
	/**
	 * Read packets until read times out or error
	 * @param socket DatagramSocket to be used to read available ArtPoll replies
	 */
	public void readArtNetPollPackets(DatagramSocket socket) {
		while ( readArtNetPacket(socket) > 0 ) {
		}
	}
	
	public void readArtNetPollPackets() {
		if ( dmxsocket != null ) {
			readArtNetPollPackets(dmxsocket);
		}
	}
	
	public void setPollReplyListener(LXArtNetPollReplyListener l) {
		_reply_Listener = l;
	}
	
	/**
	 * parses DatagramPacket for Art-Net content
	 * @param socket Possibly used to send reply.
	 * @param receivePacket UDP packet to process.
	 * @return opcode of the Art-Net packet
	 */
	public int processDatagramPacket(DatagramSocket socket, DatagramPacket receivePacket) {
		int opcode = ARTNET_NOP;
		byte[] receivedData = receivePacket.getData();
		int receivedDataLength = receivePacket.getLength();

		opcode = parseHeader();
		switch ( opcode ) {
			case ARTNET_ART_DMX:
				// check universe and protocol version, ignore sequence[12], physical[13] and subnet/universe hi byte[15]
				boolean no_dmx = true;
				if (( _universe == receivedData[14] ) && ( _net == receivedData[15] ) && (byte2int(receivedData[11]) >= 14)) {
					int dmxlen = byte2int(receivedData[16]) * 256 + byte2int(receivedData[17]);  // check size/slots
					if ( receivedDataLength >= dmxlen + 18 ) {
						if ( _dmx_source1 == null ) {		//if first sender, remember address
							_dmx_source1 = receivePacket.getAddress();
						}
						if ( _dmx_source1.equals(receivePacket.getAddress()) ) {
							if ( dmxlen > _dmx_slots ) {
								_dmx_slots = dmxlen;
							}
							for(int j=0; j<_dmx_slots; j++) {
								if ( j < dmxlen ) {
									_dmx_buffer1[j] = receivedData[18+j];
								} else {
									_dmx_buffer1[j] = 0;
								}
							}
							no_dmx = false;
						} else {	// matched sender1
							if ( _dmx_source2 == null ) {		//if first sender, remember address
								_dmx_source2 = receivePacket.getAddress();
							}
							if ( _dmx_source2.equals(receivePacket.getAddress()) ) {
								if ( dmxlen > _dmx_slots ) {
									_dmx_slots = dmxlen;
								}
								for(int j=0; j<_dmx_slots; j++) {
									if ( j < dmxlen ) {
										_dmx_buffer2[j] = receivedData[18+j];
									} else {
										_dmx_buffer2[j] = 0;
									}
								}
								no_dmx = false;
							}
						}
					}     // matched size
				}        // matched universe
				if ( no_dmx ) {
					opcode = ARTNET_NOP;    //return ARTNET_NOP if not good
				}
				break;
			case ARTNET_ART_POLL:
				if  (byte2int(receivedData[11]) >= 14) {
					sendArtPollReply(socket, receivePacket.getAddress());
				}
				break;
			case ARTNET_ART_ADDRESS:
				if (( receivedDataLength >= 107 ) && ( receivedData[11] >= 14 )) {  //protocol version [10] hi byte [11] lo byte 
			   	   opcode = parseArtAddress();
			   	   sendArtPollReply(socket, receivePacket.getAddress());
			   	}
		   	break;
			case ARTNET_ART_POLL_REPLY:
				if ( _reply_Listener != null ) {
					if ( _reply_Listener.pollReplyReceived(new LXArtNetPollReplyInfo(receivedData, receivePacket.getAddress())) ) {
						if ( ! receivePacket.getAddress().equals(_my_address) ) {
							_output_node_address = receivePacket.getAddress();	// ! from _my_address 
						}
					}
				}
				break;
				
		}
		return opcode;
	}

	/**
	 * Sends Art-Net DMX packet to address using socket.
	 * <p>Assumes that the socket is already setup for the type of address unicast/broadcast.</p>
	 * @param socket Open and configured socket used to send the packet.
	 * @param to_ip address to which packet is sent
	 */
	public void sendDMX ( DatagramSocket socket, InetAddress to_ip ) {
		setStringInByteArray("Art-Net", _packet_buffer, 0, true);
		_packet_buffer[8] = 0;        //op code lo-hi
		_packet_buffer[9] = 0x50;
		_packet_buffer[10] = 0;
		_packet_buffer[11] = 14;		//protocol version
		
		if ( _sequence == 0 ) {
		  _sequence = 1;
		} else {
			_sequence++;
		}
		_packet_buffer[12] = _sequence;
		_packet_buffer[13] = 0;
		_packet_buffer[14] = _universe;
		_packet_buffer[15] = _net;
		_packet_buffer[16] = (byte)(_dmx_slots >> 8);
		_packet_buffer[17] = (byte)(_dmx_slots & 0xFF);
		
		// write from dmx buffer #1 to packet buffer for output
		// (setSlot puts values into dmx buffer #1)
		for(int j=0; j<_dmx_slots; j++)  {
			_packet_buffer[18+j] = _dmx_buffer1[j];
  		}
  
  		DatagramPacket sendPacket = new DatagramPacket(_packet_buffer, _dmx_slots+18, to_ip, ARTNET_PORT);

		try {
			socket.send(sendPacket);
		} catch ( Exception e) {
			System.out.println("send dmx exception " + e);
		}
	}
	
	/**
	 * Sends Art-Net DMX packet to member output node address using socket.
	 * <p>Assumes that the socket is already setup for the type of address (unicast or broadcast).</p>
	 * <p>Does nothing if broadcast address is not set</p>
	 * @see #_broadcast_address
	 * @param socket Open and configured socket used to send the packet.
	 */
	public void sendDMX ( DatagramSocket socket ) {
		if ( _output_node_address != null ) {
			sendDMX(socket, _output_node_address	);
		} else if ( _broadcast_dmx_enabled && ( _broadcast_address != null )) {
			sendDMX(socket, _broadcast_address);
		}
	}
	
	/**
	 * Sends Art Poll
	 * <p>If  broadcast address is set, poll is sent to that address. Otherwise does nothing.</p>
	 * @see #_broadcast_address
	 * @param socket used to send the packet.  Must be open and configured for broadcast.
	 */
	public void sendArtPoll ( DatagramSocket socket) {
		if ( _broadcast_address != null ) {
	  		byte[] pollBuffer = new byte[ARTNET_POLL_SIZE];
	  		int i;
	  		for ( i = 0; i < ARTNET_POLL_SIZE; i++ ) {
	  			pollBuffer[i] = 0;
	  		}
	  		setStringInByteArray("Art-Net", pollBuffer, 0, true);
	  		pollBuffer[8] = 0;        // op code lo-hi
	  		pollBuffer[9] = (byte)0x20;
	  		pollBuffer[10] = 0;
	  		pollBuffer[11] = (byte)14;//protocol version lo byte
	  		pollBuffer[12] = 0;	   // send poll reply only
	  		pollBuffer[13] = 0;
	
	  		DatagramPacket sendPacket = new DatagramPacket(pollBuffer, pollBuffer.length, _broadcast_address, ARTNET_PORT);
	  		
			try {
				socket.send(sendPacket);
			} catch ( Exception e) {
				System.out.println("send poll exception " + e);
			}
		}
	}
	
	/**
	 * Sends ArtPoll using dmxsocket
	 * * <p>If  broadcast address is set, poll is sent to that address. Otherwise does nothing.</p>
	 */
	public void sendArtPoll () {
		sendArtPoll(dmxsocket);
	}
	
	/**
	 * Sends Art Poll Reply using socket
	 * <p>If  broadcast address is set, poll reply is sent to that address.<BR>
	 * Otherwise it is sent to the specified address which is assumed to be the sender of the poll.</p>
	 * @see #_broadcast_address
	 * @param socket Open and configured socket used to send the packet.
	 * @param to_ip address for direct reply to poll
	 */
	public void sendArtPollReply ( DatagramSocket socket, InetAddress to_ip ) {
  		byte[] replyBuffer = new byte[ARTNET_REPLY_SIZE];
  		int i;
  		for ( i = 0; i < ARTNET_REPLY_SIZE; i++ ) {
		 replyBuffer[i] = 0;
  		}
  		setStringInByteArray("Art-Net", replyBuffer, 0, true);
  		replyBuffer[8] = 0;        // op code lo-hi
  		replyBuffer[9] = 0x21;
  		byte[] raw = _my_address.getAddress();
  		replyBuffer[10] = (byte)(raw[0] & 0xff);      //ip address
  		replyBuffer[11] = (byte)(raw[1] & 0xff);
  		replyBuffer[12] = (byte)(raw[2] & 0xff);
  		replyBuffer[13] = (byte)(raw[3] & 0xff);
  		replyBuffer[14] = 0x36;    // port lo first always 0x1936
  		replyBuffer[15] = 0x19;
  		replyBuffer[16] = 0;       // firmware hi-lo
  		replyBuffer[17] = 0;
  		replyBuffer[18] = 0;       // subnet hi-lo
  		replyBuffer[19] = 0;
  		replyBuffer[20] = 0;       // oem hi-lo
  		replyBuffer[21] = 0;
  		replyBuffer[22] = 0;       // ubea
  		replyBuffer[23] = 0;       // status
  		replyBuffer[24] = 0x50;    //     Mfg Code
  		replyBuffer[25] = 0x12;    //     seems DMX workshop reads these bytes backwards
  		
  		setStringInByteArray("LXforProcessing", replyBuffer, 26, true);// short name
  		setStringInByteArray("LXforProcessing", replyBuffer, 44, true);// short name// long name
  		
  		replyBuffer[173] = 1;    // number of ports
  		replyBuffer[174] = (byte)128;  // can output from network
  		replyBuffer[182] = (byte)128;  //  good output... change if error
  		replyBuffer[190] = _universe;

  		InetAddress a = _broadcast_address;
  		if ( a == null ) {
  			a = to_ip;   // reply directly if no broadcast address is supplied
  		}
  		DatagramPacket sendPacket = new DatagramPacket(replyBuffer, replyBuffer.length, a, ARTNET_PORT);
		try {
			socket.send(sendPacket);
		} catch ( Exception e) {
			System.out.println("send poll reply exception " + e);
		}
	}

	/**
	 * sends an ArtAddress packet containing a command
	 * <p>See Art-Net specification for commands eg. AcCancelMerge and AcClearOp0.</p>.
	 * @param socket Open and configured socket used to send the packet.
	 * @param to_ip address to which the packet is sent
	 * @param command the ArtAddressCommand
	 */
	public void sendArtAddressCommand ( DatagramSocket socket, InetAddress to_ip, int command ) {
		byte[] buffer = new byte[ARTNET_ART_ADDRESS_SIZE];
  		int i;
  		for ( i = 0; i < ARTNET_ART_ADDRESS_SIZE; i++ ) {
  			buffer[i] = 0;
  		}
  		setStringInByteArray("Art-Net", buffer, 0, true);
  		buffer[8] = 0;        // op code lo-hi
  		buffer[9] = (byte)0x21;
  		buffer[10] = 0;        // op code hi-lo
  		buffer[11] = (byte)14;
  		for(int j=96; j<104; j++) {
  			buffer[j] = (byte)0x7f;//no change
  		}
  		buffer[106] = (byte)command;
	}
	
	/**
	 * parses and ArtAddress type packet
	 * <p>Supports remote setting of subnet/universe address<BR>
	 * Supports AcCancelMerge and AcClearOp0 commands.</p>.
	 * @return opcode Can be ARTNET_ART_DMX so caller of processDatagramPacket can respond to changed data
	 */
	public int parseArtAddress() {
		setNetAddress(_packet_buffer[12]);
		//[14] to [31] short name <= 18 bytes
		//[32] to [95] long name  <= 64 bytes
		//[96][97][98][99]                  input universe   ch 1 to 4
		//[100][101][102][103]               output universe   ch 1 to 4
		setUniverseAddress(_packet_buffer[100]);
		//[104]                   subnet switch
		setSubnetAddress(_packet_buffer[104]);
		//[105]                                   reserved
		int command = byte2int(_packet_buffer[106]); // command
		switch ( command ) {
			case 0x01:	//cancel merge: resets ip address used to identify dmx sender
				_dmx_source1 = null;
				_dmx_source2 = null;
				for(int j=0; j<DMX_UNIVERSE_MAX; j++) {
					_dmx_buffer2[j] = 0;
				}
				_dmx_slots = 0;
				break;
			case 0x90:	//clear buffer
				_dmx_source1 = null;
				_dmx_source2 = null;
				clearSlots();
				return ARTNET_ART_DMX;	// return ARTNET_ART_DMX so function calling readPacket
										// knows there has been a change in levels
		}
		return ARTNET_ART_ADDRESS;
	}
	

	/**
	 * utility for testing Art-Net packet header
	 * @return Opcode of Art-Net packet or ARTNET_NOP
	 */
	public int parseHeader() {
		String header = new String( _packet_buffer, 0, 7 );
		if ( header.equals("Art-Net") ) {
			return byte2int(_packet_buffer[9]) * 256 + byte2int(_packet_buffer[8]);  //opcode lo byte first
		}
		return ARTNET_NOP;
	}
	
}