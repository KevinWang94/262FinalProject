package common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CostTracker {
	
	public enum Stage {
		ELECTION, BROADCAST, QUERY, RESPONSE
	}
	
	ConcurrentHashMap<Stage, ConcurrentHashMap<Integer, Integer>> costs;
	
	public CostTracker(int[] ids) {
		for(Stage s : Stage.values()) {
			ConcurrentHashMap<Integer, Integer> stageCosts = new ConcurrentHashMap<Integer, Integer>();
			for(Integer id : ids) {
				stageCosts.put(id, 0);
			}
			costs.put(s, stageCosts);
		}
	}
	
	public void registerCosts(Stage s, Integer processID, Integer cost) {
		ConcurrentHashMap<Integer, Integer> stageCosts = costs.get(s);
		stageCosts.put(processID, stageCosts.get(processID) + cost);
		costs.put(s, stageCosts);
	}

}
