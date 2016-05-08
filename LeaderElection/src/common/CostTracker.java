package common;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

public class CostTracker {
	
	public enum Stage {
		ELECTION, BROADCAST, QUERY, RESPONSE
	}
	
	ConcurrentHashMap<Stage, ConcurrentHashMap<Integer, Double>> costs;
	String outfile;
	
	public CostTracker(int[] ids, String outfile) {
		this.costs = new ConcurrentHashMap<Stage, ConcurrentHashMap<Integer, Double>>();
		for(Stage s : Stage.values()) {
			ConcurrentHashMap<Integer, Double> stageCosts = new ConcurrentHashMap<Integer, Double>();
			for(Integer id : ids) {
				stageCosts.put(id, 0.);
			}
			costs.put(s, stageCosts);
		}
		this.outfile = outfile;
	}	
	
	public void registerCosts(Stage s, Integer processID, Double cost) {
		ConcurrentHashMap<Integer, Double> stageCosts = costs.get(s);
		stageCosts.put(processID, stageCosts.get(processID) + cost);
		costs.put(s, stageCosts);
	}
	
	public void dumpCosts() {
		try(PrintWriter out = new PrintWriter(outfile)) {
			for(Stage s : Stage.values()) {
				ConcurrentHashMap<Integer, Double> stageCosts = costs.get(s);
				double sum = 0;
				for(Double d : stageCosts.values()) {
					sum += d;
				}
				out.println("The cost for stage " + s.name() + " is: " + Double.toString(sum));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 		
	}
}