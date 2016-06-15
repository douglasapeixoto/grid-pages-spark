package uq.spatial.distance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import uq.spatial.Point;

/**
* Euclidean distance for multidimensional time series. 
*
* @author uqdalves, uqhsu1 
*/
@SuppressWarnings("serial")
class EuclideanTimeSeriesDistanceCalculator implements Serializable, DistanceInterface {

	/**
	 * Euclidean distance for multidimensional time series.
	 */
	public double getDistance(ArrayList<Point> r, ArrayList<Point> s){
		double dist = getEDC(r, s); 
		if(Double.isNaN(dist)){
			return INFINITY;
		}
		return dist;
	}

	private double getEDC(ArrayList<Point> r, ArrayList<Point> s)
	{
		List<Point> longT = new ArrayList<Point>();
		List<Point> shortT = new ArrayList<Point>();

		if (r.size() == 0 && s.size() == 0)
		{
			return 0;
		}
		if (r.size() == 0 || s.size() == 0)
		{
			return Double.MAX_VALUE;
		}

		if (r.size() < s.size())
		{
			shortT = r;
			longT = s;
		}
		else
		{
			shortT = s;
			longT = r;
		}
		int k = shortT.size();

		double[] distanceOption = new double[longT.size() - shortT.size() + 1];

		for (int i = 0; i < distanceOption.length; i++)
		{
			double tempResult = 0;
			for (int j = 0; j < k; j++)
			{
				tempResult += shortT.get(j).dist(longT.get(j + i));
			}
			tempResult /= k;
			distanceOption[i] = tempResult;
		}

		return GetMin(distanceOption);

	}

	private double GetMin(double[] a)
	{
		assert (a.length > 0);

		double result = a[0];

		for (int i = 0; i < a.length; i++)
		{
			if (result < a[i])
			{
				result = a[i];
			}
		}

		return result;
	}

	public String toString()
	{
		return "ED with MultiDimension";
	}

}
