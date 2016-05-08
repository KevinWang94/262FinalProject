package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.CostTracker.Stage;
import common.Message;
import common.MessageContent;
import common.Process;

public class BaselineProcess extends Process {
	public static final int UUID_MAX = 1000000;
	public static final int UUID_INVALID = -1;

	/* This process's UUID, randomly generated at construction */
	private int uuid = UUID_INVALID;
	/* UUID of leader */
	private int leaderUuid = UUID_INVALID;

	/*
	 * Whether you've already broadcasted your UUID as part of leader election
	 */
	private boolean broadcastedUuid = false;
	/* Number of UUIDs received from other processes, during leader election */
	private int numUuidsReceived = 0;
	/*
	 * Number of acks received that this process is the leader, during leader
	 * election
	 */
	private int numLeaderAcksReceived = 0;


	public BaselineProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		/* Select a random UUID */
		this.uuid = (int) (Math.random() * UUID_MAX);
	}

	/* =========== Public API =========== */

	@Override
	public void broadcast(int messageType, MessageContent mc) throws InterruptedException {
		assert (mc instanceof BaselineMessageContent);
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], messageType, mc));
			}
		}
	}

	@Override
	public void queryLeader(int messageType, MessageContent mc) throws InterruptedException {
		/*
		 * No leader chosen yet, or is leader. This should not be possible in our implementation.
		 */
		assert(this.leaderId != BaselineProcess.ID_NONE);
		assert(!isLeader);
			
		sendMessage(new Message(id, leaderId, messageType, mc));
	}

	@Override
	public void triggerLeaderElection() throws InterruptedException {
		broadcastUuidForElection();
	}


	/* ======== Message sending helpers ========= */

	private void broadcastUuidForElection() throws InterruptedException {
		/* You should only broadcast once */
		assert (!broadcastedUuid);
		broadcastedUuid = true;

		/* Broadcast UUID to all */
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], Message.MSG_BASELINE_ELECT_LEADER, new BaselineMessageContent(uuid)));
			}
		}
	}

	@Override
	protected void ackLeader() throws InterruptedException {
		assert(this.leaderId != BaselineProcess.ID_NONE);
		sendMessage(new Message(id, leaderId, Message.MSG_ACK_LEADER, null));
	}

	/* ======== Message receipt handlers ========= */

	@Override
	protected void processMessageAckLeader() throws InterruptedException {
		numLeaderAcksReceived++;
		if (numLeaderAcksReceived == allProcesses.length - 1 && isLeader) {
			/*
			 * If everyone knows I'm the leader, including myself, then I can
			 * act as leader
			 */
			startRunningSimple();
		}
	}

	private void processMessageElectLeader(Message m) throws InterruptedException {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert (m.getType() == Message.MSG_BASELINE_ELECT_LEADER);

		int senderUuid = bmc.getUuid();
		assert (senderUuid != UUID_INVALID);

		if (!broadcastedUuid) {
			broadcastUuidForElection();
		}

		if (leaderUuid < senderUuid) {
			leaderUuid = senderUuid;
			leaderId = m.getSender();
		}

		numUuidsReceived++;
		if (numUuidsReceived == allProcesses.length - 1) {
			/* Now, leaderId is the actual leader's ID */
			if (leaderId == id) {
				/* I'm the leader */
				isLeader = true;
				if (numLeaderAcksReceived == allProcesses.length - 1) {
					/*
					 * Everyone also knows I'm the leader, so I can start acting
					 * as such. Start the workload! Note: this would be nice if we had a closure 
					 * TODO LOL KEVIN PLS REMOVE 
					 */
					startRunningSimple();
				}
			} else {
				assert (numLeaderAcksReceived == 0);
				ackLeader();
			}
		}
	}

	public void processMessageSpecial(Message m) throws InterruptedException {
		switch (m.getType()) {
		case Message.MSG_BASELINE_ELECT_LEADER:
			registerCost(Stage.ELECTION, m);
			processMessageElectLeader(m);
			break;
		default:
			// TODO error
			break;
		}
	}
}
