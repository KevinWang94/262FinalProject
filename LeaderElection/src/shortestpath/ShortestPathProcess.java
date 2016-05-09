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
import common.MessageContent;
import mst.MSTBase;
import mst.MSTProcess;

public class ShortestPathProcess extends MSTBase {
	public enum ShortestPathState {
		STATE_TRANSMIT,
		STATE_RECEIVING,
		STATE_SATURATED,
		STATE_UNASSIGNED
	}
	
	public enum LeaderMethod {
		METHOD_MAX,
		METHOD_SUM
	}
	
	private ShortestPathState state;
	HashMap<Pair, PathInfo> pd;
	int count;
	HashSet<Integer> seen;
	private LeaderMethod method;
	int acksReceived = 0;
	
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
		method = LeaderMethod.METHOD_SUM;
	}

	@Override
	public void processFinish(Message m) {
		initializePDMatrix();
		boolean isLeaf = passMessageMST(m.getType(), m.getContent());
		if (isLeaf) {
			state = ShortestPathState.STATE_TRANSMIT;
		} else {
			state = ShortestPathState.STATE_RECEIVING;
		}
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
			if ((se.get(i) == MSTProcess.SE_BRANCH) && 
					(!seen.contains(i))) {
				sendId = i;
				break;
			}
		}
		System.out.println("Transmitting partial distance matrix.");
		this.sendMessage(new Message(id, sendId, 
				MessageType.MSG_PATH_PARTIAL,
				new ShortestPathMessageContent(pd)));
	}
	
	public void augmentPd(HashMap<Pair, PathInfo> newPd) {
		for (Pair pair : newPd.keySet()) {
			if (newPd.get(pair).getCost() < pd.get(pair).getCost()) {
				pd.put(pair, newPd.get(pair));
			}
		}
		for (Pair pair : pd.keySet()) {
			for (Integer i : allProcesses) {
				int fst = pair.getFst();
				int snd = pair.getSnd();
				Pair pair1 = new Pair(fst, i);
				Pair pair2 = new Pair(i, snd);
				double cost1 = pd.get(pair1).getCost();
				double cost2 = pd.get(pair2).getCost();
				double newCost = cost1 + cost2;
				if (newCost < pd.get(pair).getCost()) {
					ArrayList<Integer> newPath = pd.get(pair1).getPath();
					newPath.addAll(pd.get(pair2).getPath());
					pd.put(pair, new PathInfo(newPath, newCost));
				}
			}
		}
	}
	
	public void chooseLeader() {
		double bestVal = Double.MAX_VALUE; 
		int bestId = -1;
		for (Integer i : allProcesses) {
			double val = (method == LeaderMethod.METHOD_MAX) ? -1 : 0; 
			for (Pair pair : pd.keySet()) {
				if (pair.getFst() == i) {
					double newCost = pd.get(pair).getCost();
					if (method == LeaderMethod.METHOD_MAX) {
						if (newCost > val) {
							val = newCost;
						}
					} else {
						val += newCost;
					}
				}
			}
			if (val < bestVal) {
				bestVal = val;
				bestId = i;
			}
 		}

		leaderId = bestId;
		isLeader = (leaderId == id);
	}
	
	public void printDebugInfo() {
		System.out.println("Leader: " + leaderId);
		System.out.println("Costs: ");
		for (Integer i : costs.keySet()) {
			for (Integer j : costs.get(i).keySet()) {
				System.out.println(i + " " + j + " " + costs.get(i).get(j));
			}
		}
		System.out.println("Path Costs: ");
		for (Integer i : costs.keySet()) {
			for (Integer j : costs.get(i).keySet()) {
				Pair pair = new Pair(i,j);
				System.out.println(i + " " + j + " " + pd.get(pair).getCost());
			}
		}
	}
	
	public void sendFinalPaths(int noSendId) {
		for (Integer i : se.keySet()) {
			if ((se.get(i) == MSTProcess.SE_BRANCH) && (i != noSendId)) {
				this.sendMessage(new Message(id, i, MessageType.MSG_PATH_FINAL,
						new ShortestPathMessageContent(pd)));				
			}
		}
		chooseLeader();
		ackLeader();
	}
	
	public void processPathPartial(Message m) {
		ShortestPathMessageContent mContent = (ShortestPathMessageContent) m.getContent();
		augmentPd(mContent.getPaths());
		count++;
		seen.add(m.getSender());
		if (count == numBranch - 1) {
			state = ShortestPathState.STATE_TRANSMIT;
			transmittingNodeProcess();
		}
		if (count == numBranch) {
			state = ShortestPathState.STATE_SATURATED;
			if (DEBUG) 
				printDebugInfo();
			sendFinalPaths(-1);
		}
	}
	
	public void processPathFinal(Message m) {
		ShortestPathMessageContent mContent = (ShortestPathMessageContent) m.getContent();
		HashMap<Pair, PathInfo> paths = mContent.getPaths();
		pd = paths;
		sendFinalPaths(m.getSender());
	}
	
	public boolean processMessageSpecial(Message m) {
		boolean done = super.processMessageSpecial(m);
		if (done) 
			return true;
		switch (m.getType()) {
			case MSG_PATH_PARTIAL:
				processPathPartial(m);
				System.out.println("Processing path partial from " + m.getSender() + " to " + m.getReceiver());
				return true;
			case MSG_PATH_FINAL:
				processPathFinal(m);
				System.out.println("Processing path final from " + m.getSender() + " to " + m.getReceiver());
				return true;
			default:
				return false;
		}
	}

	@Override
	public void triggerLeaderElection() {
		wakeup();
	}
	
	protected void passMessagePath(MessageType messageType, MessageContent m, int sender) {
		for (Integer nextId : se.keySet()) {
			if ((nextId != sender) && se.get(nextId) == SE_BRANCH) {
				this.sendMessage(new Message(id, nextId, messageType, m));
			}
		}
	}

	@Override
	public void broadcast(MessageType messageType, MessageContent mc) {
		assert (id == this.leaderId);
		passMessagePath(messageType, mc, -1);
	}

	@Override
	public void queryLeader(MessageContent mc) {
		assert(!isLeader);
		
		ArrayList<Integer> path = pd.get(new Pair(id, leaderId)).getPath();
		sendMessage(new Message(id, path.get(1), MessageType.MSG_QUERY_SIMPLE, mc));
	}
	
	@Override
	protected void ackLeader() {
		if (!isLeader) {
			ArrayList<Integer> path = pd.get(new Pair(id, leaderId)).getPath();
			sendMessage(new Message(id, path.get(1), MessageType.MSG_ACK_LEADER, null));
		} else {
			acksReceived++;
			if (acksReceived == numBranch) {
				System.out.println("Leader acked!");
				startRunningSimple();			
			}
		}
	}

	@Override
	protected void processMessageAckLeader() {
		ackLeader();
	}

	@Override
	protected void processLeaderBroadcastSimple(Message m) {
		assert(!isLeader);
		passMessagePath(m.getType(), m.getContent(), m.getSender());
		super.processLeaderBroadcastSimpleForReceiver(m);
	}

	@Override
	protected boolean processQuerySimple(Message m) {
		if (id == leaderId) {
			return super.processQuerySimpleForLeader(m);
		} else {
			queryLeader(m.getContent());
			return false;
		}
	}
}
