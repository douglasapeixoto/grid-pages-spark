package uq.spark.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.broadcast.Broadcast;

import scala.Tuple2;
import uq.fs.DataConverter;
import uq.spark.EnvironmentVariables;
import uq.spatial.GeoInterface;
import uq.spatial.Point;
import uq.spatial.Trajectory;

/**
 * Service to partition and index trajectory sample points.
 * Build the Grid diagram, the Grid pages and
 * the trajectory track table.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class DataPartitioningModule implements Serializable, EnvironmentVariables, GeoInterface {
	// The Grid diagram partition itself.
	private GridPagesRDD gridPagesRDD = null;

	// Hash table to keep track of trajectories within partitions
	private TrajectoryTrackTableRDD trajectoryTrackTable = null;
	
	// the Grid diagram with a list of cells. Read-only variable to be
	// cached on each machine.
	private Broadcast<Grid> gridDiagram = null;
	
	/**
	 * Return the Grid RDD with the pages
	 * created in this service.
	 */
	public GridPagesRDD getGridPagesRDD(){
		return gridPagesRDD;
	}
	
	/**
	 * Return the Grid diagram built in this service.
	 * Broadcasted diagram.
	 * <br>
	 * Abstract representation only, with the list of cells.
	 */
	public Broadcast<Grid> getGridDiagram(){
		return gridDiagram;
	}
	
	/**
	 * Return a Trajectory Track Table to keep track of 
	 * trajectories across partitions.
	 */
	public TrajectoryTrackTableRDD getTrajectoryTrackTable(){
		return trajectoryTrackTable;
	}
	
	/**
	 * Run data partitioning and indexing.
	 * Build the Grid diagram, assign trajectory points to
	 * Grid pages, and build the trajectory track table.
	 */
	public void run(){
		/**
    	 * READ DATA AND BUILD THE GRID
    	 */
    	// read data and convert to trajectories
     	JavaRDD<String> fileRDD = SC.textFile(DATA_PATH, NUM_PARTITIONS_DATA);
     	fileRDD.persist(STORAGE_LEVEL_PARTITIONIG);   
		
		// convert the input data to a RDD of trajectory objects
		DataConverter converter = new DataConverter(); 
		JavaRDD<Trajectory> trajectoryRDD = 
				converter.mapRawDataToTrajectoryRDD(fileRDD);
     	
		// create a grid and broadcast
		Grid grid = new Grid(
				SIZE_X, SIZE_Y, MIN_X, MIN_Y, MAX_X, MAX_Y);
		gridDiagram = SC.broadcast(grid);

		/**
		 * BUILD GRID INDEX (MAP)
		 */
     	// Second map to assign each sub-trajectory to a Grid page index
     	JavaPairRDD<PageIndex, Trajectory> trajectoryToPageIndexRDD = 
     			mapTrajectoriesToPageIndex(trajectoryRDD, getGridDiagram());

     	/**
     	 * BUILD GRID PAGES RDD (REDUCE)
     	 */
     	// group pages by Index (buid the pages)
      	gridPagesRDD = new GridPagesRDD();
		gridPagesRDD.build(trajectoryToPageIndexRDD);
		
		/**
		 * BUILD TRAJECTORY TRACK TABLE (MAP/REDUCE)
		 */
     	trajectoryTrackTable = new TrajectoryTrackTableRDD();
     	trajectoryTrackTable.build(trajectoryToPageIndexRDD);
	}
	
	/**
	 * Construct the Grid page index:
	 * </br>
	 * Map a input RDD of trajectories to Grid Pages.
	 * </br>
     * Split the trajectory and map each sub-trajectory 
     * to its grid cell and time window (page).
     * </br>
     * Note: Boundary trajectory segments are assigned to both 
     * pages it crosses with.
     * 
     * @return Return a RDD of pairs: 
     * (PageIndex = (CSI,TPI), Sub-Trajectory)
	 */
	private JavaPairRDD<PageIndex, Trajectory> mapTrajectoriesToPageIndex(
				final JavaRDD<Trajectory> trajectoryRDD, 
				final Broadcast<Grid> voronoiDiagram){

		// Split and map trajectories to cells page indexes
		JavaPairRDD<PageIndex, Trajectory> trajectoriesToPagesRDD = trajectoryRDD
	     	.mapPartitionsToPair(new PairFlatMapFunction<Iterator<Trajectory>, PageIndex, Trajectory>() {
	     		
	     		// get the grid	
				final Grid grid = gridDiagram.value();
				
				public Iterable<Tuple2<PageIndex, Trajectory>> call(
						Iterator<Trajectory> trajectoryItr) throws Exception {
					
					// the result pairs (PageIndex, Sub-trajectory)
					List<Tuple2<PageIndex, Trajectory>> resultPairs = 
							  new ArrayList<Tuple2<PageIndex,Trajectory>>();
					
					// read each trajectory in this partition
					//int VSI, TPI, prevVSI, prevTPI;
					while(trajectoryItr.hasNext()){
						// current trajectory
						Trajectory trajectory = trajectoryItr.next();

						// info of the previous point
						Point prev = null;
						int prevCSI = 0;
						int prevTPI = 0;
						
						// an empty sub-trajectory
						String id = trajectory.id;
						Trajectory sub = new Trajectory(id);
						
						// split the trajectory into sub-trajectories
						// for each page it intersects with
						for(Point point : trajectory.getPointsList()){
							// find the cell page in the grid containing this point
							// Cell Spatial Index
							int CSI = grid.getOverlappingCell(point);  
									  point.gridId = CSI;
							// Time Page Index
							int TPI = (int)(point.time / TIME_WINDOW_SIZE) + 1; 
							
							// check for boundary objects
							if(prev == null){
								sub.addPoint(point);
							} else if(CSI == prevCSI && TPI == prevTPI){
								sub.addPoint(point);
							} 
							// space/time boundary segment
							else { 
								// the current sub-trajectory also receives this boundary point
								sub.addPoint(point);
								 
								// create the page for the previous sub-trajectory
								PageIndex index = new PageIndex(prevCSI, prevTPI);
								GridPage page = new GridPage();
								page.add(sub);
								
								// add pair <(CSI,TPI), Sub-Trajectory>
								resultPairs.add(new Tuple2<PageIndex, Trajectory>(index, sub));
								
								// new sub-trajectory for this boundary segment
								sub = new Trajectory(id);
								sub.addPoint(prev);
								sub.addPoint(point);
							}
							prev = point;
							prevCSI = CSI;
							prevTPI = TPI;
						}
						// add the page for the last sub-trajectory read
						PageIndex index = new PageIndex(prevCSI, prevTPI);
						// add pair <PageIndex, Page>
						resultPairs.add(new Tuple2<PageIndex, Trajectory>(index, sub));
					}
					
					// the iterable map list
					return resultPairs;
				}
			});

		return trajectoriesToPagesRDD;
	}
}
