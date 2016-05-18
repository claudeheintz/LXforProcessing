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
import java.io.*;

/**
 * LXHueInterface provides a network interface connection to a Phillips Hue Bridge.
 * It is capable of locating a Phillips Hue Bridge on the network using UPnP
 * 
 * @author Claude Heintz
 */

public class LXHueInterface implements LXUPnPDelegate  {
	
	/**
	 *  userName string for api requests (authorized by pressing button on bridge)
	 */
	public String userName = null;
	
	/**
	 *  the URLBase returned when querying the location URL
	 */
	public String urlBase = null;
	
	/**
	 *  status of connection
	 */
	public int status = 0;
	
	/**
	 *  uses UPnP to find Hue bridges on the network
	 */
	LXUPnPDiscoverer bridgeFinder = null;
	
	/**
	 * initialize the hue interface with a user name to use when connecting to the Hue Bridge
	 * @param user_name
	 */
	
   public LXHueInterface(String user_name) {
      userName = user_name;
   }
   
   /**
    * closes the bridgeFinder and its network socket
    */
   public void close() {
	   if ( bridgeFinder != null ) {
		   bridgeFinder.close();
		   bridgeFinder = null;
	   }
   }
   
   /**
    * Starts the process of connecting to a Hue Bridge by finding its URL
    * @param address the IP address to use in binding the UPnP socket corresponding to the network interface connection to the bridge
    * @param nicName the name of the interface to use in binding the UPnP socket eg "en0", "en1", "eth0", etc.
    */
   public void connectToBridge(String address, String nicName) {
	   status = 0;
	   LXUPnPDiscoverer.startDiscovery(nicName, address, "IpBridge", this);
   }
   
   /**
    * method required by LXUPnPDelegate interface
    * delegates are able to call close() which terminates a search thread
    */
   public void setLXUPnPDiscoverer(LXUPnPDiscoverer d) {
	   bridgeFinder = d;
   }
   
   /**
    * method required by LXUPnPDelegate interface
    * called 
    */
   public void foundURLBase(String u) {
	   urlBase = u;
	   System.out.println("Found IpBridge: " + urlBase);
	   bridgeFinder = null;
	   testConnection();
   }
   
   /**
    * If there's been an error, tests the connection to the bridge which clears the error if it succeeds
    * If testConnection returns error containing "unauthorized user" clearStatus will attempt to create the user
    * The button on the Hue Bridge should be pressed before attempting to create a user.
    */
   public void clearStatus() {
	   if ( status == -1 ) {
		   testConnection();
	   } else if ( status == -2 ) {
		   createUser();
	   }
   }
   
   /**
    * queries the Hue Bridge for the current state of the lights
    * returns a string with the status in JSON format
    */
   public String getHueLightsState() {
	  String rstr = null;
	  String rurl = urlBase + "api/" + userName + "/lights";
      try {
		URL url = new URL(rurl);
		HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
		urlc.setAllowUserInteraction( false );
		urlc.setDoInput( true );
		urlc.setDoOutput( false );
		urlc.setUseCaches( false );
		urlc.setRequestMethod("GET");
		if ( urlc.getResponseCode() == 200 ) {	//  200 = OK
			rstr = getResponse(urlc.getInputStream());
		}
		urlc.disconnect();
	  } catch (Exception e) {
		  System.out.println("Exception getting the Hue state.");
		  status = -1;
	  }
      return rstr;
   }
   
   public void testConnection() {
	   String jstr = getHueLightsState();
	   status = -1;
	   if ( jstr != null ) {
		   LXJSONParser jparser = new LXJSONParser();
		   LXJSONElement jroot = jparser.parseString(jstr);
		   if ( jroot != null ) {
			   LXJSONElement jerror = jroot.findSubElement("error");
			   if ( jerror != null ) {
				   LXJSONElement jdesc = jerror.findSubElement("description");
				   if ( jdesc != null ) {
					   if ( jdesc.value.equals("unauthorized user") ) {
						   System.out.println("Unauthorized User:  press the button on the Hue Bridge, then press the 'c' key.");
						   status = -2;
					   } 
				   }
			   } else {
				   status = 1;	// no error!
			   }
		   } else {
			   System.out.println("Error parsing JSON to determine Hue state");
		   }
	   }
   }
   
