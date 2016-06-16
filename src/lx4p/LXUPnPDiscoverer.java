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

import javax.xml.parsers.*;

import org.w3c.dom.*;

/**
 *  LXUPnPDiscoverer is a class for implementing Universal Plug and Play discovery.
 *  It implements runnable so it can search for a device in its own thread.
 *  
 *  LXUPnPDiscoverer sends an M-SEARCH to the SSDP multicast address.
 *  It then listens for reply packets.
 *  When it receives an SSDP packet containing the targetServer,
 *  it retrieves the XML device description from the location in the SSDP packet.
 *  It calls its delegate's foundURLBase method with the device URLBase string.
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXUPnPDelegate
 */

public class LXUPnPDiscoverer extends Object implements Runnable  {
	
	public static byte[] UPNP_MULTICAST_ADDRESS = new byte[]{(byte)224,(byte)0,(byte)0,(byte)251};
	public static int UPNP_MULTICAST_PORT = 1900;
	
	/**
	 *  A socket for network communication
	 */
	DatagramSocket udpsocket = null;
	
	/**
	 *  The string to look for in the SSDP server: line
	 */
	String targetServer = null;
	
	/**
	 *  The URLBase string returned when querying the location found with SSDP
	 *  for the device description in XML
	 */
	public String urlBase = null;
	
	/**
	 *  A delegate to receive the foundURLBase() call when a device description
     *  matching the target string is located and read.
	 */
	public LXUPnPDelegate delegate = null;
	
	boolean searching;
	
	/**
	 * buffer for reading and sending packets
	 */
	byte[] _packet_buffer = new byte[1024];
	
	/**
	 * @param target string to look for in received SSDP packets
	 */
	public LXUPnPDiscoverer(String target) {
		targetServer = target;
	}
	
	public void setDelegate(LXUPnPDelegate d) {
		delegate = d;
		d.setLXUPnPDiscoverer(this);
	}
	
