package uq.exp;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.spark.broadcast.Broadcast;

import uq.fs.HDFSFileService;
import uq.spark.EnvironmentVariables;
import uq.spark.MyLogger;
import uq.spark.index.DataPartitioningModule;
import uq.spark.index.Grid;
import uq.spark.index.TrajectoryTrackTableRDD;
import uq.spark.index.GridPagesRDD;
import uq.spark.query.NearNeighbor;
import uq.spark.query.QueryProcessingModule;
import uq.spatial.Point;
import uq.spatial.STBox;
import uq.spatial.Trajectory;

/**
 * Experiment to evaluate throughput of the algorithm (query/minute).
 * Generate log result with query performance information.
 * </br>
 * Process multiple concurrent Threads (multithreading - 
 * multiple queries  using Spark's FAIR scheduling).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class PerformanceConcurrentTest implements Serializable, EnvironmentVariables {
	private static final HDFSFileService HDFS = new HDFSFileService();
	// maximum number of concurrent Threads
	private static final int NUM_THREADS = 5;
	// the time threshold to terminate the concurrent queries
	private static final int TERMINATE_TIME = 300; //  minutes
	// experiments log
	private static final MyLogger LOG = new MyLogger();
	// query finish count
	private static int QUERY_COUNT = 0;
	// experiment log file name
	private static final String LOG_NAME = "grid-concurrent-"
			+ SIZE_X + "x" + SIZE_Y + "-" + TIME_WINDOW_SIZE + "s";
	
	/**
	 * Main: Performance testing.
	 */
	public static void main(String[] args){
		System.out.println("\n[EXPERIMENTS MODULE] Running Concurrent Performance Test..");
		System.out.println("\n[EXPERIMENTS MODULE] Module Starts At "
				+ System.currentTimeMillis() + " ms.\n");
		
		/************************
		 * DATA PARTITIONING
		 ************************/
		DataPartitioningModule partitioningService = 
				new DataPartitioningModule();

		// run data partitioning and indexing
		partitioningService.run();
		
		// get the grid diagram abstraction.
		final Broadcast<Grid> gridDiagram = 
				partitioningService.getGridDiagram();
		// get the RDD of grid cells pages 
		final GridPagesRDD gridPagesRDD = 
				partitioningService.getGridPagesRDD();
		// get trajectory track table
		final TrajectoryTrackTableRDD trajectoryTrackTable = 
				partitioningService.getTrajectoryTrackTable();
		
		System.out.println("Num. Pages: " + gridPagesRDD.count());
		System.out.println("Num. TTT Tuples: " + trajectoryTrackTable.count());
		
		/************************
		 * QUERIES INITIALIZATION 
		 ************************/
		QueryProcessingModule queryService = new QueryProcessingModule(
				gridPagesRDD, trajectoryTrackTable, gridDiagram);
		
		// ST-SELECTION QUERY
	    // run the first query - non-concurrent - force to build the RDD
/*	    final List<STBox> stTestCases = readSpatialTemporalTestCases(); // test cases
		List<Trajectory> first_result = queryService.getSpatialTemporalSelection( 
				stTestCases.get(0), stTestCases.get(0).timeIni, stTestCases.get(0).timeEnd, true);
		System.out.println("First Query Result Size: " + first_result.size());
*/		
		// K-NN QUERY
	    final List<Trajectory> nnUseCases = readNearestNeighborTestCases();
	    Trajectory first_nn_result = queryService.getNearestNeighbor(
	    		nnUseCases.get(0), nnUseCases.get(0).timeIni(), nnUseCases.get(0).timeEnd());
		System.out.println("First Query Result Size: " + first_nn_result.id);
		
		/************************
		 * MULTI-THREAD QUERIES SETUP 
		 ************************/
		// concurrent threads start
	    long startTime = System.currentTimeMillis();
	    System.out.println("[EXPERIMENTS MODULE] Concurrent Threads starts at: " + startTime + " ms.");
	    // At any point, at most NUM_THREADS will be active processing tasks.
	    ExecutorService executorService = 
	    		Executors.newFixedThreadPool(NUM_THREADS);
	    
		/******
		 * SPATIAL TEMPORAL SELECTION QUERIES (WHOLE)
		 ******/
/*	    LOG.appendln("Spatial-Temporal Selection Concurrent Query Result (Whole):\n");
	    LOG.appendln("Queries Start at: " + startTime + " ms.\n");
	    // create one thread per query
	    for(int i=1; i<stTestCases.size(); i++){
	    	final STBox stQueryObj = stTestCases.get(i);
	    	// thread starts
	    	executorService.submit(new Runnable() {
				public void run() {
					long qStart = System.currentTimeMillis();
					// run query - exact selection trajectories
					QueryProcessingModule queryService = new QueryProcessingModule(
							gridPagesRDD, trajectoryTrackTable, gridDiagram);
					List<Trajectory> result = queryService.getSpatialTemporalSelection( 
							stQueryObj, stQueryObj.timeIni, stQueryObj.timeEnd, true);
					// query finish
					int resultSize = result.size();
					long qEnd = System.currentTimeMillis();
					// add result to log
					addSelectionResultLog(qStart, qEnd, resultSize);
					System.out.println("[THREAD] Query Time: " + (qEnd-qStart) + " ms.");
				}
			});
	    }
*/	    
		/******
		 * NN QUERIES
		 ******/
	    LOG.appendln("Nearest-Neighbor Concurrent Query Result:\n");
	    LOG.appendln("Queries Start at: " + startTime + " ms.\n");
	    // create one thread per query
	    for(int i=1; i<nnUseCases.size(); i++){
	    	final Trajectory nnQueryObj = nnUseCases.get(i);
	    	// thread starts
	    	executorService.submit(new Runnable() {
				public void run() {
					long qStart = System.currentTimeMillis();
					// run query - NN trajectories
					QueryProcessingModule queryService = new QueryProcessingModule(
							gridPagesRDD, trajectoryTrackTable, gridDiagram);
					// run query
					long tIni = nnQueryObj.timeIni();
					long tEnd = nnQueryObj.timeEnd();
					Trajectory result = queryService
							.getNearestNeighbor(nnQueryObj, tIni, tEnd);
					// query finish
					String resultId = result.id;
					long qEnd = System.currentTimeMillis();
					// add result to log
					addNNResultLog(nnQueryObj.id, qStart, qEnd, resultId);
					System.out.println("[THREAD] Query Time: " + (qEnd-qStart) + " ms.");
				}
			});
	    }
    
		/******
		 * K-NN QUERIES
		 ******/
	    LOG.appendln("k-Nearest-Neighbors Concurrent Query Result:\n");
	    LOG.appendln("Queries Start at: " + startTime + " ms.\n");
	    // create one thread per query
	    final int NUM_K = 10; // number of neighbors
	    for(int i=1; i<nnUseCases.size(); i++){
	    	final Trajectory nnQueryObj = nnUseCases.get(i);
	    	// thread starts
	    	executorService.submit(new Runnable() {
				public void run() {
					long qStart = System.currentTimeMillis();
					// run query - NN trajectories
					QueryProcessingModule queryService = new QueryProcessingModule(
							gridPagesRDD, trajectoryTrackTable, gridDiagram);
					// run query
					long tIni = nnQueryObj.timeIni();
					long tEnd = nnQueryObj.timeEnd();
					List<NearNeighbor> result = queryService
							.getKNearestNeighbors(nnQueryObj, tIni, tEnd, NUM_K);
					// query finish
					int resultSize = result.size();
					long qEnd = System.currentTimeMillis();
					// add result to log
					addNNResultLog(nnQueryObj.id, qStart, qEnd, resultSize+" trajectories ");
					System.out.println("[THREAD] Query Time: " + (qEnd-qStart) + " ms.");
				}
			});
	   }

	   // await TERMINATE_TIME to the threads execution to finish
	   try {
		   	executorService.shutdown();
			executorService.awaitTermination(TERMINATE_TIME, TimeUnit.MINUTES);
			System.out.println("\n[EXPERIMENTS MODULE] Module Ends At "
						+ System.currentTimeMillis() + " ms.\n");	
			// save the result log to HDFS
			LOG.appendln();
			LOG.appendln("Max. Number of Concurrent Queries: " + NUM_THREADS);
			LOG.append  ("Total Queries Finished: " + QUERY_COUNT);
			LOG.save(LOG_NAME);
			// unpersist RDDs
			gridPagesRDD.unpersist();
			trajectoryTrackTable.unpersist();
		} catch (InterruptedException e) {
			System.out.println("[EXPERIMENTS MODULE] Threw an "
					+ "'InterruptedException'!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Append the result of each Selection query thread to the log
	 */
	public static /*synchronized*/ void addSelectionResultLog(long start, long end, int resultSize){
		QUERY_COUNT++;
		LOG.appendln("Query " + QUERY_COUNT + ": " + resultSize + " trajectories in " + (end-start) + " ms.");
		LOG.appendln("Query ends at: " + end + " ms.");
	}
	
	/**
	 * Append the result of each NN query thread to the log
	 */
	public static /*synchronized*/ void addNNResultLog(String queryId, long start, long end, String resultId){
		QUERY_COUNT++;
		LOG.appendln("Query " + queryId + ": returned " + resultId + " in " + (end-start) + " ms.");
		LOG.appendln("Query ends at: " + end + " ms.");
	}
	
	/**
	 * Read the uses cases for spatial-temporal selection queries
	 */
	public static List<STBox> readSpatialTemporalTestCases(){
		List<String> lines = 
				HDFS.readFileHDFS("/spark-data/test-cases/spatial-temporal-test-cases");
		// process lines
		long timeIni, timeEnd;
		double left, right, bottom, top;
		List<STBox> stList = new LinkedList<STBox>(); 
		for(String line : lines){
			if(line.length() > 3){
				String[] tokens = line.split(" ");
				left	= Double.parseDouble(tokens[0]);
				right	= Double.parseDouble(tokens[1]);
				bottom	= Double.parseDouble(tokens[2]);
				top		= Double.parseDouble(tokens[3]);
				timeIni = Long.parseLong(tokens[4]);
				timeEnd = Long.parseLong(tokens[5]); 
				
				stList.add(new STBox(left, right, bottom, top, timeIni, timeEnd));
			}
		}
		return stList;
	}
	
	/**
	 * Read the uses cases for Nearest Neighbors queries.
	 */
	public static List<Trajectory> readNearestNeighborTestCases(){
		List<String> lines = 
				HDFS.readFileHDFS("/spark-data/test-cases/nn-test-cases");
		// process lines
		int id=1;
		double x, y;
		long time;
		List<Trajectory> list = new LinkedList<Trajectory>();
		for(String line : lines){
			if(line.length() > 4){
				String[] tokens = line.split(" ");
				// first tokens is the id
				Trajectory t = new Trajectory("Q" + id++);
				for(int i=1; i<=tokens.length-3; i+=3){
					x = Double.parseDouble(tokens[i]);
					y = Double.parseDouble(tokens[i+1]);
					time = Long.parseLong(tokens[i+2]);
					t.addPoint(new Point(x, y, time));
				}
				list.add(t);
			}
		}
		return list;
	}
}
