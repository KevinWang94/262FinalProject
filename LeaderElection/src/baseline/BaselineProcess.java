package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.MessageContent;
import common.Process;

public class BaselineProcess extends Process {
	public static final int UUID_MAX = 1000000;

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
	
	@Override
	public void electLeader() throws InterruptedException {
		broadcastUuid();
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
