package uq.spark.query;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator to compare NN trajectories.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class NeighborComparator<T> implements Serializable, Comparator<NearNeighbor>{

	/**
	 * Compare NN trajectories by their distance to the query object.
	 */
	public int compare(NearNeighbor near0, NearNeighbor near1) {
		return (near0.distance > near1.distance ? 1 : 
			(near0.distance == near1.distance ? 0 : -1));
	}
}
