package uq.spark;

import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerApplicationEnd;
import org.apache.spark.scheduler.SparkListenerApplicationStart;
import org.apache.spark.scheduler.SparkListenerBlockManagerAdded;
import org.apache.spark.scheduler.SparkListenerBlockManagerRemoved;
import org.apache.spark.scheduler.SparkListenerBlockUpdated;
import org.apache.spark.scheduler.SparkListenerEnvironmentUpdate;
import org.apache.spark.scheduler.SparkListenerExecutorAdded;
import org.apache.spark.scheduler.SparkListenerExecutorMetricsUpdate;
import org.apache.spark.scheduler.SparkListenerExecutorRemoved;
import org.apache.spark.scheduler.SparkListenerJobEnd;
import org.apache.spark.scheduler.SparkListenerJobStart;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.scheduler.SparkListenerTaskGettingResult;
import org.apache.spark.scheduler.SparkListenerTaskStart;
import org.apache.spark.scheduler.SparkListenerUnpersistRDD;
import org.apache.spark.scheduler.StageInfo;

import uq.fs.HDFSFileService;

/**
 * A Spark Listener to track run time
 * information about the application
 * events.
 * 
 * Save the log to HDFS output folder
 * as 'AppName-log'
 * 
 * @author uqdalves
 *
 */
public class MySparkListener implements SparkListener {
	// HDFS
	private HDFSFileService hdfs = new HDFSFileService();
	// A information script to save after application run time
	private final StringBuffer LOG = new StringBuffer();
	private final String appName = "SparkProject";
	private long timeIni;
	private long timeEnd;
	private long stageSum=0;
	private long jobSum=0;
	private long jobIni = 0;
	
	public void onApplicationStart(SparkListenerApplicationStart arg0) {
		//appName = arg0.appName();
		timeIni = arg0.time();
		LOG.append("Application " + appName + " starts at: " + timeIni + " ms.\n\n");
	}
	
	public void onApplicationEnd(SparkListenerApplicationEnd arg0) {
		timeEnd = arg0.time();
		LOG.append("Application " + appName + " ends at: " + timeEnd + " ms.\n");
		LOG.append("TOTAL APPLICATION TIME: " + (timeEnd-timeIni) + " ms.\n");
		LOG.append("TOTAL STAGES TIME: " + stageSum + " ms.\n");
		LOG.append("TOTAL JOBS TIME: " + jobSum + " ms.");
		// save log to HDFS
		hdfs.saveLogFileHDFS(LOG, appName);
	}

	public void onJobStart(SparkListenerJobStart arg0) {
		if(arg0 != null){
			int jobId = arg0.jobId();
			jobIni = arg0.time();
			LOG.append("Job (" + jobId + ") started at: " + jobIni + " ms.\n\n");	
		}
	}
	
	public void onJobEnd(SparkListenerJobEnd arg0) {
		if(arg0 != null){
			int jobId = arg0.jobId();
			long jobEnd = arg0.time();
			LOG.append("Job (" + jobId + ") completed at: " + jobEnd + " ms.\n");	
			LOG.append("Job (" + jobId + ") total time: " + (jobEnd - jobIni) + " ms.\n\n");
			jobSum += (jobEnd - jobIni);
		}
	}	

	public void onStageCompleted(SparkListenerStageCompleted arg0) {
		StageInfo info = arg0.stageInfo();
		if(info!=null){
			long ini = Long.parseLong(info.submissionTime().get().toString());
			long end = Long.parseLong(info.completionTime().get().toString());
			LOG.append("Stage ("+info.stageId()+") " + info.name() + " completed.\n");
			LOG.append("Number of tasks: " + info.numTasks() + ".\n");
			LOG.append("Stage submission time: " + ini + " ms.\n");
			LOG.append("Stage completion time: " + end + " ms.\n");
			LOG.append("Stage total time: " + (end-ini) + " ms.\n\n");
			stageSum += (end - ini);
		}
	}
	
	// not used
	public void onTaskStart(SparkListenerTaskStart arg0) {}	
	public void onTaskEnd(SparkListenerTaskEnd arg0) {}
	public void onStageSubmitted(SparkListenerStageSubmitted arg0) {}
	public void onTaskGettingResult(SparkListenerTaskGettingResult arg0) {}
	public void onBlockManagerAdded(SparkListenerBlockManagerAdded arg0) {}
	public void onBlockManagerRemoved(SparkListenerBlockManagerRemoved arg0) {}
	public void onBlockUpdated(SparkListenerBlockUpdated arg0) {}
	public void onEnvironmentUpdate(SparkListenerEnvironmentUpdate arg0) {}
	public void onExecutorAdded(SparkListenerExecutorAdded arg0) {}
	public void onExecutorMetricsUpdate(SparkListenerExecutorMetricsUpdate arg0) {}
	public void onExecutorRemoved(SparkListenerExecutorRemoved arg0) {}
	public void onUnpersistRDD(SparkListenerUnpersistRDD arg0) {}
}
