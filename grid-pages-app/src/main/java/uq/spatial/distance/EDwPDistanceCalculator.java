package uq.spatial.distance;

import java.io.Serializable;
import java.util.ArrayList;
import uq.spatial.Point;

/**
* EDwP: Edit Distance with Projections 
*
* @author uqdalves
*/
@SuppressWarnings("serial")
class EDwPDistanceCalculator implements Serializable, DistanceInterface {

	public double getDistance(ArrayList<Point> r, ArrayList<Point> s) {
		double dist = getEDwP(r, s);
		if(Double.isNaN(dist)){
			return INFINITY;
		}
		return dist;
	}
	
	/**
	 * Edit Distance with Projections
	 */
	private static double getEDwP(ArrayList<Point> r, ArrayList<Point> s) {
		double total_cost_edwp = 0;
		
		if(length(r) == 0 && length(s) == 0){
			return 0;
		}
		if(length(r) == 0 || length(s) == 0){
			return INFINITY;
		}

		boolean flag = false;
		while(!flag) {
			// cost of the replacement
			double replacement = 0;	
			double coverage = 0;
			
			// Insert and replacement
			if (s.size() == 1 && r.size() > 1){
				Point e_p1 = r.get(0);
				Point e_p2 = r.get(1);
				Point p = s.get(0);

				replacement = replacement(e_p1, e_p2, p, p);
				coverage = coverage(e_p1, e_p2, p, p);
			}
			// Insert and replacement
			else if(r.size() == 1 && s.size() > 1){			
				Point e_p1 = s.get(0);
				Point e_p2 = s.get(1);
				Point p = r.get(0);
			
				replacement = replacement(e_p1, e_p2, p, p);
				coverage = coverage(e_p1, e_p2, p, p);
			} 
			// Insert and replacement
			else if(r.size() > 1 && s.size() > 1){
				Point e1_p1 = r.get(0);
				Point e1_p2 = r.get(1);
				Point e2_p1 = s.get(0);
				Point e2_p2 = s.get(1);
	
				// get the coordinates of p_ins (projections)
				Point p_ins_e1 = projection(e1_p1, e1_p2, e2_p2);
				Point p_ins_e2 = projection(e2_p1, e2_p2, e1_p2);
				
				// test which replacement is better
				double replace_e1 = replacement(e1_p1, p_ins_e1, e2_p1, e2_p2);
				double replace_e2 = replacement(e2_p1, p_ins_e2, e1_p1, e1_p2);
				double cover_e1 = coverage(e1_p1, p_ins_e1, e2_p1, e2_p2);
				double cover_e2 = coverage(e2_p1, p_ins_e2, e1_p1, e1_p2);

				if((cover_e1*replace_e1) <= (cover_e2*replace_e2)){
					// replacement 1 is better
					replacement = replace_e1;
					coverage = cover_e1;
					
					// if the projection is not already there
					if(!p_ins_e1.isSamePosition(e1_p1) && 
					   !p_ins_e1.isSamePosition(e1_p2)){
						r.add(1, p_ins_e1);
					}
				} else {
					// replacement 2 is better
					replacement = replace_e2;
					coverage = cover_e2;
					
					// if the projection is not already there
					if(!p_ins_e2.isSamePosition(e2_p1) && 
					   !p_ins_e2.isSamePosition(e2_p2)){
						s.add(1, p_ins_e2);
					}
				}
			}
			// end
			else {
				flag = true;
			}
			
			r = rest(r);
			s = rest(s);
			
			total_cost_edwp += (replacement * coverage);
		}
		
		return total_cost_edwp;
	}
	
	/**
	 * Cost of the operation where the segment e1 is matched with e2.
	 */
	private static double replacement(Point e1_p1, Point e1_p2, Point e2_p1, Point e2_p2) {
		// Euclidean distances between segments' points
		double dist_p1 = e1_p1.dist(e2_p1);
		double dist_p2 = e1_p2.dist(e2_p2);

		// replacement cost
		double rep_cost = dist_p1 + dist_p2;
		
		return rep_cost;
	}
	
	/**
	 * Coverage; quantifies how representative the segment being
	 * edit are of the overall trajectory. Segment e1 and e2.
	 * e1 = [e1.p1, e1.p2], e2 = [e2.p1, e2.p2]
	 */
	private static double coverage(Point e1_p1, Point e1_p2, Point e2_p1, Point e2_p2){
		// segments coverage
		double cover = e1_p1.dist(e1_p2) + e2_p1.dist(e2_p2);
		return cover;
	}
	
	/**
	 * Returns a sub-trajectory containing all segments of 
	 * the list except the first one.
	 */
	private static ArrayList<Point> rest(ArrayList<Point> list){
		if(!list.isEmpty()){
			list.remove(0);
		}
		return list;	
	}

	/**
	 * The length of the trajectory (sum of the segments length)
	 */
	private static double length(ArrayList<Point> list){
		double length = 0;
		
		for(int i=0; i<list.size()-1; i++){
			length += list.get(i).dist(list.get(i+1));
		}
		
		return length;
	}
		
	/**
	 * Calculate the projection of the point p on to the segment e
	 * e = [e.p1, e.p2]
	 */
	private static Point projection(Point e_p1, Point e_p2, Point p){
		// get dot product of [e.p2 - e.p1] and [p_proj - p]
		double dot_product = dotProduct(e_p1, e_p2, p);
	
		// get squared length of e
		double len_2 = Math.pow(e_p2.x - e_p1.x, 2) + 
					   Math.pow(e_p2.y - e_p1.y, 2);
		
		// Calculate the coordinates of p_proj (projection) using the
		// dot product and the squared length of e
		double x = e_p1.x + 
			(dot_product * (e_p2.x - e_p1.x)) / len_2;
		double y = e_p1.y + 
			(dot_product * (e_p2.y - e_p1.y)) / len_2;
		
		// calculate the time of the projection
		//long time = (long)(e_p1.time + (e_p1.dist(p) * (e_p2.time - e_p1.time))/(e_p1.dist(e_p2)));
		
		Point p_proj = new Point(x, y);
		
		return p_proj;
	}
	
	/**
	 * Calculates the dot product between segment e and point p.
	 * e = [e.p1, e.p2]
	 */
	private static double dotProduct(Point e_p1, Point e_p2, Point p){
		// shift the points to the origin
		double e1_x = e_p2.x - e_p1.x;
		double e1_y = e_p2.y - e_p1.y;
		double e2_x = p.x - e_p1.x;
		double e2_y = p.y - e_p1.y;

		// calculate the dot product
		double dot_product = (e1_x * e2_x) + (e1_y * e2_y);
		
		return dot_product;
	}
	
	@Override
	public String toString() {
		return "EDwP";
	}

}



