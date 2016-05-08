package util;

import java.util.ArrayList;

public class PathInfo {
	private ArrayList<Integer> path;
	private double cost;
	
	public PathInfo(ArrayList<Integer> path, double cost) {
		this.setPath(path);
		this.setCost(cost);
	}

	public ArrayList<Integer> getPath() {
		return path;
	}

	public void setPath(ArrayList<Integer> path) {
		this.path = path;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}
	
	
}
