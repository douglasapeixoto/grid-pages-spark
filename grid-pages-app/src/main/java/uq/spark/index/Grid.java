package uq.spark.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import uq.spatial.Box;
import uq.spatial.Circle;
import uq.spatial.Point;
import uq.spatial.Segment;

/**
 * A grid object made of n x m cells.
 * </br>
 * The grid is constructed from left to right, 
 * bottom to top. The first position in the grid
 * is the position index 0 (zero)=(0,0).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class Grid implements Serializable {
	// grid cells
	private List<Box> cellsList;
	// grid endpoint coordinates
	public double min_x;
	public double min_y;
	public double max_x;
	public double max_y;
	// grid dimensions (number of cells)
	public int size_x;
	public int size_y;
	
	/**
	 * Create a new grid of (n x m) cells for the given dimensions. 
	 */
	public Grid(int n, int m, double minX, double minY, double maxX, double maxY){
		this.min_x = minX;
		this.min_y = minY;
		this.max_x = maxX;
		this.max_y = maxY;
		this.size_x = n;
		this.size_y = m;
		cellsList = new ArrayList<Box>(size_x*size_y);
		// build the grid
		build();
	}
	
	/**
	 * Generates the grid cells.
	 */
	private void build() {
		// increments
		double incrX = (max_x-min_x) / size_x;
		double incrY = (max_y-min_y) / size_y;
		double currentX, currentY=min_y;
		currentY=min_y;
		for(int y=0; y<size_y; y++){	
			currentX=min_x;
			for(int x=0; x<size_x; x++){
				cellsList.add(new Box(currentX, currentX+incrX, currentY, currentY+incrY));
				currentX += incrX;
			}
			currentY += incrY;
		}
	}
	
	/**
	 * The list of cells in this grid.
	 */
	public List<Box> getCells(){
		return cellsList;
	}
	
	/**
	 * Return the i-th cell in this grid.
	 */
	public Box get(final int i) {
		assert(i>=0 && i<size())
		: "Grid index out of bound.";
		return cellsList.get(i);
	}
	
	/**
	 * Return the cell in the position [x,y] 
	 * in the grid. Grid x and y position start 
	 * from (0,0).
	 */
	public Box get(final int x, final int y) {
		assert(x>=0 && x<size_x && y>=0 && y<size_y)
		: "Grid index out of bound.";
		int index = y*size_x + x;
		return cellsList.get(index);
	}
	
	/**
	 * Number of cells in this grid.
	 */
	public int size(){
		return cellsList.size();
	}
	
	/**
	 * The total area covered by this grid.
	 */
	public double area(){
		return (max_x-min_x)*(max_y-min_y);
	}
	
	/**
	 * The total perimeter of this grid.
	 */
	public double perimeter(){
		return 2*(max_x-min_x)+2*(max_y-min_y);
	}
	
	/**
	 * The height (Y axis) of the cells
	 * in this grid.
	 */
	public double cellsHeight(){
		double height = (max_y - min_y) / size_y;
		return height;
	}
	
	/**
	 * The width (X axis) of the cells
	 * in this grid.
	 */
	public double cellsWidth(){
		double width = (max_x - min_x) / size_x;
		return width;
	}

	/**
	 * Return the positions (index) of the cells in this grid that
	 * overlap with the given rectangular area. 
	 */
	public HashSet<Integer> getOverlappingCells(Box r){
		HashSet<Integer> posList = new HashSet<Integer>();
		for(int i=0; i<cellsList.size(); i++){
			Box rec = cellsList.get(i);
			if(rec.overlap(r)){
				posList.add(i);
			}
		}
		return posList;
	}

	/**
	 * Return the positions (index) of the cells in this grid that
	 * overlap with the given circular area. 
	 */
	public HashSet<Integer> getOverlappingCells(Circle c){
		HashSet<Integer> posList = new HashSet<Integer>();
		for(int i=0; i<cellsList.size(); i++){
			Box rec = cellsList.get(i);
			if(c.overlap(rec.leftEdge())){
				posList.add(i);
			} else if(c.overlap(rec.rightEdge())){
				posList.add(i);
			} else if(c.overlap(rec.lowerEdge())){
				posList.add(i);
			} else if(c.overlap(rec.upperEdge())){
				posList.add(i);
			}
		}
		return posList;
	}
	
	/**
	 * Return the positions (index) of the cells in this grid that
	 * overlaps with the given line segment, that is, the id of the
	 * rectangles that either contains or intersect the line segment
	 */
	public List<Integer> getOverlappingCells(Segment s) {
		List<Integer> posList = new ArrayList<Integer>();
		for(int i=0; i<cellsList.size(); i++){
			Box r = cellsList.get(i);
			if(r.overlap(s)){
				posList.add(i);
			}
		}
		return posList;
	}
	
	/**
	 * Return the position (index) of the rectangle in this grid that
	 * overlaps with the given point, that is, the id of the
	 * rectangle that contains or touches the point. If the point
	 * is a border point, return the first occurrence in the grid.
	 */
	public int getOverlappingCell(Point p) {
		int xi = (int) ((p.x - min_x) / cellsWidth());
		int yi = (int) ((p.y - min_y) / cellsHeight());
		int index = yi*size_x + xi;
		return index;
	}

	/**
	 * Return the positions (index) of the adjacent cells
	 * from the given grid position. 
	 */
	public List<Integer> getAdjacentCells(final int x, final int y){
		assert(x>=0 && x<size_x && y>=0 && y<size_y)
		: "Grid index out of bound.";
		
		List<Integer> posList = new ArrayList<Integer>();
		int adjX, adjY;
		int index;

		adjX = x-1; adjY = y-1;
		if(adjX>=0 && adjY>=0){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x; adjY = y-1;
		if(adjY>=0){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x+1; adjY = y-1;
		if(adjX<size_x && adjY>=0){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x-1; adjY = y;
		if(adjX>=0){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x+1; adjY = y;
		if(adjX<size_x){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x-1; adjY = y+1;
		if(adjX>=0 && adjY<size_y){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x; adjY = y+1;
		if(adjY<size_y){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		adjX = x+1; adjY = y+1;
		if(adjX<size_x && adjY<size_y){
			index = adjY*size_x + adjX;
			posList.add(index);
		}
		
		return posList;
	}
	
	/**
	 * Print grid dimensions: system out
	 */
	public void print(){
		System.out.println("Grid Dimensions: [" + size_x + " x " + size_y + "]\n");
		for(int y=size_y-1; y>=0; y--){
			for(int x=0; x<size_x; x++){
				int index = y*size_x + x;
				Box r = cellsList.get(index);
				System.out.format("[(%.2f,%.2f)(%.2f,%.2f)] ",r.minX,r.maxY,r.maxX,r.maxY);
			}	
			System.out.println();
			for(int x=0; x<size_x; x++){
				int index = y*size_x + x;
				Box r = cellsList.get(index);
				System.out.format("[(%.2f,%.2f)(%.2f,%.2f)] ",r.minX,r.minY,r.maxX,r.minY);
			}
			System.out.println("\n");
		}
	}
}
