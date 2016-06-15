package uq.spark.index;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A set of page index, to be used 
 * as trajectory track table tuple.
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class PageIndexSet extends HashSet<PageIndex> implements Serializable{
	public PageIndexSet() {
		super();
	}

	@Override
	public String toString() {
		Iterator<PageIndex> it = iterator();
		String string = "";
		while(it.hasNext()){
			PageIndex index = it.next();
			string +=index.toString() + ",";
		}
		return string.substring(0, string.length()-1);
	}
	
}
