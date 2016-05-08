package shortestpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import util.Pair;
import common.CostTracker;
import common.Message;
import mst.MSTProcess;

public class ShortestPathProcess extends MSTProcess {

	HashMap<Pair, ArrayList<Integer>> pd;
	
	public ShortestPathProcess(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
	}

	@Override
	public void processFinish(Message m) {
		
	}
	
	public void initiateAllPairs() {
		
	}


}
