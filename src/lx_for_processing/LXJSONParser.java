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


public class LXJSONParser {
	
	int state;
	int bracecount = 0;
	boolean escape = false;
	boolean quote = false;
	boolean valid = false;
	boolean complete = false;
	LXJSONElement root = null;
	LXJSONElement currentelement;
	
	StringBuffer currentdata = new StringBuffer();
	
	public LXJSONElement parseString(String astr) {
		state = 0;
		bracecount = 0;
		int alen = astr.length();
		if ( alen > 0 ) {
			valid = true;
		}
		
		for (int k=0; k<alen; k++) {
			processCharacter(astr.charAt(k));
			if ( (! valid ) || complete ) {
				break;
			}
		}
		if ( ! valid ) {
			System.out.println("json parse error: " + currentdata);
			return null;
		}
		return root;
	}
	
	void processCharacter( int c ) {
		if ( escape ) {
			currentdata.append((char)c);
			escape = false;
		} else if ( quote ) {
			if ( c == '"' ) {
				quote = false;
			} else {
				currentdata.append((char)c);
			}
		} else {
			if ( c == '\\' ) {
				escape = true;
			} else if ( c == '{' ) {
				processLeftBrace();
			} else if ( c == '}' ) {
				processRightBrace();
			} else if ( c == '"' ) {
				quote = true;
			} else if ( c == ':' ) {
				if ( state == 0 ) {
					state = 1;
					currentelement.tag = currentdata.toString();
					currentdata.delete(0, currentdata.length());
				} else {
					valid = false;
				}
			} else if ( c == ',' ) {
				if ( state == 1) {									//finish value and add another element on same level ready for tag
					currentelement.value = currentdata.toString();
					currentdata.delete(0, currentdata.length());
					state = 0;
					LXJSONElement nelem =  currentelement.parent.addSubElement();	//sibling
					currentelement = nelem;
				} else if ( state == 0) {											//sibling
					if ( currentelement.parent.is_array ) {
						currentelement.tag = currentdata.toString();
						currentdata.delete(0, currentdata.length());
					}
					currentelement = currentelement.parent.addSubElement();
				} else if ( state == 3) {
					currentdata.append(",");
				}
			} else if ( c == '[' ) {
				if ( state == 1 ) {
					state = 0;
					currentelement.is_array = true;
					LXJSONElement nelem =  currentelement.addSubElement();
					currentelement = nelem;
				}
			} else if ( c == ']' ) {
				if ( currentelement.parent.is_array ) {
					currentelement.tag = currentdata.toString();
					currentdata.delete(0, currentdata.length());
					currentelement = currentelement.parent;
				} else {
					valid = false;
				}
			} else if ( c == ' ' ) {
				// white space not inside quote, ignore
			} else {
				currentdata.append((char)c);
			}
		}
	}
	
	void processLeftBrace() {
		if ( bracecount == 0 ) {		//top level root has no tag
			root = new LXJSONElement();
			currentelement = root.addSubElement();
			bracecount += 1;
		} else if ( state == 1) {		//has tag, begin new level
			bracecount += 1;
			state = 0;
			LXJSONElement nelem =  currentelement.addSubElement();
			currentelement = nelem;
		} else {
			valid = false;
		}
	}
	
	void processRightBrace() {
		bracecount -= 1;
		if ( bracecount == 0 ) {
			complete = true;
		} else {
			if ( state == 1 ) {									//finish value
				currentelement.value = currentdata.toString();
				currentdata.delete(0, currentdata.length());
			}
			state = 0;
			currentelement = currentelement.parent;				//move up a level
		}
	}
	
}