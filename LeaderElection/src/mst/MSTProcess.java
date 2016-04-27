package mst;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.Process;

public class MSTProcess extends Process {
	public static final int SN_SLEEPING = 1;
	public static final int SN_FIND = 2;
	public static final int SN_FOUND = 3;

	public static final int SE_BASIC = 1;
	public static final int SE_BRANCH = 2;
	public static final int SE_REJECTED = 3;

	int ln;
	int sn;
	HashMap<Integer, Integer> se;
	int findCount = 0;
	int testEdge = -1; // -1 is used as nil value
	double bestWt = -1;
	int bestEdge = -1;
	int inBranch = -1;
	
	public MSTProcess(int id, int[] allProcesses, 
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
		this.ln = 0;
		this.sn = SN_SLEEPING;
		this.se = new HashMap<Integer, Integer>();
		Iterator<Integer> it = costs.get(id).keySet().iterator();
		while (it.hasNext()) {
			int nextId = it.next();
			this.se.put(nextId, SE_BASIC);
		}
	}
	
	@Override
	public void broadcast() throws InterruptedException {
		
	}
		
	/**
	 * Gets the index of the minimum adjacent edge.
	 * 
	 * @return
	 */
	public int getMinEdge() {
		int minEdge = -1;
		double minCost = Double.MAX_VALUE;
		HashMap<Integer, Double> edgeCosts = costs.get(id);
		Iterator<Map.Entry<Integer, Double>> it = edgeCosts.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Double> pair = it.next();
			if (pair.getValue() < minCost) {
				minEdge = pair.getKey();
				minCost = pair.getValue();
			}
		}
		return minEdge;
	}
	
	public void wakeup() throws InterruptedException {
		int minEdge = getMinEdge();
		se.put(minEdge, SE_BRANCH);
		sn = SN_FOUND;
		double[] args = new double[1];
		args[0] = 0;
		this.sendMessage(allProcesses[minEdge], new Message(id,
				allProcesses[minEdge], new MSTMessageContent(
						MSTMessageContent.MSG_CONNECT, args)));
	}
	
	public void processConnect(double[] args) {
		
	}

	public void processAccept(int sender) {
		testEdge = -1;
		double newCost = costs.get(id).get(sender);
		if (newCost < bestWt) {
			bestEdge = sender;
			bestWt = newCost;
			report();
		}
	}
	
	public void processReject(int sender) {
		if (se.get(sender) == SE_BASIC) {
			se.put(sender, SE_REJECTED);
			test();
		}
	}
	
	public void processReport(Message m) {
		MSTMessageContent msg = (MSTMessageContent) m.getContent();
		double w = (msg.getArgs())[0];
		int sender = m.getSender();
		if (sender != inBranch) {
			findCount -= 1;
			if (w < bestWt) {
				bestWt = w;
				bestEdge = sender;
			}
			report();
		} else {
			if (sn == SN_FIND) {
				try {
					incomingMessages.put(m);
				} catch(InterruptedException e) {
					System.err.println("Failed to find incoming messages\n");
				}
			} else {
				if (w > bestWt) {
					changeRoot();
				} else if ((w == bestWt) && (w == Double.MAX_VALUE)) {
					int leader = Math.min(id, sender);
					
				}
			}
		}
	}

	public void changeRoot() {
		if (se.get(bestEdge) == SE_BRANCH) {
			try {
				this.sendMessage(bestEdge, new Message(id,
					bestEdge, new MSTMessageContent(
							MSTMessageContent.MSG_CHANGEROOT, null)));
			} catch(InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		} else {
			double[] args = new double[1];
			args[0] = ln;
			try {
				this.sendMessage(bestEdge, new Message(id,
					bestEdge, new MSTMessageContent(
							MSTMessageContent.MSG_CONNECT, args)));
			} catch(InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}	
			se.put(bestEdge, SE_BRANCH);
		}
	}

	public void processChangeRoot() {
		changeRoot();
	}
	
	public void test() {
		
	}
	
	public void report() {
		if (findCount == 0 && testEdge == -1) {
			sn = SN_FOUND;
			double[] args = new double[1];
			args[0] = bestWt;
			try {
				this.sendMessage(inBranch, new Message(id,
					inBranch, new MSTMessageContent(
							MSTMessageContent.MSG_REPORT, args)));
			} catch(InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		}
	}

	@Override
	public void processMessage(Message m) {
		MSTMessageContent msg = (MSTMessageContent) m.getContent();
		if (msg.getType() == MSTMessageContent.MSG_CONNECT) {
			processConnect(msg.getArgs());
		}
		if (msg.getType() == MSTMessageContent.MSG_ACCEPT) {
			processAccept(m.getSender());
		}
		if (msg.getType() == MSTMessageContent.MSG_REJECT) {
			processReject(m.getSender());
		}
		if (msg.getType() == MSTMessageContent.MSG_REPORT) {
			processReport(m);
		}
		if (msg.getType() == MSTMessageContent.MSG_CHANGEROOT) {
			processChangeRoot();
		}
	}
}
