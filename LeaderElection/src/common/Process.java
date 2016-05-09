package common;
// TODO do we really need comments for these srsly adlfj.sfdslfjsafkfs.agklaf;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker.Stage;
import common.Message.MessageType;

/**
 * This is an abstract class simulating a process in our simulated distributed system. 
 * The processes are programmed to do the following simple test workload:
 *   1. They elect a leader.
 *   2. The leader broadcasts to the others.
 *   3. The others respond to the leader with a basic query.
 * After being initialized, each process simply handles one incoming message per second
 * in a FIFO manner, until its workload is complete. 
 * 
 * The communication-related algorithms for electing a leader, broadcasting,
 * and querying the leader must be implemented by subclasses that override this.
 * For example, see {@link BaselineProcess} or {@link MSTProcess}.
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
	 * The {@code CostTracker} object shared by all the threads, used for registering 
	 * costs incurred upon sending messages.
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

	/**
	 * Whether to output debugging messages.
	 */
	protected boolean DEBUG = false;

	
	// CONSTRUCTOR ////////////////////////////////////////////////////////////
	/**
	 * Constructor. Fields pertaining to leaders are initialized to reflect 
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
	 * 
	 * @param messageType	the type of message to broadcast
	 * @param mc			the content of the message to broadcast
	 */
	public abstract void broadcast(MessageType messageType, MessageContent mc);
	
	/**
	 * Query the leader. This should only be invoked after a leader has been chosen,
	 * by non-leader processes. Implementation is determined by subclass.
	 * 
	 * @param mc			the content of the query
	 */
	public abstract void queryLeader(MessageContent mc);
	/**
	 * The first part of the simple test workload, in which the leader broadcasts to everyone
	 * else. The leader is responsible for running this after leader election completes. 
	 */
	public void startWorkloadSimple() {
		assert(isLeader);
		broadcast(MessageType.MSG_LEADER_BROADCAST_SIMPLE, new MessageContent("Hello!"));
	}
	/** 
	 * Send a message from one node to another directly, and register the cost.
	 * 
	 * @param m		the message to be sent
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
	/**
	 * Handler for all messages. Forwards to the appropriate handler for the message type.
	 * 
	 * @param m		the message received  
	 * @return 		whether this process should exit after handling this message
	 */
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
	/**
	 * Handler for all communication-protocol-specific messages.
	 * 
	 * @param m		the message received
	 * @return 		whether this process should exit after handling this message
	 */
	protected abstract boolean processMessageSpecial(Message m);
	/**
	 * Top-level handler for message acknowledging the leader's identity, sent during leader election.
	 */
	protected abstract void processMessageAckLeader();
	/**
	 * Handler for messages broadcasted by the leader as part of the test workload. Does any necessary
	 * forwarding of the message depending on the communication protocol, and then has this process
	 * query the leader back.
	 * 
	 * @param m		the message received
	 */
	protected abstract void processLeaderBroadcastSimple(Message m);
	/**
	 * Shared code used by handler for messages broadcasted by the leader as part of the test workload. 
	 * Here, this process sends a query back to the leader.
	 * 
	 * @param m		the message received
	 */
	protected void processLeaderBroadcastSimpleForReceiver(Message m) {
		assert(!isLeader);
		System.out.println(id + " has received!");
		queryLeader(new MessageContent("Why are you talking to me?"));
	}
	/**
	 * Top-level handler for queries made to the leader during the test workload. Includes any 
	 * necessary forwarding of the message depending on the communication protocol.
	 * 
	 * @param m		the message received
	 * @return		whether this process should exit after handling this message
	 */
	protected abstract boolean processQuerySimple(Message m);
	
	/**
	 * Handler used by the leader for queries made to the leader during the test workload.
	 * When the leader has received all such queries, it terminates the simulation.
	 * 
	 * @param m		the message received
	 * @return		whether this process should exit after handling this message
	 */
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


	// COST TRACKING ////////////////////////////////////////////////////////////
	/**
	 * Registers the cost of sending a message to the global {@code CostTracker} object. 
	 * 
	 * @param m		the message being sent
	 */
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

	// RUNTIME //////////////////////////////////////////////////////////////
	/**
	 * Handle and remove the oldest message on this process's message queue, if one exists.
	 * 
	 * @return	whether this process is done running and should exit
	 */
	public boolean checkForMessages() throws InterruptedException {
		Message m = incomingMessages.poll();
		if (m == null) {
			return false;
		}
		return processMessage(m);
	}
	/**
	 * Main run loop. Check for and handle one incoming message per second, until the 
	 * workload is complete. 
	 */
	@Override
	public void run() {
		/* signals that the workload is complete, and we should break from the loop and exit */
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
