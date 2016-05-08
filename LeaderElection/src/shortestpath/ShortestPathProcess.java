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

	public enum ShortestPathState {
		STATE_TRANSMIT,
		STATE_RECEIVING,
		STATE_SATURATED,
	}
	
	private ShortestPathState state;
		
	public ShortestPathProcess(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
	}

	@Override
	public void processFinish(Message m) {
		System.out.println(m.getSender() + " to " + id);
		
		boolean isLeaf = passMessage(m.getType(), m.getContent());
		if (isLeaf) {
			state = ShortestPathState.STATE_TRANSMIT;
		} else {
			state = ShortestPathState.STATE_RECEIVING;
		}
		System.out.println(id + ": " + state);
	}
	
	public void initiateAllPairs() {
		
	}


}
