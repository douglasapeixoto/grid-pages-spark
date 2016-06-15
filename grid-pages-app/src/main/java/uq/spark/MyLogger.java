package uq.spark;

import java.io.Serializable;

import uq.fs.HDFSFileService;

/**
 * A String buffer to keep and save log
 * information in the application.
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class MyLogger implements Serializable {
	private final StringBuffer log = 
			new StringBuffer();
	private String name = "";

	public MyLogger() {}
	public MyLogger(String name) {
		this.name = name;
	}

	/**
	 * Add a string to this log.
	 */
	public void append(String log){
		this.log.append(log);
	}
	
	/**
	 * Add a string line to this log.
	 * Add a break line after the string.
	 */
	public void appendln(String log){
		this.log.append(log + "\n");
	}
	
	/**
	 * Add a break line int this log.
	 */
	public void appendln(){
		this.log.append("\n");
	}
		
	/**
	 * Print this log: System out.
	 */
	public void print(){
		System.out.println(log.toString());
	}
	
	/**
	 * Returns a string representing thi log. 
	 */
	@Override
	public String toString(){
		return log.toString();
	}
	
	/**
	 * Save this log to the HDFS output log folder.
	 * 
	 * @param name Log file name.
	 */
	public void save(String name){
		HDFSFileService hdfs = new HDFSFileService();
		hdfs.saveLogFileHDFS(log, name);
	}
	
	/**
	 * Save this log to the HDFS output log folder.
	 */
	public void save(){
		HDFSFileService hdfs = new HDFSFileService();
		hdfs.saveLogFileHDFS(log, this.name);
	}
}
