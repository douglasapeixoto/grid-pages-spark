package uq.spatial.distance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import uq.spatial.Point;
import uq.spatial.Trajectory;

/**
 * Service to access trajectory distance functions.
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class DistanceService implements Serializable {

	/**
	 * Edit distance with projections.
	 */
	public double EDwP(Trajectory r, Trajectory s){		
		// make sure the original trajectories will not be changed
		ArrayList<Point> r_clone = clonePointsList(r.getPointsList());
		ArrayList<Point> s_clone = clonePointsList(s.getPointsList());
		
		EDwPDistanceCalculator edwp = new EDwPDistanceCalculator();
		return edwp.getDistance(r_clone, s_clone);	
	}
	
	/**
	 * Euclidean distance for multidimensional time series.
	 */
	public double Euclidean(Trajectory r, Trajectory s){
		// make sure the original trajectories will not be changed
		ArrayList<Point> r_clone = clonePointsList(r.getPointsList());
		ArrayList<Point> s_clone = clonePointsList(s.getPointsList());
		
		EuclideanTimeSeriesDistanceCalculator euclid = 
				new EuclideanTimeSeriesDistanceCalculator();
		return euclid.getDistance(r_clone, s_clone);	
	}
		
	/**
	 * Spatial Temporal Largest Common Sub-sequence.
	 */
	public double STLCSS(Trajectory r, Trajectory s){
		// make sure the original trajectories will not be changed
		ArrayList<Point> r_clone = clonePointsList(r.getPointsList());
		ArrayList<Point> s_clone = clonePointsList(s.getPointsList());
		
		STLCSSDistanceCalculator stlcss = 
				new STLCSSDistanceCalculator(0.0001, 1);
		return stlcss.getDistance(r_clone, s_clone);
	}
	
	/**
	 * Make a clone of this list of points.
	 */
	private ArrayList<Point> clonePointsList(List<Point> list){
		ArrayList<Point> new_list = new ArrayList<Point>();
		for(Point p : list){
			Point p_clone = p.clone();
			new_list.add(p_clone);
		}
		return new_list;
	}
/*	
	public static void main(String[] arg)
	{
		Point p1 = new Point(0, 0, 0);
		Point p2 = new Point(1, 1, 1);
		Point p3 = new Point(2, 0, 2);
		Point p4 = new Point(3, 1, 3);
		Point p5 = new Point(4, 1, 4);
		Point p6 = new Point(5, 0, 5);
		Point p7 = new Point(6, 1, 6);
		Trajectory t1 = new Trajectory();
		t1.addPoint(p1); t1.addPoint(p2); t1.addPoint(p3); t1.addPoint(p4);
		t1.addPoint(p5); t1.addPoint(p6); t1.addPoint(p7);
		
		Point p8 = new Point(0, 0, 0);
		Point p9 = new Point(1, -1, 1);
		Point p10 = new Point(2, 0, 2);
		Point p11 = new Point(3, -1, 3);
		Point p12 = new Point(4, 0, 4);
		Point p13 = new Point(5, -1, 5);
		Point p14 = new Point(6, 0, 6);
		Trajectory t2 = new Trajectory();
		t2.addPoint(p8); t2.addPoint(p9); t2.addPoint(p10); t2.addPoint(p11);
		t2.addPoint(p12); t2.addPoint(p13); t2.addPoint(p14);
		
		Trajectory t3 = new Trajectory();
		t3.addPoint(p8); t3.addPoint(p9); t3.addPoint(p10); t3.addPoint(p11);
		
		Trajectory t4 = new Trajectory();
		t4.addPoint(p12); t4.addPoint(p13); t4.addPoint(p14);
		
		DistanceService dist = new DistanceService();
		System.out.println("Dist T1 to T3: " + dist.STLCSS(t1, t3));
		System.out.println("Dist T1 to T4: " + dist.STLCSS(t1, t4));
		System.out.println("Dist T1 to (T3+T4): " + dist.STLCSS(t1, t2));
	}*/
	/*	
	public static void main(String[] args) {
        Point p1 = new Point(new double[]{0,0});
        Point p2 = new Point(new double[]{0,10});
        Point p3 = new Point(new double[]{0,12});
        ArrayList<Point> r =  new ArrayList<>();
        r.add(p1); r.add(p2); r.add(p3);
        
        Point p4 = new Point(new double[]{2,0});
        Point p5 = new Point(new double[]{2,7});
        Point p6 = new Point(new double[]{2,10});
        ArrayList<Point> s =  new ArrayList<>();
        s.add(p4); s.add(p5); s.add(p6);
        
        double cost = getEDwP(r, s);
        
        System.out.println("Cost: " + cost);
    }
*/	
}
