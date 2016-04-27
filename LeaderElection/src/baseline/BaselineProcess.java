package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.Process;

public class BaselineProcess extends Process {
	public BaselineProcess(int id, int[] allProcesses, 
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
	}
	
	public void broadcast() throws InterruptedException {
		//TODO
		System.out.println("Broadcasting " + id);

		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(allProcesses[i], new Message(id, allProcesses[i], null));
			}
		}
	}
	
	public void processMessage(Message m) {
		// TODO
	}
}
