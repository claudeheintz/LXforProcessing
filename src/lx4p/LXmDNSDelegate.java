/**
 * Copyright (c) 2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
*/

package lx4p;

/**
 *  LXmDNSDelegate is an interface for searching for devices using multicast DNS
 *  Classes implementing LXmDNSDelegate receive a receivedResponse() call when
 *  LXmDNSDiscoverer receives a mDNS packet with a matching qualified domain name.
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXmDNSDiscoverer
 */

public interface LXmDNSDelegate {
	
	/**
	 * Called in a LXmDNSDiscoverer object's setDelegate method to pass back a reference.
	 * Delegates should keep this reference in order to call close() which stops search and closes socket properly
	 * @param d the LXmDNSDiscoverer associated with the delegate
	 */
	public void setLXmDNSDiscoverer(LXmDNSDiscoverer d);
	
	/**
	 * Called when the mDNS discovery receives an mDNS reply record as part of a Standard Query Answer Packet
	 * @param rr reply record that has been received and is being passed to delegate
	 */
	public void receivedMDNSQueryAnswerRecord(LXDNSReplyRecord rr);
	
	/**
	 * Called when the mDNS discovery receives an mDNS reply record as part of a Standard Query Packet
	 * * @param rr query record that has been received and is being passed to delegate
	 */
	public void receivedMDNSQueryRecord(LXDNSReplyRecord rr);
	
}