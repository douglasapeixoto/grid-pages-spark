package uq.spatial;

import java.io.Serializable;

import uq.spatial.distance.EuclideanDistanceCalculator;
 
/**
 * A simple circle, defined by its center
 * coordinates and radius.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Circle implements Serializable, GeoInterface {
	private EuclideanDistanceCalculator dist = 
			new EuclideanDistanceCalculator();
	/**
	 * Circle center X coordinate
	 */
	public double center_x;
	/**
	 * Circle center Y coordinate
	 */
	public double center_y;
	/**
	 * The radius of this circle
	 */
	public double radius;
		
	public Circle(){}
	public Circle(Point center, double radius) {
		this.center_x = center.x;
		this.center_y = center.y;
		this.radius = radius;
	}
	public Circle(double center_x, double center_y, double radius) {
		this.center_x = center_x;
		this.center_y = center_y;
		this.radius = radius;
	}

	/**
	 * The perimeter of this circle
	 */
	public double perimeter(){
		return (2*PI*radius);
	}
	
	/**
	 * The area of this circle
	 */
	public double area(){
		return (PI*radius*radius);
	}
	
	/**
	 * Returns the center of this circle as a coordinate point.
	 */
	public Point center(){
		return new Point(center_x, center_y);
	}

	/**
	 * Get the Minimum Bounding Rectangle (MBR)
	 * of this circle.
	 */
	public Box mbr(){
		double min_x = center_x - radius;
		double min_y = center_y - radius;
		double max_x = center_x + radius;
		double max_y = center_y + radius;
		return new Box(min_x, max_x, min_y, max_y);
	}
	/**
	 * True is this circle contains the given point inside its perimeter.
	 * Check if the point lies inside the circle area.
	 */
	public boolean contains(Point p){
		double dist = p.dist(center_x, center_y);
		return (dist <= radius);
	}
	
	/**
	 * True is this circle contains the given point inside its perimeter.
	 * Check if the point lies inside the circle area.
	 * </br>
	 * Point given by x,y,z coordinates
	 */
	public boolean contains(double x, double y){
		return contains(new Point(x, y));
	}

	/**
	 * True if the given point touches this circle (circle perimeter).
	 */
	public boolean touch(Point p){
		double dist = p.dist(center_x, center_y);
		return (dist == radius);
	}
	
	/**
	 * True if the given point touches this circle (circle perimeter).
	 *</br>
	 * Point given by X and Y coordinates
	 */
	public boolean touch(double x, double y){
		return touch(new Point(x, y));
	}

	/**
	 * True if the given line segment intersects this circle.
	 * Line segment is given by end point coordinates.
	 */
	public boolean intersect(
			double x1, double y1, 
			double x2, double y2){
		// triangle sides
		double distP1 = dist.getDistance(center_x, center_y, x1, y1);
		double distP2 =	dist.getDistance(center_x, center_y, x2, y2);
		double base   = dist.getDistance(x1, y1, x2, y2); 
		// triangle area
		double p = (distP1 + distP2 + base) / 2;
		double area = Math.sqrt(p*(p-distP1)*(p-distP2)*(p-base));
		// use triangulation to calculate distance from 
		// the circle center to the segment
		double height = 2 * area * base;
		if(height >= radius){
			return false;
		} 
		// get distance between the center of the circle 
		// and the first endpoint
		if(distP1 > radius){
			return true;
		}
		// get distance between the center of the circle 
		//and the second endpoint
		if(distP2 > radius){
			return true;
		}
		return false;
	}
	
	/**
	 * True is this circle overlaps with the given line segment.
	 */
	public boolean overlap(Segment s) {
		return overlap(s.x1, s.y1, s.x2, s.y2);
	}
	
	/**
	 * True is this circle overlaps with the given line segment.
	 * Line segment given by end points X and Y coordinates.
	 */
	public boolean overlap(double x1, double y1, double x2, double y2){
		if(contains(x1, y1) || touch(x1, y1)){
			if(contains(x2, y2) || touch(x2, y2)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString(){
		String s = "Center: (" + center().toString() + ") Radius: " + radius;
		return s;
	}

}
