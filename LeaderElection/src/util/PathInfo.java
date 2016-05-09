package util;

import java.util.ArrayList;

/**
 * A class representing the topology and cost of a path traversed by a message
 */
public class PathInfo {
	/**
	 * The IDs of the nodes (processes) along the path, in order of traversal
	 */
	private ArrayList<Integer> path;
	/**
	 * the path's cost
	 */
	private double cost;

	/**
	 * Simple constructor
	 * 
	 * @param path
	 * @param cost
	 */
	public PathInfo(ArrayList<Integer> path, double cost) {
		this.setPath(path);
		this.setCost(cost);
	}

	/**
	 * Simple getter
	 * 
	 * @return list of IDs of processes that the path includes
	 */
	public ArrayList<Integer> getPath() {
		return path;
	}

	/**
	 * Simple setter
	 * 
	 * @param path
	 */
	public void setPath(ArrayList<Integer> path) {
		this.path = path;
	}

	/**
	 * Simple getter
	 * 
	 * @return cost
	 */
	public double getCost() {
		return cost;
	}

	/**
	 * Simple setter
	 * 
	 * @param cost
	 */
	public void setCost(double cost) {
		this.cost = cost;
	}
}