	public void run() {
		searching = true;
		long lastSearchSent = 0;
		while ( searching ) {
			if ( System.currentTimeMillis() > lastSearchSent ) {
				sendSearch();
				lastSearchSent = System.currentTimeMillis() + 5000;
			}
			if ( readPacket() == 0 ) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					searching = false;
				}
			}
		}
		udpsocket.close();
		udpsocket = null;
	}
	
	/**
	 * Attempt to read an SSDP packet from udpsocket
	 * When an SSDP packet is received, looks for the targetServer string in the server line.
	 * If the packet is of interest, use the location to read the device description XML.
	 * Calls getDescription to read the URLBase property from the XML returned from location
	 * @return 0 for no packet, 1 for packet but did not retrieve urlBase, 2 if got urlBase
	 */
	public int readPacket() {
		int rstatus = 0;
		if ( udpsocket != null ) {
			DatagramPacket receivePacket = new DatagramPacket(_packet_buffer, _packet_buffer.length);
			try {
				udpsocket.receive(receivePacket);
				if ( receivePacket.getLength() > 0 ) {
					rstatus = 1;
					byte[] receivedData = receivePacket.getData();
					String receivedString = new String(receivedData, "UTF-8");
					if ( receivedString.startsWith("NOTIFY * HTTP/1.1") || receivedString.startsWith("HTTP/1.1 200 OK") ) {
						String lines[] = receivedString.split("\\r?\\n");
						String server = null;
						String location = null;
						for (int li=0; li<lines.length; li++) {
							String t[] = lines[li].split(" ");
							if ( t.length >= 2 ) {
								if ( t[0].toLowerCase().equals("location:")) {
									location = t[1];
								} else if ( t[0].toLowerCase().equals("server:")) {
									server = lines[li];
								}
							}
						}
						if ((location != null) && (server != null)) {
							int sindex = server.indexOf(targetServer);
							if ( sindex >= 0 ) {
								getDescription(new URL(location));
								if (urlBase != null) {
									rstatus = 2;
									searching = false;
									if ( delegate != null ) {
										delegate.foundURLBase(urlBase);
									}
								}
							}
						}
					}
				}
			} catch ( Exception e) {
				// will catch receive time out exception
				//System.out.println("receive exception " + e);
			}
		}
		return rstatus;
	}
	
	/**
	 * Sends a search request to the SSDP multicast address.
	 * The search target is any basic device on the network
	 */
	public void sendSearch() {
		if ( udpsocket != null ) {
			String message = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX:3\r\nST: urn:schemas-upnp-org:device:basic:1";
			byte[] mbytes = message.getBytes();
			try {
				DatagramPacket sendPacket = new DatagramPacket(mbytes, 0, mbytes.length, new InetSocketAddress(InetAddress.getByAddress(UPNP_MULTICAST_ADDRESS), UPNP_MULTICAST_PORT));
				udpsocket.send(sendPacket);
			} catch ( Exception e) {
				System.out.println("UPnP send search exception " + e);
			}
		}
	}
	
	/**
	 * Reads an XML Description and extracts the URLBase
	 * @param location is the URL of the description XML resource
	 */
	public void getDescription(URL location) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(location.openStream());
			NodeList nlist = doc.getElementsByTagName("URLBase");
			if ( nlist.getLength() > 0 ) {
				Node node = nlist.item(0);
	            urlBase = node.getTextContent();
			}
		} catch ( Exception e) {
			System.out.println("UPnP exception retreiving URLBase" + e);
		}
	}
	
	/**
	 * stops the search and closes the udpsocket if it exists
	 */
	public void close() {
		if ( searching ) {
			searching = false;	// setting this to false will cause run() loop to exit, closing udpsocket
		} else if ( udpsocket != null ) {
			udpsocket.close();
			udpsocket = null;
		}
	}
	
	/**
	 * Factory method to create an LXUPnPDiscoverer
	 * @param networkInterface if not null, searches for an InetAddress associated with the named interface (eg "en0")
	 * @param networkAddress specific IP address to use for binding socket.  Can be null if networkInterface is specified
	 * @return created instance of LXUPnPDiscovereror null if socket could not be opened
	 */
	
	public static LXUPnPDiscoverer createUPnPDiscoverer(String networkInterface, String networkAddress, String target) {
		LXUPnPDiscoverer upnpd = null;
		String myNetworkAddress = networkAddress;

		// look through available NetworkInterfaces for one matching networkInterface
		// lists addresses leaving myNetworkAddress set to the last one found
		// repeats if not found and prints interfaces
		boolean searchDone = false;
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
		            if ( astr.indexOf(":") < 0 ) {                   //IPv6 addresses contain colons
		            	if ( printNames ) {
		            		System.out.println("   " + astr);    
		            	} else {
			            	myNetworkAddress = astr;
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
	      
	      upnpd = new LXUPnPDiscoverer(target);
	      upnpd.udpsocket = new DatagramSocket( null );
	      upnpd.udpsocket.setReuseAddress(true);
	         if ( networkAddress.equals("0.0.0.0") ) {
	        	 upnpd.udpsocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 1900));
	         } else {
	        	 upnpd.udpsocket.bind(new InetSocketAddress(nicAddress, 1900));
	         }
	         upnpd.udpsocket.setSoTimeout(1000);
	         upnpd.udpsocket.setBroadcast(true);
	   }  catch ( Exception e) {
	      System.out.println("Can't open socket for UPnP discovery " + e);
	      upnpd = null;
	   }

	    return upnpd;
	}
	
	
	public static void startDiscovery(String networkInterface, String networkAddress, String target, LXUPnPDelegate d) {
		LXUPnPDiscoverer explr = LXUPnPDiscoverer.createUPnPDiscoverer(networkInterface, networkAddress, target);
		explr.setDelegate(d);
		Thread runner = new Thread ( explr );
		runner.start();
	}
	
}