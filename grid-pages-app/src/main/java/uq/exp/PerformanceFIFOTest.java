package uq.exp;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.broadcast.Broadcast;

import uq.fs.HDFSFileService;
import uq.spark.MyLogger;
import uq.spark.EnvironmentVariables;
import uq.spark.index.Grid;
import uq.spark.index.IndexParameters;
import uq.spark.index.DataPartitioningModule;
import uq.spark.index.TrajectoryTrackTableRDD;
import uq.spark.index.GridPagesRDD;
import uq.spark.query.NearNeighbor;
import uq.spark.query.QueryProcessingModule;
import uq.spatial.GeoInterface;
import uq.spatial.Point;
import uq.spatial.STBox;
import uq.spatial.Trajectory;

/**
 * Experiment to evaluate performance of queries.
 * Generate log result with query performance information.
 * </br>
 * Process queries in FIFO way (using Spark's FIFO scheduling).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class PerformanceFIFOTest implements 
	Serializable, EnvironmentVariables, IndexParameters, GeoInterface {
	private static final HDFSFileService HDFS = 
			new HDFSFileService();
	// experiments log
	private static final MyLogger LOG = new MyLogger();
	// experiment log file name
	private static final String LOG_NAME = "grid-fifo-"
			 + SIZE_X + "x" + SIZE_Y + "-" + TIME_WINDOW_SIZE + "s";
	/**
	 * Main
	 */
	public static void main(String[] args){
		System.out.println("\n[EXPERIMENTS MODULE] Running Performance FIFO Test..");
		System.out.println("\n[EXPERIMENTS MODULE] Module Starts At "
				+ System.currentTimeMillis() + " ms.\n");
		
		/************************
		 * DATA INDEXING 
		 ************************/
		DataPartitioningModule partitioningService = 
				new DataPartitioningModule();

		// run data partitioning and indexing
		partitioningService.run();
		
		// get the grid diagram abstraction.
		Broadcast<Grid> gridDiagram = 
				partitioningService.getGridDiagram();
		// get the RDD of grid cells pages 
		GridPagesRDD gridPagesRDD = 
				partitioningService.getGridPagesRDD();
		// get trajectory track table
		TrajectoryTrackTableRDD trajectoryTrackTable = 
				partitioningService.getTrajectoryTrackTable();
	
		// action to force building the index
		System.out.println("Num. Pages: " + gridPagesRDD.count());
		System.out.println("Num. TTT Tuples: " + trajectoryTrackTable.count());
		
		/************************
		 * QUERIES PROCESING 
		 ************************/
		System.out.println("\n[EXPERIMENTS MODULE] Query Processing Starts At "
				+ System.currentTimeMillis() + " ms.\n");
		QueryProcessingModule queryService = new QueryProcessingModule(
				gridPagesRDD, trajectoryTrackTable, gridDiagram); 
		
		/******
		 * SPATIAL TEMPORAL SELECTION QUERIES (WHOLE)
		 ******/
		final List<STBox> stUseCases = readSpatialTemporalTestCases();
		{
			LOG.appendln("Spatial-Temporal Selection Query Result (Whole):\n");
			long selecQueryTime=0;
			int queryId=1;
			for(STBox stObj : stUseCases){
				System.out.println("Query " + queryId);
				long start = System.currentTimeMillis();
				LOG.appendln("Query " + queryId + " starts at: " + start + "ms."); 
				// run query - whole trajectories
				List<Trajectory> result = queryService 
						.getSpatialTemporalSelection(stObj, stObj.timeIni, stObj.timeEnd, true);
				long time = System.currentTimeMillis()-start;
				LOG.appendln("Query " + queryId++ + ": " + result.size() + " trajectories in " + time + " ms.");
				selecQueryTime += time;		
			}
			LOG.appendln("Spatial-Temporal Selection ends at: " + System.currentTimeMillis() + " ms.");
			LOG.appendln("Total Spatial-Temporal Selection Query Time: " + selecQueryTime + " ms.\n");
		}
		
		/******
		 * SPATIAL TEMPORAL SELECTION QUERIES (EXACT)
		 ******/
		{
			LOG.appendln("Spatial-Temporal Selection Query Result (Exact):\n");
			long selecQueryTime=0;
			int queryId=1;
			for(STBox stObj : stUseCases){
				System.out.println("Query " + queryId);
				long start = System.currentTimeMillis();
				LOG.appendln("Query " + queryId + " starts at: " + start + " ms."); 
				// run query - exact sub-trajectories
				List<Trajectory> result = queryService
						.getSpatialTemporalSelection(stObj, stObj.timeIni, stObj.timeEnd, false);
				long time = System.currentTimeMillis()-start;
				LOG.appendln("Query " + queryId++ + ": " + result.size() + " sub-trajectories in " + time + " ms.");
				selecQueryTime += time;		
			}
			LOG.appendln("Spatial-Temporal Selection (Exact) ends at: " + System.currentTimeMillis() + " ms.");
			LOG.appendln("Total Spatial-Temporal Selection Query Time: " + selecQueryTime + " ms.\n");
		}
		
		/******
		 * NN QUERIES
		 ******/
		final List<Trajectory> nnUseCases = readNearestNeighborTestCases();
		{
			LOG.appendln("NN Query Result:\n");
			long nnQueryTime=0;
			int queryId=1;
			for(Trajectory t : nnUseCases){
				System.out.println("Query " + queryId);
				long start = System.currentTimeMillis();
				LOG.appendln("Query " + queryId + " starts at: " + start + " ms."); 
				// run query
				long tIni = t.timeIni();
				long tEnd = t.timeEnd();
				Trajectory result = queryService
						.getNearestNeighbor(t, tIni, tEnd);
				long time = System.currentTimeMillis() - start;
				LOG.appendln("NN Query " + queryId++ + ": " +  result.id + " in " + time + " ms.");
				nnQueryTime += time;
			}
			LOG.appendln("NN query ends at: " + System.currentTimeMillis() + " ms.");
			LOG.appendln("Total NN Time: " + nnQueryTime + " ms.\n");
			
			// save query stats info to HDFS
			MyLogger statsLog = new MyLogger();
			statsLog.append(QUERY_STATS_ACCUM.value());
			statsLog.save("grid-pruning-ratio-nn");
		}
		
		/******
		 * K-NN QUERIES
		 ******/
		{
			LOG.appendln("K-NN Query Result:\n");
			long nnQueryTime=0;
			int queryId=1;
			final int NUM_K = 10;
			for(Trajectory t : nnUseCases){
				System.out.println("Query " + queryId);
				long start = System.currentTimeMillis();
				LOG.appendln("Query " + queryId + " starts at: " + start + " ms."); 				
				// run query
				long tIni = t.timeIni();
				long tEnd = t.timeEnd();
				List<NearNeighbor> resultList = queryService
						.getKNearestNeighbors(t, tIni, tEnd, NUM_K);
				long time = System.currentTimeMillis() - start;
				LOG.appendln(NUM_K + "-NN Query " + queryId++ + ": " +  resultList.size() + " in " + time + " ms.");
				int i=1;
				for(NearNeighbor nn : resultList){
					LOG.appendln(i++ + "-NN: " + nn.id);
				}
				nnQueryTime += time;
			}
			LOG.appendln(NUM_K + "-NN query ends at: " + System.currentTimeMillis() + " ms.");
			LOG.appendln("Total " + NUM_K + "-NN Time: " + nnQueryTime + " ms.\n");
			
			// save query stats info to HDFS
			MyLogger statsLog = new MyLogger();
			statsLog.append(QUERY_STATS_ACCUM.value());
			statsLog.save("grid-pruning-ratio-" + NUM_K + "nn");
		}
	
		// save the result log to HDFS
		LOG.save(LOG_NAME);
		
		// unpersist
		gridPagesRDD.unpersist();
		trajectoryTrackTable.unpersist();
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
