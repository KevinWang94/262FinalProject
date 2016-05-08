package common;

/**
 * This is our main class, running desire simulations and dumping results out to files
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import shortestpath.ShortestPathProcess;
import mst.MSTProcess;
import baseline.BaselineProcess;

public class ElectionRunner {
	
	/**
	 * What model we're running
	 */
	public enum Model {
		BASELINE, MST, SHORTESTPATH
	}

	/**
	 * Helper function for updating costs
	 * @param costs	costs map
	 * @param i		from node
	 * @param j		to node
	 * @param cost	cost it should be
	 * @return	updated cost map
	 */
	private static HashMap<Integer, HashMap<Integer, Double>> addToCosts(
			HashMap<Integer, HashMap<Integer, Double>> costs, int i, int j, double cost) {
		if (costs.containsKey(i)) {
			costs.get(i).put(j, cost);
		} else {
			HashMap<Integer, Double> newMap = new HashMap<Integer, Double>();
			newMap.put(j, cost);
			costs.put(i, newMap);
		}
		return costs;
	}
	
	/**
	 * Generate an array of ids
	 * @param numProcesses	the number of ids we must generated
	 * @return	the generated ids
	 */
	public static int[] genIds(int numProcesses) {
		HashSet<Integer> seen = new HashSet<Integer>();
		int[] ids = new int[numProcesses];
		for (int i = 0; i < numProcesses; i++) {
			// TODO: better generation of random id
			int id = (int) (Math.random() * 1000);
			while (seen.contains(id)) {
				id = (int) (Math.random() * 1000);
			}

			ids[i] = id;
		}
		return ids;
	}
	
	/**
	 * Randomly generate edge costs
	 * @param ids	the ids of the processes
	 * @return		the costs that have been generated, a mapping from in node to out node to cost
	 */
	public static HashMap<Integer, HashMap<Integer, Double>> genCosts(int[] ids) {
		HashMap<Integer, HashMap<Integer, Double>> costs = new HashMap<Integer, HashMap<Integer, Double>>();

		for (int i = 0; i < ids.length; i++) {
			for (int j = 0; j < i; j++) {
				double cost = Math.random() * 10;
				costs = addToCosts(costs, ids[i], ids[j], cost);
				costs = addToCosts(costs, ids[j], ids[i], cost);
			}
		}
		
		return costs;
	}

	/**
	 * Instantiate and run a simulation
	 * @param ids		the ids generated above
	 * @param costs		the randomly generated costs
	 * @param m			the model
	 * @param outfile	the outfile we should write results to
	 */
	public static void instantiateAndRun(int[] ids, HashMap<Integer, HashMap<Integer, Double>> costs, Model m, String outfile) {
		HashMap<Integer, LinkedBlockingQueue<Message>> queues = new HashMap<Integer, LinkedBlockingQueue<Message>>();
		HashMap<Integer, Process> processes = new HashMap<Integer, Process>();

		for (int i = 0; i < ids.length; i++) {
			queues.put(ids[i], new LinkedBlockingQueue<Message>());
		}

		CostTracker tracker = new CostTracker(ids, outfile);
		
		for (int i = 0; i < ids.length; i++) {
			// System.out.println(ids[i]);
		}

		for (int i = 0; i < ids.length; i++) {
			Process curr = null;
			switch (m) {
			case MST:
				curr = new MSTProcess(ids[i], ids, costs, queues, queues.get(ids[i]), tracker);
				break;
			case BASELINE:
				curr = new BaselineProcess(ids[i], ids, costs, queues, queues.get(ids[i]), tracker);
				break;
			case SHORTESTPATH:
				curr = new ShortestPathProcess(ids[i], ids, costs, queues, queues.get(ids[i]), tracker);
				break;
			}
			(new Thread(curr)).start();
			processes.put(ids[i], curr);
		}

		processes.get(ids[0]).triggerLeaderElection();
	}
	
	/**
	 * Main driver method
	 * 
	 * @param args
	 * 		args[0] is the number of processes desired
	 * 		args[1], [2], and [3] are the outfiles for MST, Baseline, and ShortestPath
	 */
	public static void main(String[] args) {
		int[] ids = genIds(Integer.parseInt(args[0]));
		HashMap<Integer, HashMap<Integer, Double>> costs = genCosts(ids);
		//instantiateAndRun(ids, costs, Model.MST, args[1]);
		instantiateAndRun(ids, costs, Model.BASELINE, args[2]);
		//instantiateAndRun(ids, costs, Model.SHORTESTPATH, args[3]);	
	}
}
