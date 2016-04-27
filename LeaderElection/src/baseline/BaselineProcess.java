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
	
	@Override
	public void broadcast() throws InterruptedException {
		// TODO
		System.out.println("[Baseline] Broadcasting " + id);

		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				BaselineMessageContent bmc = new BaselineMessageContent(
						BaselineMessageContent.MSG_ELECT_LEADER,
						uuid);
						
				sendMessage(allProcesses[i], new Message(uuid, allProcesses[i], bmc));
			}
		}
	}
	
	public void queryLeader() {
		
	}
	
	public void electLeader() throws InterruptedException {
		broadcast();
	}
	
	@Override
	public void processMessage(Message m) {
		// TODO
	}
}
