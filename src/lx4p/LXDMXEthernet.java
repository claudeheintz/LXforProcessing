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

/**
 * LXDMXEthernet
 * 
 * <p>LXArtNet partially implements the Art-Net Ethernet Communication Standard.<BR>
 * http://www.artisticlicence.com</p>
 * 
 * <p>LXArtNet supports capturing a single universe of DMX data<BR>
 * from Art-Net packets read from UDP.<BR>
 * LXArtNet will automatically respond to ArtPoll packets<BR>
 * by sending a unicast reply directly to the poll.</p>
 * 
 * <p>LXArtNet does not support merge and will only accept ArtDMX output packets <BR>
 * from the first IP address that it receives a packet from.<BR>
 * This can be reset by sending an ArtAddress cancel merge command.</p>
 * 
 * <p>Art-Net(TM) Designed by and Copyright Artistic Licence Holdings Ltd.</p>
 * @author Claude Heintz
 */

public abstract class LXDMXEthernet extends LXDMXInterface  {
	
	public static final boolean CREATE_ARTNET = false;
	public static final boolean CREATE_SACN = true;
	
	/**
	 * holds Socket for network communication
	 */
	DatagramSocket dmxsocket = null;

	/**
	 * port for protocol subclass
	 * @return the standard network port for the protocol subclass
	 */
	public abstract int getPort();
	
	/**
	 * attempt to read a protocol packet
	 * @param socket Open and configured socket used to read the dmx protocol packet
	 * @return true if packet contained dmx output
	 */
	public abstract boolean readPacket(DatagramSocket socket);
	
	/**
	 * attempt to read a protocol packet from this.udpsocket
	 */
	public boolean readPacket() {
		if ( dmxsocket != null ) {
			 return this.readPacket(dmxsocket);
		}
		return false;
	}
	
	/**
	 * send a dmx packet containing level data stored in the data buffer
	 * <p>Assumes that the socket is already setup for the type of address unicast/broadcast/multicast.</p>
	 * @param socket Open and configured socket used to read the dmx protocol packet
	 * @param to_ip InetAddress to which packet is sent.
	 */
	public abstract void sendDMX ( DatagramSocket socket, InetAddress to_ip );
	/**
	 * send a dmx packet containing level data stored in the data buffer to a preset address
	 * <p>assumes that the socket is already setup for the type of address unicast/broadcast/multicast</p>
	 * @param socket open and configured socket used to send the dmx protocol packet
	 */
	public abstract void sendDMX ( DatagramSocket socket );
	
	/**
	 * attempt to send a dmx packet using this.udpsocket
	 */
	public void sendDMX() {
		if ( dmxsocket != null ) {
			this.sendDMX(dmxsocket);
		}
	}
	
	/**
	 * closes the udpsocket if it exists
	 */
	public void close() {
		if ( dmxsocket != null ) {
			dmxsocket.close();
		}
	}
	
	/**
	 * Factory method to create an LXDMXEthernet interface
	 * @param use_sacn	Determines if returned interface will be LXArtNet or LXSACN class
	 * @param networkInterface if not null, searches for an InetAddress associated with the named interface (eg "en0")
	 * @param networkAddress specific IP address to use for binding socket.  Can be null if networkInterface is specified
	 * @param targetAddress either broadcast address for Art-Net or multicast address for sACN
	 * @return created instance of LXArtNet or LXSACN or null if socket could not be opened
	 */
	
