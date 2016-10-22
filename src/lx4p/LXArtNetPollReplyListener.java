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

/** LXArtNetPollReplyListener
 * 
 * <p>LXArtNetPollListener is an interface for objects that want to be informed when
 * an LXArtNet object receives an Art-Net Poll Reply.</p>
 * 
 * <p>Art-Net(TM) Designed by and Copyright Artistic Licence Holdings Ltd.</p>
*/


public interface LXArtNetPollReplyListener  {

	/**
	 * called when ArtPollReply is received
	 * @param info LXArtNetPollReplyInfo containing fields from ArtPollReply
	 * @return true if the node address should be used
	 */
	public boolean pollReplyReceived(LXArtNetPollReplyInfo info);

}