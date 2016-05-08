package common;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An object shared by all the threads which tracks the costs of various parts
 * of the simulation
 */
public class CostTracker {

	/**
	 * What stage of the simulation we are in, as we track cost for them
	 * separately Either we are incurring costs in the election of the leader, a
	 * broadcast from the leader, or a query to the leader
	 *
	 */
	public enum Stage {
		ELECTION, BROADCAST, QUERY
	}

	/**
	 * The costs, ConcurrentHashMap because it's being accessed by many threads.
	 * We store the costs by the process that's recording them so as to further
	 * ensure no race conditions
	 */
	private ConcurrentHashMap<Stage, ConcurrentHashMap<Integer, Double>> costs;

	/**
	 * Where the results should eventually be written
	 */
	private String outfile;

	/**
	 * Constructor for the CostTracker
	 * 
	 * @param ids
	 *            The ids of the various processes
	 * @param outfile
	 *            The outfile where things should be written
	 */
	public CostTracker(int[] ids, String outfile) {
		this.costs = new ConcurrentHashMap<Stage, ConcurrentHashMap<Integer, Double>>();
		for (Stage s : Stage.values()) {
			ConcurrentHashMap<Integer, Double> stageCosts = new ConcurrentHashMap<Integer, Double>();
			for (Integer id : ids) {
				stageCosts.put(id, 0.);
			}
			costs.put(s, stageCosts);
		}
		this.outfile = outfile;
	}

	/**
	 * Register costs to a particular stage by a particular processID
	 * 
	 * @param s
	 *            The stage we are in
	 * @param processID
	 *            The processID registering this cost
	 * @param cost
	 *            The cost to be registered
	 */
	public void registerCosts(Stage s, Integer processID, Double cost) {
		ConcurrentHashMap<Integer, Double> stageCosts = costs.get(s);
		stageCosts.put(processID, stageCosts.get(processID) + cost);
		costs.put(s, stageCosts);
	}

	/**
	 * Write out the costs to the designated file
	 */
	public void dumpCosts() {
		try (PrintWriter out = new PrintWriter(outfile)) {
			for (Stage s : Stage.values()) {
				ConcurrentHashMap<Integer, Double> stageCosts = costs.get(s);
				double sum = 0;
				for (Double d : stageCosts.values()) {
					sum += d;
				}
				out.println("The cost for stage " + s.name() + " is: " + Double.toString(sum));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}