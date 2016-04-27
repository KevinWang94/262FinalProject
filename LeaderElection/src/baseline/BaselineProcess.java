package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

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
	
	/* Whether you've already broadcasted your UUID as part of leader election */
	private boolean broadcastedUuid = false;
	/* Number of UUIDs received from other processes, during leader election */
	private int numUuidsReceived = 0;
	/* Number of acks received that this process is the leader, during leader election */
	private int numLeaderAcksReceived = 0;
	
	public BaselineProcess(int id, int[] allProcesses, 
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
		/* Select a random UUID */
		this.uuid = (int) (Math.random() * UUID_MAX);
	}
	
	/* =========== Public API =========== */
	
	@Override
	public void broadcast(MessageContent mc) throws InterruptedException {
		assert(mc instanceof BaselineMessageContent);
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(allProcesses[i], new Message(id, allProcesses[i], mc));
			}
		}
	}
	
	@Override
	public void queryLeader(String queryString) throws InterruptedException {		
		if (this.leaderId == BaselineProcess.LEADER_ID_NONE) {
			// TODO elect leader?
		}
		
		sendMessage(leaderId, new Message(id, leaderId, BaselineMessageContent.createBMCQueryLeader(queryString)));
	}
	
	@Override
	public void triggerLeaderElection() throws InterruptedException {
		broadcastUuidForElection();
	}
	
	/* ========= Workload specific =========== */
	@Override
	protected void leaderRoutine() {
		// TODO
	}
	
	// Responding to a query
	private void leaderResponse(Message m) {
		// TODO
	}
	
	private void workerQuery() {
		// TODO
	}
	
	/*======== Message sending helpers  =========*/
	
	private void broadcastLeaderHello() throws InterruptedException {
		assert(isLeader);		
		broadcast(BaselineMessageContent.createBMCLeaderHello());
	}
	
	private void broadcastUuidForElection() throws InterruptedException {
		/* You should only broadcast once */
		assert(!broadcastedUuid);
		broadcastedUuid = true;
		
		/* Broadcast UUID to all */
		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(allProcesses[i], new Message(id, allProcesses[i], BaselineMessageContent.createBMCElectLeader(uuid)));
			}
		}
	}
	
	private void ackLeader() throws InterruptedException {
		assert(this.leaderId != BaselineProcess.LEADER_ID_NONE);
		sendMessage(leaderId, new Message(id, leaderId, BaselineMessageContent.createBMCAckLeader()));
	}
	
	/*======== Message receipt handlers =========*/
	
	private void processMessageAckLeader() {
		numLeaderAcksReceived++;
		if (numLeaderAcksReceived == allProcesses.length - 1 && isLeader) {
			/* If everyone knows I'm the leader, including myself, then I can act as leader */	
			leaderRoutine();			
		}
	}

	private void processMessageQueryLeader(Message m) {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert(bmc.getType() == BaselineMessageContent.MSG_QUERY_LEADER);
		assert(isLeader);
		
		leaderResponse(m);
	}
	
	private void processMessageFromLeader(Message m) {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert(bmc.getType() == BaselineMessageContent.MSG_LEADER_RESPONSE);
		assert(!isLeader);
		
		// TODO
	}
	
	private void processMessageElectLeader(Message m) throws InterruptedException {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		assert(bmc.getType() == BaselineMessageContent.MSG_ELECT_LEADER);
		
		int senderUuid = bmc.getUuid();
		assert(senderUuid != UUID_INVALID);
		
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
					/* Everyone also knows I'm the leader, so I can start acting as such*/
					leaderRoutine();
				}
			} else {
				assert(numLeaderAcksReceived == 0);
				ackLeader();
			}
		}
	}
	
	@Override
	public void processMessage(Message m) throws InterruptedException {
		BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
		switch (bmc.getType()) {
         case BaselineMessageContent.MSG_ELECT_LEADER:
        	 processMessageElectLeader(m);
        	 break;
         case BaselineMessageContent.MSG_ACK_LEADER:
        	 processMessageAckLeader();
        	 break;
         case BaselineMessageContent.MSG_QUERY_LEADER:
        	 processMessageQueryLeader(m);
        	 break;
         case BaselineMessageContent.MSG_LEADER_RESPONSE:
        	 processMessageFromLeader(m);
        	 break;
         default: 
        	 // TODO error
        	 break;
		}
	}
}
