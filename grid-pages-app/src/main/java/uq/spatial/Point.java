package uq.spatial;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.apache.hadoop.io.Writable;

import uq.spatial.distance.EuclideanDistanceCalculator;

/**
 * Implements a simple 2D point entity.
 * X and Y coordinates and time-stamp.
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class Point implements Serializable, Cloneable, Writable {
	public double x;
	public double y;
	public long time;
	
	// the grid cell containing this point
	public int gridId;

	// points distance metric
	private EuclideanDistanceCalculator dist = 
			new EuclideanDistanceCalculator();
	
	public Point(){}
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
		this.time = 0;
	}
	public Point(double x, double y, long time) {
		this.x = x;
		this.y = y;
		this.time = time;
	}
	
	/**
	 * Returns the Euclidean distance between this point
	 * and a given point p.
	 */
	public double dist(Point p){
		return dist.getDistance(p.x, p.y,this.x, this.y);
	}
	
	/**
	 * Returns the Euclidean distance between this point
	 * and a given point p.
	 * </br>
	 * Point given by x and y coordinates.
	 */
	public double dist(double x, double y){
		return dist.getDistance(x, y, this.x, this.y);
	}
	
	/**
	 * Return true if these two points are in same position (x,y).
	 */
	public boolean isSamePosition(Point p){
		return isSamePosition(p.x, p.y);
	}
	
	/**
	 * Return true if this point is the specified position (x,y).
	 */
	public boolean isSamePosition(double x, double y){
		return (x == this.x && y == this.y) ? true : false;
	}
	
	/**
	 * Print this point: System out.
	 */
	public void print(){
		System.out.format("(%.3f,%.3f",x,y);
		System.out.println("," + time + ")");
	}
	
	/**
     * Makes an identical copy of this element.
     */
    @Override
    public Point clone() {
    	Point clone = new Point(x,y,time);
    	clone.gridId = this.gridId;
		return clone;
    }
  
	@Override
    public boolean equals(Object ob) {
        if (ob instanceof Point) {
            Point p = (Point) ob;
            return (p.x == x && p.y == y && p.time == time);
        }
        return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (time ^ (time >>> 32));
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return  (x + " " + y + " " + time);
	}

	public void readFields(DataInput in) throws IOException {
		x = in.readDouble();
		y = in.readDouble();
		time = in.readLong();
		gridId = in.readInt();
	}
	
	public void write(DataOutput out) throws IOException {
		out.writeDouble(x);
		out.writeDouble(y);
		out.writeLong(time);
		out.writeInt(gridId);
	}
}
