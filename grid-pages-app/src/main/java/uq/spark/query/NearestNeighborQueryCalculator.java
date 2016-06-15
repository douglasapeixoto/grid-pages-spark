package uq.spark.query;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;

import uq.spark.EnvironmentVariables;
import uq.spark.index.Grid;
import uq.spark.index.IndexParameters;
import uq.spark.index.TrajectoryCollector;
import uq.spark.index.TrajectoryTrackTableRDD;
import uq.spark.index.GridPagesRDD;
import uq.spatial.Circle;
import uq.spatial.Point;
import uq.spatial.Trajectory;
import uq.spatial.distance.DistanceService;

/**
 * Implement Most Similar Trajectory (nearest neighbor) 
 * queries over the Grid RDD.
 * </br>
 * Note: This is an approximate algorithm, based
 * on the trajectories centroids distances.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class NearestNeighborQueryCalculator implements 
	Serializable, EnvironmentVariables, IndexParameters {
	private GridPagesRDD pagesRDD;
	private TrajectoryTrackTableRDD trackTable;
	private Grid diagram;
	
	// trajectory collector service
	private TrajectoryCollector collector = null;
	
	// service to calculate trajectory distances
	private final DistanceService distService = 
			new DistanceService();
	
	// NN comparator
	private final NeighborComparator<NearNeighbor> nnComparator = 
			new NeighborComparator<NearNeighbor>();

	/**
	 * Constructor. Receives the PagesRDD, an instance of  
	 * the Grid diagram and the track table.
	 */
	public NearestNeighborQueryCalculator(
			final GridPagesRDD pagesRDD, 
			final Broadcast<Grid> diagram,
			final TrajectoryTrackTableRDD trackTable) {
		this.pagesRDD = pagesRDD;
		this.diagram = diagram.value();
		this.trackTable = trackTable;
		collector = new TrajectoryCollector(this.pagesRDD, this.trackTable);
	}

	/**
	 * NN Query:
	 * Given a query trajectory Q and a time interval t0 to t1,
	 * return the Nearest Neighbor (Most Similar Trajectory) 
	 * from Q, within the interval [t0,t1]. 
	 */
	public NearNeighbor runNearestNeighborQuery(
			final Trajectory q, 
			final long t0, final long t1){
		List<NearNeighbor> result = 
				runKNearestNeighborsQuery(q, t0, t1, 1);
		NearNeighbor nn = new NearNeighbor();
		if(result!=null && !result.isEmpty()){
			nn = result.get(0);
		}
		return nn;
	}
	
	/**
	 * K-NN Query:
	 * Given a query trajectory Q, a time interval t0 to t1,
	 * and a integer K, return the K Nearest Neighbors (Most  
	 * Similar Trajectories) from Q, within the interval [t0,t1]. 
	 */
	public List<NearNeighbor> runKNearestNeighborsQuery(
			final Trajectory query, 
			final long t0, final long t1, 
			final int k){
		/**
		 * FIRST FILTER:
		 */
		// check the grid cells that overlaps with the query 
		final HashSet<Integer> gridIdSet = 
				getOverlappingRectangles(query);

		// get page(s) time index to retrieve
		final int TPIini = (int)(t0 / TIME_WINDOW_SIZE) + 1;
		final int TPIend = (int)(t1 / TIME_WINDOW_SIZE) + 1;
		
		// collect candidate trajectories inside the given pages (whole trajectories)
		JavaRDD<Trajectory> candidatesRDD = 
				collector.collectTrajectoriesByPageIndex(gridIdSet, TPIini, TPIend);	
		
		/**
		 * FIRST REFINEMENT:
		 * Get the kNN inside the partitions containing the query trajectory
		 */
		// get first candidates (trajectories in the grids containing Q), 
		// map each trajectory to a NN object
		List<NearNeighbor> candidatesList = new LinkedList<NearNeighbor>();
		candidatesList = getCandidatesNN(candidatesRDD, candidatesList, query, t0, t1);
		
		/**
		 * SECOND FILTER:
		 */
		// get the k-th-NN returned
		Trajectory knn;
		if(candidatesList.size() >= k){
			knn = candidatesList.get(k-1);
		} else if(candidatesList.size() > 0){
			knn = candidatesList.get(candidatesList.size()-1);
		} else{ // no candidates to return 
			// TODO: need to extend the search area
			return candidatesList;
		}
		
		// get the circle made from the centroids
		Circle c = getCentroidCircle(query, knn);

		// check the grid rectangles that overlaps with the query,
		// except those already retrieved
		final HashSet<Integer> extendGridIdSet = 
				diagram.getOverlappingCells(c.mbr());
		extendGridIdSet.removeAll(gridIdSet);

		/**
		 * SECOND REFINEMENT:
		 */
		// if there are other grids to check
		if(extendGridIdSet.size() > 0){
			// collect the new trajectories
			JavaRDD<Trajectory> extendTrajectoryRDD =
					collector.collectTrajectoriesByPageIndex(extendGridIdSet, TPIini, TPIend);
			// refine and update the candidates list
			candidatesList = getCandidatesNN(extendTrajectoryRDD, candidatesList, query, t0, t1);
		}
		
		// collect result
		if(candidatesList.size() >= k){
			return candidatesList.subList(0, k);
		}
		return candidatesList;
	}

	/**
	 * Return the positions (index) of the cells in the grid that
	 * overlaps with the given trajectory, that is, rectangles that 
	 * contain or intersect any of the trajectory's segments.
	 */
	private HashSet<Integer> getOverlappingRectangles(Trajectory t) {
		HashSet<Integer> posSet = new HashSet<Integer>();
		for(Point p : t.getPointsList()){
			/*Point p1 = t.get(i);
			Point p2 = t.get(i+1);
			Segment s = new Segment(p1.x, p1.y, p2.x, p2.y);*/
			posSet.add(diagram.getOverlappingCell(p));
		}
		return posSet;
	}
	
	/**
	 * Given two trajectories t1 and t2, calculate the circle
	 * composed of the centroid of t1 as center, and the maximum
	 * distance between t1 and t2 as radius.
	 */
	private Circle getCentroidCircle(Trajectory t1, Trajectory t2) {
		// get farthest distance
		double dist, max = 0;
		for(Point p1 : t1.getPointsList()){
			for(Point p2 : t2.getPointsList()){
				dist = p1.dist(p2);
				if(dist > max){
					max = dist;
				}
			}
		}
		return new Circle(t1.centroid(), max);
	}	

	/**
	 * Check the trajectories time-stamp and 
	 * calculate the distance between every trajectory in the list to
	 * the query trajectory, return a sorted list of NN by distance.
	 * </br>
	 * Calculate the NN object only for the new trajectories (i.e.  
	 * trajectories not contained in current list).
	 * 
	 * @return Return the updated current NN list.
	 */
	private List<NearNeighbor> getCandidatesNN(
			final JavaRDD<Trajectory> candidateRDD, 
			final List<NearNeighbor> currentList,
			final Trajectory q,
			final long t0, final long t1){
		if(candidateRDD != null){
			List<NearNeighbor> nnCandidatesList = 
				// filter out new trajectories, and refine time
				candidateRDD.filter(new Function<Trajectory, Boolean>() {
					public Boolean call(Trajectory t) throws Exception {
						if(t.timeIni() > t1 || t.timeEnd() < t0){
							return false;
						}
						return (!currentList.contains(t));
					}
				// map each new trajectory in the candidates list to a NN
				}).map(new Function<Trajectory, NearNeighbor>() {
					public NearNeighbor call(Trajectory t) throws Exception {
						NearNeighbor nn = new NearNeighbor(t);
						nn.distance = distService.EDwP(q, t);
						return nn;			}
				}).collect();
			// add new candidates
			currentList.addAll(nnCandidatesList);
			// sort by distance to Q
			Collections.sort(currentList, nnComparator);
		}
		return currentList;
	}
}
