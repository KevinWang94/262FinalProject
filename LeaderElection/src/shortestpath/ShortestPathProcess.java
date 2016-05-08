package shortestpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import util.Pair;
import util.PathInfo;
import common.CostTracker;
import common.Message;
import common.Message.MessageType;
import mst.MSTMessageContent;
import mst.MSTProcess;

public class ShortestPathProcess extends MSTProcess {

	public enum ShortestPathState {
		STATE_TRANSMIT,
		STATE_RECEIVING,
		STATE_SATURATED,
		STATE_UNASSIGNED
	}
	
	private ShortestPathState state;
	HashMap<Pair, PathInfo> pd;
	int count;
	HashSet<Integer> seen;
	
	public ShortestPathProcess(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		state = ShortestPathState.STATE_UNASSIGNED;
		pd = new HashMap<Pair, PathInfo>();
		count = 0;
		seen = new HashSet<Integer>();
	}

	@Override
	public void processFinish(Message m) {
		System.out.println(m.getSender() + " to " + id);
		
		initializePDMatrix();
		boolean isLeaf = passMessage(m.getType(), m.getContent());
		if (isLeaf) {
			state = ShortestPathState.STATE_TRANSMIT;
		} else {
			state = ShortestPathState.STATE_RECEIVING;
		}
		System.out.println(id + ": " + state);
		nodeAction();
	}
	
	public void initializePDMatrix() {
		for (int i = 0; i < allProcesses.length; i++) {
			for (int j = 0; j < allProcesses.length; j++) {
				int idI = allProcesses[i];
				int idJ = allProcesses[j];
				Pair pair = new Pair(idI, idJ);
				ArrayList<Integer> path = new ArrayList<Integer>();
				path.add(idI);
				double cost = 0;
				if (i != j) {
					if ((idI == id) || (idJ == id)) {
						cost = costs.get(idI).get(idJ);
						path.add(idJ);
					} else {
						cost = costs.get(idI).get(id);
						cost += costs.get(idJ).get(id);
						path.add(id);
						path.add(idJ);
					}
				}
				PathInfo pathInfo = new PathInfo(path, cost); 
				pd.put(pair, pathInfo);
			}
		}
	}
	
	public void nodeAction() {
		if (state == ShortestPathState.STATE_TRANSMIT) {
			transmittingNodeProcess();
		}
	}

	public void transmittingNodeProcess() {
		int sendId = -1;
		for (Integer i : se.keySet()) {
			if (!seen.contains(i)) {
				sendId = i;
				break;
			}
		}

		this.sendMessage(new Message(id, sendId, 
				MessageType.MSG_PATH_PARTIAL,
				new ShortestPathMessageContent(pd)));
	}
	
	public void processPathPartial(Message m) {
		if (count == numChildren)
	}
	
	public void processMessageSpecial(Message m) throws InterruptedException {
		// TODO: costs need to be registered here
		switch (m.getType()) {
			case MSG_PATH_PARTIAL:
				processPathPartial(m);
				break;
			case MSG_PATH_FINAL:
				processPathFinal(m);
				break;
			default:
				// TODO FAIL
		}
	}


}
