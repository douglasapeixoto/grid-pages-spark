package uq.spatial;

import java.io.Serializable;

/**
 * A 2D Spatial-Temporal line segment object with time-stamp.
 * </br>
 * Line segment from points (x1,y1,t1) to (x2,y2,t2).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class STSegment extends Segment implements Serializable{
	/**
	 * The parent (trajectory) of this segment
	 */
	public String parentId;
	/**
	 * Mark as boundary segment.
	 */
	public boolean boundary = false;
	// end points time-stamp
	public long t1;
	public long t2;
	
	public STSegment(){}
	public STSegment(double x1, double y1, long t1, double x2, double y2, long t2) {
		super(x1, y1, x2, y2);
		this.t1 = t1;
		this.t2 = t2;
	}
	public STSegment(Point p1, Point p2) {
		super(p1.x, p1.y, p2.x, p2.y);
		this.t1 = p1.time;
		this.t2 = p2.time;
	}
	
	/**
	 * Get the fist spatial-temporal endpoint (x1,y1,t1).
	 */
	@Override
	public Point p1(){
		return new Point(x1, y1, t1);
	}
	
	/**
	 * Get the second spatial-temporal endpoint (x2,y2,t2).
	 */
	@Override
	public Point p2(){
		return new Point(x2, y2, t2);
	}
	
	/**
	 * Calculate the projection of the given spatial-temporal point
	 * on to this spatial-temporal line segment. Point given by 
	 * x and y coordinates and time-stamp time.
	 * 
	 * @return Return the projection with time-stamp.
	 */
	public Point projection(double x, double y, long time){
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
	    long   pt = (long) (t1 + t*(t2 - t1));
	   
	    return new Point(px, py, pt);
	}
	
	/**
	 * Get the string representation of this object.
	 */
	public String toString(){
		String s = "[("+x1+","+y1+","+t1+")("+x2+","+y2+","+t2+")]";
		return s;
	}
}
