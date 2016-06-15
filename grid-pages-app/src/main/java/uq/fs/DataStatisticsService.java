package uq.fs;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import uq.spark.EnvironmentVariables;
import uq.spatial.Trajectory;

/**
 * Service to calculate some statistics on the 
 * trajectory dataset (length, speed, etc.)
 * 
 * Use Spark MapReduce functions
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class DataStatisticsService implements Serializable, EnvironmentVariables{

	/**
	 * Number of trajectories in this dataset (RDD count).
	 */
	public static long numTrajectories(JavaRDD<Trajectory> trajectoryRDD){
			return trajectoryRDD.count();
	}
	
	/**
	 * Number of trajectories points in this dataset.
	 */
	public static long numTrajectoryPoints(JavaRDD<Trajectory> trajectoryRDD){
		final long totalPoints = 
			trajectoryRDD.glom().map(new Function<List<Trajectory>, Long>() {
				// map each trajectory to its size
				public Long call(List<Trajectory> tList) throws Exception {
					long sum=0;
					for(Trajectory t : tList){
						sum += t.size();
					}
					return sum;
				}
			}).reduce(new Function2<Long, Long, Long>() {
				// sum the trajectory size
				public Long call(Long v1, Long v2) throws Exception {
					return (v1 + v2);
				}
			});
		return totalPoints;
	}
	
	/**
	 * Statistics about length of trajectories in this dataset.
	 * 
	 * @return A double vector containing the mean: [0], min: [1],
	 * max: [2], and std: [3] trajectories length in this dataset. 
	 */
	public static double[] trajectoryLengthStats(JavaRDD<Trajectory> trajectoryRDD){
		// total trajectories in this dataset
		final double numTrajectories = numTrajectories(trajectoryRDD);
	
		// get mean, min, max and std length of trajectories
		double[] lenghtVector = 
			trajectoryRDD.glom().map(new Function<List<Trajectory>, double[]>() {
				public double[] call(List<Trajectory> tList) throws Exception {
					double[] vec = new double[]{0.0,Double.MAX_VALUE,0.0,0.0};
					for(Trajectory t : tList){
						double lenght = t.length();
						vec[0] += lenght;
						vec[1] = Math.min(vec[1], lenght); 
						vec[2] = Math.max(vec[2], lenght); 
						vec[3] += (lenght*lenght);
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
		lenghtVector[3] = (lenghtVector[3] - 
				(1/numTrajectories)*(lenghtVector[0]*lenghtVector[0]));
		lenghtVector[3] = Math.sqrt(lenghtVector[3]/numTrajectories);
		// get mean
		lenghtVector[0] = lenghtVector[0]/numTrajectories;
		
		return lenghtVector;
	}
	
	/**
	 * Statistics about duration of trajectories in this dataset.
	 * 
	 * @return A double vector containing the mean: [0], min: [1],
	 * max: [2], adn std: [3] trajectories duration (time) in this dataset. 
	 */
	public static double[] trajectoryDurationStats(JavaRDD<Trajectory> trajectoryRDD){
		// total trajectories in this dataset
		final double numTrajectories = numTrajectories(trajectoryRDD);		

		// get mean, min, max and std duration of trajectories
		double[] durationVector = 
			trajectoryRDD.glom().map(new Function<List<Trajectory>, double[]>() {
				public double[] call(List<Trajectory> tList) throws Exception {
					double[] vec = new double[]{0.0,Double.MAX_VALUE,0.0,0.0};
					for(Trajectory t : tList){
						double duration = t.duration();
						vec[0] += duration; 
						vec[1] = Math.min(vec[1], duration); 
						vec[2] = Math.max(vec[2], duration);						
						vec[3] += (duration*duration);
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
		durationVector[3] = (durationVector[3] - 
				(1/numTrajectories)*(durationVector[0]*durationVector[0]));
		durationVector[3] = Math.sqrt(durationVector[3]/numTrajectories);
		// get mean
		durationVector[0] = durationVector[0]/numTrajectories;

		return durationVector;
	}
	
	/**
	 * Statistics about speed of trajectories in this dataset.
	 * 
	 * @return A double vector containing the mean: [0], min: [1]
	 * and max: [2] trajectories average speed in this dataset. 
	 */
	public static double[] trajectorySpeedStats(JavaRDD<Trajectory> trajectoryRDD){
		// total trajectories in this dataset
		final double numTrajectories = numTrajectories(trajectoryRDD);

		// get mean, min and max speed of trajectories
		double[] speedVector = 
			trajectoryRDD.glom().map(new Function<List<Trajectory>, double[]>() {
				public double[] call(List<Trajectory> tList) throws Exception {
					double[] vec = new double[]{0.0,Double.MAX_VALUE,0.0,0.0};
					for(Trajectory t : tList){
						double speed = t.speed();
						vec[0] += speed; 
						vec[1] = Math.min(vec[1], speed); 
						vec[2] = Math.max(vec[2], speed);
						vec[3] += (speed*speed);
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
		speedVector[3] = (speedVector[3] - 
				(1/numTrajectories)*(speedVector[0]*speedVector[0]));
		speedVector[3] = Math.sqrt(speedVector[3]/numTrajectories);
		// get mean
		speedVector[0] = speedVector[0]/numTrajectories;

		return speedVector;
	}
	
	/**
	 * Statistics about sampling rate of trajectories in this dataset.
	 * 
	 * @return A double vector containing the mean: [0], min: [1],
	 * max: [2], and std: [3] trajectories average sampling rate in this dataset. 
	 */
	public static double[] trajectorySamplingRateStats(JavaRDD<Trajectory> trajectoryRDD){
		// total trajectories in this dataset
		final double numTrajectories = numTrajectories(trajectoryRDD);
		
		// get mean, min, max and std sampling rates
		double[] rateVector =
			trajectoryRDD.glom().map(new Function<List<Trajectory>, double[]>() {
				public double[] call(List<Trajectory> tList) throws Exception {
					double[] vec = new double[]{0.0,Double.MAX_VALUE,0.0,0.0};
					for(Trajectory t : tList){
						double rate = t.samplingRate();
						vec[0] += rate; 
						vec[1] = Math.min(vec[1], rate); 
						vec[2] = Math.max(vec[2], rate);
						vec[3] += (rate*rate);
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
		rateVector[3] = (rateVector[3] - 
				(1/numTrajectories)*(rateVector[0]*rateVector[0]));
		rateVector[3] = Math.sqrt(rateVector[3]/numTrajectories);
		// get mean
		rateVector[0] = rateVector[0]/numTrajectories;

		return rateVector;
	}
	
	/**
	 * Statistics about the average number of the points per 
	 * trajectory in this dataset.
	 * 
	 * @return A double vector containing the mean: [0], min: [1],
	 * max: [2], std: [3] average points per trajectory in this dataset. 
	 */
	public static double[] trajectoryNumberOfPointsStats(JavaRDD<Trajectory> trajectoryRDD){
		// total trajectories in this dataset
		final double numTrajectories = numTrajectories(trajectoryRDD);

		// get mean, min, max and std number of points 
		double[] numPtsVector = 
			trajectoryRDD.glom().map(new Function<List<Trajectory>, double[]>() {
				public double[] call(List<Trajectory> tList) throws Exception {
					double[] vec = new double[]{0.0,Double.MAX_VALUE,0.0,0.0};
					for(Trajectory t : tList){
						double numPts = t.size();
						vec[0] += numPts; 
						vec[1] = Math.min(vec[1], numPts); 
						vec[2] = Math.max(vec[2], numPts);
						vec[3] += (numPts*numPts);
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
		numPtsVector[3] = (numPtsVector[3] - 
				(1/numTrajectories)*(numPtsVector[0]*numPtsVector[0]));
		numPtsVector[3] = Math.sqrt(numPtsVector[3]/numTrajectories);
		//get mean
		numPtsVector[0] = numPtsVector[0]/numTrajectories;

		return numPtsVector;
	}
	
	/**
	 * Calculate and save the dataset statistics as a file to HDFS.
	 * </br>
	 * Save as "dataset-statistics" in the HDFS output folder.
	 */
	public static void saveStatistics(JavaRDD<Trajectory> trajectoryRDD){
     	long numTraj = numTrajectories(trajectoryRDD);
     	long numPts = numTrajectoryPoints(trajectoryRDD);
     	double[] avgPts = trajectoryNumberOfPointsStats(trajectoryRDD);
     	double[] avgDur = trajectoryDurationStats(trajectoryRDD);
     	double[] avgLen = trajectoryLengthStats(trajectoryRDD);
     	double[] avgSpeed = trajectorySpeedStats(trajectoryRDD);
     	double[] avgRate = trajectorySamplingRateStats(trajectoryRDD);
     	
     	// save results to HDFS
     	String script ="";
     	script += "Total Number of Trajectories: " + numTraj + "\n";
     	script += "Total Number of Points: " + numPts + "\n";
     	script += "Avg. Number of Points per Trajectory: " + avgPts[0] + "\n";
     	script += "Min. Number of Points per Trajectory: " + avgPts[1] + "\n";
     	script += "Max. Number of Points per Trajectory: " + avgPts[2] + "\n";
     	script += "Std. Number of Points per Trajectory: " + avgPts[3] + "\n";
     	script += "Avg. Trajectory Length: " + avgLen[0] + "\n";
     	script += "Min. Trajectory Length: " + avgLen[1] + "\n";
     	script += "Max. Trajectory Length: " + avgLen[2] + "\n";
     	script += "Std. Trajectory Length: " + avgLen[3] + "\n";
     	script += "Avg. Trajectory Duration: " + avgDur[0] + "\n";
     	script += "Min. Trajectory Duration: " + avgDur[1] + "\n"; 
     	script += "Max. Trajectory Duration: " + avgDur[2] + "\n"; 
     	script += "Std. Trajectory Duration: " + avgDur[3] + "\n"; 
     	script += "Avg. Trajectory Speed: " + avgSpeed[0] + "\n";
     	script += "Min. Trajectory Speed: " + avgSpeed[1] + "\n";
     	script += "Max. Trajectory Speed: " + avgSpeed[2] + "\n";
     	script += "Std. Trajectory Speed: " + avgSpeed[3] + "\n";
     	script += "Avg. Sampling Rate: " + avgRate[0] + "\n";
     	script += "Min. Sampling Rate: " + avgRate[1] + "\n";
     	script += "Max. Sampling Rate: " + avgRate[2] + "\n";
     	script += "Std. Sampling Rate: " + avgRate[3];
     	
     	// save to HDFS
     	HDFSFileService hdfs = new HDFSFileService();
     	hdfs.saveFileHDFS(script, "dataset-statistics");
	}
	
	/**
	 * Calculate ands save the dataset statistics as a History file to HDFS.
	 * </br>
	 * "id size length duration speed sampling-rate"
	 * </br>
	 * This is useful for histogram graph construction.
	 * </br>
	 * Save as "dataset-statistics-histogram" in the HDFS output folder.
	 */
	public static void saveStatisticsAsHist(JavaRDD<Trajectory> trajectoryRDD){
		// id, num pts, length, duration, speed, sampling rate
		List<String> emptyList = new LinkedList<String>();
		Function2<List<String>, Trajectory, List<String>> seqOp = 
				new Function2<List<String>, Trajectory, List<String>>() {
			public List<String> call(List<String> list, Trajectory t) throws Exception {
				String script="";
				script += t.id + " ";
				script += t.size() + " ";
				script += t.length() + " ";
				script += t.duration() + " ";
				script += t.speed() + " ";
				script += t.samplingRate();
				list.add(script);
				return list;
			}
		};
		Function2<List<String>, List<String>, List<String>> combOp = 
				new Function2<List<String>, List<String>, List<String>>() {
			public List<String> call(List<String> list1, List<String> list2) throws Exception {
				list1.addAll(list2);
				return list1;
			}
		};
		String header = "id size length duration speed sampling-rate";
		List<String> hist = new LinkedList<String>();
		hist.add(header);
		hist.addAll(trajectoryRDD.aggregate(emptyList, seqOp, combOp));
				
		// save to HDFS
		HDFSFileService hdfs = new HDFSFileService();
     	hdfs.saveStringListHDFS(hist, "dataset-statistics-histogram");
	}
	
	/**
	 * Main
	 */
	public static void main(String[] arg0){
		System.out.println();
		System.out.println("Running Dataset Statistics..\n");
		
    	// read trajectory data files
     	JavaRDD<String> fileRDD = SC.textFile(DATA_PATH, NUM_PARTITIONS_DATA);
     	//fileRDD.persist(StorageLevel.MEMORY_AND_DISK());
     	
     	// convert the input dataset to trajectory objects (read the dataset in lat/lon)
     	DataConverter rddService = new DataConverter();
     	JavaRDD<Trajectory> trajectoryRDD = rddService.mapRawDataToTrajectoryRDD(fileRDD);
   	
     	// calculate and save statistics to HDFS
     	saveStatistics(trajectoryRDD);
     	//saveDatasetStatisticsHist(trajectoryRDD);
     	
     	// clear cache
     	fileRDD.unpersist();
     	trajectoryRDD.unpersist();
	}
}
