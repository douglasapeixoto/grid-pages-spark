package uq.spark.index;

/**
 * Parameters interface.
 * Setup application parameters.
 * 
 * @author uqdalves
 *
 */
public interface IndexParameters {
	// index parameters
	static final int TIME_WINDOW_SIZE = 1200; // seconds: 600 1200 3600 7200
	// number of grid partitions
	static final int SIZE_X = 32; // 16 23 28 32 45;
	static final int SIZE_Y = 32; // 16 22 27 32 45;
}