   /**
    * Sends a request to Hue Bridge to create a user.  Should be sent AFTER pressing the button on the Hue bridge.
    * @return true if request returned OK
    */
   public boolean createUser() {
   	  boolean setOK = false;
	  String rurl = urlBase + "api";
	  String body = "{\"devicetype\": \"lx#script\", \"username\": \"" + userName + "\"}";
      try {
		URL url = new URL(rurl);
		HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
		urlc.setAllowUserInteraction( false );
		urlc.setDoInput( true );
		urlc.setDoOutput( true );
		urlc.setUseCaches( false );
		urlc.setRequestMethod("GET");
		
		OutputStreamWriter osw = new OutputStreamWriter(urlc.getOutputStream());
        osw.write(body);
        osw.flush();
        osw.close();
		
		if ( urlc.getResponseCode() == 200 ) {	//  200 = OK
			setOK = true;
			System.out.println("Create User OK");
			testConnection();					// clears -2 status if success
		} else {
			System.out.println("Create User FAILED");
		}
		urlc.disconnect();
	  } catch (Exception e) {
		  System.out.println("Exception in createUser().\n" + e);
	  }
      return setOK;
   }
   
   /**
    * setLight sends PUT to bridge with light parameters in body of request
    * @param light string with number of Hue light bulb
    * @param body JSON String contents of PUT request
    * @return true if request sent successfully
    */
   public boolean setLight(String light, String body) {
   	  boolean setOK = false;
	  String rurl = urlBase + "api/" + userName + "/lights/" + light + "/state";
      try {
		URL url = new URL(rurl);
		HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
		urlc.setAllowUserInteraction( false );
		urlc.setDoInput( true );
		urlc.setDoOutput( true );
		urlc.setUseCaches( false );
		urlc.setRequestMethod("PUT");
		
		OutputStreamWriter osw = new OutputStreamWriter(urlc.getOutputStream());
        osw.write(body);
        osw.flush();
        osw.close();
		
		if ( urlc.getResponseCode() == 200 ) {	//  200 = OK
			setOK = true;
		}
		urlc.disconnect();
	  } catch (Exception e) {
		  System.out.println("Exception in setLight().\n" + e);
		  status = -1;
	  }
      return setOK;
   }
   
   /**
    * setLight sends PUT to bridge with light parameters in body of request
    * @param light number of Hue light bulb
    * @param body JSON String contents of PUT request
    * @return true if request sent successfully
    */
   public boolean setLight(int light, String body) {
	   return setLight(Integer.toString(light), body);
   }
   
   /**
    * setLight sends PUT to bridge with light parameters in body of request
    * @param light 1,2,3
    * @param on_str	"true" or "false"
    * @param sat_str saturation 0-255
    * @param bri_str 0-255
    * @param hue_str 0-65535
    * @return true if request sent successfully
    */
   public boolean setLight(String light, String on_str, String sat_str, String bri_str, String hue_str ) {
	   return setLight(light, params2json(on_str, sat_str, bri_str, hue_str));
   }
   
   /**
    * setLight sends PUT to bridge with light parameters in body of request
    * @param light 1,2,3
    * @param ltsw on/off
    * @param sat saturation 0-255
    * @param bri intensity 0-255
    * @param hue color 0-65535
    * @return true if request sent successfully
    */
   public boolean setLight(int light, boolean ltsw, int sat, int bri, int hue ) {
	   return setLight(light, params2json(ltsw, sat, bri, hue));
   }
   
   /**
    * params2json assembles a JSON String from individual parameters to pass to setLight
    * @param on_str	"true" or "false"
    * @param sat_str saturation 0-255
    * @param bri_str intensity 0-255
    * @param hue_str color 0-65535
    * @return JSON string for PUT request
    */
   public String params2json ( String on_str, String sat_str, String bri_str, String hue_str ) {
	   return "{\"on\": " + on_str+", \"sat\": "+sat_str+", \"bri\": "+bri_str+", \"hue\": "+hue_str+"}";
   }
   
   /**
    * params2json assembles a JSON String from individual parameters to pass to setLight
    * @param ltsw on=true, off=false
    * @param sat saturation 0-255
    * @param bri intensity 0-255
    * @param hue color 0-65535
    * @return JSON string for PUT request
    */
   public String params2json ( boolean ltsw, int sat, int bri, int hue ) {
	   String on_str;
	   if ( ltsw ) {
		   on_str = "true";
	   } else {
		   on_str = "false";
	   }
	   return params2json(on_str, Integer.toString(sat), Integer.toString(bri), Integer.toString(hue));
   }
   
   /**
   * utility method assembles a string from an InputStream
   * @param instr the stream
   * @return a string read from the stream
   */
   public static String getResponse(InputStream instr) {
   	  StringBuilder sb = new StringBuilder("");
      try {
   	    BufferedReader in = new BufferedReader(new InputStreamReader(instr));
   	    String line = null;
   	    while((line = in.readLine()) != null) {
   		   sb.append(line);
   	    }
      } catch (Exception e) {
   	   System.out.println("Exception reading response from Hue bridge.");
   	  }
   	  return sb.toString();
   }
   
}