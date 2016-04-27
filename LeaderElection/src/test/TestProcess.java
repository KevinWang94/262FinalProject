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
	public void processMessage(Message m) {
		System.out.println(id + " receiving " + m.getSender());
	}
}
