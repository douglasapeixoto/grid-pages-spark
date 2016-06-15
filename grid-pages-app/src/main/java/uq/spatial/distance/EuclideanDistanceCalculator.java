package uq.spatial.distance;

import java.io.Serializable;

/**
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class EuclideanDistanceCalculator implements Serializable{
	
	/**
	 * Euclidean distance between two points in 3D, 
	 * given by x,y,z coordinates.
	 */
	public double getDistance(
			double x1, double y1, double z1, 
			double x2, double y2, double z2){
		double dist = (x1-x2)*(x1-x2) + 
					  (y1-y2)*(y1-y2) + 
					  (z1-z2)*(z1-z2);
		return Math.sqrt(dist);
	}
	
	/**
	 * Euclidean distance between two points in 2D,
	 * given by x,y coordinates.
	 */
	public double getDistance(
			double x1, double y1, 
			double x2, double y2){
		double dist = (x1-x2)*(x1-x2) + 
					  (y1-y2)*(y1-y2);
		return Math.sqrt(dist);
	}
}
