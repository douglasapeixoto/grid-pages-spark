package uq.spark.index;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.hadoop.io.Writable;

import uq.spark.EnvironmentVariables;
import uq.spatial.Point;
import uq.spatial.Trajectory;
import uq.spatial.TrajectoryRTree; 

/**
 * Implements a Grid Page.
 * </br></br>
 * A page is a spatial-temporal Grid cell with a tree of
 * sub-trajectories during a certain time interval.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class GridPage implements Serializable, Writable, EnvironmentVariables{
	/**
	 * The tree containing the MBR of the 
	 * trajectories/sub-trajectories in this page
	 */
	private TrajectoryRTree trajectoryTree = 
			new TrajectoryRTree();

	/**
	 * List of the parent trajectories of
	 * the sub-trajectories in this page
	 */
	private HashSet<String> parentIdSet = 
			new HashSet<String>();

	/**
	 * Add a trajectory/sub-trajectory to this page tree. 
	 */
	public void add(Trajectory trajectory){
		// add the MBR of this trajectory to the RTree
		trajectoryTree.add(trajectory);
		parentIdSet.add(trajectory.id);
	}
	
	/**
	 * Merge two pages. 
	 * Add the trajectories/sub-trajectories from the given 
	 * page to this page trajectory tree. 
	 * 
	 * @return Return this merged page.
	 */
	public GridPage merge(GridPage page){
		trajectoryTree.addAll(page.getTrajectoryList());
		parentIdSet.addAll(page.parentIdSet);	
		return this;
	}
	
	/**
	 * Return the tree of sub-trajectories in this page.
	 */
	public TrajectoryRTree getTrajectoryTree(){
		return trajectoryTree;
	}
	
	/**
	 * Return the set of trajectories that overlaps with this page.
	 * </br>
	 * The parent trajectories of the sub-trajectories in this page.
	 * 
	 * @return Return trajectories ID.
	 */
	public HashSet<String> getTrajectoryIdSet(){
		return parentIdSet;
	}	
	
	/**
	 * The list of trajectories/sub-trajectories in this page.
	 * </br></br>
	 * Note that if a trajectory is contained in more than one page, 
	 * this function will return only those sub-trajectories contained
	 * in the tree of this page specifically.
	 */
	public List<Trajectory> getTrajectoryList() {
		return trajectoryTree.getTrajectoryList();
	}
	
	/**
	 * Given a trajectory T id, return all 
	 * sub-trajectories in this page belonging to T.
	 */
	public List<Trajectory> getTrajectoriesById(String trajectoryId){
		List<Trajectory> list = new ArrayList<Trajectory>();
		for(Trajectory t : trajectoryTree.getTrajectoryList()){
			if(trajectoryId.equals(t.id)){
				list.add(t);
			}
		}
		return list;	
	}
	
	/**
	 * A list with all trajectory points in this page.
	 */
	public List<Point> getPointsList() {
		List<Point> pointsList = 
				new ArrayList<Point>();
		for(Trajectory t : trajectoryTree.getTrajectoryList()){
			pointsList.addAll(t.getPointsList());
		}
		return pointsList;
	}
	
	/**
	 * Return the number of trajectories/sub-trajectories
	 * in this page.
	 */
	public int size(){
		return trajectoryTree.numTrajectories();
	}
	
	/**
	 * Return true is this page has no element
	 * or is null.
	 */
	public boolean isEmpty(){
		return trajectoryTree.isEmpty();
	}

	/**
	 * Print this page: System out.
	 */
	public String print() {
		String string = "Page [";
		for(Trajectory t : trajectoryTree.getTrajectoryList()){
			string += t.id + " {";
			for(Point p : t.getPointsList()){
				string += p.toString();
			}
			string += "}";
		}
		string += "]";
		return string;
	}
	
	@Override
	public String toString() {
		// sub-trajectories separated by ":"
		// sample points separated by " "
		String string = "";
		for(Trajectory t : trajectoryTree.getTrajectoryList()){
			string += t.id;
			for(Point p : t.getPointsList()){
				string += " " + p.toString();
			}
			string += ":";
		}		
		return string.substring(0, string.length()-1);
	}
	
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		trajectoryTree = new TrajectoryRTree();
		parentIdSet = new HashSet<String>(size);
	    for(int i = 0; i < size; i++){
	        Trajectory t = new Trajectory();
	        t.readFields(in);
	        trajectoryTree.add(t);
	        parentIdSet.add(t.id);
	    }
	}

	public void write(DataOutput out) throws IOException {
	    out.writeInt(this.size());
	    for(Trajectory t : trajectoryTree.getTrajectoryList()) {
	        t.write(out);
	    }
	}
}
