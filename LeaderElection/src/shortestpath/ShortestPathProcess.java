package shortestpath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.MessageContent;
import common.Message.MessageType;
import mst.MSTMessageContent;
import mst.MSTProcess;

public class ShortestPathProcess extends MSTProcess {

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
			CostTracker costTracker, String outfile) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker, outfile);
	}

	@Override
	public void processFinish(Message m) {
		boolean isLeaf = passMessage(m.getType(), m.getContent());
		if (isLeaf) {
			state = ShortestPathState.STATE_TRANSMIT;
		} else {
			state = ShortestPathState.STATE_RECEIVING;
		}
	}
	
	public void initiateAllPairs() {
		
	}


}
