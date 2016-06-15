package uq.spatial.distance;

import java.util.ArrayList;

import uq.spatial.Point;

public interface DistanceInterface {
	static final double INFINITY = Double.MAX_VALUE;
	
	/**
	 * Distance between two trajectories given a list of points
	 */
	double getDistance(ArrayList<Point> r, ArrayList<Point> s);
}
