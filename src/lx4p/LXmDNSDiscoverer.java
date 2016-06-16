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

/**
 *  LXmDNSDiscoverer is a class for implementing mDNS discovery of devices
 *  on a local network.
 *  
 *  LXmDNSDiscoverer receives DNS packets on a thread and informs its
 *  delegate of their contents.
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXUPnPDelegate
 */

public class LXmDNSDiscoverer extends Object implements Runnable  {
	
	/**
	 *  A socket for network communication
	 */
	MulticastSocket multisocket = null;
	
	/**
	 *  The string to look for in the fully qualified domain name string
	 */
	String targetName = null;
	
	/**
	 *  The type of record (255 is all)
	 */
	int targetType = 1;
	
	public static byte[] MDNS_MULTICAST_ADDRESS = new byte[]{(byte)224, (byte)0, (byte)0, (byte)251};
	public static int MDNS_MULTICAST_PORT = 5353;
	
	/**
	 *  A delegate to receive the foundURLBase() call when a device description
     *  matching the target string is located and read.
	 */
	public LXmDNSDelegate delegate = null;
	
	/**
	 * set while listening thread is running
	 */
	boolean listening;
	
	/**
	 * can also passively wait for queries
	 */
	boolean searchMode = true;
	
	public static int LXMDNS_PRINT_SILENT = 0;
	public static int LXMDNS_PRINT_ERRORS = 1;
	public static int LXMDNS_PRINT_ALL = 2;
	int printlevel = LXMDNS_PRINT_SILENT;
	
	/**
	 * buffer for reading and sending packets
	 */
	byte[] _packet_buffer = new byte[1024];
	
	/**
	 * sequence number identified reply records as belonging to the same query or response
	 */
	int _packetID = 0;
	
	/**
	 * @param target string to look for in fully qualified domain name string
	 */
	public LXmDNSDiscoverer(String target, int type) {
		targetName = target;
		targetType = type;
	}
	
	public void setDelegate(LXmDNSDelegate d) {
		delegate = d;
		d.setLXmDNSDiscoverer(this);
	}
	
	public void setPrintLevel(int v) {
		printlevel = v;
	}
	
	public void setSearchMode(boolean sm) {
		searchMode = sm;
	}
	
	public void printError( String estr ) {
		if ( printlevel > LXMDNS_PRINT_SILENT ) {
			System.out.println(estr);
		}
	}
	
	public static void printError(int level, String estr) {
		if ( level > LXMDNS_PRINT_SILENT ) {
			System.out.println(estr);
		}
	}

	public void printMessage(String mstr) {
		if ( printlevel > 1 ) {
			System.out.println(mstr);
		}
	}
	
