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

import processing.serial.*;
import processing.core.*;

/**
 * LXDMXENTTEC.java
 * 
 * <p>LXENTTEC partially implements the ENTTEC DMX USB Pro API v1.44</p>
*/

public class LXENTTEC extends LXDMXInterface  {
	
	public static final int DMX_UNIVERSE_MAX = 512;
	public static final int ENTTEC_BUFFER_MAX = 518;
	
	public static final int ENTTEC_LABEL_NONE = 0;
	public static final int ENTTEC_LABEL_GET_INFO = 3;
	public static final int ENTTEC_LABEL_RECEIVED_DMX = 5;
	public static final int ENTTEC_LABEL_SEND_DMX = 6;
	public static final int ENTTEC_LABEL_RECEIVE_DMX = 8;
	public static final int ENTTEC_LABEL_GET_SERIAL = 10;
	
	public static final int ENTTEC_START_PACKET = 0x7E;
	public static final int ENTTEC_END_PACKET = 0xE7;

	/**
	 * buffer for reading and sending packets
	 */
	byte[] _packet_buffer = new byte[ENTTEC_BUFFER_MAX+100];
	/**
	 * buffer for dmx data
	 * <p>Includes dmx start code.</p>
	 */
	byte[] _dmx_buffer = new byte[DMX_UNIVERSE_MAX+1];
	/**
	 * number of slots aka addresses or channels
	 */
	int _dmx_slots = 0;
	
	/**
	 * serial port object
	 */
	Serial serialPort = null;
	
	public LXENTTEC() {
		_dmx_slots = DMX_MIN_SLOTS;
	}
	
	public LXENTTEC(Serial sPort) {
		serialPort = sPort;
		_dmx_slots = DMX_MIN_SLOTS;
	}

	/**
	 * dmx level data
	 * @param slot the address or channel of the data (1-512)
	 * @return the level 0-255 for the slot (aka address or channel)
	 */
	public int getSlot(int slot) {
		return LXDMXEthernet.byte2int( _dmx_buffer[slot] );
	}

	/**
	 * set byte dmx level data in slot
	 * @param slot aka the address or channel of the level data (1-512)
	 * @param value the level 0-255 for the slot (aka address or channel)
	 */
	public void setSlot(int slot, byte value) {
		_dmx_buffer[slot] = value;
	}
	
	/**
	 * set int dmx level data in slot
	 * @param slot aka the address or channel of the level data (1-512)
	 * @param value the level 0-255 for the slot (aka address or channel)
	 */
	public void setSlot(int slot, int value) {			// convenience override
		setSlot(slot, (byte)value);
	}
	
	/**
	 * clears dmx data buffer
	 */
	public void clearSlots() {
		for(int j=0; j<DMX_UNIVERSE_MAX+1; j++) {
			_dmx_buffer[j] = 0;
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
	 * @param slots number of slots aka addresses or channels)
	 */
	public void setNumberOfSlots(int slots) {
		_dmx_slots = Math.max(slots, DMX_MIN_SLOTS);
	}
	
	/**
	 * DMX Start Code (zero for normal dmx data)
	 * @return dmx start code
	 */
	public int getStartCode() {
		return getSlot(0);
	}
	
	/**
	 * set DMX Start Code, normally zero for regular dmx data
	 * @param c start code
	 */
	public void setStartCode(int c) {
		setSlot(0, (byte) c);
	}

	/**
	 * attempts to read an ENTTEC packet sent from the widget via the serial port
	 * @param sPort An open serial port 
	 * @return label of the packet received from widget
	 */
	public int readSerialPacket(Serial sPort) {
		int label = ENTTEC_LABEL_NONE;
		if ( sPort.available() > 0) {  // If data is available,
			int delim = sPort.read();         // read it and store it in val
			if ( delim == ENTTEC_START_PACKET ) {
				int size = sPort.readBytesUntil(ENTTEC_END_PACKET, _packet_buffer);	//includes ENTTEC_END_PACKET delimiter
				if ( size > 1 ) {
					label = _packet_buffer[0];
					switch ( label ) {
					case ENTTEC_LABEL_RECEIVED_DMX:
						if (_packet_buffer[3] == 0 ) {	//good status
							int slots = LXDMXEthernet.byte2int(_packet_buffer[1]);
							slots += LXDMXEthernet.byte2int(_packet_buffer[2]) << 8;
							_dmx_slots = slots - 1;		//first byte is receive status!!
							for(int j=0; j<slots; j++) {
								_dmx_buffer[j] = _packet_buffer[4+j];	//label, size, size, status
							}
						}
					}
				}
			}
		}
		return label;
	}
	
	/**
	 * attempts to read an ENTTEC packet sent from the widget via the serial port
	 * @return label of the packet received from widget
	 */
	public int readSerialPacket() {
		return readSerialPacket(serialPort);
	}
	
	/**
	 * not applicable
	 */
	public int getUniverse() {
		return 0;
	}
	
	/**
	 * not applicable
	 */
	public void setUniverse(int u) {
	}
	
	/**
	 * 
	 */
	
	public boolean readPacket() {
		if ( serialPort != null ) {
			return (readSerialPacket(serialPort) == ENTTEC_LABEL_RECEIVED_DMX);
		}
		return false;
	}
	
	/**
	 * send a ENTTEC_LABEL_SEND_DMX request to widget using the serial port
	 * <p>This should cause the widget to output dmx from its dmx port.</p>
	 * @param sPort An open serial port 
	 */
	public void sendDMX ( Serial sPort ) {
		int dlen = _dmx_slots + 1;
		byte[] _buffer = new byte[dlen+5];
		_buffer[0] = (byte) ENTTEC_START_PACKET;
		_buffer[1] = (byte) ENTTEC_LABEL_SEND_DMX;
		_buffer[2] = (byte)(dlen & 0xFF);
		_buffer[3] = (byte)(dlen >> 8);
		for (int j=0; j<dlen; j++) {
			_buffer[4+j] = _dmx_buffer[j];
		}
		_buffer[4+dlen] = (byte) ENTTEC_END_PACKET;
		sPort.write(_buffer);
	}
	
	public void sendDMX() {
		if ( serialPort != null ) {
			sendDMX(serialPort);
		}
	}

	public void close() {
		if ( serialPort != null ) {
			serialPort.stop();
			serialPort = null;
		}
	}
	
	public static LXENTTEC createDMXSerial(PApplet parent, String portName, int baud) {
		LXENTTEC dmx = null;
		try {
		    Serial sPort = new Serial(parent, portName, baud);
		    dmx = new LXENTTEC(sPort);
		    System.out.println("Opened serial port " + portName);
		  } catch (Exception e) {
		    System.out.println("Could not open serial port. " + e);
		  }
		return dmx;
	}
	
}  //class LXENTTEC