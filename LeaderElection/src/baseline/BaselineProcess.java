package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.Message.MessageType;
import common.MessageContent;
import common.Process;

public class BaselineProcess extends Process {
	public static final int UUID_MAX = 1000000;
	public static final int UUID_INVALID = -1;

	/* This process's UUID, randomly generated at construction */
	private int uuid = UUID_INVALID;
	/* UUID of leader */
	private int leaderUuid;

	/*
	 * Whether you've already broadcasted your UUID as part of leader election
	 */
	private boolean broadcastedUuid = false;
	/* Number of UUIDs received from other processes, during leader election */
	private int numUuidsReceived = 0;
	// Number of acks received that this process is the leader, during leader election
	protected int numLeaderAcksReceived = 0;

	public BaselineProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		/* Select a random UUID */
		this.uuid = (int) (Math.random() * UUID_MAX);
		this.leaderUuid = uuid;
		this.leaderId = id;
	}

	/* =========== Public API =========== */

	@Override
	public void broadcast(MessageType messageType, MessageContent mc) {
//		assert (mc instanceof BaselineMessageContent);
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], messageType, mc));
			}
		}
	}

	@Override
	public void queryLeader(MessageContent mc) {
		/*
		 * No leader chosen yet, or is leader. This should not be possible in our implementation.
		 */
		assert(this.leaderId != BaselineProcess.ID_NONE);
		assert(!isLeader);
			
		sendMessage(new Message(id, leaderId, MessageType.MSG_QUERY_SIMPLE, mc));
	}

	@Override
	public void triggerLeaderElection() {
		broadcastUuidForElection();
	}


	/* ======== Message sending helpers ========= */

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

	@Override
	protected void ackLeader() {
		assert(this.leaderId != BaselineProcess.ID_NONE);
		sendMessage(new Message(id, leaderId, MessageType.MSG_ACK_LEADER, null));
	}

	/* ======== Message receipt handlers ========= */

	// TODO MICHELLE where to put this
	protected void processLeaderBroadcastSimple(Message m) {
		processLeaderBroadcastSimpleForReceiver(m);
	}
	
	protected void processQuerySimple(Message m) {
		super.processQuerySimpleForLeader(m);
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
		assert (senderUuid != UUID_INVALID);

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
					startRunningSimple();
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
			startRunningSimple();
		}
	}

	/**
	 * Handler for all MessageTypes specific to BaselineProcess (not common to Process in general).
	 * Redirects to specific handler for the MessageType.
	 *
	 * @param  m	the message received
	 */
	public void processMessageSpecial(Message m) {
		switch (m.getType()) {
		case MSG_BASELINE_ELECT_LEADER:
			processMessageElectLeader(m);
			break;
		default:
			// TODO error
			break;
		}
	}
}
