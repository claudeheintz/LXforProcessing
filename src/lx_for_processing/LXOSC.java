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

package lxprocessing;

import java.net.*;
import java.util.*;

/** LXOSC
 * 
 * <p>LXOSC provides an interface for sending and receiving OSC 1.1 packets.</p>
 * 
*/


public class LXOSC  {

	public static final int OSC_BUFFER_MAX = 400;

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
	 * @return Vector of LXOSCMessage objects
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
				pr.parseBuffer(receivedData, receivedDataLength, _packet_buffer.length);
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
	
}