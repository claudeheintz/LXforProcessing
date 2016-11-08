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
import java.util.*;


/** LXOSC
 * 
 * <p>LXOSC provides an interface for sending and receiving OSC 1.1 packets.</p>
 * 
*/


public class LXOSC  {

	public static final int OSC_BUFFER_MAX = 1024;

	/**
	 * buffer for reading and sending packets
	 */
	byte[] _packet_buffer = new byte[OSC_BUFFER_MAX];
	
	DatagramSocket oscsocket = null;
	
	public InetAddress receivedFrom = null;

	/**
	 * construct LXOSC interface
	 */
	public LXOSC() {
	}
	
	/**
	 * Set the DatagramSocket used for sending/receiving
	 * @param sk DatagramSocket
	 */
	public void setDatagramSocket(DatagramSocket sk) {
		oscsocket = sk;
	}
	
	/**
	 * close the socket
	 */
	public void close() {
		if ( oscsocket != null ) {
			oscsocket.close();
			oscsocket = null;
		}
	}
	
	/**
	 * Factory method to create an LXOSC interface containing a configured datagram socket
	 * @param networkInterface if not null, searches for an InetAddress associated with the named interface (eg "en0")
	 * @param networkAddress specific IP address to use for binding socket.  Can be null if networkInterface is specified
	 * @param port UDP port for receiving packets usually used with networkInterface = null and networkAddress = "0.0.0.0"
	 * @return LXOSC object
	 */
	public static LXOSC createLXOSC( String networkInterface, String networkAddress, int port ) {
		LXOSC osc = null;
		String myNetworkAddress = networkAddress;
		
		// look through available NetworkInterfaces for one matching networkInterface
		// lists addresses leaving myNetworkAddress set to the last one found
		if ( networkInterface != null ) {
		    try {
		      Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		      while ( nets.hasMoreElements() ) {
		        NetworkInterface nic = nets.nextElement();
		        if ( nic.getName().equals(networkInterface) ) {
		          List<InterfaceAddress> interfaceAddresses = nic.getInterfaceAddresses();
		          Iterator<InterfaceAddress>inetAddresses = interfaceAddresses.iterator();
		          while ( inetAddresses.hasNext() ) {
		        	InterfaceAddress addr = inetAddresses.next();
		            String astr = addr.getAddress().getHostAddress();
		            if ( astr.indexOf(":") < 0 ) {                   //IPv6 addresses contain colons
		            	myNetworkAddress = astr;
		            }
		          }
		        }
		      }
		    } catch (Exception e) {
		      System.out.println("Could not get address for " + networkInterface);
		    }
		}
		  
	    try {
	      InetAddress nicAddress = InetAddress.getByName(myNetworkAddress);

	      osc = new LXOSC();
	      osc.oscsocket = new DatagramSocket( null );
	      osc.oscsocket.setReuseAddress(true);
	      if ( networkAddress.equals("0.0.0.0") ) {
	    	  osc.oscsocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
	      } else {
	    	  osc.oscsocket.bind(new InetSocketAddress(nicAddress, port));
	      }
	      osc.oscsocket.setSoTimeout(50);	//don't wait long for packet...
         
	      if ( osc != null ) {
	    	  System.out.println("Created osc interface using: " + myNetworkAddress);
	      }
	   }  catch ( Exception e) {
	      System.out.println("Can't open socket " + e);
	      osc = null;
	   }
	    
	    return osc;
	}

	/**
	 * attempt to read an OSC packet from oscsocket using LXOSCPacketReader object
	 * @return Vector of LXOSCMessage objects (can be empty but should not be null)
	 */
	public Vector<LXOSCMessage> readPacket() {
		LXOSCPacketReader pr = new LXOSCPacketReader();
		receivedFrom = null;
		if ( oscsocket != null ) {
			DatagramPacket receivePacket = new DatagramPacket(_packet_buffer, _packet_buffer.length);
			try {
				oscsocket.receive(receivePacket);
				byte[] receivedData = receivePacket.getData();
				int receivedDataLength = receivePacket.getLength();
				pr.parseBuffer(receivedData, receivedDataLength);
				if ( pr.results().size() > 0 ) {
					receivedFrom = receivePacket.getAddress();
				}
			} catch ( Exception e) {
				//will catch receive time out exception
				//System.out.println("receive exception " + e);
			}
		}
      return pr.results();
	}

	/**
	 * Sends OSC message packet to an address/port
	 * @param msg holds address pattern and arguments
	 * @param to_addr String address to which packet is sent
	 * @param port port number for sending packet
	 */
	public void sendOSC ( LXOSCMessage msg, String to_addr, int port ) {
		try {
			sendOSC(msg, InetAddress.getByName(to_addr), port);
		} catch ( Exception e) {
			System.out.println("send osc address exception " + e);
		}
	}
	
