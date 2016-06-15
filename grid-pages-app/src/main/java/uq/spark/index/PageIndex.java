package uq.spark.index;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.apache.hadoop.io.WritableComparable;

/**
 * The composite key index of a grid page: <CSI, TPI>.
 * 
 * @author uqdalves
 *
 */
// http://tutorials.techmytalk.com/2014/11/14/mapreduce-composite-key-operation-part2/
@SuppressWarnings("serial")
public class PageIndex implements WritableComparable<PageIndex>, Serializable {
	/**
	 * Cell Spatial Index (Cell ID)
	 */
	public Integer CSI;
	/**
	 * Time Page Index
	 */
	public Integer TPI;
	
	public PageIndex(){}
	public PageIndex(Integer CSI, Integer TPI) {
		this.CSI = CSI;
		this.TPI = TPI;
	}
	
	/**
	 * Print this index: System out
	 */
	public void print(){
		System.out.println("<CSI,TPI>:(" + CSI + "," + TPI + ")");
	}

	public void readFields(DataInput in) throws IOException {
		CSI = in.readInt();
		TPI = in.readInt();
	}
	
	public void write(DataOutput out) throws IOException {
		out.writeInt(CSI);
        out.writeInt(TPI);
	}
	
	public int compareTo(PageIndex obj) {		
		return CSI == obj.CSI ? (TPI - obj.TPI) : (CSI - obj.CSI);
	}
		
	@Override
	public int hashCode() {
		final int prime = 83;
		int result = 1;
		result = prime * result + ((TPI == null) ? 0 : TPI.hashCode());
		result = prime * result + ((CSI == null) ? 0 : CSI.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof PageIndex) {
        	PageIndex index = (PageIndex) obj;
            return (index.CSI.equals(CSI) && index.TPI.equals(TPI));
        }
        return false;
	}

	@Override
	public String toString() {
		return (CSI + "," + TPI);
	}
}
