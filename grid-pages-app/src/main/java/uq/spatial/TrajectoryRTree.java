package uq.spatial;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.procedure.TIntProcedure;
import net.sf.jsi.Rectangle;
import net.sf.jsi.rtree.RTree;


/**
 * A serializable RTree of trajectories (MBR).
 *  * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class TrajectoryRTree extends RTree implements Serializable, GeoInterface{
	// the list of trajectories in this tree
	private List<Trajectory> trajectoryList = 
			new ArrayList<Trajectory>();

	/**
	 * Initialize this tree
	 */
	public TrajectoryRTree() {
		super();
		super.init(null);
	}

	/**
	 * True is this tree is empty or null.
	 */
	public boolean isEmpty(){
		return (this.size()==0);
	}
	
	/**
	 * Add a trajectory to this tree. 
	 */
	public void add(Trajectory trajectory){
		// add the MBR of this trajectory to the RTree
		this.add(trajectory.mbr().rectangle(), trajectoryList.size());
		this.trajectoryList.add(trajectory);
	}
	
	/**
	 * Add a list of trajectories to this tree. 
	 */
	public void addAll(List<Trajectory> tList){
		// add the MBR of each trajectory to the RTree
		int i = trajectoryList.size();
		for(Trajectory t : tList){
			this.add(t.mbr().rectangle(), i++);
		}
		trajectoryList.addAll(tList);	
	}

	/**
	 * Given a rectangular region, return the trajectories in this
	 * tree whose the MBR intersect/overlap with the given region
	 */
	public List<Trajectory> getTrajectoriesByMBR(
			final Box region){
		final List<Trajectory> tList = new ArrayList<Trajectory>();
		if(isEmpty()){
			return tList;
		}
		this.intersects(region.rectangle(), new TIntProcedure() {
			public boolean execute(int i) {
				tList.add(trajectoryList.get(i));	
				return true;  // continue receiving results
			}
		});
		return tList;
	}
	
	/**
	 * Return a list with all trajectories in this tree.
	 */
	public List<Trajectory> getTrajectoryList() {
		return trajectoryList;
	}
	
	/**
	 * The number of trajectories in this tree.
	 */
	public int numTrajectories(){
		return trajectoryList.size();
	}
	
	/**
	 * Merge these two trajectory trees.
	 * 
	 * @return Return this merged tree.
	 */
	public TrajectoryRTree merge(TrajectoryRTree tree){
		for(Trajectory t : tree.trajectoryList){
			add(t);
		}
		return this;
	}
	
	/**
	 * Given a rectangular region, and a integer K, return the trajectories
	 * within the K closest MBRs (tree nodes) from the given region (unsorted). 
	 */
	public List<Trajectory> getKNearest(
			final Rectangle region, final int k){
		final List<Trajectory> tList = new ArrayList<Trajectory>();
		if(isEmpty()){
			return tList;
		} 
		this.nearestNUnsorted(region.centre(), new TIntProcedure() {
			public boolean execute(int i) {
				tList.add(trajectoryList.get(i));
				return true; // continue receiving results
			}
		}, k, (float)MAX_X);
		return tList;
	}
}
