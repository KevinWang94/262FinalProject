package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.MessageContent;
import common.Process;
import common.put;

public class BaselineProcess extends Process {
	public static final int UUID_MAX = 1000000;
	public static final int UUID_INVALID = -1;
	
	public static final String broadcastString = "kevin is s3xy";

	private int uuid;
	
	public BaselineProcess(int id, int[] allProcesses, 
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
		this.uuid = (int) (Math.random() * UUID_MAX);
	}
	
	@Override
	public void broadcast(MessageContent mContent) throws InterruptedException {
		System.out.println("Broadcasting from " + id);

		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(allProcesses[i], new Message(id, allProcesses[i], mContent));
			}
		}
	}
	
	private void broadcastUuid() throws InterruptedException {
		BaselineMessageContent bmc = new BaselineMessageContent(
				BaselineMessageContent.MSG_ELECT_LEADER,
				uuid);
		
		broadcast(bmc);
	}
	
	int num_things_seen = 0;
	
	public void processUUIDBroadcast() {
		num_things_seen++;
		if (done && other done (below)) {
			broadcast(BROADCAST_STRING);
		}
	}
	
	int num_election_end_received = 0;
	
	public void processElectionEndMessage() {
		num_election_end_received ++;
		if (other done && isLeader) {
			broadcast(BROADCAST_STRING);
		}
	}
		
	
	@Override
	public void electLeader() throws InterruptedException {
		broadcastUuid();
		
		int uuidsReceived = 0;
		int leaderUuid = UUID_INVALID;
		
		while (uuidsReceived < allProcesses.length - 1) {
			Message m = incomingMessages.poll();
			if (m == null) {
				continue;
			}
			
			BaselineMessageContent bmc = (BaselineMessageContent) m.getContent();
			
			assert(bmc.getType() == bmc.MSG_ELECT_LEADER);
			
			int senderUuid = bmc.getUuid();
			assert(senderUuid != UUID_INVALID);
			
			if (leaderUuid < senderUuid) {
				leaderUuid = senderUuid;
				leaderId = m.getSender();
			}
			uuidsReceived++;
		}
	}
	
	@Override
	public void queryLeader(MessageContent mContent) throws InterruptedException {
		// no-op if no leader elected yet. TODO: should this throw an error?
		
		if (this.leaderId == this.LEADER_ID_NONE) {
			// todo elect leader?
		}
		
		sendMessage(leaderId, new Message(id, leaderId, mContent));
	}
	
	@Override
	public void processMessage(Message m) {
		// TODO
	}
}
