/**
 * Copyright (c) 2015-2016 by Claude Heintz Design
 *
 * This file is part of a library called LXforProcessing - https://github.com/claudeheintz/LXforProcessing
 * 
 * LXforProcessing is free software: you can redistribute it and/or modify
 * it under the terms of a BSD style license that should have been included with this file.
 * If not, see https://www.claudeheintzdesign.com/lx/opensource.html.
*/

package lx4p;

/**
 *  LXUPnPDelegate is an interface for implementing Universal Plug and Play discovery.
 *  Classes implementing LXUPnPDelegate can receive a foundURLBase() call when the
 *  LXPUnPDiscoverer locates a device's description file.
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXUPnPDiscoverer
 */

public interface LXUPnPDelegate {
	
	/**
	 * Called when the delegate of LXUPnPDiscoverer is set to pass back a reference.
	 * Delegates should keep this reference in order to call close() which stops search and closes socket properly
	 * @param d the LXUPnPDiscoverer associated with the delegate
	 */
	public void setLXUPnPDiscoverer(LXUPnPDiscoverer d);
	
	/**
	 * Called when the search returns the URLBase from the XML Device description located with SSDP
	 * @param u the URL string of the base URL for the located device
	 */
	public void foundURLBase(String u);
	
}