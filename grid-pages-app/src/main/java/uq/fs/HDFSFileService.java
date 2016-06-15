package uq.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter; 
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import uq.spark.EnvironmentVariables;
import uq.spark.index.IndexParameters;
import uq.spatial.Point;
import uq.spatial.Trajectory;

/**
 * Service to read/write from/to the 
 * Hadoop File System (HDFS).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class HDFSFileService implements EnvironmentVariables, Serializable, IndexParameters {
	
	/**
	 * Hadoop access configuration.
	 */
	private Configuration config = null;
	
	/**
	 * Constructor, setup HDFS configuration (standard access)
	 */
	public HDFSFileService() {
		// standard configuration
		config = new Configuration();
		config.addResource(new Path(HADOOP_HOME + "/etc/hadoop/core-site.xml"));
		config.addResource(new Path(HADOOP_HOME + "/etc/hadoop/hdfs-site.xml"));
	}

	/**
	 * Constructor.
	 * 
	 * @param config Hadoop access configuration
	 */
	public HDFSFileService(Configuration config) {
		this.config = config;
	}
	
	/**
	 * Set Hadoop access configuration.
	 */
	public void setConf(Configuration config){
		this.config = config;
	}
	
	/**
	 * Read file from HDFS file system.
	 * 
	 * @param filePath the path to the file, inside the HDFS
	 * @return Return a list of Stings with the file lines,
	 * one line per String in the list.
	 */
	public List<String> readFileHDFS(final String filePath){			
		// fields to be read from the file
		try {
            FileSystem hdfs = FileSystem.get(config);
            Path path = new Path(HDFS_PATH + filePath);
            BufferedReader buffer = 
            		new BufferedReader(new InputStreamReader(hdfs.open(path)));
            // read file lines as string
            List<String> lines = new LinkedList<String>();
            while (buffer.ready()) {
            	lines.add(buffer.readLine());
            }
            System.out.println("File '" + filePath + "' successfully read from HDFS."); 
            buffer.close();
            
            return lines;
        } catch (IOException e) {
        	System.out.println("File '" + filePath + "' not found in the HDFS.");
            e.printStackTrace();
        } 
		return null;
	}
	
	/**
	 * Save this list of points to the HDFS /output folder.
	 */
	public void savePointListHDFS(
			List<Point> pointList, 
			final String fileName){
		List<String> scriptList = new LinkedList<String>();
		for(Point p : pointList){
			String script = p.x + " " + p.y + " " + p.time;
			scriptList.add(script);
		}
		// save to HDFS
		saveStringListHDFS(scriptList, fileName);
	}
	
	/**
	 * Save this list of trajectories to the HDFS /output folder.
	 */
	public void saveTrajectoryListHDFS(
			List<Trajectory> trajectoryList, 
			final String fileName){
		List<String> scriptList = new LinkedList<String>();
		for(Trajectory t : trajectoryList){
			String script = t.id;
			for(Point p : t.getPointsList()){
				script += " " + p.x + " " + p.y + " " + p.time;
			}
			scriptList.add(script);
		}
		// save to HDFS
		saveStringListHDFS(scriptList, fileName);
	}
	
	/**
	 * Save this list of Objects to the HDFS folder.
	 * One element per line.
	 * Objects should override toString() method.
	 * 
	 * @param listObj A list of object to save.
	 * @param fileName Name of the file, with its extension.
	 */
	public void saveObjectListHDFS(
			List<Object> listObj, 
			final String fileName){
		List<String> scriptList = new LinkedList<String>();
		for(Object obj : listObj){
			scriptList.add(obj.toString());
		}
		// save to HDFS
		saveStringListHDFS(scriptList, fileName);
	}
	
	/**
	 * Save this list of Strings to the HDFS folder.
	 * One String per line.
	 * 
	 * @param listObj A list of Strings to save.
	 * @param fileName Name of the file, with its extension.
	 */
	public void saveStringListHDFS(
			List<String> stringList, 
			final String fileName){
		String name = fileName;
        try {
			Path file = new Path(HDFS_PATH + HDFS_OUTPUT + name);
			FileSystem fs = FileSystem.get(new URI(HDFS_PATH), config);
			
			int i = 1;
			while(fs.isFile(file)){
				name = fileName + "-" + i++;
				file = new Path(HDFS_PATH + HDFS_OUTPUT + name);
			}
			
			BufferedWriter writer =
					new BufferedWriter(new OutputStreamWriter(fs.create(file,true)));
			//FSDataOutputStream out = fs.create(file);
			for(String record : stringList){
				writer.write(record + "\n");
			}
			writer.close();
			System.out.println("File '" + HDFS_OUTPUT + name + "' successfully saved to HDFS."); 
	    } catch(Exception e){
	    	System.out.println("ERROR when writing '" + name + "' to HDFS.");
	        e.printStackTrace();
	    }       
	}
	
	/**
	 * Save this String buffer to a file in the HDFS folder.
	 * If the file already exists, then save as 
	 * 'fileName-1', 'fileName-2', and so on.
	 * 
	 * @param scriptBuffer The buffer with the content of the file.
	 * @param fileName Name of the file, with its extension.
	 */
	public void saveStringBufferHDFS(
			StringBuffer scriptBuffer, 
			final String fileName) {
		String name = fileName;
		try {
			FileSystem fs = FileSystem.get(new URI(HDFS_PATH), config);
			Path file = new Path(HDFS_PATH + HDFS_OUTPUT + name);
			
			int i = 1;
			while(fs.isFile(file)){
				name = fileName + "-" + i++;
				file = new Path(HDFS_PATH + HDFS_OUTPUT + name);
			}
			
			BufferedWriter writer =
					new BufferedWriter(new OutputStreamWriter(fs.create(file,true)));
			writer.write(scriptBuffer.toString());
			writer.close();
			fs.close();
			System.out.println("File '" + HDFS_OUTPUT + name + "' successfully saved to HDFS."); 
	    } catch(Exception e){
	    	System.out.println("ERROR when writing '" + name + "' to HDFS.");
	        e.printStackTrace();
	    }
	}
	
	/**
	 * Save the application log file to the HDFS folder.
	 * If the file already exists, then save as 
	 * 'fileName-log-1', 'fileName-log-2', and so on. 
	 */
	public void saveLogFileHDFS(
			StringBuffer log, 
			final String fileName){
		String baseName = fileName + "-log";
		String name = baseName;
        try {
        	Path file = new Path(HDFS_PATH + APP_LOG + name);
        	FileSystem fs = FileSystem.get(new URI(HDFS_PATH), config);
        	int i = 1;
			while(fs.isFile(file)){
				name = baseName + "-" + i++;
				file = new Path(HDFS_PATH + APP_LOG + name);
			}
			BufferedWriter writer =
					new BufferedWriter(new OutputStreamWriter(fs.create(file,true)));
			writer.write(log.toString());
			writer.close();
			System.out.println("Log file '" + APP_LOG + name + "' successfully saved to HDFS."); 
	    } catch(Exception e){
	    	System.out.println("ERROR when writing log file '" + name + "' to HDFS.");
	        e.printStackTrace();
	    }   
	}
	
	/**
	 * Save the file to the HDFS folder.
	 * If the file already exists, then save as 
	 * 'fileName-1', 'fileName-2', and so on.
	 * 
	 * @param script The content of the file
	 * @param fileName Name of the file, with its extension.
	 */
	public void saveFileHDFS(
			final String script, 
			final String fileName){        
		String name = fileName;
		try {
			FileSystem fs = FileSystem.get(new URI(HDFS_PATH), config);
			Path file = new Path(HDFS_PATH + HDFS_OUTPUT + name);
			
			int i = 1;
			while(fs.isFile(file)){
				name = fileName + "-" + i++;
				file = new Path(HDFS_PATH + HDFS_OUTPUT + name);
			}
			
			BufferedWriter writer =
					new BufferedWriter(new OutputStreamWriter(fs.create(file,true)));
			writer.write(script);
			writer.close();
			fs.close();
			System.out.println("File '" + HDFS_OUTPUT + name + "' successfully saved to HDFS."); 
	    } catch(Exception e){
	    	System.out.println("ERROR when writing '" + name + "' to HDFS.");
	        e.printStackTrace();
	    }
	}
}
