package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.Message.MessageType;
import common.MessageContent;
import common.Process;

/**
 * This class represents a process that uses a baseline algorithm for leader election and 
 * interprocess communication. 
 * 
 * Specifically, each process randomly chooses a universally unique identifier (UUID) during
 * initialization. During leader election, all processes broadcast their UUIDs, and everyone
 * agrees that the maximal UUID is the leader of the network. To broadcast, a process directly
 * sends a message to each other process. To query the leader, a (non-leader) process just 
 * directly sends a message to the leader.
 */
public class BaselineProcess extends Process {

	// INSTANCE FIELDS ////////////////////////////////////////////////////////////
	/**
	 * Maximum UUID to which a process can be assigned to
	 */
	public static final int UUID_MAX = 100000000;
	/**
	 * This process's UUID, randomly generated at construction 
	 */
	private int uuid;
	/**
	 * UUID of the leader. During election, this is repeatedly updated as the process receives
	 * broadcasts of more UUIDs, until after all messages are received, this is guaranteed
	 * to be the maximal UUID of all process, and therefore the UUID of the leader.
	 */
	private int leaderUuid;
	/**
	 * Whether you've already broadcasted your UUID, used during leader election
	 */
	private boolean broadcastedUuid = false;
	/**
	 * Number of UUIDs received from other processes, used during leader election 
	 */
	private int numUuidsReceived = 0;
	/**
	 * Number of acks received that this process is the leader, used during leader election
	 */
	protected int numLeaderAcksReceived = 0;

