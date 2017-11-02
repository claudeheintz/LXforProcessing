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

import java.util.*;

/**
 *  LXJSONElement is a class that holds an element of a JSON object tree.
 *  
 *  A tree of LXJSONElements is created by a LXJSONParser from a JSON string.
 *  Each leaf element holds a tag/value.
 *  An LXJSONElement can also hold sub-elements.
 *  
 *  @version 001
 *  @author Claude Heintz
 *  @see LXJSONParser
 */

public class LXJSONElement {
	
	public String tag;
	public String value;
	public boolean is_array = false;
	public Vector<LXJSONElement> subelements;
	public LXJSONElement parent;
	
	/**
	 * construct an empty element
	 */
	public LXJSONElement() {
		this("", "", null);
	}
	
	/**
	 * construct an element that is a sub-element of a parent element
	 * @param p construct with a JSONElement
	 */
	public LXJSONElement(LXJSONElement p) {
		this("", "", p);
	}
	
	/**
	 * construct an element and set the is_array property
	 * @param a true if the element is an array of sub-elements
	 * @param p the element's parent
	 */
	public LXJSONElement(boolean a, LXJSONElement p) {
		this("", "", null);
		is_array = a;
	}
	
	/**
	 * construct an element with a tag, value and parent element
	 * @param t the element's tag
	 * @param v the element's value
	 * @param p the element's parent
	 */
	public LXJSONElement(String t, String v, LXJSONElement p)	{
		tag = t;
		value = v;
		parent = p;
		subelements = new Vector<LXJSONElement>();
	}
	
	/**
	 * makes a new empty sub-element and returns it
	 * @return the new sub-element
	 */
	public LXJSONElement addSubElement() {
		LXJSONElement nelem = new LXJSONElement(this);
		subelements.addElement(nelem);
		return nelem;
	}
	
	/**
	 * finds a sub-element matching the supplied tag
	 * @param sstr the tag to search for
	 * @return the first element whose tag matches sstr or null if none found
	 */
	public LXJSONElement findSubElement(String sstr) {
		LXJSONElement ce;
		Enumeration<LXJSONElement> en = subelements.elements();
		while ( en.hasMoreElements() ) {
			ce = en.nextElement();
			if ( ce.tag.equals(sstr) ) {
				return ce;
			}
		}
		return null;
	}
	
	/**
	 * prints the element and sub-elements, indenting to show the structure
	 */
	public void print() {
		print(0);
	}
	
	/**
	 * prints the element and sub-elements, indenting to show the structure
	 * @param level indentation level
	 */
	public void print(int level) {
		// indent
		for(int j=0; j<level; j++) {
			System.out.print("   ");
		}
		//print tag/value or just tag if sub-elements compose an array
		if ( is_array ) {
			System.out.print(tag + " []:\n");
		} else {
			System.out.print(tag + " : " + value + "\n");
		}
		// print sub-elements, increasing indent to show tree structure
		Enumeration<LXJSONElement> en = subelements.elements();
		while ( en.hasMoreElements() ) {
			en.nextElement().print(level+1);
		}
	}
}