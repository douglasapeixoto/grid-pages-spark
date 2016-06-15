package uq.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
 
/**
 * My Spark Context. 
 * Tells Spark how to access a cluster.
 * 
 * @author uqdalves
 */
public class MySparkContext {
	// tells Spark how to access a cluster
	private static final SparkConf conf = 
			new SparkConf()
			//.setMaster("local") 
			.setAppName("SparkProject")
			.set("spark.scheduler.mode", "FAIR"); // round-robin scheduling
			/*.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
	 		.set("spark.kryo.registrator", KryoClassRegistrator.class.getName())
			.set("spark.kryoserializer.buffer.mb","128");*/// 24mb
	private static final JavaSparkContext sc = new JavaSparkContext(conf);
	
	// listener to keep a log of the application runtime
	private static final MySparkListener listener = new MySparkListener();
	
	/**
	 * An instance of this application Spark context.
	 */
	public static synchronized final JavaSparkContext getInstance(){
		 sc.sc().addSparkListener(listener);
		 return sc;
	}
}
