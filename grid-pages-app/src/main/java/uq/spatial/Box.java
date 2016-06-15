package uq.spatial;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.jsi.Rectangle;

/**
 * A Box is defined as a 2D rectangle whose edges  
 * are parallel to the X and Y axis.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Box implements Serializable {
	// X and Y axis position
	public double minX;
	public double maxX;
	public double minY;
	public double maxY;
	
	public Box(){}
	public Box(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}
	
	/**
	 * Return the rectangle of this box.
	 */
	public Rectangle rectangle(){
		return new Rectangle((float)minX,(float)minY,(float)maxX,(float)maxY);
	}
	
	/**
	 * The perimeter of this box
	 */
	public double perimeter(){
		return (2*Math.abs(maxY-minY) + 2*Math.abs(maxX-minX));
	}
	
	/**
	 * The area of this box
	 */
	public double area(){
		return (maxY-minY)*(maxX-minX);
	}
	
	/**
	 * Returns the center of this box as a coordinate point.
	 */
	public Point center(){
		double xCenter = minX + (maxX - minX)/2;
		double yCenter = minY + (maxY - minY)/2; 
		return new Point(xCenter, yCenter);
	}

	/**
	 * True is this box contains the given point inside its perimeter.
	 * Check if the point lies inside the box area.
	 */
	public boolean contains(Point p){
		return contains(p.x, p.y);
	}
	
	/**
	 * Check is this rectangle contains the given line segment,
	 * that is, line segment is inside the rectangle area.
	 */
	public boolean contains(Segment s){
		return contains(s.x1, s.y1, s.x2, s.y2);
	}
	
	/**
	 * True is this box contains the given point inside its perimeter.
	 * Check if the point lies inside the box area.
	 * Point given by X and Y coordinates
	 */
	public boolean contains(double x, double y){
		if(x >= minX && x <= maxX &&
		   y >= minY && y <= maxY){
			return true;
		}
		return false;
	}

	/**
	 * Check is this rectangle contains the given line segment,
	 * that is, line segment is inside the rectangle area.
	 * </br>
	 * Line segment given by end points X and Y coordinates.
	 */
	public boolean contains(double x1, double y1, double x2, double y2){
		if((contains(x1, y1) || touch(x1, y1)) && 
		   (contains(x2, y2) || touch(x2, y2))){
			return true;
		}
		return false;
	}
	
	/**
	 * True is this box touches the specified point.
	 * Check if the point touches the box edges.
	 */
	public boolean touch(Point p){
		return touch(p.x, p.y);
	}
	
	/**
	 * True is this box touches the specified point.
	 * Check if the point touches the box edges.
	 * Point given by X and Y coordinates.
	 */
	public boolean touch(double x, double y){
		// check top and bottom edges
		if( x >= minX && x <= maxX && 
		   (y == maxY || y == minY) ){
			return true;
		}
		// check left and right edges
		if( y >= minY && y <= maxY && 
		   (x == minX || x == maxX) ){
			return true;
		}
		return false;
	}

	/**
	 * Check is these two rectangles overlap.
	 */
	public boolean overlap(Box r){
		if(this.maxX < r.minX) return false;
		if(this.minX > r.maxX) return false;
		if(this.maxY < r.minY) return false;
		if(this.minY > r.maxY) return false;
		return true;
	}

	/**
	 * Check if the given line segment overlaps with this rectangle, 
	 * that is, check if this rectangle either contains or intersect 
	 * the given line segment.
	 */
	public boolean overlap(Segment s){
		if(contains(s) || intersect(s)){
			return true;
		}
		return false;
	}
	
	/**
	 * True is this box overlaps with the given line segment.
	 * Line segment given by end points X and Y coordinates.
	 */
	public boolean overlap(double x1, double y1, double x2, double y2){
		if(contains(x1, y1) && contains(x2, y2)){
				return true;
		}
		return false;
	}

	/**
	 * Check if the given line segment intersects this rectangle.
	 */
	public boolean intersect(Segment s){
		return intersect(s.x1, s.y1, s.x2, s.y2);
	}
	
	/**
	 * True if the given line segment intersects this Box.
	 * Line segment is given by end point coordinates.
	 * </br></br>
	 * If the line segment do not cross or only touches the
	 *  box edges or vertexes than return false.
	 */
	public boolean intersect(double x1, double y1, double x2, double y2){
		// check box LEFT edge
		if(intersect(x1, y1, x2, y2, 
				minX, minY, minX, maxY)){
			return true;
		}
		// check RIGHT edge
		if(intersect(x1, y1, x2, y2, 
				maxX, minY, maxX, maxY)){
			return true;
		}
		// check TOP edge
		if(intersect(x1, y1, x2, y2, 
				minX, maxY, maxX, maxY)){
			return true;
		}
		// check BOTTOM edge
		if(intersect(x1, y1, x2, y2, 
				minX, minY, maxX, minY)){
			return true;
		}
    
	    return false;
	}

	/**
	 * Left edge.
	 */
	public Segment leftEdge(){
		return new Segment(minX, minY, minX, maxY);
	}
	
	/**
	 * Right edge.
	 */
	public Segment rightEdge(){
		return new Segment(maxX, minY, maxX, maxY);
	}
	
	/**
	 * Bottom edge.
	 */
	public Segment lowerEdge(){
		return new Segment(minX, minY, maxX, minY);
	}
	
	/**
	 * Top edge.
	 */
	public Segment upperEdge(){
		return new Segment(minX, maxY, maxX, maxY);
	}
	/**
	 * Return the coordinates of the four vertexes of this Box.
	 */
	public List<Point> getVertexList(){
		List<Point> corners = new ArrayList<Point>();
		Point p1 = new Point(minX, maxY);
		Point p2 = new Point(maxX, maxY);
		Point p3 = new Point(minX, minY);
		Point p4 = new Point(maxX, minY);
		corners.add(p1);	corners.add(p2);
		corners.add(p3);	corners.add(p4);
		
		return corners;
	}
	
	/**
	 * Print this Box: System out.
	 */
	public void print(){
		System.out.println("Box:");
		System.out.println("("+minX+", "+maxY+") " + " ("+maxX+", "+maxY+")");
		System.out.println("("+minX+", "+minY+") " + " ("+maxX+", "+minY+")");
		System.out.println("Area: " + area());
		System.out.println("Perimeter: " + perimeter());
	}
	
	/**
	 * True if these two line segments R and S intersect.
	 * Line segments given by end points X and Y coordinates.
	 */
	private boolean intersect(double r_x1, double r_y1, double r_x2, double r_y2,
							  double s_x1, double s_y1, double s_x2, double s_y2){
		// vectors r and s
		double rx = r_x2 - r_x1;
		double ry = r_y2 - r_y1;		
		double sx = s_x2 - s_x1;
		double sy = s_y2 - s_y1;
		
		// cross product r x s
		double cross = (rx*sy) - (ry*sx);
			
		// they are parallel or colinear
		if(cross == 0.0) return false;
	
		double t = (s_x1 - r_x1)*sy - (s_y1 - r_y1)*sx;
			   t = t / cross;
		double u = (s_x1 - r_x1)*ry - (s_y1 - r_y1)*rx;
			   u = u / cross;

	    if(t > 0.0 && t < 1.0 && 
	       u > 0.0 && u < 1.0){
	    	return true;
	    }
	    return false;
	}
}
