package uq.spatial;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator to sort trajectories and points 
 * by time-stamp.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class TimeComparator<T> implements Serializable, Comparator<T>{
	
	/**
	 * Compare objects (Points or Trajectories) by time stamp 
	 * in ascending order.
	 */
	public int compare(T obj1, T obj2) {
		if(obj1 instanceof Point){
			Point p1 = (Point)obj1; 
			Point p2 = (Point)obj2;
			return compare(p1, p2);
		} 
		if(obj1 instanceof Trajectory){
			Trajectory t1 = (Trajectory)obj1;
			Trajectory t2 = (Trajectory)obj2;
			return compare(t1, t2);
		}
		return 0;
	}
	
	/**
	 * Compare points by time stamp by ascending order.
	 */
	private int compare(Point p1, Point p2) {
		return p1.time > p2.time ? 1 : (p1.time < p2.time ? -1 : 0);
	}
	
	/**
	 * Compare trajectories by initial time stamp by ascending order.
	 */
	private int compare(Trajectory t1, Trajectory t2) {
		return t1.timeIni() > t2.timeIni() ? 1 : (t1.timeIni() < t2.timeIni() ? -1 : 0);
	}
}