	/**
	 * Sends OSC message packet to an address/port
	 * @param msg holds address pattern and arguments
	 * @param to_ip InetAddress to which packet is sent
	 * @param port port number for sending packet
	 */
	public void sendOSC ( LXOSCMessage msg, InetAddress to_ip, int port ) {
		if ( oscsocket != null ) {
			int osc_packet_length = msg.addOSCMessageToBytes(_packet_buffer);
			if ( osc_packet_length <= 0 ) {
				return; // addOSCMessageToBytes returned error
			}
	
			try {
		  		DatagramPacket sendPacket = new DatagramPacket(_packet_buffer, osc_packet_length, to_ip, port);
				oscsocket.send(sendPacket);
			} catch ( Exception e) {
				System.out.println("send osc exception " + e);
			}
		}
	}
	
/***
 * Converts Java time to 64-bit NTP time representation.
 * This method is from https://commons.apache.org/proper/commons-net/apidocs/src-html/org/apache/commons/net/ntp/TimeStamp.html
 * This method is covered under the following license:  http://www.apache.org/licenses/LICENSE-2.0
 * @param t Java time
 * @return NTP timestamp representation of Java time value.
 */
	protected static long toNtpTime(long t) {
		boolean useBase1 = t < 2085978496000L;    // time < Feb-2036
		long baseTime;
		if (useBase1) {
			baseTime = t + 2208988800000L; // dates <= Feb-2036
		} else {
			// if base0 needed for dates >= Feb-2036
			baseTime = t - 2085978496000L;
		}

		long seconds = baseTime / 1000;
		long fraction = ((baseTime % 1000) * 0x100000000L) / 1000;

		if (useBase1) {
			seconds |= 0x80000000L; // set high-order bit if msb1baseTime 1900 used
		}

		long time = seconds << 32 | fraction;
		return time;
	}
	
	public void sendOSCBundle(Vector<LXOSCMessage> msgs, InetAddress to_ip, int port ) {
		if ( oscsocket != null ) {
			LXOSCBundleMessage bmsg = new LXOSCBundleMessage(msgs);
			int msgsize = bmsg.addOSCMessageToBytes(_packet_buffer, 0);
			if ( msgsize <= 0 ) {
				return;
			}
			
		   /*_packet_buffer[0] = '#';
		   _packet_buffer[1] = 'b';
		   _packet_buffer[2] = 'u';
		   _packet_buffer[3] = 'n';
		   _packet_buffer[4] = 'd';
		   _packet_buffer[5] = 'l';
		   _packet_buffer[6] = 'e';
		   _packet_buffer[7] = 0;
		   
		   // handle timestamp here
		   //long mtime = Calendar.getInstance().getTimeInMillis() - 220898880L;	//convert from Epoch to 1900 still milliseconds
		   //long time = Double.doubleToLongBits((System.currentTimeMillis()/1000.0) + 2208988800.0);						//64 bit float seconds to bytes
		   long time = toNtpTime(System.currentTimeMillis());
		   _packet_buffer[8]  = (byte)((time >> 56) & 0xff);
		   _packet_buffer[9]  = (byte)((time >> 48) & 0xff);
		   _packet_buffer[10]  = (byte)((time >> 40) & 0xff);
		   _packet_buffer[11]  = (byte)((time >> 32) & 0xff);
		   _packet_buffer[12]  = (byte)((time >> 24) & 0xff);
		   _packet_buffer[13]  = (byte)((time >> 16) & 0xff);
		   _packet_buffer[14]  = (byte)((time >> 8) & 0xff);
		   _packet_buffer[15]  = (byte)(time & 0xff);
		   
		   int s = 20;
		   Enumeration<LXOSCMessage> en = msgs.elements();
		   LXOSCMessage msg;
		   int msgsize;
		   int t = s;
		   while ( en.hasMoreElements() ) {
			   msg = en.nextElement();
			   t = msg.addOSCMessageToBytes(_packet_buffer, s);
			   msgsize = t-s;
			   if ( msgsize <= 0 ) {
					return; // addOSCMessageToBytes returned error
			   }
			   _packet_buffer[s-4] = (byte) ((msgsize>>24) & 0xFF);
			   _packet_buffer[s-3] = (byte) ((msgsize>>16) & 0xFF);
			   _packet_buffer[s-2] = (byte) ((msgsize>>8) & 0xFF);
			   _packet_buffer[s-1] = (byte) (msgsize & 0xFF);
			   s = t + 4;	//leave room for next size bytes
		   }*/
		
		   try {
		  		DatagramPacket sendPacket = new DatagramPacket(_packet_buffer, msgsize, to_ip, port);
				oscsocket.send(sendPacket);
			} catch ( Exception e) {
				System.out.println("send osc exception " + e);
			}
		}
	}
	
}