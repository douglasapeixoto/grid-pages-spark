package uq.spark.index;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;

import uq.spark.EnvironmentVariables;
import uq.spatial.Trajectory;
import uq.spatial.TrajectoryRTree;

/**
 * Service responsible to collect trajectories from
 * the index structure (PagesRDD). Collect trajectories
 * using the Page index structure and the Trajectory
 * Track Table. Post-process trajectories after collection.
 * 
 * The collection process of trajectories is done in 3 steps:
 * (1) Filter: filter pages containing the trajectories
 * (2) Collect: collect the sub-trajectories from the pages
 * (3) Post-processing: Merge sub-trajectories and remove duplicate points
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class TrajectoryCollector implements Serializable, EnvironmentVariables{
	private GridPagesRDD pagesRDD;
	private TrajectoryTrackTableRDD trackTable;

	// log info
	/*private static int TOTAL_PAGES_FILTERED = 0;
	private static int TOTAL_TRAJ_FILTERED = 0;
	private static int TOTAL_TRAJ_COLLECTED = 0;*/
	
	/**
	 * Creates a new collector.
	 */
	public TrajectoryCollector(
			final GridPagesRDD pagesRDD, 
			final TrajectoryTrackTableRDD trackTable) {
		this.pagesRDD = pagesRDD;
		this.trackTable = trackTable;
	}
	
	/**
	 * Given a trajectory ID, retrieve the given 
	 * trajectory from the Pages RDD. Retrieve the
	 * whole trajectory. Post-process after
	 * retrieval.
	 * 
	 * @return Return the given trajectory.
	 * (Note: the given trajectories must be in the dataset)
	 */
	public Trajectory collectTrajectoryById(
			final String id){
		// retrieve from the TTT the indexes of all pages that 
		// contains the trajectories in the list
		HashSet<PageIndex> indexSet = 
				trackTable.collectPageIndexListByTrajectoryId(id);

		// filter pages that contains the specified trajectory
		JavaPairRDD<PageIndex, GridPage> filteredPagesRDD = 
				pagesRDD.filterPagesByIndex(indexSet);
		
		// map each page to a list of sub-trajectories, and merge them
		Trajectory trajectory =
			filteredPagesRDD.values().flatMap(new FlatMapFunction<GridPage, Trajectory>() {
				public Iterable<Trajectory> call(GridPage page) throws Exception {
					return page.getTrajectoriesById(id);
				}
			}).reduce(new Function2<Trajectory, Trajectory, Trajectory>() {
				public Trajectory call(Trajectory sub1, Trajectory sub2) throws Exception {
					sub1.merge(sub2);
					return sub1;
				}
			});
		
		//post processing
		trajectory = postProcess(trajectory);
		
		return trajectory;
	}
		
	/**
	 * Given a set of trajectory IDs, retrieve the given 
	 * trajectories from the Pages RDD. 
	 * </br>
	 * Retrieve whole trajectories.
	 * </br>
	 * Post-process after collection.
	 * 
	 * @return Return a distributed dataset (RDD) of trajectories.
	 * (Note: the given trajectories must be in the dataset)
	 */
	public JavaRDD<Trajectory> collectTrajectoriesById(
			final Collection<String> trajectoryIdSet){
		// retrieve from the TTT the indexes of all pages that 
		// contains the trajectories in the list
		HashSet<PageIndex> indexSet = 
				trackTable.collectPageIndexListByTrajectoryId(trajectoryIdSet);
	
		// filter pages that contains the specified trajectories
		JavaPairRDD<PageIndex, GridPage> filteredPagesRDD = 
				pagesRDD.filterPagesByIndex(indexSet);

		// collection log
		/*QUERY_STATS_ACCUM.add("Total Pages: " + pagesRDD.count());
		QUERY_STATS_ACCUM.add("Total Pages Filtered: " + filteredPagesRDD.count());
		// get the number of trajectories that intersect with these pages
		int totalTrajectories = 
			filteredPagesRDD.values().flatMap(new FlatMapFunction<GridPage, String>() {
				public Iterable<String> call(GridPage page) throws Exception {
					return page.getTrajectoryIdSet();
				}
			}).distinct().collect().size();
		QUERY_STATS_ACCUM.add("Total Trajectories Filtered (TP+FP): " + totalTrajectories);
		TOTAL_PAGES_FILTERED += filteredPagesRDD.count();
		TOTAL_TRAJ_FILTERED  += totalTrajectories;*/		

		// map each page to a list key value pairs containing 
		// the desired trajectories
		JavaRDD<Trajectory> trajectoryRDD =
			filteredPagesRDD.values().flatMapToPair(new PairFlatMapFunction<GridPage, String, Trajectory>() {
				public Iterable<Tuple2<String, Trajectory>> call(GridPage page) throws Exception {
					// iterable list to return
					List<Tuple2<String, Trajectory>> list = 
							new LinkedList<Tuple2<String, Trajectory>>();
					for(Trajectory sub : page.getTrajectoryList()){
						if(trajectoryIdSet.contains(sub.id)){
							list.add(new Tuple2<String, Trajectory>(sub.id, sub));
						}
					}
					return list;
				}
				// merge trajectories by key
			}).reduceByKey(new Function2<Trajectory, Trajectory, Trajectory>() {
				public Trajectory call(Trajectory sub1, Trajectory sub2) throws Exception {
					sub1.merge(sub2);
					return sub1;
				}
			}).values();
		
		//post processing
		trajectoryRDD = postProcess(trajectoryRDD);
	
		// collection log
		/*QUERY_STATS_ACCUM.add("Total Trajectories Collected (TP): " + trajectoryRDD.count());
		System.out.println("Total Trajectories Collected (TP): " + trajectoryRDD.count());
		TOTAL_TRAJ_COLLECTED += trajectoryRDD.count();
		System.out.println("TOTAIS: ");
		System.out.println("TOTAL PAGES FILT.: " + TOTAL_PAGES_FILTERED);
		System.out.println("TOTAL TRAJ FILT.:  " + TOTAL_TRAJ_FILTERED);
		System.out.println("TOTAL TRAJ COL.:   " + TOTAL_TRAJ_COLLECTED);*/
		System.out.println();

		return trajectoryRDD;
	}
	
	/**
	 * Given a set of page index, collect from the RDD the trajectories 
	 * inside the given pages. That is, trajectories inside pages with 
	 * spatial index VSI in VSIlist and time page TPI in [TPIini, TPIend].
	 * </br>
	 * Return whole trajectories (also filter from the PagesRDD other pages
	 * that might contain trajectories in the given pages set.
	 * </br>
	 * May receive a logger to keep collection info.
	 * 
	 * @return Return a distributed dataset (RDD) of trajectories.
	 * If there is no trajectories in the given page set, then return null. 
	 */
	public JavaRDD<Trajectory> collectTrajectoriesByPageIndex(
			final Collection<Integer> CSIlist, 
			final int TPIini, final int TPIend) {
		// get the list of indexes to filter out
		List<PageIndex> indexList = 
				getIndexCombination(CSIlist, TPIini, TPIend);

		return collectTrajectoriesByPageIndex(indexList);
	}
	
	/**
	 * Given a set of page index, collect from the RDD the trajectories 
	 * inside the given pages.
	 * </br>
	 * Return whole trajectories (also filter from the PagesRDD other pages
	 * that might contain trajectories in the given pages set.
	 * 
	 * @return Return a distributed dataset (RDD) of trajectories.
	 * If there is no trajectories in the given page set, then return null. 
	 */
	public JavaRDD<Trajectory> collectTrajectoriesByPageIndex(
			final Collection<PageIndex> indexSet) {
		// Filter the given pages
		JavaPairRDD<PageIndex, GridPage> filteredPagesRDD = 
				pagesRDD.filterPagesByIndex(indexSet);

		// collection log
		/*System.out.println("Collect Trajectories by Page Index.");
		System.out.println("Total Pages: " + pagesRDD.count());
		System.out.println("Total Pages to Collect: " + filteredPagesRDD.count());
		TOTAL_PAGES_TO_COLLECT += filteredPagesRDD.count();*/
		
		// check if there is any page to for the given parameters
		// Note: (it might be there is no page in the given time interval for the given polygon)
		if(!filteredPagesRDD.isEmpty()){			
			// Collect the IDs of the trajectories inside the given pages.
			final List<String> idList = 	
				filteredPagesRDD.values().flatMap(new FlatMapFunction<GridPage, String>() {
					public Iterable<String> call(GridPage page) throws Exception {
						return page.getTrajectoryIdSet();
					}
				}).distinct().collect();

			// retrieve from the TTT the indexes of all pages that 
			// contains the trajectories in the list.
			HashSet<PageIndex> diffIndexSet = 
					trackTable.collectPageIndexListByTrajectoryId(idList);

			// skip the pages already retrieved 
			diffIndexSet.removeAll(indexSet);

			// filter the other pages that contain the trajectories (difference set)
			JavaPairRDD<PageIndex, GridPage>  diffPagesRDD = 
					pagesRDD.filterPagesByIndex(diffIndexSet);

			// union the two RDDs (union set)
			filteredPagesRDD = filteredPagesRDD.union(diffPagesRDD);

			// map each page to a list of key value pairs containing 
			// the desired trajectories
			JavaRDD<Trajectory> trajectoryRDD =
				filteredPagesRDD.values().flatMapToPair(new PairFlatMapFunction<GridPage, String, Trajectory>() {
					public Iterable<Tuple2<String, Trajectory>> call(GridPage page) throws Exception {
						// iterable list to return
						List<Tuple2<String, Trajectory>> list = 
								new LinkedList<Tuple2<String, Trajectory>>();
						for(Trajectory sub : page.getTrajectoryList()){
							if(idList.contains(sub.id)){
								list.add(new Tuple2<String, Trajectory>(sub.id, sub));
							}
						}
						return list;
					}
					// merge trajectories by key
				}).reduceByKey(new Function2<Trajectory, Trajectory, Trajectory>() {
					public Trajectory call(Trajectory sub1, Trajectory sub2) throws Exception {
						sub1.merge(sub2);
						return sub1;
					}
				}).values();
			
			// post processing
			trajectoryRDD = postProcess(trajectoryRDD);
			
			// collection log
			/*System.out.println("Total Pages Filtered: " + filteredPagesRDD.count());
			System.out.println("Total Trajectories Filtered (TP+FP): " + idList.size());
			System.out.println("Total Trajectories Collected (TP): " + trajectoryRDD.count());
			TOTAL_TRAJ_FILTERED += idList.size();
			TOTAL_PAGES_FILTERED += filteredPagesRDD.count();
			System.out.println("TOTAIS: ");
			System.out.println("TOTAL TRAJ FILT.: " + TOTAL_TRAJ_FILTERED);
			System.out.println("TOTAL PAGES FILT.: " + TOTAL_PAGES_FILTERED);
			System.out.println("TOTAL PAGES TO COL.: " + TOTAL_PAGES_TO_COLLECT);*/
			
			return trajectoryRDD;
		}
		return null;
	}
	
	/**
	 * Given a set of page index, collect from the RDD the trajectories 
	 * inside the given pages. Return a trajectory tree.
	 * </br>
	 * Return whole trajectories (also filter from the PagesRDD other pages
	 * that might contain trajectories in the given pages set.

	 * @return Return a trajectory tree.
	 */
	public TrajectoryRTree collectTrajectoriesByPageIndexAsTree(
			final Collection<Integer> VSIlist, 
			final int TPIini, final int TPIend) {
		// get a post-processed RDD of trajectories
		JavaRDD<Trajectory> trajectoryRDD = 
				collectTrajectoriesByPageIndex(VSIlist, TPIini, TPIend);
		// build the tree
		TrajectoryRTree tree = new TrajectoryRTree();
		tree.addAll(trajectoryRDD.collect());
	
		return tree;
	}
	
	/**
	 * Given a set of page index, collect from the RDD the trajectories 
	 * inside the given pages. Return a trajectory tree.
	 * </br>
	 * Return whole trajectories (also filter from the PagesRDD other pages
	 * that might contain trajectories in the given pages set.

	 * @return Return a trajectory tree.
	 */
	public TrajectoryRTree collectTrajectoriesByPageIndexAsTree(
			final Collection<PageIndex> indexSet) {
		// get a post-processed RDD of trajectories
		JavaRDD<Trajectory> trajectoryRDD = 
				collectTrajectoriesByPageIndex(indexSet);
		// build the tree
		TrajectoryRTree tree = new TrajectoryRTree();
		tree.addAll(trajectoryRDD.collect());
	
		return tree;
	}
	
	/**
	 * Given a list of spatial indexes, and a
	 * time index interval, return the page index
	 * combination.
	 */
	private List<PageIndex> getIndexCombination(
			final Collection<Integer> VSIlist, 
			final int TPIini, final int TPIend){
		List<PageIndex> combineList = 
				new LinkedList<PageIndex>();
		for(Integer vsi : VSIlist){
			for(int tpi = TPIini; tpi <= TPIend; tpi++){
				combineList.add(new PageIndex(vsi, tpi));
			}
		}
		return combineList;
	}
	
	/**
	 * Post processing operation.
	 * </br>
	 * Sorts the trajectory points by time stamp
	 * and removes any duplicates form the map phase.
	 * 
	 * @return A post-processed trajectory
	 */
	private Trajectory postProcess(Trajectory t) {
		t.sort();
		int size = t.size();
		for(int i = 0; i < size-1; i++){
			if(t.get(i).equals(t.get(i+1))){
				t.removePoint(i);
				size--;
				--i;
			}
		}
		return t;
	}

	/**
	 * Post processing operation. Done in parallel.
	 * </br>
	 * Sorts the trajectory points by time stamp
	 * and removes any duplicates from the map phase.
	 * 
	 * @return A post-processed RDD of trajectories
	 */
	private JavaRDD<Trajectory> postProcess(
			JavaRDD<Trajectory> trajectoryRDD){
		// map each trajec to its post-process version
		trajectoryRDD = 
			trajectoryRDD.map(new Function<Trajectory, Trajectory>() {
				public Trajectory call(Trajectory t) throws Exception {
					return postProcess(t);
				}
			});
		return trajectoryRDD;
	}
}
