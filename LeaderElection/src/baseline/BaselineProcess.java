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

	// TODO: change this to a queue to handle leader elections being triggered
	// by queries.
	BaselineMessageContent pendingQueryMC = null;

	public BaselineProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		/* Select a random UUID */
		this.uuid = (int) (Math.random() * UUID_MAX);
	}

	/* =========== Public API =========== */

	@Override
	public void broadcast(MessageContent mc) throws InterruptedException {
		assert (mc instanceof BaselineMessageContent);
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], mc));
			}
		}
	}

	@Override
	public void queryLeader(String queryString) throws InterruptedException {
		BaselineMessageContent queryMC = BaselineMessageContent.createBMCQueryLeader(queryString);

		if (this.leaderId == BaselineProcess.ID_NONE) {
			/*
			 * No leader chosen yet. Queue up the query to be sent later, and
			 * trigger the election. When the election completes, the query will
			 * be sent out. Note: this message gets dropped if it turns out this
			 * process is the leader.
			 */

			/*
			 * TODO TODO THIS IS BROKEN pendingQueryMC = queryMC;
			 * triggerLeaderElection();
			 */
			return;
		}
		sendMessage(new Message(id, leaderId, queryMC));
	}

	@Override
	public void triggerLeaderElection() throws InterruptedException {
		broadcastUuidForElection();
	}

	/* ========= Workload specific =========== */

	// Responding to a query
	private void leaderResponse(Message m) {
		// TODO
	}

	private void workerQuery() {
		// TODO
	}

	/* ======== Message sending helpers ========= */

	@Override
	protected void broadcastLeaderHello() throws InterruptedException {
		assert (isLeader);
		broadcast(BaselineMessageContent.createBMCLeaderHello());
	}

	private void broadcastUuidForElection() throws InterruptedException {
		/* You should only broadcast once */
		assert (!broadcastedUuid);
		broadcastedUuid = true;

		/* Broadcast UUID to all */
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(new Message(id, allProcesses[i], BaselineMessageContent.createBMCElectLeader(uuid)));
			}
		}
	}

	private void ackLeader() throws InterruptedException {
		assert (this.leaderId != BaselineProcess.ID_NONE);
		sendMessage(new Message(id, leaderId, BaselineMessageContent.createBMCAckLeader()));
	}

	/* ======== Message receipt handlers ========= */

	private void processMessageAckLeader() throws InterruptedException {
		numLeaderAcksReceived++;
		if (numLeaderAcksReceived == allProcesses.length - 1 && isLeader) {
			/*
			 * If everyone knows I'm the leader, including myself, then I can
			 * act as leader
			 */
			leaderRoutine();
		}
	}

	protected void processMessageQueryLeader(Message m) {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert (bmc.getType() == BaselineMessageContent.MSG_QUERY_LEADER);
		assert (isLeader);

		leaderResponse(m);
	}

	protected void processMessageFromLeader(Message m) {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert (bmc.getType() == BaselineMessageContent.MSG_LEADER_RESPONSE);
		assert (!isLeader);

		// TODO
	}

	private void processMessageElectLeader(Message m) throws InterruptedException {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert (m.getType() == Message.MSG_ELECT_LEADER);

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
					 * as such
					 */

					/*
					 * First handle whatever query to the leader that might have
					 * arrived before election finished
					 */
					if (pendingQueryMC != null) {
						sendMessage(new Message(id, leaderId, pendingQueryMC));
						pendingQueryMC = null;
					}

					leaderRoutine();
				}
			} else {
				assert (numLeaderAcksReceived == 0);
				ackLeader();

				/*
				 * If a query was queued up to be sent before leader election,
				 * do it now
				 */
				if (pendingQueryMC != null) {
					sendMessage(new Message(id, leaderId, pendingQueryMC));
					pendingQueryMC = null;
				}
			}
		}
	}

	public void processMessageSpecial(Message m) throws InterruptedException {
		switch (m.getType()) {
		case Message.MSG_ELECT_LEADER:
			registerCost(Stage.ELECTION, m);
			processMessageElectLeader(m);
			break;
		default:
			// TODO error
			break;
		}
	}

	@Override
	protected void broadcastHello() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processMessageAckLeader(Message m) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processMessageQueryLeader(Message m) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processMessageFromLeader(Message m) throws InterruptedException {
		// TODO Auto-generated method stub

	}
}
