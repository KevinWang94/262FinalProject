package test;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.MessageContent;
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
	
	@Override
	public void electLeader() {
		System.out.println(id + " electing leader");
	}

	@Override
	public void broadcast(MessageContent mContent) throws InterruptedException {
		System.out.println(id + " broadcasting");
	}
	
	@Override
	public void queryLeader(MessageContent mContent) throws InterruptedException {
		System.out.println(id + " querying leader");
	}
}