	public static LXDMXEthernet createDMXEthernet(boolean use_sacn, String networkInterface, String networkAddress, String targetAddress ) {
		LXDMXEthernet dmx = null;
		String myNetworkAddress = networkAddress;
		InetAddress myBroadcast = null;
		
		// look through available NetworkInterfaces for one matching networkInterface
		// lists addresses leaving myNetworkAddress set to the last one found
		// repeats if not found and prints interfaces
		boolean searchDone = (networkInterface==null);
		boolean searched = false;
		while ( ! searchDone ) {
			boolean printNames = searched;
			boolean dosearch = printNames;
			try {
		      Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		      while ( nets.hasMoreElements() ) {
		        NetworkInterface nic = nets.nextElement();
		        List<InterfaceAddress> interfaceAddresses = nic.getInterfaceAddresses();
		        if ( printNames ) {
		        	if ( interfaceAddresses.size() > 0 ) {
		        		System.out.println("() " + nic.getName() );
		        	}
		        } else {
		        	dosearch = nic.getName().equals(networkInterface);
		        }
		        if ( dosearch) {
		          Iterator<InterfaceAddress>inetAddresses = interfaceAddresses.iterator();
		          while ( inetAddresses.hasNext() ) {
		        	InterfaceAddress addr = inetAddresses.next();
		            String astr = addr.getAddress().getHostAddress();
		            if ( astr.indexOf(":") < 0 ) {                   //IPv6 addresses contain colons, skip
		            	if ( printNames ) {
		            		System.out.println("   " + astr);    
		            	} else {
			            	myNetworkAddress = astr;
			            	myBroadcast = addr.getBroadcast();
			            	System.out.println("Found address for " + networkInterface + " => " + myNetworkAddress);
			            	searchDone = true;
			            }
		            }
		          }
		        }
		      }
		    } catch (Exception e) {
		    	System.out.println("Warning: exception thrown while searching network interfaces.");
		    }
			if ( ! searchDone ) {
				if ( searched ) {
					searchDone = true;
				} else {
					searched = true;
					System.out.println("Could not find address for " + networkInterface + ".  Interfaces found:");
				}
			}
		}
		  
	    try {
	      InetAddress nicAddress = InetAddress.getByName(myNetworkAddress);
	      
	      if ( use_sacn ) {       // ********* sACN E1.31 *********
	         InetAddress maddr = InetAddress.getByName(targetAddress);	//universe 1 target = "239.255.0.1"
	         dmx = new LXSACN(maddr);
	         
	         if ( targetAddress.startsWith("239.") ) {
		         if ( networkAddress.equals("0.0.0.0") ) {
		        	 dmx.dmxsocket = new MulticastSocket( dmx.getPort() );
		        	 dmx.dmxsocket.setReuseAddress(true);
		         } else {
		        	 dmx.dmxsocket = new MulticastSocket( null);
		        	 dmx.dmxsocket.setReuseAddress(true);
		        	 dmx.dmxsocket.bind( new InetSocketAddress(nicAddress, dmx.getPort()) );
		         }
		         
		         dmx.dmxsocket.setSoTimeout(1000);
		         NetworkInterface nic = NetworkInterface.getByInetAddress(nicAddress);
		         ((MulticastSocket)dmx.dmxsocket).joinGroup(new InetSocketAddress(maddr, dmx.getPort()), nic);
		         ((LXSACN)dmx).setCIDwithMACAddress(nic.getHardwareAddress());
	
		         if ( dmx != null ) {
		  		   System.out.println("Created dmx interface using: " + myNetworkAddress + " multicast: " + targetAddress);
		  	     }
	         } else {	// not multicast target
	        	 dmx.dmxsocket = new DatagramSocket( null );
		         dmx.dmxsocket.setReuseAddress(true);
		         if ( networkAddress.equals("0.0.0.0") ) {
		        	 dmx.dmxsocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), dmx.getPort()));
		         } else {
		        	 dmx.dmxsocket.bind(new InetSocketAddress(nicAddress, dmx.getPort()));
		         }
		         dmx.dmxsocket.setSoTimeout(1000);
		         
		         if ( dmx != null ) {
			  		   System.out.println("Created dmx interface using: " + myNetworkAddress + " sending to: " + targetAddress);
			  	 }
	         }
	         
	      } else {               // ********* Art-Net *********
	         InetAddress baddr;
	         if (( myBroadcast != null ) && (targetAddress == null)) {
	        	 baddr = myBroadcast;
	         } else {
	        	 baddr = InetAddress.getByName(targetAddress);	//returns 127.0.0.1 if targetAddress == null ?
	         }
	         dmx = new LXArtNet(nicAddress, baddr);
	         
	         dmx.dmxsocket = new DatagramSocket( null );
	         dmx.dmxsocket.setReuseAddress(true);
	         if ( networkAddress.equals("0.0.0.0") ) {
	        	 dmx.dmxsocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), dmx.getPort()));
	        	 System.out.println("Bind to 0.0.0.0");
	         } else {
	        	 dmx.dmxsocket.bind(new InetSocketAddress(nicAddress, dmx.getPort()));
	         }
	         dmx.dmxsocket.setSoTimeout(1000);
	         dmx.dmxsocket.setBroadcast(true);
	         
	         if ( dmx != null ) {
	  		   System.out.println("Created dmx interface using: " + myNetworkAddress + " sending to: " + baddr.getHostAddress());
	  	     }
	      }
	   }  catch ( Exception e) {
	      System.out.println("Can't open socket " + e);
	      dmx = null;
	   }

	    return dmx;
	}
	
}