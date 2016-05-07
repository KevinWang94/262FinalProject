package common;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import mst.MSTProcess;

public abstract class ElectionRunner {

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

	public static void instantiateAndRun(int numProcesses) {
		HashMap<Integer, LinkedBlockingQueue<Message>> queues = new HashMap<Integer, LinkedBlockingQueue<Message>>();
		HashMap<Integer, Process> processes = new HashMap<Integer, Process>();
		int[] ids = new int[numProcesses];
		HashMap<Integer, HashMap<Integer, Double>> costs = new HashMap<Integer, HashMap<Integer, Double>>();

		for (int i = 0; i < numProcesses; i++) {
			// TODO: better generation of random id
			int id = (int) (Math.random() * 1000);
			while (queues.containsKey(id)) {
				id = (int) (Math.random() * 1000);
			}

			ids[i] = id;
			for (int j = 0; j < i; j++) {
				double cost = Math.random() * 10;
				costs = addToCosts(costs, ids[i], ids[j], cost);
				costs = addToCosts(costs, ids[j], ids[i], cost);
			}
			queues.put(id, new LinkedBlockingQueue<Message>());
		}

		for (int i = 0; i < numProcesses; i++) {
			// System.out.println(ids[i]);
		}

		for (int i = 0; i < numProcesses; i++) {
			Process curr = new MSTProcess(ids[i], ids, costs, queues, queues.get(ids[i]));
			(new Thread(curr)).start();
			processes.put(ids[i], curr);
		}

		try {
			processes.get(ids[0]).triggerLeaderElection();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// for (int i = 0; i < n; i++) {
		// Process curr = processes.get(ids[i]);
		// try {
		// curr.broadcast(null);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
	}
}
