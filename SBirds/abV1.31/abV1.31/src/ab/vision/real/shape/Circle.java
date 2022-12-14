/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2014, XiaoYu (Gary) Ge, Stephen Gould, Jochen Renz
 **  Sahan Abeyasinghe,Jim Keys,  Andrew Wang, Peng Zhang
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. 
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/
package ab.vision.real.shape;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import ab.vision.ABShape;
import ab.vision.ABType;
import ab.vision.real.ImageSegmenter;

public class Circle extends Body
{
   
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// radius of the circle
    public double r;
    public Rectangle bounds;
    /* Create a new circle
     * @param   xs, ys - coordinate of the circle centre
     *          radius - circle radius
     *          t      - type of the object
     */
    public Circle(double xs, double ys, double radius, ABType type)
    {
      
        centerX = xs;
        centerY = ys;
        r = radius;
        shape = ABShape.Circle;
        bounds = new Rectangle((int)(xs - r * Math.sin(Math.PI/4)), (int)(ys - r * Math.sin(Math.PI/4)), 
        		(int)(2 * r * Math.sin(Math.PI/4)), (int)(2 * r * Math.sin(Math.PI/4))); 
        this.type = type;
        angle = 0;
        area = (int)(Math.PI * r * r);
        super.setBounds(bounds);
        
    }

    @Override
    public Rectangle getBounds()
    {
    	return bounds;
    }
 


    public Circle(int box[], ABType type)
    {
        centerX = (box[0] + box[2]) / 2.0;
        centerY = (box[1] + box[3]) / 2.0;
        r = (box[2] - box[0] + box[3] - box[1]) / 4.0;
        area = (int)(Math.PI * r * r);
        bounds = new Rectangle((int)(centerX - r * Math.sin(Math.PI/4)), (int)(centerY - r * Math.sin(Math.PI/4)), 
        		(int)(2 * r * Math.sin(Math.PI/4)), (int)(2 * r * Math.sin(Math.PI/4))); 
        angle = 0;
        this.type = type;
        super.setBounds(bounds);
    }
    
    /* draw the circle onto canvas */
    public void draw(Graphics2D g, boolean fill, Color boxColor)
    {
        if (fill)
        {
            g.setColor(ImageSegmenter._colors[type.id]);
            g.fillOval(round(centerX - r), round(centerY - r), round(r * 2), round(r * 2));
        }
        else
        {
            g.setColor(boxColor);
            g.drawOval(round(centerX - r), round(centerY - r), round(r * 2), round(r * 2));
        }
    }
	
	public String toString()
	{
		return String.format("Circ: id:%d type:%s r:%7.3f at x:%5.1f y:%5.1f BOTTOMUP:%.2f TOPDOWN:%.2f penetration[0]:%.2f penetration[1]:%.2f displacement:%.2f support:%.2f downwards:%.2f", id, type, r, centerX, centerY,bottomUpFactorValue,topDownFactorValue,penetrationWeight[0],penetrationWeight[1],displacementWeight,supportWeight,downwardsFactorValue);
	}
}
