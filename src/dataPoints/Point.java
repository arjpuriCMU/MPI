package dataPoints;

import java.util.List;

public class Point {
	public Double x;
	public Double y;
	
	public Point(Double x, Double y){
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof Point)){
			return false;
		}
		Point new_point = (Point) o;
		if (this.x.equals(new_point.x) || this.y.equals(new_point.y)){
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode(){
		Double x = new Double(this.x);
		Double y = new Double(this.y);
		return x.hashCode() + y.hashCode();
	}
	
	public Double distance(Point p){
		Double val1 = Math.pow(x - p.x,2);
		Double val2 = Math.pow(y-p.y, 2);
		return Math.sqrt(val1+val2);
	}
	
	public static Point mean(List<Point> points){
		Double sum_x = 0.0;
		Double sum_y = 0.0;
		for (Point p : points){
			sum_x += p.x;
			sum_y += p.y;
		}
		return new Point(sum_x/points.size(), sum_y/points.size());
	}

	
	public int closetPointIndex(Point[] centroids) {
		Double temp_distance;
		Double min_distance = Double.MAX_VALUE;
		int min_index = 0;
		Point p;
		for (int i = 0; i < centroids.length; i++){
			p = centroids[i];
			temp_distance = this.distance(p); 
			if (temp_distance < min_distance){
				min_distance = temp_distance;
				min_index = i;
			}
		}
		return min_index;
	}
	
}
