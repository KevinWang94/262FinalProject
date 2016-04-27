package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
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
	
	public void broadcastUuid() throws InterruptedException {
		BaselineMessageContent bmc = new BaselineMessageContent(
				BaselineMessageContent.MSG_ELECT_LEADER,
				uuid);
		
		broadcast(bmc);
	}
	
	public void electLeader() throws InterruptedException {
	}
	
	@Override
	public void processMessage(Message m) {
		// TODO
	}
}
