package mst;

import java.util.HashMap;
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
	int[] se;
	int findCount = 0;

	public MSTProcess(int id, int[] allProcesses, double[] costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
		this.ln = 0;
		this.sn = SN_SLEEPING;
		this.se = new int[costs.length];
		for (int i = 0; i < this.se.length; i++) {
			this.se[i] = SE_BASIC;
		}
	}

	public void wakeup() throws InterruptedException {
		int minEdge = getMinEdge();
		se[minEdge] = SE_BRANCH;
		sn = SN_FOUND;
		float[] args = new float[1];
		args[0] = 0;
		this.sendMessage(allProcesses[minEdge], new Message(id,
				allProcesses[minEdge], new MSTMessageContent(
						MSTMessageContent.MSG_CONNECT, args)));
	}
	
	@Override
	public void processMessage(Message m) {
		MSTMessageContent msg = (MSTMessageContent) m.getContent();
		if (msg.getType() == MSTMessageContent.MSG_CONNECT) {
			processConnect(msg.getArgs());
		}
		
	}
	
	public void processConnect(float[] args) {
		
	}

	/**
	 * Gets the index of the minimum adjacent edge.
	 * 
	 * @return
	 */
	public int getMinEdge() {
		int minEdge = -1;
		double minCost = Double.MAX_VALUE;
		for (int i = 0; i < costs.length; i++) {
			if (allProcesses[i] != id && costs[i] < minCost) {
				minEdge = i;
				minCost = costs[i];
			}
		}
		return minEdge;
	}

}
