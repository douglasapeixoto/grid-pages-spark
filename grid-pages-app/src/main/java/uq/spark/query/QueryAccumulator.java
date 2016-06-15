package uq.spark.query;

import org.apache.spark.AccumulatorParam;

/**
 * Accumulator to keep statistics about
 * query execution (e.g. accuracy, precision).
 * 
 * @author uqdalves
 *
 */
@SuppressWarnings("serial")
public class QueryAccumulator implements AccumulatorParam<String> {

	public String addInPlace(String s1, String s2) {
		s1 = s1+"\n"+s2;
		return s1;
	}

	public String zero(String s) {
		s = "";
		return s;
	}

	public String addAccumulator(String s1, String s2) {
		s1 = s1+"\n"+s2;
		return s1;
	}
}
