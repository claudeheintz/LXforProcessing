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

import processing.core.*;

/**
 * LXColorPicker
 * 
 * Displays a color wheel type color picker.
 * If mousePressed when render() is called, the color
 * at the mouse location is saved into current 
 * 
 * @author Claude Heintz
 */

public class LXPColorPicker 
{
	/**
	 * x and y are the coordinates of the top left point
	 * w and h are the width and height
	 * current holds the last color selected
	 */
  public int x, y, w, h, current;
  
	/**
	 * cpImage is the image of the color wheel
	 * it is generated with createImage(0 and drawn with render() 
	 */
  PImage cpImage;
  
  boolean over;
  
  /**
   * LXColorPicker
   * 
   * Displays a color wheel type color picker.
   * If mousePressed when render() is called, the color
   * at the mouse location is saved into current
  */
  
  public LXPColorPicker ( int x, int y, int w, int h, int c) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    current = c;
    createImage();
  }
  
  public static int HueSat2RGB(int h, int s) {
	  double hd = h/255.0;
	  double sd = s/255.0;
	  return HueSat2RGB(hd,sd);
  }
  
  public static int HueSat2RGB(double h, double s) {
	  int c;
	  int red = 0;
	  int green = 0;
	  int blue = 0;
	  int hi;
	  double hf, hp, hq, ht;
	  hi = (int) (h*6.0);        //which 60 degrees wedge hue falls into
      hf = (h*6.0)-hi;
      hp = 1.0 - s;
      hq = 1.0 - s * hf;
      ht = 1.0 - s * (1.0 - hf);

      switch ( hi ) {
       case 0:
       case 6:
         red = 255;
         green = (int) (255.0*ht);
         blue = (int) (255.0 * hp);
         break;
       case 1:
         red = (int) (255.0*hq);
         green = 255;
         blue = (int) (255.0 * hp);
         break;
       case 2:
         red = (int) (255.0*hp);
         green = 255;
         blue = (int) (255.0 * ht);
         break;
       case 3:
         red = (int) (255.0*hp);
         green = (int) (255.0*hq);
         blue = 255;
         break;
       case 4:
         red = (int) (255.0*ht);
         green = (int) (255.0*hp);
         blue = 255;
         break;
       case 5:
         red = 255;
         green = (int) (255.0*hp);
         blue = (int) (255.0*hq);
         break;
      }  // switch
      c = (red<<16) + (green<<8) + blue;
      return c + 0xff000000;
  }
  
  /**
   * creates the image of the color wheel that is stored in cpImage
   */
  private void createImage() {
    cpImage = new PImage( w, h );
    float ctrx = w/2;
    float ctry = h/2;
    float r = Math.min(ctrx, ctry) - 10;
    double d, dx, dy;
    double h, s;
    
    int c;
    for ( int a=0; a<this.w; a++ ) {
       for ( int b=0; b<this.h; b++ ) {
         dx = a - ctrx;
         dy = b - ctry;
         d = Math.sqrt(dx*dx+dy*dy);
         if ( d <= r ) {
           if ( d > 0 ) {
             h = (360.0 - degreesToPoint(dx,dy))/360.0; // hue 0-1.0
             s = 1 - (r-d)/r;                           // saturation 0-1.0
             // HSV->RGB with V = 1.0
             c = HueSat2RGB(h,s);
           }  else {
        	   c = 0;
           }
           cpImage.set( a, b, c );
         } else { 
           cpImage.set( a, b, 0 );  // d > radius
         }
       }   // for b
    }      // for a
  }        // init
  
  /**
   * degreesToPoint is a utility function for calculating the angle from the origin to a point
   * @param dy y coordinate of the point
   * @param dx x coordinate of the point
   * @return angle in degrees counter clockwise from the positive x axis
   */
  
  public double degreesToPoint ( double dy, double dx ) {
    double t;
    
    if ( dx == 0 ) {
      if ( dy < 0 ) {
        t = 180;
      } else {
        t = 0;
      }
    } else if ( dy == 0 ) {
      if ( dx > 0 ) {
        t = 90;
      } else {
        t = 270;
      }
    } else {
      t = Math.toDegrees( Math.atan ( dy / dx ) );
      if ( dx < 0 ) {
        t = 270 - t;    //270-t
      } else {
        t = 90 - t;      //90-t
      }
    }
    return t;
  }
  
  /**
   * draw simply renders the image
   * @param p the Processing Applet in which to draw the picker
   */
  
  public void draw(PApplet p) {
    p.image( cpImage, x, y );
  }
  
  /**
   * Updates the color selected if the mouse is down inside the picker.
   * This is called in the Processing Applet's draw() method so that the mouse can be continually tracked
   * @param p the Processing Applet
   * @return true if the selected color was updated
   */
  
  public boolean pick(PApplet p) {
	boolean colorset = false;
	if( p.mousePressed   &&
	    p.mouseX >= x    &&
		p.mouseX < x + w &&
		p.mouseY >= y    &&
		p.mouseY < y + h )  {
		  int tc = cpImage.get( p.mouseX-x, p.mouseY-y );
		  if ( (tc&0xffffff) != 0 ) {					// make sure its not in a corner
			  current = tc;
			  colorset = true;
		  }
	}
    return colorset;
  }
  
} //class LXPColorPicker
