package uq.spark.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.util.SizeEstimator;

import scala.Tuple2;

import uq.fs.HDFSFileService;
import uq.spark.EnvironmentVariables;
import uq.spatial.Point;
import uq.spatial.Trajectory;

/**
 * The Grid pages RDD itself.
 * </br>
 * This is the main data object RDD. Contains the
 * RDD of pages.
 * </br>
 * To build this RDD you must either call build(), 
 * to construct the RDD from the scratch, or call 
 * load(), to load the RDD from outside source 
 * (HDFS,Tachyon, local, etc.).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class GridPagesRDD implements Serializable, EnvironmentVariables, IndexParameters {
	// the RDD for this Grid diagram. Grid pages.
	private JavaPairRDD<PageIndex, GridPage> gridPagesRDD = null;
	// number of partitions to coalesce after filtering pages
	private static final int NUM_COALESCE = NUM_PARTITIONS_DATA / 5;
	
	/**
	 * Build the pages RDD using MapReduce/Aggregate functions.
	 * </br>
	 * Aggregate (reduce) sub-trajectories by Page Index.
	 * Build the Grid pages RDD and the pages tree.
	 */
	public void build(
			final JavaPairRDD<PageIndex, Trajectory> trajectoryToPageIndexRDD){
		// build the Grid pages RDD itself.
		aggregatePagesByKey(trajectoryToPageIndexRDD);
		gridPagesRDD.setName("VoronoiPagesRDD");
		gridPagesRDD.persist(STORAGE_LEVEL_PAGES);
	}
	
	/**
	 * Build this pages RDD by loading it from the file system.
	 * </br>
	 * Read a previous saved copy of this RDD.
	 * 
	 * @param The absolute path to the RDD folder (HDFS, Tachyon, Local, etc.).
	 */
	public void load(final String path) {
		JavaRDD<String> inputRDD = SC.textFile(path);
		// map the input file to pages
		gridPagesRDD = 
			inputRDD.mapToPair(new PairFunction<String, PageIndex, GridPage>() {
				// read each line of the split file, each line is a Page
				public Tuple2<PageIndex, GridPage> call(String line) throws Exception {
					// ignore the delimiter chars (..) and split
					String[] tokens = line.split("\\(|,|\\)");
					// first and second chars are the VSI and TPI index
					int CSI = Integer.parseInt(tokens[1]);
					int TPI	= Integer.parseInt(tokens[2]);
					PageIndex index = new PageIndex(CSI, TPI);
					
					// next fields are the sub-trajectories in this page
					// separated by ":"
					String[] subTrajectories = tokens[3].split(":");
					
					// a new page for this line
					GridPage page = new GridPage();
					
					// read sub-trajectories
					for(String sub : subTrajectories){
						String[] points = sub.split(" ");
						// first token is the trajectory ID
						Trajectory t = new Trajectory(points[0]);
						double x, y; long time;
						// process points
						for(int i=1; i<points.length; i+=3){
							x = Double.parseDouble(points[i]);
							y = Double.parseDouble(points[i+1]);
							time = Long.parseLong(points[i+2]);
							
							Point p = new Point(x, y, time);
							p.gridId = CSI;
							t.addPoint(p);
						}
						page.add(t);
					}
					return new Tuple2<PageIndex, GridPage>(index, page);
				}
			});
	}

	/**
	 * Save this RDD to a given output folder 
	 * (HDFS, Tachyon, Local, etc).
	 * </br>
	 * Save in "path/index-structure/pages-rdd" folder.
	 */
	public void save(final String path){
		gridPagesRDD.saveAsTextFile(path + "/index-structure/grid-pages-rdd");
	}
	

	/**
	 * Persist this object. Set in the specified Storage Level:
	 * MEMORY_AND_DISK, MEMORY_ONLY, DISK_ONLY, etc.
	 * </br></br>
	 * Note: this RDD might be too big to be allocated in memory only.
	 */
	public void persist(StorageLevel level){
		gridPagesRDD.persist(level);
	}
	
	/**
	 * Remove this RDD from the storage level.
	 * Clean the cache.
	 */
	public void unpersist(){
		gridPagesRDD.unpersist();
	}
	
	/**
	 * The number of Grid Pages in this RDD.
	 */
	public long count(){
		return gridPagesRDD.count();
	}
	
	/**
	 * Collect all pages in this RDD.
	 * Call of Spark collect.
	 */
	public List<GridPage> collectPages(){
		return gridPagesRDD.values().collect();
	}

	/**
	 * Filter all pages from this RDD that match the given 
	 * index list. Call Spark Filter on the RDD.
	 * 
	 * @return Returns a RDD with the filtered pages.
	 */
	public JavaPairRDD<PageIndex, GridPage> filterPagesByIndex(
			final Collection<PageIndex> indexList){
		// filter partitions (pages) by the given indexes
		JavaPairRDD<PageIndex, GridPage> pagesRDD = gridPagesRDD.filter(
				new Function<Tuple2<PageIndex,GridPage>, Boolean>() {
			public Boolean call(Tuple2<PageIndex, GridPage> page) throws Exception {
				return indexList.contains(page._1);
			}
		});

		// return partition with the results
		return pagesRDD.coalesce(NUM_COALESCE);
	}
	
	/**
	 * Filter all pages from this RDD that match the given 
	 * index list. Skip pages in skipList. 
	 * Call Spark Filter on the RDD.
	 * 
	 * @return Returns a RDD with the filtered pages.
	 */
	public JavaPairRDD<PageIndex, GridPage> filterPagesByIndex(
			final Collection<PageIndex> indexList, 
			final Collection<PageIndex> skipList){
		// filter partitions (pages) by the given indexes
		JavaPairRDD<PageIndex, GridPage> pagesRDD = gridPagesRDD.filter(
				new Function<Tuple2<PageIndex,GridPage>, Boolean>() {
			public Boolean call(Tuple2<PageIndex, GridPage> page) throws Exception {
				return (indexList.contains(page._1) && 
						!skipList.contains(page._1));
			}
		});

		// return partition with the results
		return pagesRDD.coalesce(NUM_COALESCE);
	}	
	
	/**
	 * Filter all pages from this RDD that match the given
	 * Spatial index list (CSIlist), and Time index 
	 * between [TPIini, TPIend] inclusive. 
	 * Call Spark Filter on the RDD.
	 * 
	 * @return Returns a RDD with the filtered pages.
	 */
	public JavaPairRDD<PageIndex, GridPage> filterPagesByIndex(
			final Collection<Integer> CSIlist, 
			final int TPIini, final int TPIend){

		// filter partitions (pages) by the given indexes
		JavaPairRDD<PageIndex, GridPage> pagesRDD = gridPagesRDD.filter(
				new Function<Tuple2<PageIndex,GridPage>, Boolean>() {
			public Boolean call(Tuple2<PageIndex, GridPage> page) throws Exception {
				return (CSIlist.contains(page._1.CSI) && 
						page._1.TPI >= TPIini && 
						page._1.TPI <= TPIend);
			}
		});

		// return partition with the results
		return pagesRDD.coalesce(NUM_COALESCE);
	}
	
	/**
	 * Filter all pages from this  RDD that match the given  
	 * Spatial index (CSI) list. Call Spark Filter on the RDD.
	 * 
	 * @return Returns a RDD with the filtered pages.
	 */
	public JavaPairRDD<PageIndex, GridPage> filterPagesBySpatialIndex(
			final Collection<Integer> VSIlist){
		// filter partitions (pages) by the given indexes
		JavaPairRDD<PageIndex, GridPage> pagesRDD = gridPagesRDD.filter(
				new Function<Tuple2<PageIndex,GridPage>, Boolean>() {
			public Boolean call(Tuple2<PageIndex, GridPage> page) throws Exception {
				return VSIlist.contains(page._1.CSI);
			}
		});

		// return partitions with the result
		return pagesRDD.coalesce(NUM_COALESCE);
	}

	/**
	 * Filter all pages from this RDD that match the given  
	 * Time index list (TPIlist). Call Spark Filter on the RDD.
	 * 
	 * @return Returns a RDD with the filtered pages.
	 */
	public JavaPairRDD<PageIndex, GridPage> filterPagesByTimeIndex(
			final Collection<Integer> TPIlist){
		// filter partitions (pages) by the given indexes
		JavaPairRDD<PageIndex, GridPage> pagesRDD = gridPagesRDD.filter(
				new Function<Tuple2<PageIndex,GridPage>, Boolean>() {
			public Boolean call(Tuple2<PageIndex, GridPage> page) throws Exception {
				return TPIlist.contains(page._1.TPI);
			}
		});

		// return partitions with the result
		return pagesRDD.coalesce(NUM_COALESCE);
	}
	
	/**
	 * Filter all pages from this RDD with Time index between
	 * TPIini and TPIend, that is, retrieve all pages
	 * such that page TPI = [TPIini, TPIend]. 
	 * Call Spark Filter on the RDD.
	 * 
	 * @return Returns a RDD with the filtered pages.
	 */
	public JavaPairRDD<PageIndex, GridPage> filterPagesByTimeIndex(
			final int TPIini, final int TPIend){
		// filter partitions (pages) by the given indexes
		JavaPairRDD<PageIndex, GridPage> pagesRDD = gridPagesRDD.filter(
				new Function<Tuple2<PageIndex,GridPage>, Boolean>() {
			public Boolean call(Tuple2<PageIndex, GridPage> page) throws Exception {
				return (page._1.TPI >= TPIini && page._1.TPI <= TPIend);
			}
		});
		
		// return partitions with the result
		return pagesRDD.coalesce(NUM_COALESCE);
	}
	
	/**
	 * Count the number of trajectory/sub-trajectories by page index.
	 * 
	 * @return Return a pair RDD from page index to number of elements.
	 */
	public JavaPairRDD<PageIndex, Integer> countByPageIndex(){
		JavaPairRDD<PageIndex, Integer> countByKeyRDD = gridPagesRDD
			.mapToPair(new PairFunction<Tuple2<PageIndex,GridPage>, PageIndex, Integer>() {
				public Tuple2<PageIndex, Integer> call(Tuple2<PageIndex, GridPage> page) throws Exception {
					return new Tuple2<PageIndex, Integer>(page._1, page._2.size());
				}
			}).reduceByKey(new Function2<Integer, Integer, Integer>() {
				public Integer call(Integer v1, Integer v2) throws Exception {
					return (v1 + v2);
				}
			});

		return countByKeyRDD;
	}
	
	/**
	 * Return a vector with some statistical information about
	 * the number of sub-trajectories per page in this RDD.
	 * 
	 * @return A double vector containing the mean: [0], min: [1],
	 * max: [2], and std: [3] number of sub-trajectories per page
	 * in this RDD.
	 */
	public double[] subTrajectoriesPerPageInfo(){
		// total number of pages
		final double total = count();
		
		// get mean, min, max and std number of sub-trajectories per page
		double[] count = 
			gridPagesRDD.values().glom().map(new Function<List<GridPage>, double[]>() {
				public double[] call(List<GridPage> list) throws Exception {
					double[] vec = new double[]{0.0,INF,0.0,0.0};
					for(GridPage page : list){
						long count = page.size();
						vec[0] += count;
						vec[1] = Math.min(vec[1], count); 
						vec[2] = Math.max(vec[2], count); 
						vec[3] += (count*count);
					}
					return vec;
				}
			}).reduce(new Function2<double[], double[], double[]>() {
				public double[] call(double[] vec1, double[] vec2) throws Exception {
					vec1[0] = vec1[0] + vec2[0];
					vec1[1] = Math.min(vec1[1], vec2[1]);
					vec1[2] = Math.max(vec1[2], vec2[2]);
					vec1[3] = vec1[3] + vec2[3];
					return vec1;
				}
			});
		// get std
		count[3] = (count[3] - 
				(1/total)*(count[0]*count[0]));
		count[3] = Math.sqrt(count[3]/total);
		// get mean
		count[0] = count[0]/total;
		
		return count;
	}

	/**
	 * Return a vector with some statistical information about
	 * the number of trajectory points per page in this RDD.
	 * 
	 * @return A double vector containing the mean: [0], min: [1],
	 * max: [2], and std: [3] number of trajectory points per page
	 * in this RDD.
	 */
	public double[] pointsPerPageInfo(){
		// total number of pages
		final double total = count();
		
		// get mean, min, max and std number of sub-trajectories per page
		double[] count = 
			gridPagesRDD.values().glom().map(new Function<List<GridPage>, double[]>() {
				public double[] call(List<GridPage> list) throws Exception {
					double[] vec = new double[]{0.0,INF,0.0,0.0};
					for(GridPage page : list){
						long count = page.getPointsList().size();
						vec[0] += count;
						vec[1] = Math.min(vec[1], count); 
						vec[2] = Math.max(vec[2], count); 
						vec[3] += (count*count);
					}
					return vec;
				}
			}).reduce(new Function2<double[], double[], double[]>() {
				public double[] call(double[] vec1, double[] vec2) throws Exception {
					vec1[0] = vec1[0] + vec2[0];
					vec1[1] = Math.min(vec1[1], vec2[1]);
					vec1[2] = Math.max(vec1[2], vec2[2]);
					vec1[3] = vec1[3] + vec2[3];
					return vec1;
				}
			});
		// get std
		count[3] = (count[3] - 
				(1/total)*(count[0]*count[0]));
		count[3] = Math.sqrt(count[3]/total);
		// get mean
		count[0] = count[0]/total;
		
		return count;
	}
	
	/**
	 * The total number of sub-trajectories in this 
	 * RDD dataset (after the map phase).
	 */
	public long getNumSubTrajectories(){
		final long total =
			// map each page to a number of trajectories (map in blocks)
			gridPagesRDD.values().glom().map(new Function<List<GridPage>, Long>() {
				public Long call(List<GridPage> pageList) throws Exception {
					long count = 0;
					for(GridPage page : pageList){
						count += page.size();
					}
					return count;
				}
			}).reduce(new Function2<Long, Long, Long>() {
				public Long call(Long v1, Long v2) throws Exception {
					return (v1 + v2);
				}
			});
		return total;
	}
	
	/**
	 * The total number of trajectory points
	 * in this RDD dataset.
	 */
	public long getNumPoints(){
		final long total =
			// map each page to a number of points (map in blocks)
			gridPagesRDD.values().glom().map(new Function<List<GridPage>, Long>() {
				public Long call(List<GridPage> pageList) throws Exception {
					long count = 0;
					for(GridPage page : pageList){
						count += page.getPointsList().size();
					}
					return count;
				}
			}).reduce(new Function2<Long, Long, Long>() {
				public Long call(Long v1, Long v2) throws Exception {
					return (v1 + v2);
				}
			});
		return total;
	}
	
	/**
	 * The number of partitions in this RDD.
	 */
	public long getNumPartitions(){
		return gridPagesRDD.partitions().size();
	}
	
	/**
	 * Estimative of the size of this RDD.
	 */
	public long estimateSize(){
		return SizeEstimator.estimate(gridPagesRDD);
	}
	
	/**
	 * Print Grid pages information: System out.
	 */
	public void print(){
		// Print results
		System.out.println();
		System.out.println("Number of Voronoi Pages: " + count());
		System.out.println();
		
		gridPagesRDD.foreach(new VoidFunction<Tuple2<PageIndex,GridPage>>() {
			public void call(Tuple2<PageIndex, GridPage> page) throws Exception {
				System.out.println("Page: <" + page._1.CSI + ", " + page._1.TPI + ">");
				System.out.println(page._2.getTrajectoryIdSet().size() + " parent trajectories.");
				System.out.println(page._2.getTrajectoryList().size() + " sub-trajectories.");
				System.out.println(page._2.getPointsList().size() + " points.\n");
			}
		});
	}

	/**
	 * Save pages info. 
	 * Save to HDFS output folder as "grid-pages-rdd-info"
	 */
	public void savePagesInfo(){
		double[] trInfo = subTrajectoriesPerPageInfo();
		double[] ptInfo = pointsPerPageInfo();
		
		List<String> info = new ArrayList<String>();
		info.add("Number of Voronoi Pages: " + count());
		info.add("Number of RDD Partitions: " + getNumPartitions());
		info.add("Total Number of Sub-Trajectories: " + getNumSubTrajectories());
		info.add("Avg. Sub-Trajectories per Page: " + trInfo[0]);
		info.add("Min. Sub-Trajectories per Page: " + trInfo[1]);
		info.add("Max. Sub-Trajectories per Page: " + trInfo[2]);
		info.add("Std. Sub-Trajectories per Page: " + trInfo[3]);
		info.add("Total Number of Points: " + getNumPoints());
		info.add("Avg. Points per Page: " + ptInfo[0]);
		info.add("Min. Points per Page: " + ptInfo[1]);
		info.add("Max. Points per Page: " + ptInfo[2]);
		info.add("Std. Points per Page: " + ptInfo[3]);

		/*info.addAll( 
			voronoiPagesRDD.values().map(new Function<Page, String>() {
				public String call(Page page) throws Exception {
					String script = "";
					script += "("+page.toString() + ")\n";
					script += page.getTrajectoryIdSet().size() + " parent trajectories.\n";
					script += page.getTrajectoryList().size() + " sub-trajectories.\n";
					script += page.getPointsList().size() + " points.";		
					return script;
				}
			}).collect());*/

		// save to hdfs
		HDFSFileService hdfs = new HDFSFileService();
		hdfs.saveStringListHDFS(info, "grid-pages-rdd-info");
	}
	
	/**
	 * Save pages history.
	 * This is useful to print into a histogram.
	 * Save the pages info into a file, one per line, as:
	 * </br></br>
	 * "PageIndex" "Number of parent trajectories" "Number of sub-trajectories" "Number of points"  
	 * </br></br>
	 * Save to HDFS output folder as "pages_history"
	 */
	public void savePagesHistory(){
		List<String> historyList = new ArrayList<String>();
		// add header
		historyList.add("page-index #-parent-trajectories #-sub-trajectories #-points");
		// add histogram
		historyList.addAll(
			gridPagesRDD.map(new Function<Tuple2<PageIndex,GridPage>, String>() {
				public String call(Tuple2<PageIndex, GridPage> page)
						throws Exception {
					String script = "";
					script += "("+ page._1.toString() + ") ";
					script += page._2.getTrajectoryIdSet().size() + " ";
					script += page._2.getTrajectoryList().size() + " ";
					script += page._2.getPointsList().size();	
					return script;
				}
			}).collect());
		
		// save to hdfs
		HDFSFileService hdfs = new HDFSFileService();
		hdfs.saveStringListHDFS(historyList, "grid-pages-rdd-history");
	}
	
	/**
	 * Aggregate by key function to group sub-trajectories by Page Index. 
	 * </br>
	 * Receive key-value pairs of: (PageIndex = (CSI,TPI), Sub-Trajectory)
	 * and group sub-trajectories with same key (page index).  
	 *
	 * @return Return a RDD of key-value pair: 
	 * (PageIndex = (CSI,TPI), Page = [sub1, sub2, ..., subN]).
	 */
	private JavaPairRDD<PageIndex, GridPage> aggregatePagesByKey(
			final JavaPairRDD<PageIndex, Trajectory> trajectoryToPageIndexRDD){
		// an empty page to start aggregating
		GridPage emptyPage = new GridPage();
		// aggregate functions
		Function2<GridPage, Trajectory, GridPage> seqFunc = 
				new Function2<GridPage, Trajectory, GridPage>() {
			public GridPage call(GridPage page, Trajectory sub) throws Exception {
				page.add(sub);
				return page;
			}
		};
		Function2<GridPage, GridPage, GridPage> combFunc = 
				new Function2<GridPage, GridPage, GridPage>() {
			public GridPage call(GridPage page1, GridPage page2) throws Exception {
				return page1.merge(page2);
			}
		};
		// call of aggregate by key
		gridPagesRDD = trajectoryToPageIndexRDD
				.aggregateByKey(emptyPage, NUM_PARTITIONS_PAGES, seqFunc, combFunc);

		return gridPagesRDD;
	}
}
