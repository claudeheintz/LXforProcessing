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

package lxprocessing;

import processing.core.*;

/**
 * LXPTextField is a member of a LXPTextGroup
 * it highlights when the cursor hovers over it
 * When clicked, it requests the LXPTextGroup select its station to receive keyPressed messages
 * Other key presses are added to the fields value string which is displayed,
 * up to the fields maximum number of characters.
 * Pressing the delete key clears the value
 * Pressing the left arrow removes the last character
 * When tab, return, or enter are pressed, the TextGroup is notified
 * 
*/

public class LXPTextField {
  int _x;
  int _y;
  int _s;
  int _w;
  int _h;
  boolean _over;
  LXPTextGroup _group;
  public boolean focus = false;
  public boolean entry_error = false;
  public String value = "";
  public String title = null;
  
  /**
   * constructs a LXPTextField  (called by LXPTextGroup addField)
   * @param xp x location in Processing Applet window
   * @param yp y location in Processing Applet window
   * @param s  the number of characters allowed
   * @param g  the LXPTextGroup to which this button belongs
   */
  public LXPTextField(int xp, int yp, int s, LXPTextGroup g) {
    _x = xp;
    _y = yp;
    _s = s;
    _w = s*9;
    _h = 15;
    _group = g;
  }
  
  /**
   * draws the text field, showing a change when the mouse is over the field
   * and when the field has focus to receive key presses.
   * @param p the Processing Applet containing the button
   */
  public void draw(PApplet p) {
    _over = LXPButton.overRect(_x, _y, _w, _h, p);
    
    //if title, erase & draw
    if ( title != null ) {
    	p.fill(255);
    	p.stroke(255);
    	int tw = 5+9*title.length();
    	p.rect (_x-tw, _y, tw, _h);
    	
    	p.textAlign(PApplet.RIGHT);
    	p.fill(64);
    	p.text(title, _x-5, _y+13);
    }
    
    p.stroke(255);	//white
    
    if ( focus ) {
		p.fill(225,230,255);
	} else {
		if ( entry_error ) {
			p.fill(255,230,230);
		} else {
			p.fill(255);
		}
	}
    p.strokeWeight(2);
    p.rect (_x, _y, _w, _h);
    
    if ( _over ) {
    	p.strokeWeight(2);
    	p.stroke(192);
    	p.rect (_x, _y, _w, _h);
    } else {
    	if ( focus ) {
	      p.stroke(0);
	      p.strokeWeight(1);
	      
	    } else {
	      p.strokeWeight(1);
	      p.stroke(240);
	    }
    	p.rect (_x, _y, _w, _h);
    }
    
    
    p.fill(0);
    p.textAlign(PApplet.LEFT);
    p.text(value, _x+2, _y+13);
  }
  /**
   * called when a key is pressed and the field's text group has selected this field to receive the press
   * @param p the Processing Applet containing the button
   */ 
  public void keyPressed(PApplet p) {
	if ( p.key == PApplet.CODED ) {
		if ( p.keyCode == PApplet.LEFT ) {
			if ( value.length() > 0 ) {
	    		value = value.substring(0, value.length()-1);
	    	}
		}
	} else {
	    if (( p.key == PApplet.RETURN ) || ( p.key == PApplet.ENTER )) {
	      _group.returnInField();
	    } else if (( p.key == PApplet.DELETE ) || ( p.key == PApplet.BACKSPACE )) {
	    	value = "";
	    } else if ( p.key == 9 ) {
	    	_group.tabInField();
	    } else {
	    	if ( value.length() < _s ) {
	    	  if (( p.key < 127 ) && (p.key > 31)) {
	    		  value = value + p.key;
	    	  } else {
	    		  System.out.println(Integer.toString(p.key)); 
	    	  }
	    	}
	    }
	}
  }
  
  /**
   * called from the LXPTextGroup's mousePressed method
   * if the mouse is over the field, sets the key focus of the group
   */
  public void mousePressed() {
    if ( _over ) {
       _group.setFocusField(this);
       entry_error = false;
    }
  }
  
}//class LXPTextField