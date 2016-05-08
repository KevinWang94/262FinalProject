package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker.Stage;
import common.Message.MessageType;

public abstract class Process implements Runnable {
	protected static final int ID_NONE = -1;

	protected HashMap<Integer, LinkedBlockingQueue<Message>> queues;
	protected int[] allProcesses;
	protected HashMap<Integer, HashMap<Integer, Double>> costs;
	protected LinkedBlockingQueue<Message> incomingMessages;
	protected int id;
	protected int leaderId;
	protected CostTracker costTracker;
	
	/* true iff this process has computed itself as leader */
	protected boolean isLeader;

	public Process(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		this.queues = queues;
		this.incomingMessages = incomingMessages;
		this.id = id;
		this.allProcesses = allProcesses;
		this.costs = costs;
		this.leaderId = Process.ID_NONE;
		this.isLeader = false;
		this.costTracker = costTracker;
	}

	public abstract void triggerLeaderElection() throws InterruptedException;
	public abstract void broadcast(MessageType messageType, MessageContent mc) throws InterruptedException;
	public abstract void queryLeader(MessageType messageType, MessageContent mc) throws InterruptedException;
	protected abstract void ackLeader() throws InterruptedException;

	/* Message handling */
	protected abstract void processMessageAckLeader() throws InterruptedException;
	protected abstract void processMessageSpecial(Message m) throws InterruptedException;
	
	
	/************************************************************ 
	 * SIMPLE WORKLOAD
	 * leader broadcasts, and then others respond with one query 
	 ************************************************************/
		
	public void startRunningSimple() throws InterruptedException {
		assert(isLeader);
		broadcast(MessageType.MSG_LEADER_BROADCAST_SIMPLE, new MessageContent("Hello!"));
	}
	
	private void processLeaderBroadcastSimple(Message m) throws InterruptedException {
		assert(!isLeader);
		queryLeader(MessageType.MSG_QUERY_SIMPLE, new MessageContent("Why are you talking to me?"));
	}
	
	int numSimpleQueriesReceived = 0;
	private void processQuerySimple(Message m) throws InterruptedException {
		numSimpleQueriesReceived++;
		if (numSimpleQueriesReceived == allProcesses.length - 1) {
			costTracker.dumpCosts();
		}
	}

	protected void registerCost(Stage s, Message m) {
		this.costTracker.registerCosts(s, id, costs.get(id).get(m.getSender()));
	}
	
	protected void dumpCosts() {
		
	}
	
	protected void processMessage(Message m) throws InterruptedException {
		switch (m.getType()) {
		case MSG_ACK_LEADER:
			registerCost(Stage.ELECTION, m);
			processMessageAckLeader();
			break;
		case MSG_LEADER_BROADCAST_SIMPLE:
			processLeaderBroadcastSimple(m);
			break;
		case MSG_QUERY_SIMPLE:
			processQuerySimple(m);
			break;
		default:
			processMessageSpecial(m);
			break;
		}
	}
	
	
	/* 
	 * This is not what you think it is. lol. it is a helper very private very secret DO NOT INVOKE
	 */
	public void sendMessage(Message m) {
		try {
			BlockingQueue<Message> queue = queues.get(m.getReciever());
			queue.put(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void checkForMessages() throws InterruptedException {
		Message m = incomingMessages.poll();
		if (m == null) {
			return;
		}
		processMessage(m);
		return;
	}

	@Override
	public void run() {
		while (true) {
			try {
				checkForMessages();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
