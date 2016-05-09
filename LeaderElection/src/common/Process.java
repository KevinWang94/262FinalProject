package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker.Stage;
import common.Message.MessageType;

public abstract class Process implements Runnable {
	protected boolean DEBUG = false;
	
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

	public abstract void triggerLeaderElection();
	public abstract void broadcast(MessageType messageType, MessageContent mc);
	public abstract void queryLeader(MessageContent mc);
	protected abstract void ackLeader();

	/* Message handling */
	protected abstract boolean processMessageSpecial(Message m);
	protected abstract void processMessageAckLeader();
	
	/************************************************************ 
	 * SIMPLE WORKLOAD
	 * leader broadcasts, and then others respond with one query 
	 ************************************************************/
		
	public void startRunningSimple() {
		assert(isLeader);
		broadcast(MessageType.MSG_LEADER_BROADCAST_SIMPLE, new MessageContent("Hello!"));
	}
	
	// TODO MICHELLE where to put this too
	protected abstract void processLeaderBroadcastSimple(Message m);

	protected void processLeaderBroadcastSimpleForReceiver(Message m) {
		assert(!isLeader);
		System.out.println(id + " has received!");
		queryLeader(new MessageContent("Why are you talking to me?"));
	}

	protected abstract boolean processQuerySimple(Message m);
	
	int numSimpleQueriesReceived = 0;
	protected boolean processQuerySimpleForLeader(Message m) {
		numSimpleQueriesReceived++;
		if (numSimpleQueriesReceived == allProcesses.length - 1) {
			costTracker.dumpCosts();
			System.out.println("All queries received!");
			for (int i = 0; i < allProcesses.length; i++) {
				if (id != allProcesses[i]) {
					sendMessage(new Message(id, allProcesses[i], MessageType.MSG_KILL, null));
				}
			}
			return true;
		}
		return false;
	}

	protected void registerCost(Message m) {
		Stage s = null;
		switch (m.getType()) {
		case MSG_MST_CONNECT:
		case MSG_MST_ACCEPT:
		case MSG_MST_REJECT:
		case MSG_MST_REPORT:
		case MSG_MST_CHANGEROOT:
		case MSG_MST_INITIATE:
		case MSG_MST_TEST:
		case MSG_MST_FINISH:
		case MSG_BASELINE_ELECT_LEADER:
		case MSG_ACK_LEADER:
		case MSG_PATH_PARTIAL:
		case MSG_PATH_FINAL:
			s = Stage.ELECTION;
			break;
		case MSG_LEADER_BROADCAST_SIMPLE:
			s = Stage.BROADCAST;
			break;
		case MSG_QUERY_SIMPLE:
			s = Stage.QUERY;
			break;
		case MSG_KILL:
			break;
		}
		if (m.getType() != MessageType.MSG_KILL && id != m.getReceiver()) {
			this.costTracker.registerCosts(s, id, costs.get(id).get(m.getReceiver()));
		}
	}
	
	protected void dumpCosts() {
		
	}
	
	protected boolean processMessage(Message m) {
		boolean finished = false;
		switch (m.getType()) {
		case MSG_ACK_LEADER:
			processMessageAckLeader();
			break;
		case MSG_LEADER_BROADCAST_SIMPLE:
			processLeaderBroadcastSimple(m);
			break;
		case MSG_QUERY_SIMPLE:
			finished = processQuerySimple(m);
			break;
		case MSG_KILL:
			finished = true;
			break;
		default:
			processMessageSpecial(m);
			break;
		}
		return finished;
	}
	
	
	/* 
	 * This is not what you think it is. lol. it is a helper very private very secret DO NOT INVOKE
	 */
	public void sendMessage(Message m) {
		registerCost(m);
		try {
			BlockingQueue<Message> queue = queues.get(m.getReceiver());
			queue.put(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean checkForMessages() throws InterruptedException {
		Message m = incomingMessages.poll();
		if (m == null) {
			return false;
		}
		return processMessage(m);
	}

	@Override
	public void run() {
		boolean done = false;
		while (!done) {
			try {
				done = checkForMessages();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
