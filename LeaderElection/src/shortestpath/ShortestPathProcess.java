package shortestpath;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import mst.MSTMessageContent;
import mst.MSTProcess;

public class ShortestPathProcess extends MSTProcess {

	public ShortestPathProcess(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker, String outfile) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker, outfile);
	}

	@Override
	public void processFinish(Message m) {
		
	}
	
	public void initiateAllPairs() {
		
	}


}
