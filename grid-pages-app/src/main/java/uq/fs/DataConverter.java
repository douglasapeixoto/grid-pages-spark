package uq.fs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;

import uq.spark.index.IndexParameters;
import uq.spatial.GeoInterface;
import uq.spatial.Point;
import uq.spatial.Trajectory;
import uq.spatial.transformation.ProjectionTransformation;

/**
 * Service to convert input files (raw data) to RDDs 
 * and other objects (e.g. Trajectory, Points, etc.).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class DataConverter implements Serializable, IndexParameters, GeoInterface {
	// Number of decimals to divide the input points coordinates
	static final long DIVISOR = 100000;
	
	/**
	 * Convert an input dataset file of trajectories
	 * to point objects.
	 * 
	 * @return Return a RDD of points.
	 */
	public JavaRDD<Point> mapRawDataToPointRDD(JavaRDD<String> fileRDD){
		// Map to read the file and convert each line to trajectory points objects
     	JavaRDD<Point> pointsRDD = 
     			fileRDD.mapPartitions(new FlatMapFunction<Iterator<String>, Point>() {

			public Iterable<Point> call(Iterator<String> partition) throws Exception {
				// A list of points to return	
				List<Point> pointList = 
						  new ArrayList<Point>();
		
				// read each line of the file of this partition
				// each line is one trajectory
				while(partition.hasNext()){
					// read the line
					String line = partition.next();
			
					if(line.length() > 3){
						// read attributes
						String[] tokens = line.split(" "); 

						// read sample points (token[0] is the id)
						double x, y;
						long time;
						for(int i=1; i<tokens.length; i+=3){
							x = Double.parseDouble(tokens[i]);
							y = Double.parseDouble(tokens[i+1]);
							time = Long.parseLong(tokens[i+2]);
							
							pointList.add(new Point(x, y, time));
						}
					}
				}
				return pointList;
			}
		});
     	
     	return pointsRDD;
	}
	
	/**
	 * Convert a input RDD file to trajectory objects.
	 * 
	 * @return Return a RDD of trajectories.
	 */
	public JavaRDD<Trajectory> mapRawDataToTrajectoryRDD(JavaRDD<String> fileRDD){
		// Map to read the file and convert each line to trajectory objects
     	JavaRDD<Trajectory> trajectoriesRDD = 
     			fileRDD.mapPartitions(new FlatMapFunction<Iterator<String>, Trajectory>() {

			public Iterable<Trajectory> call(Iterator<String> partition) throws Exception {
				// A list of points to return	
				List<Trajectory> trajectoryList = 
						  new ArrayList<Trajectory>();

				// read each line of the file of this partition
				// each line is one trajectory
				while(partition.hasNext()){
					// read the line
					String line = partition.next();
			
					if(line.length() > 3){
						// read attributes
						String[] tokens = line.split(" ");

						// new trajectory for this line
						Trajectory t = new Trajectory(tokens[0]);		
		//Trajectory t2 = new Trajectory(tokens[0]+"_2");		
						// read sample points
						double x, y;
						long time;
						for(int i=1; i<tokens.length; i+=3){
							x = Double.parseDouble(tokens[i]);
							y = Double.parseDouble(tokens[i+1]);
							time = Long.parseLong(tokens[i+2]);
		//t2.addPoint(new Point(x, y, time));			
							t.addPoint(new Point(x, y, time));
						}
						trajectoryList.add(t);
		//trajectoryList.add(t2);
					}
				}
				return trajectoryList;
			}
		});
     	
     	return trajectoriesRDD;
	}
	
	/**
	 * Convert a input dataset file RDD to point objects.
	 * Use Spark MapReduce. Convert the input coordinates 
	 * (longitude and latitude) to Cartesian (x, y) using 
	 * Mercator projection.
	 * 
	 * @return Return a RDD of points.
	 */
	public JavaRDD<Point> mapRawDataToPointMercRDD(JavaRDD<String> fileRDD){
  	
		// Map to read the file and convert each line to trajectory points objects
     	JavaRDD<Point> pointsRDD = 
     			fileRDD.mapPartitions(new FlatMapFunction<Iterator<String>, Point>() {

			public Iterable<Point> call(Iterator<String> partition) throws Exception {
				// A list of points to return	
				List<Point> pointList = 
						  new ArrayList<Point>();
		
				// read each line of the file of this partition
				// each line is one trajectory
				while(partition.hasNext()){
					// read the line
					String line = partition.next();
			
					if(line.length() > 3){
						// read attributes
						String[] tokens = line.split(","); 
						
						// 28th column are the trajectory sample points
						String[] samplePoints = tokens[28].split("\\|");

						// read the first point
						String[] attributes = samplePoints[0].split(":");
						double lon_ini = Double.parseDouble(attributes[0]) / DIVISOR;
						double lat_ini = Double.parseDouble(attributes[1]) / DIVISOR;
						long time_ini  = (long)Double.parseDouble(attributes[3]);
						
						// Create an object for the first point 
						double[] mercProj = ProjectionTransformation.getMercatorProjection(lon_ini, lat_ini);
						Point p_ini = new Point(mercProj[0], mercProj[1], time_ini);
						pointList.add(p_ini);
						
					    // process sample points
					    double lon, lat; long time;
					    
						for(int i=1; i<samplePoints.length; i++){
							// split the point attributes
							attributes = samplePoints[i].split(":");					
							
							// The point sequence is recored as the offset value from first point.
							// 0:longitude 1:latitude 2:speed 3:time 4:direction
							if(attributes.length > 4){
								lon  = lon_ini  + Double.parseDouble(attributes[0]) / DIVISOR;
								lat  = lat_ini  + Double.parseDouble(attributes[1]) / DIVISOR;
								time = time_ini + (long)Double.parseDouble(attributes[3]);
								
								// new point for this input (in cartesian coordinates)
								mercProj = ProjectionTransformation.getMercatorProjection(lon, lat);
								Point p = new Point(mercProj[0], mercProj[1], time);
								pointList.add(p);
							}
						}
					}

				}
				return pointList;
			}
		});
     	
     	return pointsRDD;
	}
		
	/**
	 * Convert a input dataset file to trajectory objects.
	 * Use Spark MapReduce. Convert the input coordinates 
	 * (longitude and latitude) to Cartesian (x, y) using 
	 * Mercator projection.
	 * 
	 * @return Return a RDD of trajectories.
	 */
	public JavaRDD<Trajectory> mapRawDataToTrajectoryMercRDD(JavaRDD<String> fileRDD){
  	
		JavaRDD<Trajectory> trajectoriesRDD = 
			// First map to read the file and convert each line to trajectory object
	     	fileRDD.mapPartitions(new FlatMapFunction<Iterator<String>, Trajectory>() {

			public Iterable<Trajectory> call(Iterator<String> partition) throws Exception {
				// A list of trajectories to return	
				List<Trajectory> trajectoryList = 
						  new ArrayList<Trajectory>();
		
				// read each line of the file of this partition
				// each line is one trajectory
				while(partition.hasNext()){
					// read the line
					String line = partition.next();
			
					if(line.length() > 3){
						// read attributes
						String[] tokens = line.split(","); 

						// first token is the trajectory id
						String trajId = "T_" + tokens[0];
						
						// 29th column are the trajectory sample points
						String[] samplePoints = tokens[28].split("\\|");

						// read the first point
						String[] attributes = samplePoints[0].split(":");
						double lon_ini = Double.parseDouble(attributes[0]) / DIVISOR;
						double lat_ini = Double.parseDouble(attributes[1]) / DIVISOR;
						long time_ini  = (long)Double.parseDouble(attributes[3]);
						
						// Create a trajectory for this line
						// and add the first point
						Trajectory trajectory = new Trajectory(trajId);
						// new point for this input (in cartesian coordinates)
						double[] mercProj = ProjectionTransformation.getMercatorProjection(lon_ini, lat_ini);
						Point p_ini = new Point(mercProj[0], mercProj[1], time_ini);
					    trajectory.addPoint(p_ini);

					    // process sample points
					    double lon, lat; long time;
					    
						for(int i=1; i<samplePoints.length; i++){
							// split the point attributes
							attributes = samplePoints[i].split(":");					
							
							// The point sequence is recored as the offset value from first point.
							// 0:longitude 1:latitude 2:speed 3:time 4:direction
							if(attributes.length > 4){
								lon  = lon_ini  + Double.parseDouble(attributes[0]) / DIVISOR;
								lat  = lat_ini  + Double.parseDouble(attributes[1]) / DIVISOR;
								time = time_ini + (long)Double.parseDouble(attributes[3]); 

								// new point for this input (in cartesian coordinates)
								mercProj = ProjectionTransformation.getMercatorProjection(lon, lat);
								Point p = new Point(mercProj[0], mercProj[1], time);
							    trajectory.addPoint(p);
							}
						}
						
						trajectoryList.add(trajectory);
					}

				}
				return trajectoryList;
			}
		});
		
		return trajectoriesRDD;
	}

	/**
	 * Convert a input dataset file to trajectory objects.
	 * Use Spark MapReduce. Use the original dataset Latitude
	 * and Longitude coordinates.
	 *  
	 * @return Return a RDD of trajectories.
	 */
	public JavaRDD<Trajectory> mapRawDataToTrajectoryLatLongRDD(JavaRDD<String> fileRDD){
  	
		JavaRDD<Trajectory> trajectoriesRDD = 
			// First map to read the file and convert each line to trajectory object
	     	fileRDD.mapPartitions(new FlatMapFunction<Iterator<String>, Trajectory>() {

			public Iterable<Trajectory> call(Iterator<String> partition) throws Exception {
				// A list of trajectories to return	
				List<Trajectory> trajectoryList = 
						  new ArrayList<Trajectory>();
		
				// read each line of the file of this partition
				// each line is one trajectory
				while(partition.hasNext()){
					// read the line
					String line = partition.next();
			
					if(line.length() > 3){
						// read attributes
						String[] tokens = line.split(","); 

						// first token is the trajectory id
						String trajId = "T_" + tokens[0];
						
						// 29th column are the trajectory sample points
						String[] samplePoints = tokens[28].split("\\|");

						// read the first point
						String[] attributes = samplePoints[0].split(":");
						double lon_ini = Double.parseDouble(attributes[0]) / DIVISOR;
						double lat_ini = Double.parseDouble(attributes[1]) / DIVISOR;
						long time_ini  = (long)Double.parseDouble(attributes[3]);
						
						// Create a trajectory for this line
						// and add the first point
						Trajectory trajectory = new Trajectory(trajId);
						// new point for this input
						Point p_ini = new Point(lon_ini, lat_ini, time_ini);
					    trajectory.addPoint(p_ini);

					    // process sample points
					    double lon, lat; long time;
					    
						for(int i=1; i<samplePoints.length; i++){
							// split the point attributes
							attributes = samplePoints[i].split(":");					
							
							// The point sequence is recored as the offset value from first point.
							// 0:longitude 1:latitude 2:speed 3:time 4:direction
							if(attributes.length > 4){
								lon  = lon_ini  + Double.parseDouble(attributes[0]) / DIVISOR;
								lat  = lat_ini  + Double.parseDouble(attributes[1]) / DIVISOR;
								time = time_ini + (long)Double.parseDouble(attributes[3]); 

								// new point for this input
								Point p = new Point(lon, lat, time);
							    trajectory.addPoint(p);
							}
						}
						
						trajectoryList.add(trajectory);
					}

				}
				return trajectoryList;
			}
		});
		
		return trajectoriesRDD;
	}
	/**
	 * Read the dataset and select a given number of 
	 * sample points. Save to HDFS if required, save
	 * as 'sample-points-$num'.

	 * @return A list with num points
	 */
	public List<Point> selectSamplePoints(
			JavaRDD<String> fileRDD, 
			final int num, boolean save){
		System.out.println("\nSelecting " + num + " Sample Points..");
     	System.out.println();
     	
		// map the input dataset to point objects
	 	JavaRDD<Point> pointsRDD = mapRawDataToPointRDD(fileRDD);
	 	// select sample
	 	List<Point> sampleList = pointsRDD.takeSample(false, num);

	 	// save to HDFS
	 	if(save){
	 		HDFSFileService hdfs = new HDFSFileService();
	 		hdfs.savePointListHDFS(sampleList, "sample-points-"+ num);
	 	}

	 	return sampleList;
	}	
	
	/**
	 * Read the dataset and select a given number of 
	 * sample points (in Mercator projection). 
	 * Save to HDFS if required, save as 'sample-points-merc-$num'.

	 * @return A list of num points
	 */
	public List<Point> selectSamplePointsMerc(
			JavaRDD<String> fileRDD, 
			final int num, boolean save){
		System.out.println("\nSelecting " + num + " Sample Points (Mercator)..");
     	System.out.println();
     	
		// map the input dataset to point objects
	 	JavaRDD<Point> pointsRDD = mapRawDataToPointMercRDD(fileRDD);
	 	// select sample
	 	List<Point> sampleList = pointsRDD.takeSample(false, num);

	 	// save to HDFS
	 	if(save){
	 		HDFSFileService hdfs = new HDFSFileService();
	 		hdfs.savePointListHDFS(sampleList, "sample-points-merc-"+ num);
	 	}

	 	return sampleList;
	}
	
	/**
	 * Read the dataset and select a given number of 
	 * sample trajectories. Save to HDFS if required,
	 * save as 'sample-trajectories-$num'.

	 * @return A list with num trajectories.
	 */
	public List<Trajectory> selectSampleTrajectories(
			JavaRDD<String> fileRDD, 
			final int num, boolean save){
	 	System.out.println("\nSelecting " + num + " Sample Trajectories..");
     	System.out.println();

     	// map the input dataset to point objects
 	 	JavaRDD<Trajectory> trajectoryRDD = mapRawDataToTrajectoryRDD(fileRDD);
 	 	// select sample
 		List<Trajectory> sampleList = trajectoryRDD.takeSample(false, num);
 	 	
 	 	// save to HDFS
 		if(save){
 			HDFSFileService hdfs = new HDFSFileService();
	 		hdfs.saveTrajectoryListHDFS(sampleList, "sample-trajectories-"+ num);
	 	}
 		
 		return sampleList;
	}
	
	/**
	 * Read the dataset and select a given number of 
	 * sample trajectories (in Mercator projection). 
	 * Save to HDFS if required, save as 
	 * 'sample-trajectories-merc-$num'.
	 * 
	 * @return A list with num trajectories.
	 */
	public List<Trajectory> selectSampleTrajectoriesMerc(
			JavaRDD<String> fileRDD, 
			final int num, boolean save){
	 	System.out.println("\nSelecting " + num + " Sample Trajectories (Mercator)..");
     	System.out.println();

     	// map the input dataset to point objects
 	 	JavaRDD<Trajectory> pointsRDD = mapRawDataToTrajectoryMercRDD(fileRDD);
 	 	
 	 	// select sample
 		List<Trajectory> sampleList = pointsRDD.takeSample(false, num);
 	 	
 	 	// save to HDFS
 		if(save){
 			HDFSFileService hdfs = new HDFSFileService();
	 		hdfs.saveTrajectoryListHDFS(sampleList, "sample-trajectories-merc-"+ num);
	 	}
 		
 		return sampleList;
	}
}
