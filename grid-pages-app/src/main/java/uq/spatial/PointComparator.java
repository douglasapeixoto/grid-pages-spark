package uq.spatial;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator to compare points in clockwise order based on a 
 * referential point (clock center).
 * 
 * @author uqdalves
 * 
 */
@SuppressWarnings("serial")
public class PointComparator<T> implements Serializable, Comparator<Point>{

	// referential point
	private Point center = new Point(0,0,0);

	/**
	 * A 'clock' center point as referential.
	 */
	public PointComparator(Point center) {
		this.center = center;
	}

	/**
	 * Compare points in clockwise order from the center.
	 * Order starts from the Y+ X+ axis.
	 */
	public int compare(Point p1, Point p2) {
		if (p1.x - center.x >= 0 && p2.x - center.x < 0)
	        return -1;
	    if (p1.x - center.x < 0 && p2.x - center.x >= 0)
	        return 1;
	    if (p1.x - center.x == 0 && p2.x - center.x == 0) {
	        if (p1.y - center.y >= 0 || p1.y - center.y >= 0)
	            return (p1.y > p2.y ? -1 : 1);
	        return (p2.y > p1.y ? 1 : -1);
	    }
	    
	    // compute the cross product of vectors (center -> p1) x (center -> p2)
	    double det = (p1.x - center.x) * (p2.y - center.y) - (p2.x - center.x) * (p1.y - center.y);
	    if (det < 0.0)
	        return -1;
	    if (det > 0)
	        return 1;
	    
	    // points a and b are on the same line from the center
	    // check which point is closer to the center
	    double d1 = (p1.x - center.x) * (p1.x - center.x) + (p1.y - center.y) * (p1.y - center.y);
	    double d2 = (p2.x - center.x) * (p2.x - center.x) + (p2.y - center.y) * (p2.y - center.y);
	    return (d1 > d2 ? -1 : 1);
	}
	
	/**
	 * Compare points in clockwise order from the center.
	 * Points given by X and Y coordinates.
	 * Order starts from the Y+ X+ axis.
	 */
	public int compare(double x1, double y1, double x2, double y2) {
		return compare(new Point(x1, y1), new Point(x2, y2));
	}
}
