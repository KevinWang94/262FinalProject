package common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import mst.MSTProcess;
import baseline.BaselineProcess;

public class ElectionRunner {
	
	public enum Model {
		BASELINE, MST
	}

	public static HashMap<Integer, HashMap<Integer, Double>> addToCosts(
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
			}
			(new Thread(curr)).start();
			processes.put(ids[i], curr);
		}

		try {
			processes.get(ids[0]).triggerLeaderElection();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		int[] ids = genIds(Integer.parseInt(args[0]));
		HashMap<Integer, HashMap<Integer, Double>> costs = genCosts(ids);
		instantiateAndRun(ids, costs, Model.MST, args[1]);
		//instantiateAndRun(ids, costs, Model.BASELINE, args[2]);
	}
}