	// CONSTRUCTOR ////////////////////////////////////////////////////////////
	/**
	 * Constructor. For more details on parameters, see {@link Process}.
	 * 
	 * @param id			ID (not UUID) of this process
	 * @param allProcesses	IDs of all processes in the network
	 * @param costs			Costs associated with transmitting messages between every pair of processes
	 * @param queues		Message queues associated with each process
	 * @param incomingMessages	Message queue for this process
	 * @param costTracker	Global {@link CostTracker} object for tracking communication costs incurred by this process
	 */
	public BaselineProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		/* Select a random UUID */
		this.uuid = (int) (Math.random() * UUID_MAX);
		this.leaderUuid = uuid;
		this.leaderId = id;
	}

	// OUTGOING MESSAGES ///////////////////////////////////////////////////////
	/**
	 * Broadcast by directly sending the desired message to each other process. 
	 * 
	 * See also {@link Process#broadcast(MessageType, MessageContent)}. 
	 * 
	 * @param messageType	Type of the message to broadcast (see {@link Message.MessageType})
	 * @param mc 			Contents of the message to broadcast
	 */
	@Override
	public void broadcast(MessageType messageType, MessageContent mc) {
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], messageType, mc));
			}
		}
	}
	/**
	 * Query the leader by directly sending a message containing the desired contents
	 * to the leader. Can only run after leader election is complete, by non-leaders.
	 * 
	 * See also {@link Process#queryLeader(MessageContent)}. 
	 * 
	 * @param mc 	Contents of the message
	 */
	@Override
	public void queryLeader(MessageContent mc) {
		/*
		 * No leader chosen yet, or is leader. This should not be possible in our implementation.
		 */
		assert(this.leaderId != BaselineProcess.ID_NONE);
		assert(!isLeader);
			
		/*
		 * Type is MSG_QUERY_SIMPLE because this is only invoked in simple workload
		 */
		sendMessage(new Message(id, leaderId, MessageType.MSG_QUERY_SIMPLE, mc));
	}
	/**
	 * Triggers the leader election process by broadcasting this process's UUID to everyone. 
	 * 
	 * See also {@link Process#triggerLeaderElection()}. 
	 */
	@Override
	public void triggerLeaderElection() {
		broadcastUuidForElection();
	}
	/**
	 * Broadcasts this process's UUID to all other processes during leader election. Each process should only
	 * run this one time.
	 */
	private void broadcastUuidForElection() {
		/* You should only broadcast once */
		assert (!broadcastedUuid);
		broadcastedUuid = true;

		/* Broadcast UUID to all */
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], MessageType.MSG_BASELINE_ELECT_LEADER, new BaselineMessageContent(uuid)));
			}
		}
	}
	/**
	 * Send a message to the leader acknowledging that this process recognizes it as the 
	 * leader. Invoked after this process receives UUID broadcasts from all other 
	 * processes in the network.
	 * 
	 * See also {@link Process#ackLeader()}. 
	 */
	@Override
	protected void ackLeader() {
		assert(this.leaderId != BaselineProcess.ID_NONE);
		sendMessage(new Message(id, leaderId, MessageType.MSG_ACK_LEADER, null));
	}

	// INCOMING MESSAGES ////////////////////////////////////////////////////////////
	/**
	 * Top-level handler for message acknowledging the leader's identity, sent during leader election.
	 * For baseline algorithm, no routing processing is required, so this simply invokes the shared
	 * logic of sending a query back to the leader.
	 * 
	 * See also {@link Process#processLeaderBroadcastSimple(Message m)} and 
	 * {@link Process#processLeaderBroadcastSimpleForReceiver(Message m)}.
	 * 
	 * @param m	The message received
	 */
	protected void processLeaderBroadcastSimple(Message m) {
		processLeaderBroadcastSimpleForReceiver(m);
	}
	/**
	 * Top-level handler for queries made to the leader during the test workload. For baseline algorithm,
	 * only the leader ever receives such messages, so this simply invokes the shared logic of 
	 * terminating the simulation after all queries from non-leader processes are received.
	 * 
	 * See also {@link Process#processQuerySimple(Message m)} and 
	 * {@link Process#processQuerySimpleForLeader(Message m)}.
	 * 
	 * @param m		The message received
	 * @return		Whether this process should exit after handling this message
	 */
	protected boolean processQuerySimple(Message m) {
		return super.processQuerySimpleForLeader(m);
	}
	/**
	 * Handle receipts of messages sent as part of baseline leader election, containing
	 * the sender's UUID.
	 * 
	 * The first time a {@link BaselineProcess} receives one of these messages,
	 * it enters leader election and broadcasts its UUID. Every time, it updates
	 * the max UUID seen thus far. When it has received messages from all other
	 * processes, it knows chooses the max UUID as the leader, and acknowledges
	 * to the leader that it is done. If it turns out this process is the leader,
	 * AND all other processes have already acknowledged this, then start 
	 * running the workload as the leader.
	 * 
	 * @param  m	the message received
	 */
	private void processMessageElectLeader(Message m) {
		assert (m.getType() == MessageType.MSG_BASELINE_ELECT_LEADER);

		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		int senderUuid = bmc.getUuid();

		// We only want to broadcast our UUID once
		if (!broadcastedUuid) {
			broadcastUuidForElection();
		}

		// Update leader to be max UUID seen thus far
		if (leaderUuid < senderUuid) {
			leaderUuid = senderUuid;
			leaderId = m.getSender();
		}

		numUuidsReceived++;
		if (numUuidsReceived == allProcesses.length - 1) {
			// We're done once we've seen leader election messages from all the other processes
			/* Now, leaderId is the actual leader's ID */
			if (leaderId == id) {
				/* I'm the leader */
				isLeader = true;
				if (numLeaderAcksReceived == allProcesses.length - 1) {
					/*
					 * Everyone also knows I'm the leader, so I can start acting
					 * as such. Start the workload!
					 */
					startWorkloadSimple();
				}
			} else {
				assert (numLeaderAcksReceived == 0);
				ackLeader();
			}
		}
	}
	/**
	 * This handles messages that non-leaders send to the leader
	 * at the during baseline leader election, once they know the identity
	 * of the leader (i.e., after they have received all UUIDs from all 
	 * processes). Only leaders should ever receive this.
	 * 
	 * This method simply counts the number of ack's received. Once all 
	 * ack's have been received from all other processes, AND this process
	 * has also independently determined that it is the leader based
	 * on broadcasted UUIDs, then this process knows that everyone agrees
	 * that it is the leader. Leader election thus terminates, and the
	 * the actual workload for the system begins running.
	 */
	protected void processMessageAckLeader() {
		numLeaderAcksReceived++;
		if (numLeaderAcksReceived == allProcesses.length - 1 && isLeader) {
			/*
			 * If everyone knows I'm the leader, including myself, then I can
			 * act as leader. 
			 */
			startWorkloadSimple();
		}
	}
	/**
	 * Handler for all {@code MessageType}s specific to {@code BaselineProcess}.
	 * Redirects to specific handler for the {@code MessageType}.
	 *
	 * @param  m	the message received
	 * @return  true if message was processed; false if case not handled
	 */
	public boolean processMessageSpecial(Message m) {
		switch (m.getType()) {
		case MSG_BASELINE_ELECT_LEADER:
			processMessageElectLeader(m);
			return true;
		default:
			return false;
		}
	}
}