	public void run() {
		listening = true;
		long lastSearchSent = 0;
		while ( listening ) {
			if ( searchMode && ( System.currentTimeMillis() > lastSearchSent ) ) {
				sendSearch();
				lastSearchSent = System.currentTimeMillis() + 5000;
			}
			if ( readPacket() == 0 ) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					printError("mdns thread sleep exception " + e);
					listening = false;
				}
			}
		}
		multisocket.close();
		multisocket = null;
	}
	
	public LXDNSReplyRecord readReplyRecordFromPacket(byte[] bpacket, int start, boolean isQuery) {
		return readReplyRecordFromPacket(bpacket, start, isQuery, false);
	}
	
	public LXDNSReplyRecord readReplyRecordFromPacket(byte[] bpacket, int start, boolean isQuery, boolean labelsOnly) {
		int s = start;
		boolean done = false;
		StringBuffer sb = new StringBuffer();
		while ( ! done ) {
			int l = (bpacket[s] & 0xFF);
			if ( l != 0 ) {
				s++;
				if ( (l &0xC0) == 0xC0 ) {	//upper two bits set indicate pointer
					// handle pointer  bpacket[s] is pointer and NO terminator
					int p = (bpacket[s] & 0xff) + ((l & 0x2F) << 8);
					LXDNSReplyRecord resolvePtr = readReplyRecordFromPacket(bpacket, p, false, true);
					if ( sb.length() != 0 ) {
						sb.append(".");
					}
					sb.append(resolvePtr.getName());
					break;
				}
				if ( sb.length() != 0 ) {
					sb.append(".");
				}
				String lstr = new String( Arrays.copyOfRange(bpacket, s, s+l));
				sb.append(lstr);
				s += l;
			} else {
				done = true;
			}
		}
		if ( labelsOnly ) {
			return new LXDNSReplyRecord(sb.toString(),0,0,0,0, null);
		}
		
		int qtype = ((bpacket[s+1]&0xFF)<<8) + (bpacket[s+2]&0xFF);
		int qclass = ((bpacket[s+3]&0xFF)<<8) + ( bpacket[s+4]&0xFF);	// 0x8000 bit is cache flush (extracted in reply record)
		s += 5;//1@zero term + 2@qt + 2@qc
		if ( isQuery ) {
			return new LXDNSReplyRecord(sb.toString(), qtype, qclass, 0, s, null);	//query has no data!
		}
		// TTL is next 4 bytes
		long ttl = ((bpacket[s]&0xFF)<<24) + ((bpacket[s+1]&0xFF)<<16) + ((bpacket[s+2]&0xFF)<<8) + (bpacket[s+3]&0xFF);
		s += 4;	//4@ttl
	    int datalength = ((bpacket[s]&0x7F)<<8) + ( bpacket[s+1]&0xFF);	// 0x8000 bit is cache flush
		s+=2;
		return new LXDNSReplyRecord(sb.toString(), qtype, qclass, ttl, s, Arrays.copyOfRange(bpacket, s, s+datalength));
	}
	
	/**
	 * Attempt to read an mDNS packet from multisocket
	 */
	public int readPacket() {
		int rstatus = 0;
		if ( multisocket != null ) {
			DatagramPacket receivePacket = new DatagramPacket(_packet_buffer, _packet_buffer.length);
			byte[] bpacket = null;
			boolean isQuery = false;
			boolean isResponse = false;
			try {
				multisocket.receive(receivePacket);
				bpacket = receivePacket.getData();
				if ((bpacket[0] + bpacket[1]) != 0 ) {
					printError("bad dns packet header");
					return 0;
				}
				isQuery = (( bpacket[2] == 0 ) && ( bpacket[3] == 0 ));
				isResponse = (( (bpacket[2]&0xFF) == 0x84 ) && ( bpacket[3] == 0 ));
			    boolean done = ( !(isQuery||isResponse));  // standard query response, no error
			    if ( ! done ) {
			    	int questionRecords = ((bpacket[4]&0xFF)<<8) + (bpacket[5]&0xFF);
			    	int answerRecords = ((bpacket[6]&0xFF)<<8) + (bpacket[7]&0xFF);
			    	int authorityRecords = ((bpacket[8]&0xFF)<<8) + (bpacket[9]&0xFF);
			    	int additionalRecords = ((bpacket[10]&0xFF)<<8) + (bpacket[11]&0xFF);
			    	int s = 12;
			    	LXDNSReplyRecord rr;
			    	_packetID++;		//identifies all responses as belonging to a group
			    	
			    	printMessage("? " +questionRecords + ", * " + answerRecords + ", ! " + authorityRecords + ", + " + additionalRecords);
			    	
			    	for(int i=0; i<questionRecords; i++) {
			    		rr = readReplyRecordFromPacket(bpacket, s, isQuery);
			    		rr.setPacketID(_packetID);
			    		printMessage("question" + (i+1) + ": " + rr.getName() + ", " + rr.getQType() + ", " + rr.getQClass());
			    		if ( delegate != null ) {
			    			delegate.receivedMDNSQueryRecord(rr);
			    		}
			    		s=rr.getNextLocation();
			    	}
			    	for(int i=0; i<answerRecords; i++) {
			    		rr = readReplyRecordFromPacket(bpacket, s, isQuery);
			    		rr.setPacketID(_packetID);
			    		printMessage("answer" + (i+1) + ": " + rr.getName() + ", " + rr.getQType() + ", " + rr.getQClass());
			    		if ( delegate != null ) {
			    			delegate.receivedMDNSQueryAnswerRecord(rr);
			    		}
			    		s=rr.getNextLocation();
			    	}
			    	for(int i=0; i<authorityRecords; i++) {
			    		rr = readReplyRecordFromPacket(bpacket, s, isQuery);	//todo determine if false or isQuery
			    		rr.setPacketID(_packetID);
			    		printMessage("authority" + (i+1) + ": " + rr.getName() + ", " + rr.getQType() + ", " + rr.getQClass());
			    		if ( delegate != null ) {
			    			delegate.receivedMDNSQueryAnswerRecord(rr);
			    		}
			    		s=rr.getNextLocation();
			    	}
			    	for(int i=0; i<additionalRecords; i++) {
			    		rr = readReplyRecordFromPacket(bpacket, s, isQuery);
			    		rr.setPacketID(_packetID);
			    		printMessage("additional" + (i+1) + ": " + rr.getName() + ", " + rr.getQType() + ", " + rr.getQClass());
			    		if ( delegate != null ) {
			    			delegate.receivedMDNSQueryAnswerRecord(rr);
			    		}
			    		s=rr.getNextLocation();
			    	}
			    	rstatus = 1;
			    }
			} catch ( Exception e) {
			      //   will catch receive time out exception
			      if ( ! (e instanceof java.net.SocketTimeoutException ) ) {
			    	printError("multicast receive exception " + e);
			      }
			}
		}
		return rstatus;
	}
	
	/**
	 * sends a question record asking for A type, IN class records matching targetName
	 * 
	 */
	public void sendSearch() {
		if ( multisocket != null ) {
			byte[] mbytes = new byte[255];
			String[] labels = targetName.split("[.]");
			int s = 0;
			for ( ; s<12; s++ ) {
				mbytes[s] = 0;
			}
			mbytes[5] = 1;	// one question record
			for(int i=0; i<labels.length; i++) {
				try {
					byte[] sbytes = labels[i].getBytes("UTF-8");
					int l = sbytes.length;
					for(int j=0;j<l; j++) {
						mbytes[s+j+1] = sbytes[j];
					}
					mbytes[s] = (byte) l;
					s+=(l+1);
				} catch(Exception e) {
					return;
				}
			}
			mbytes[s] = 0;	// termination
			s++;
			mbytes[s] = 0;	// type A
			s++;
			mbytes[s] = (byte)targetType;
			s++;
			mbytes[s] = 0;  // class IN
			s++;
			mbytes[s] = 1;
			s++;
			
			try {
				DatagramPacket sendPacket = new DatagramPacket(mbytes, 0, s, new InetSocketAddress(InetAddress.getByAddress(MDNS_MULTICAST_ADDRESS), MDNS_MULTICAST_PORT));
				multisocket.send(sendPacket);
			} catch ( Exception e) {
				printError("mDNS send search exception " + e);
			}
			
		}
	}
	
	
	/**
	 * stops the search and closes the multisocket if it exists
	 */
	public void close() {
		delegate = null;
		
		if ( listening ) {
			listening = false;	// setting this to false will cause run() loop to exit, closing multisocket
		} else if ( multisocket != null ) {
			multisocket.close();
			multisocket = null;
		}
	}
	
	/**
	 * Factory method to create an LXmDNSDiscoverer
	 * @param networkInterface if not null, searches for an InetAddress associated with the named interface (eg "en0")
	 * @param networkAddress specific IP address to use for binding socket.  Can be null if networkInterface is specified
	 * @return created instance of LXUPnPDiscovereror null if socket could not be opened
	 */
	
	public static LXmDNSDiscoverer createLXmDNSDiscoverer(String networkInterface, String networkAddress, String target, int type, int printing) {
		LXmDNSDiscoverer mdnsd = null;
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
		        		printError(printing,"() " + nic.getName());
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
		            		printError(printing,"   " + astr);    
		            	} else {
			            	myNetworkAddress = astr;
			            	if ( printing > 1 ) {
			            		printError(printing,"Found address for " + networkInterface + " => " + myNetworkAddress);
			            	}
			            	searchDone = true;
			            }
		            }
		          }
		        }
		      }
		    } catch (Exception e) {
		    	printError(printing,"Warning: exception thrown while searching network interfaces.");
		    }
			if ( ! searchDone ) {
				if ( searched ) {
					searchDone = true;
				} else {
					searched = true;
					printError(printing,"Could not find address for " + networkInterface + ".  Interfaces found:");
				}
			}
		}
		  
	    try {
	      InetAddress nicAddress = InetAddress.getByName(myNetworkAddress);
	      
	      mdnsd = new LXmDNSDiscoverer(target, type);
	      mdnsd.multisocket = new MulticastSocket( null );
	      mdnsd.multisocket.setReuseAddress(true);
	         if ( networkAddress.equals("0.0.0.0") ) {
	        	 mdnsd.multisocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), MDNS_MULTICAST_PORT));
	         } else {
	        	 mdnsd.multisocket.bind(new InetSocketAddress(nicAddress, MDNS_MULTICAST_PORT));
	         }
	        
	         NetworkInterface nic = NetworkInterface.getByInetAddress(nicAddress);
	         mdnsd.multisocket.joinGroup(new InetSocketAddress(InetAddress.getByAddress(MDNS_MULTICAST_ADDRESS), MDNS_MULTICAST_PORT), nic);
	         
	         mdnsd.multisocket.setSoTimeout(1000);
	         mdnsd.multisocket.setBroadcast(true);
	         mdnsd.setPrintLevel(printing);
	   }  catch ( Exception e) {
		  printError(printing,"Can't open socket for mDNS discovery " + e);
	      mdnsd = null;
	   }

	    return mdnsd;
	}
	
	
	public static void startDiscovery(String networkInterface, String networkAddress, String target, int type, LXmDNSDelegate d, int printing) {
		LXmDNSDiscoverer explr = LXmDNSDiscoverer.createLXmDNSDiscoverer(networkInterface, networkAddress, target, type, printing);
		explr.setDelegate(d);
		Thread runner = new Thread ( explr );
		runner.start();
	}
	
}