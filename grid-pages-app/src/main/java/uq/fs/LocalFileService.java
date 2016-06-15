package uq.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter; 
import java.util.ArrayList; 
import java.util.List;
import java.util.Random;

import uq.spatial.Point;
import uq.spatial.Trajectory;

/**
 * Service to deal with files locally (open, read, save).
 * 
 * @author uqdalves
 */
public class LocalFileService {
	// Root directory of the files in the disc
	private static final String ROOT_PATH = "/media/bigdata/uqdalves/my-data";
	private static final String INPUT_FOLDER = "/input/";
	private static final String OUTPUT_FOLDER = "/output/";

	/**
	 * Return a list of Trajectory objects from local disc.
	 */
	public ArrayList<Trajectory> readTrajectories(){
		// trajectories to read
		ArrayList<Trajectory> trajectoryList = 
				new ArrayList<Trajectory>();
		
		try {	
			// open files from folder
			File diretory = new File(ROOT_PATH + INPUT_FOLDER);
			File files[] = openDirectoryFiles(diretory);
			
			// read files
			for(int fileId=0; fileId<files.length; fileId++){
				File currentFile = files[fileId];

				// read file
				BufferedReader buffer = new BufferedReader(
	        			new FileReader(currentFile));
				
				// each line of the file
	        	String line;
	        	
	        	// read the first line of the file
	        	line = buffer.readLine();
				
				// fields to be read from the file
				double x, y;
				long time;
				
				// new trajectory for this file, set features
				Trajectory trajectory = new Trajectory();
				
				// read file lines (coordinates)
				while (buffer.ready()) {
					line = buffer.readLine();
					String[] tokens = line.split(",");
					
					// if new trajectory
					if(tokens[0].equals("#")){
						trajectoryList.add(trajectory);
						// new trajectory for this file, set features
						trajectory = new Trajectory();
					} else {
						// Parse the inputs
						x = Double.parseDouble(tokens[0]);
						y = Double.parseDouble(tokens[1]);
						time = Long.parseLong(tokens[2]);
						
						// create a new point from the line input, set features
						Point point = new Point(x, y, time);
				    	
				    	trajectory.addPoint(point);					
					}
				}
				
				// adds the last trajectory in the file
		    	trajectoryList.add(trajectory);
				
				// close file
				buffer.close();
			} 
			
		} catch (IOException e) {
			System.out.println("Error opening input files.");
			e.printStackTrace();
		}
		
		return trajectoryList;
	}
	
	/**
	 * Read a trajectory (Spark format) from local disc by its id.
	 */
	public Trajectory readSparkTrajectoryById(String trajId){
		File file = new File(ROOT_PATH + INPUT_FOLDER + "spark_trajectories"); 

		// new trajectory for this file, set features
		Trajectory trajectory = new Trajectory();
		
		try {
			// read file
			BufferedReader buffer = new BufferedReader(
					new FileReader(file));
			// each line of the file
	    	String line;
	    	
			// fields to be read from the file
			double x, y;
			long time;
			String id;
			
			// read file lines (coordinates)
			while (buffer.ready()) {
				line = buffer.readLine();
				String[] tokens = line.split(" ");
				
				// New Point for this line
				id = tokens[0];
				if(trajId.equals(id)){
					x = Double.parseDouble(tokens[1]);
					y = Double.parseDouble(tokens[2]);
					time = Long.parseLong(tokens[3]);
					Point p = new Point(x, y, time);
					trajectory.addPoint(p);
				}
				
			}
			// close file
			buffer.close();
			
		} catch (IOException e) {
			System.out.println("Error opening input files.");
			e.printStackTrace();
		}
		
		return trajectory;
	}

	/**
	 * Read trajectory points (Spark format) as a list of points.
	 * All points from all trajectories in one list.
	 * Read as if the trajectories were a dataset of points.
	 */
	public ArrayList<Point> readSparkTrajectoriesAsPointList(){
		ArrayList<Point> pointsList = new ArrayList<Point>();

		try {	
			// open files from folder
			File diretory = new File(ROOT_PATH + INPUT_FOLDER);
			File files[] = openDirectoryFiles(diretory);
			
			// read files
			for(int fileId=0; fileId<files.length; fileId++){
				File currentFile = files[fileId];

				// read file
				BufferedReader buffer = new BufferedReader(
	        			new FileReader(currentFile));
				
				// each line of the current file
	        	String line;
	        	
	        	// fields to be read from the file
				double x, y;
				long time;
				
				// read file lines (coordinates)
				while (buffer.ready()) {
					line = buffer.readLine();
					String[] tokens = line.split(" ");
					
					x = Double.parseDouble(tokens[1]);
					y = Double.parseDouble(tokens[2]);
					time = Long.parseLong(tokens[3]);
					
					// create and add a new point from the line input, set features 	
				    pointsList.add(new Point(x,y,time));					

				}

				// close file
				buffer.close();
			} 
			
		} catch (IOException e) {
			System.out.println("Error opening input files.");
			e.printStackTrace();
		}		

		return pointsList;
	}
	
