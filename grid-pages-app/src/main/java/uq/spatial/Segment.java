package uq.spatial;

import java.io.Serializable;

import uq.spatial.distance.EuclideanDistanceCalculator;

/**
 * A 2D line segment object.
 * </br>
 * Line segment from coordinate points (x1,y1) to (x2,y2).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class Segment implements Serializable, GeoInterface {
	public double x1;
	public double y1;
	public double x2;
	public double y2;

	public Segment(){}
	public Segment(double x1, double y1, double x2, double y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	/**
	 * The length of this line segment.
	 */
	public double lenght(){
		EuclideanDistanceCalculator dist = 
				new EuclideanDistanceCalculator();
		return dist.getDistance(x1, y1, x2, y2);
	}
	
	/**
	 * True is this segment is parallel to the Y axis.
	 */
	public boolean isVertical(){
		return (x2-x1)==0;
	}
	
	/**
	 * True is this segment is parallel to the X axis.
	 */
	public boolean isHorizontal(){
		return (y2-y1)==0;
	}
	
	/**
	 * True if these two line segments intersect.
	 */
	public boolean intersect(Segment s){
		return intersect(s.x1, s.y1, s.x2, s.y2);
	}
	
	/**
	 * True if these two line segments intersect.
	 * Line segments given by end points X and Y coordinates.
	 */
	public boolean intersect(double x1, double y1, double x2, double y2){
		// vectors r and s
		double rx = x2 - x1;
		double ry = y2 - y1;		
		double sx = this.x2 - this.x1;
		double sy = this.y2 - this.y1;
		
		// cross product r x s
		double cross = (rx*sy) - (ry*sx);
			
		// they are parallel or colinear
		if(cross == 0.0) return false;
	
		double t = (this.x1 - x1)*sy - (this.y1 - y1)*sx;
			   t = t / cross;
		double u = (this.x1 - x1)*ry - (this.y1 - y1)*rx;
			   u = u / cross;

	    if(t > 0.0 && t < 1.0 && 
	       u > 0.0 && u < 1.0){
	    	return true;
	    }
	    return false;
	}

	/**
	 * Calculate the projection of the given point on to this
	 * line segment. Point given by x and y coordinates.
	 */
	public Point projection(double x, double y){
		// segments vector
		double v1x = x2 - x1;
		double v1y = y2 - y1;
		double v2x = x - x1;
		double v2y = y - y1;

		// get squared length of this segment e
		double len2 = (x2 - x1)*(x2 - x1) + 
					  (y2 - y1)*(y2 - y1);
		
		// p1 and p2 are the same point
		if(len2 == 0){
			return p1();
		}
		
		// the projection falls where t = [(p - p1) . (p2 - p1)] / |p2 - p1|^2
	    double t = dotProduct(v2x, v2y, v1x, v1y) / len2;

	    // "Before" e.p1 on the line
	    if (t < 0.0) {
	    	return p1();
	    }
	    // after e.p2 on the line 
	    if(t > 1.0){
	    	return p2();
	    }

		// projection is "in between" e.p1 and e.p2
	    // get projection coordinates
	    double px = x1 + t*(x2 - x1);
	    double py = y1 + t*(y2 - y1);
	    //long time = (long) (e_p1.time + t*(e_p2.time - e_p1.time));
	   
	    return new Point(px, py);
	}
	
	/**
	 * Get the fist segment endpoint (x1,y1).
	 */
	public Point p1(){
		return new Point(x1, y1);
	}
	
	/**
	 * Get the second segment endpoint (x2,y2).
	 */
	public Point p2(){
		return new Point(x2, y2);
	}
	
	/**
	 * Get the string representation of this object.
	 */
	public String toString(){
		String s = "[("+x1+","+y1+")("+x2+","+y2+")]";
		return s;
	}
	
	/**
	 * The distance between this line segment and 
	 * the given point (shortest distance). Point
	 * given by x and y coordinates.
	 */
	public double dist(double x, double y){
		double num = (y2 - y1)*x - (x2 - x1)*y + x2*y1 - y2*x1;
		double den = (y2 - y1)*(y2 - y1) + (x2 - x1)*(x2 - x1);
		double dist = Math.abs(num) / Math.sqrt(den);
		return dist;
	}
	
	/**
	 * The distance between these two line segments
	 * (shortest distance).
	 */
	public double dist(Segment s){
		return dist(s.x1, s.x2, s.y1, s.y2);
	}
	
	/**
	 * The distance between these two line segments
	 * (shortest distance). Segment given by x and y
	 * end points coordinates.
	 */
	public double dist(double x1, double y1, double x2, double y2){
		// if they intersect the shortest distance is zero
		if(intersect(x1, y1, x2, y2)){
			return 0.0;
		}

		// vectors 
		double ux = this.x2 - this.x1;
		double uy = this.y2 - this.y1;
		double vx = x2 - x1;
		double vy = y2 - y1;
		double wx = this.x1 - x1;
		double wy = this.y1 - y1;

		double a = dotProduct(ux, uy, ux, uy); // dot(u,u) always >= 0
	    double b = dotProduct(ux, uy, vx, vy); // dot(u,v) 
	    double c = dotProduct(vx, vy, vx, vy); // dot(v,v) always >= 0
	    double d = dotProduct(ux, uy, wx, wy); // dot(u,w);
	    double e = dotProduct(vx, vy, wx, wy); // dot(v,w);
	    double D = a*c - b*b;    // always >= 0
	    double sc, sN, sD = D;   // sc = sN / sD, default sD = D >= 0
	    double tc, tN, tD = D;   // tc = tN / tD, default tD = D >= 0

	    // compute the line parameters of the two closest points
	    if (D < SMALL_NUM) {  // the lines are almost parallel
	        sN = 0.0;         // force using point P0 on segment S1
	        sD = 1.0;         // to prevent possible division by 0.0 later
	        tN = e;
	        tD = c;
	    }
	    // get the closest points on the infinite lines
	    else {                 
	        sN = (b*e - c*d);
	        tN = (a*e - b*d);
	        // sc < 0 => the s=0 edge is visible
	        if (sN < 0.0) {        
	            sN = 0.0;
	            tN = e;
	            tD = c;
	        }
	        // sc > 1  => the s=1 edge is visible
	        else if (sN > sD) {  
	            sN = sD;
	            tN = e + b;
	            tD = c;
	        }
	    }
	    
	    // tc < 0 => the t=0 edge is visible
	    if (tN < 0.0) {            
	        tN = 0.0;
	        // recompute sc for this edge
	        if (-d < 0.0)
	            sN = 0.0;
	        else if (-d > a)
	            sN = sD;
	        else {
	            sN = -d;
	            sD = a;
	        }
	    }
	    // tc > 1  => the t=1 edge is visible
	    else if (tN > tD) {      
	        tN = tD;
	        // recompute sc for this edge
	        if ((-d + b) < 0.0)
	            sN = 0;
	        else if ((-d + b) > a)
	            sN = sD;
	        else {
	            sN = (-d +  b);
	            sD = a;
	        }
	    }
	    
	    // finally do the division to get sc and tc
	    sc = (Math.abs(sN) < SMALL_NUM ? 0.0 : sN / sD);
	    tc = (Math.abs(tN) < SMALL_NUM ? 0.0 : tN / tD);

	    // get the difference of the two closest points
	    double dx = wx + sc*ux - tc*vx; 
	    double dy = wy + sc*uy - tc*vy;
	    // vector norm
	    double dist = Math.sqrt(dx*dx + dy*dy);
	    
	    return dist;
	}
	
	/**
	 * Dot product between two vectors.
	 */
	protected double dotProduct(double v1x, double v1y, double v2x, double v2y){
		double dot_product = (v1x * v2x) + (v1y * v2y);
		return dot_product;
	}

}
