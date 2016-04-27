package test;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.Process;

public class TestProcess extends Process {
	
	public TestProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs, HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
		if (allProcesses == null) {
			System.out.println("Goddammit\n");
		}
	}

	@Override
	public void broadcast() throws InterruptedException {
		System.out.println("Broadcasting " + id);

		for (int i = 0; i < allProcesses.length; i++) {
			if (allProcesses[i] != id) {
				sendMessage(allProcesses[i], new Message(id, allProcesses[i], null));
			}
		}
	}
	
	@Override
	public void processMessage(Message m) {
		System.out.println(id + " receiving " + m.getSender());
	}
}
