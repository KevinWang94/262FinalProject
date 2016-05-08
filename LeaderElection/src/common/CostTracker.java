package common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CostTracker {
	
	public enum Stage {
		ELECTION, BROADCAST, QUERY, RESPONSE
	}
	
	ConcurrentHashMap<Stage, ConcurrentHashMap<Integer, Double>> costs;
	
	public CostTracker(int[] ids) {
		for(Stage s : Stage.values()) {
			ConcurrentHashMap<Integer, Double> stageCosts = new ConcurrentHashMap<Integer, Double>();
			for(Integer id : ids) {
				stageCosts.put(id, 0.);
			}
			costs.put(s, stageCosts);
		}
	}
	
	public void registerCosts(Stage s, Integer processID, Double cost) {
		ConcurrentHashMap<Integer, Double> stageCosts = costs.get(s);
		stageCosts.put(processID, stageCosts.get(processID) + cost);
		costs.put(s, stageCosts);
	}
	
	public void printCosts() {
		for(Stage s : Stage.values()) {
			ConcurrentHashMap<Integer, Double> stageCosts = costs.get(s);
			double sum = 0;
			for(Double d : stageCosts.values()) {
				sum += d;
			}
			System.out.println("The cost for stage " + s.name() + " is: " + Double.toString(sum));
		}
	}
}