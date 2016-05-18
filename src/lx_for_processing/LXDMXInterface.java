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

/**
 * LXDMXInterface
 * 
 * Abstract class representing an interface capable of reading and writing dmx
 * contained in a serial or network packet.
 *
 * @author Claude Heintz
 */

public abstract class LXDMXInterface extends Object  {

  /**
	*  Size of one dmx universe.
   */
	
	public static final int DMX_UNIVERSE_MAX = 512;

	/**
	 * dmx level data byte
	 * @param slot the address or channel of the data (1-512)
	 * @return the level 0-255 for the slot (aka address or channel)
	 */
	public abstract int getSlot(int slot);
	/**
	 * set byte dmx level data in slot
	 * @param slot aka the address or channel of the level data (1-512)
	 * @param value the level 0-255 for the slot (aka address or channel)
	 */
	public abstract void setSlot(int slot, byte value);
	/**
	 * set int dmx level data in slot
	 * @param slot aka the address or channel of the level data (1-512)
	 * @param value the level 0-255 for the slot (aka address or channel)
	 */
	public void setSlot(int slot, int value) {			// convenience override
		setSlot(slot, (byte)value);
	}
	
	/**
	 * dmx has variable number of addresses ~24min to 512max
	 * @return number of slots slot (aka addresses or channels)
	 */
	public abstract int getNumberOfSlots();
	
	/**
	 * dmx has variable number of addresses ~24min to 512max
	 * @param slots number of slots (aka addresses or channels)
	 */
	public abstract void setNumberOfSlots(int slots);

	/**
	 * DMX universe for send/receive
	 * <p>Each dmx stream of up to 512 slots/addresses/channels is called a universe<BR>
	 * universe numbers vary by protocol</p>
	 * <p>The first universe in zero for Art-Net and one for sACN</p>
	 * @return the dmx universe
	 */
	public abstract int getUniverse();
	/**
	 * set DMX universe for send/receive
	 * <p>Each dmx stream of up to 512 slots/addresses/channels is called a universe<BR>
	 * universe numbers vary by protocol</p>
	 * <p>The first universe in zero for Art-Net and one for sACN</p>
	 * @param u the dmx universe
	 */
	public abstract void setUniverse(int u);
	
	/**
	 * attempt to read a protocol packet
	 * @return true if packet contained dmx output
	 */
	public abstract boolean readPacket();
	
	/**
	 * attempt to send a dmx packet
	 */
	public abstract void sendDMX();
	
	/**
	 * closes the connection
	 */
	public abstract void close();
	

	/**
	 * conversion utility byte->int
	 * <p>Java seems to do a straight cast to int from byte as if the byte is signed</p>
	 * @param b byte to convert
	 * @return int value of unsigned byte
	 */
	public static int byte2int(byte b) {
		return (int)(b & 0xff);
	}
	
}