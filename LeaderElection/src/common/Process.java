package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Process implements Runnable {
	
	protected HashMap<Integer, LinkedBlockingQueue<Message>> queues;
	protected int[] allProcesses;
	protected double[] costs;
	protected LinkedBlockingQueue<Message> incomingMessages;
	protected int id;
	
	public Process(int id, int[] allProcesses, double[] costs, HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages) {
		this.queues = queues;
		this.incomingMessages = incomingMessages;
		this.id = id;
		this.allProcesses = allProcesses;
		this.costs = costs;
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
	
	public void sendMessage(int id, Message m) throws InterruptedException {
		BlockingQueue<Message> queue = queues.get(id);
		queue.put(m);
	}
	
	public void processMessage(Message m) {
		System.out.println(id + " receiving " + m.getSender());
		// TODO
	}
	
	public void checkForMessages() {
		Message m = incomingMessages.poll();
		if (m == null) {
			return;
		}
		processMessage(m);
		return;
	}
	
	
	@Override
	public void run() {
		while (true) {
			checkForMessages();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
