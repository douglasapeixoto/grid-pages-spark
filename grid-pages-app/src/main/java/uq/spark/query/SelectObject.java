package uq.spark.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uq.spatial.TimeComparator;
import uq.spatial.Trajectory;

/**
 * Used in the trajectory Selection query.
 * 
 * Contain a list of sub-trajectories
 * satisfying a query Q. All sub-trajectories
 * must belong to the same trajectory (parent).
 *  
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class SelectObject implements Serializable {
	// the sub-trajectories that composes this query object
	private List<Trajectory> subTrajectoryList = 
			new ArrayList<Trajectory>();
	/**
	 * Initiates an empty bag.
	 */
	public SelectObject(){}
	/**
	 * Initial selected sub-trajectory.
	 */
	public SelectObject(Trajectory obj) {
		this.subTrajectoryList.add(obj);
	}

	/** 
	 * Add a sub-trajectory to this object.
	 */
	public void add(Trajectory t){
		subTrajectoryList.add(t);
	}
	
	/**
	 * The list of sub-trajectories that compose
	 * this query object. All sub-trajectories belong 
	 * to the same trajectory (same parent ID).
	 */
	public List<Trajectory> getSubTrajectoryList(){
		return subTrajectoryList;
	}

	/**
	 * Merge these two objects.
	 * 
	 * @return Return this merged bag
	 */
	public SelectObject merge(SelectObject obj){
		subTrajectoryList.addAll(obj.getSubTrajectoryList());
		return this;
	}
	
	/**
	 * Post-process the sub-trajectories in this 
	 * object. Merge consecutive sub-trajectories
	 * if needed. 
	 * 
	 * @return Return a list of post-processed
	 * trajectories
	 */
	public List<Trajectory> postProcess(){
		if(subTrajectoryList.size() <= 1){
			return subTrajectoryList;
		}
		
		// sort the sub-trajectories by initial time-stamp
		TimeComparator<Trajectory> comparator = 
				new TimeComparator<Trajectory>();
		Collections.sort(subTrajectoryList, comparator);
		
		// merge consecutive sub-trajectories
		Trajectory sub_0 = subTrajectoryList.get(0);
		List<Trajectory> tList = new ArrayList<Trajectory>();
		for(int i=1; i<subTrajectoryList.size(); i++){
			Trajectory sub_i = subTrajectoryList.get(i);
			// bug do sort fix
			if(sub_0.head().equals(sub_i.head())){
				sub_0 = sub_0.size() > sub_i.size() ? sub_0 : sub_i;
			} else {
				// check last segment of sub_0 with first of sub_i
				if(sub_i.size() > 1){
					// merge the segments
					if(sub_0.tail().equals(sub_i.head())){
						sub_0.merge(sub_i.subTrajectory(1, sub_i.size()));
					}
					else if(sub_0.tail().equals(sub_i.get(1))){
						sub_0.merge(sub_i.subTrajectory(2, sub_i.size()));
					} else{
						tList.add(sub_0);
						sub_0 = subTrajectoryList.get(i);
					}
				} else if(sub_i.size() == 1){
					if(!sub_0.tail().equals(sub_i.head())){
						tList.add(sub_0);
						sub_0 = subTrajectoryList.get(i);
					}
				}
			}
		}
		// add final sub-trajectory
		tList.add(sub_0);

		return tList;
	}
}
