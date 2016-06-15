package uq.spark.query;

import java.io.Serializable;

/**
 * Statistics about query execution.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class QueryStats implements Serializable{
	// True positive
	public long TP;
	// False positive
	public long FP;
	// True negatives
	public long TN;
	// False negatives
	public long FN;
	
	/**
	 * @param tp True Positives
	 * @param fp False Positives
	 * @param tn True Negatives
	 * @param fn False Negatives
	 */
	public QueryStats(long tp, long fp, long tn, long fn){
		this.TP = tp;
		this.FP = fp;
		this.TN = tn;
		this.FN = fn;
	}
	
	/**
	 * Query accuracy.
	 */
	public double accuracy(){
		double accu = (double)(TP + TN) / (double)(TP + FP + TN + FN);
		return accu;
	}
	
	/**
	 * Query precision.
	 */
	public double precision(){
		double prec = (double)TP / (double)(TP + FP);
		return prec;
	}
	
	/**
	 * Query recall.
	 */
	public double recall(){
		double recall = (double)TP / (double)(TP + FN);
		return recall;
	}
	
	public String toString(){
		String s = "";
		s += "TP: " + TP + "\n";
		s += "FP: " + FP + "\n";
		s += "TN: " + TN + "\n";
		s += "FN: " + FN + "\n";
		s += "Recall: " + recall() + "\n";
		s += "Precision: " + precision() + "\n";
		s += "Accuracy: " + accuracy() + "\n";
		return s;
	}
}