	/**
	 * Generates a set of artificial trajectories.
	 * @param ref is the pivot, around which one wants to create the coordinates.
	 */
	public ArrayList<Trajectory> generateSyntheticTrajectories(
			int numTrajectories, int minSize, int maxSize, int ref){
		ArrayList<Trajectory> trajList = new ArrayList<Trajectory>();
				
		System.out.println("\n Generating Synthetic Trajectories.. \n");
		 
		for(int i=0; i < numTrajectories; i++){
			Random rand = new Random();
			// chose a random number of points for this trajectory
			int totalPoints = rand.nextInt(maxSize - minSize + 1) + minSize;
			
			Trajectory traj = new Trajectory();
			for(int j=0; j<totalPoints; j++){
				rand = new Random();
				
				// choose a random point
				Point p = new Point();
				p.x = rand.nextInt(ref);
				p.y = rand.nextInt(ref);
				p.time = j;
				
				traj.addPoint(p);
			}
			
			System.out.println("Trajectory " + i);	
			trajList.add(traj);
		}
		
		return trajList;
	}
	
	
	/**
	 * Save this list of trajectories to local disc files, 
	 * save every trajPerFile trajectories into a different file.
	 */
	public void saveTrajectoryList(
			ArrayList<Trajectory> trajectoryList, int trajPerFile) {
		
		System.out.println("\n Saving Trajectories.. \n");
		
		int count = 1;
		String script = "";
		int fileCount = trajPerFile;
		for(Trajectory traj : trajectoryList){
			//script += "#\n";
			for(Point p : traj.getPointsList()){
				script += "T" + count + " " +
						  p.x + " " +
						  p.y + " " +
						  p.time + "\n";
			}
			
			if(count % trajPerFile == 0){
				// remove last \n
				script = script.substring(0, script.lastIndexOf("\n"));
				// save the script into a file
				saveFile(script, ""+fileCount);
				fileCount += trajPerFile;
				script="";
			}
			
			count++;
		}
	}
	
	/**
	 * Save this list of points to a local disc.
	 */
	public void savePointsList(List<Point> pointsList, String fileName) {
		System.out.println("\n Saving Points Locally.. \n");

		String script = "";
		for(Point p : pointsList){
			script += p.x + " " + p.y + " " + p.time + "\n";
			
		}
		saveFile(script, fileName);
	}
	
	/**
	 * Read the files inside a directory. Recursively read
	 * directories into other directory.
	 * 
	 * @param diretory
	 * @return File[] a list with the files read
	 */
	private File[] openDirectoryFiles(File diretory) {  
	    List<File> fileList = new ArrayList<File>(); 
	    
	    File[] files = diretory.listFiles();  
	    for (int i = 0; i < files.length; i++) {  
	        if (files[i].isDirectory()) {  
	            // add in the list the files found in 'files[i]':  
	            File[] recFiles = openDirectoryFiles(files[i]);  
	            for (int j = 0; j < recFiles.length; j++) { 
	            	fileList.add(recFiles[j]);  
	            }  
	        } else {  
	            // add in the list the file found in 'directory'  
	            fileList.add(files[i]);  
	        }  
	    }  
	      
	    // turn the List into a File[]:  
	    File[] filesFound = new File[fileList.size()];  
	    for (int i=0; i<fileList.size(); i++) {  
	        filesFound[i] = fileList.get(i);  
	    }  
	    
	    return filesFound;  
	} 
	
	/**
	 * Save the file to the disc folder.
	 * @param script The content of the file
	 * @param fileName Name of the file, with its extension
	 */
	private void saveFile(String script, String fileName){
		File file = new File(ROOT_PATH + OUTPUT_FOLDER + fileName);

		try {
			if(!file.exists()){  
				file.createNewFile();
	        // System.out.println("File '" + fileName + "' successfully created.");  
	        }else{  
	            System.out.println("File '" + fileName + "' already exists.");  
	        } 
			
			BufferedWriter buffer = 
					new BufferedWriter(new PrintWriter(file));
			buffer.write(script); 
			//buffer.flush();  
		    buffer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} 	
	}
	
}
