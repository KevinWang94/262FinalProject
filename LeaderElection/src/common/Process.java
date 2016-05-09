package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker.Stage;
import common.Message.MessageType;

/**
 * This is an abstract class simulating a process in a distributed system. 
 * The processes are programmed to do the following simple test workload:
 *   1. They elect a leader.
 *   2. The leader broadcasts to the others.
 *   3. The others respond to the leader with a basic query.
 *   
 * The communication-related algorithms for electing a leader, broadcasting,
 * and querying the leader must be implemented by subclasses of Process.
 */
public abstract class Process implements Runnable {
	
	// INSTANCE FIELDS ////////////////////////////////////////////////////////////
	/** 
	 * Uninitialized default ID, used to signal that no leader has been elected
	 */
	protected static final int ID_NONE = -1;
	/** 
	 * The ID of this process. This can be thought of a networking ID: it associates
	 * a process with its message queue. It is randomly chosen during simulation 
	 * initialization.
	 */
	protected int id;
	/**
	 * The ID of the leader; initialized after leader election is complete.
	 */
	protected int leaderId;
	/**
	 * Array of IDs of all processes, indexed in order of initialization.
	 */
	protected int[] allProcesses;

	
	/**
	 * FIFO queue of incoming messages for this process
	 */
	protected LinkedBlockingQueue<Message> incomingMessages;
	/**
	 * Map of ID to message queue for all processes, used by processes to send
	 * messages to one another. 
	 */
	protected HashMap<Integer, LinkedBlockingQueue<Message>> queues;
	
	/**
	 * This maps pairs of processes to the cost of direct communication between them.
	 * Specifically, for any processes with IDs x and y, this maps x to a second map,
	 * which in turn maps y to the cost of communication between processes x and y.
	 * That is, the second layer map specifies all communication costs between x and 
	 * the other processes, indexed by their IDs.
	 * 
	 * Costs are symmetric. Therefore, {@code costs.get[x].get[y] == costs.get[y].get[x]}. 
	 */
	protected HashMap<Integer, HashMap<Integer, Double>> costs;
	/**
	 * TODO KEVIN ADD COMMENT
	 */
	protected CostTracker costTracker;
	
	/**
	 * Indicates whether this process has been elected leader in the system.
	 */
	protected boolean isLeader;

	/**
	 * The number of queries received by this process during execution of the
	 * test workload. Initialized to 0.
	 */
	int numSimpleQueriesReceived;

	
	// CONSTRUCTOR ////////////////////////////////////////////////////////////
	/**
	 * Basic constructor. Fields pertaining to leaders are initialized to reflect 
	 * that no leader has yet been elected in the system.
	 * 
	 * @param id
	 * @param allProcesses
	 * @param costs
	 * @param queues
	 * @param incomingMessages
	 * @param costTracker
	 */
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
		this.numSimpleQueriesReceived = 0;

	}

	// OUTGOING MESSAGES ///////////////////////////////////////////////////////
	/** 
	 * Initiates the leader election. Implementation is determined by subclass.
	 */
	public abstract void triggerLeaderElection();
	/**
	 * During leader election, once a process recognizes the identity of the leader, it
	 * send a message to the leader acknowledging the fact. This allows the leader to 
	 * wait for all processes to complete leader election before proceeding with the 
	 * workload.
	 * 
	 * This function sends the acknowledge message; it should only be invoked by non-leader 
	 * processes. Implementation is determined by subclass.
	 */
	protected abstract void ackLeader();
	
	/**
	 * Broadcasts a message to all other processes. Implementation is determined by subclass.
	 * @param messageType	the type of message to broadcast
	 * @param mc			the content of the message to broadcast
	 */
	public abstract void broadcast(MessageType messageType, MessageContent mc);
	
	/**
	 * Query the leader. This should only be invoked after a leader has been chosen,
	 * by non-leader processes. Implementation is determined by subclass.
	 * @param mc			the content of the query
	 */
	public abstract void queryLeader(MessageContent mc);
	
	/*
	 * TODO MD
	 */
	public void startRunningSimple() {
		assert(isLeader);
		broadcast(MessageType.MSG_LEADER_BROADCAST_SIMPLE, new MessageContent("Hello!"));
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

	// INCOMING MESSAGES ////////////////////////////////////////////////////////////
	
	protected abstract boolean processMessageSpecial(Message m);
	protected abstract void processMessageAckLeader();
	
	protected abstract void processLeaderBroadcastSimple(Message m);

	protected void processLeaderBroadcastSimpleForReceiver(Message m) {
		assert(!isLeader);
		System.out.println(id + " has received!");
		queryLeader(new MessageContent("Why are you talking to me?"));
	}

	protected abstract void processQuerySimple(Message m);
	
	protected void processQuerySimpleForLeader(Message m) {
		numSimpleQueriesReceived++;
		if (numSimpleQueriesReceived == allProcesses.length - 1) {
			costTracker.dumpCosts();
			System.out.println("All queries received!");
		}
	}
	protected void processMessage(Message m) {
		switch (m.getType()) {
		case MSG_ACK_LEADER:
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
	

	

	
	// COST TRACKING ////////////////////////////////////////////////////////////

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
		}
		if (id != m.getReceiver()) {
			this.costTracker.registerCosts(s, id, costs.get(id).get(m.getReceiver()));
		}
	}


	// RUNTIME //////////////////////////////////////////////////////////////

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
