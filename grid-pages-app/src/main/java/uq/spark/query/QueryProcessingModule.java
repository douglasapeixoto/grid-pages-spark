package uq.spark.query; 

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.broadcast.Broadcast;

import uq.spark.index.Grid;
import uq.spark.index.TrajectoryCollector;
import uq.spark.index.TrajectoryTrackTableRDD;
import uq.spark.index.GridPagesRDD;
import uq.spatial.Box;
import uq.spatial.Trajectory;

/**
 * Service to process trajectory queries
 * on a previously built Grid index.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class QueryProcessingModule implements Serializable {
	// trajectory collector service
	private TrajectoryCollector collector = null;
	
	// Query services
	private SelectionQueryCalculator selectionQuery = null;
	private NearestNeighborQueryCalculator nnQuery = null;
	
	/**
	 * Service Constructor
	 */
	public QueryProcessingModule(final GridPagesRDD pagesRDD, 
								  final TrajectoryTrackTableRDD trackTable,
								  final Broadcast<Grid> diagram){
		// initialize trajectory collector service
		collector = new TrajectoryCollector(pagesRDD, trackTable);
		// initialize query services
		selectionQuery = new SelectionQueryCalculator(pagesRDD, diagram);
		nnQuery = new NearestNeighborQueryCalculator(pagesRDD, diagram, trackTable);
	}

	/**
	 * Given a rectangular geographic region, and a time window
	 * from t0 to t1, return all trajectories that overlap with
	 * the given region and time window [t0, t1]. 
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public List<Trajectory> getSpatialTemporalSelection (
			final Box region, 
			final long t0, final long t1, 
			final boolean whole){
		System.out.println("\n[QUERY MODULE] Running " +
				"Spatial Temporal Selection Query..\n");
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		if(whole){
			// collect whole trajectories
			List<String> resultIdList =
					selectionQuery.runSpatialTemporalSelectionId(region, t0, t1);
			trajectoryList = 
					collector.collectTrajectoriesById(resultIdList).collect();
		} else{
			// sub-trajectories only
			trajectoryList = 
				selectionQuery.runSpatialTemporalSelection(region, t0, t1);
		}
		return trajectoryList;	
	}
	
	/**
	 * Given a rectangular geographic region, return all trajectories 
	 * that overlap with the given region. 
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public List<Trajectory> getSpatialSelection(
			final Box region,
			final boolean whole){
		System.out.println("\n[QUERY MODULE] Running " +
				"Spatial Selection Query..\n");
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		if(whole){
			// collect whole trajectories
			List<String> resultIdList =
					selectionQuery.runSpatialSelectionId(region);
			trajectoryList = 
					collector.collectTrajectoriesById(resultIdList).collect();
		} else {
			// sub-trajectories only
			trajectoryList = 
					selectionQuery.runSpatialSelection(region);
		}
		return trajectoryList;
	}
	
	/**
	 * Given a a time window from t0 to t1, return all trajectories 
	 * that overlap with the time window, that is, return all trajectories 
	 * that have been active during [t0, t1].
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public List<Trajectory> getTimeSlice(
			final long t0, final long t1, 
			final boolean whole){
		System.out.println("\n[QUERY MODULE] Running " +
				"Time Slice Selection Query..\n");
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		if(whole){		
			// collect whole trajectories
			List<String> resultIdList = 
					selectionQuery.runTemporalSelectionId(t0, t1);
			trajectoryList = 
				collector.collectTrajectoriesById(resultIdList).collect();
		} else{
			// sub-trajectories only
			trajectoryList =
				selectionQuery.runTemporalSelection(t0, t1);
		}
		return trajectoryList;	
	}
	
	/**
	 * Given a query trajectory Q, a time interval t0 to t1,
	 * return the Nearest Neighbors (Most Similar Trajectory)  
	 * from Q, within the interval [t0,t1]. 
	 */
	public Trajectory getNearestNeighbor(
			final Trajectory q, 
			final long t0, final long t1){
		System.out.println("\n[QUERY MODULE] Running " +
				"NN Query..\n");
		// query result
		NearNeighbor nnResult = 
				nnQuery.runNearestNeighborQuery(q, t0, t1);
		return nnResult;
	}
	
	/**
	 * Given a query trajectory Q, a time interval t0 to t1,
	 * and a integer K, return the K Nearest Neighbors (Most  
	 * Similar Trajectories) from Q, within the interval [t0,t1]. 
	 */
	public List<NearNeighbor> getKNearestNeighbors(
			final Trajectory q, 
			final long t0, final long t1, 
			final int k){
		System.out.println("\n[QUERY MODULE] Running "
				+ k + "-NN Query..\n");
		// query result
		List<NearNeighbor> resultList = 
				nnQuery.runKNearestNeighborsQuery(q, t0, t1, k);
		return resultList;
	}
}
