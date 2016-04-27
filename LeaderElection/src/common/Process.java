package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Process implements Runnable {
	
	protected HashMap<Integer, LinkedBlockingQueue<Message>> queues;
	protected int[] allProcesses;
	protected HashMap<Integer, HashMap<Integer, Double>> costs;
	protected LinkedBlockingQueue<Message> incomingMessages;
	protected int id;
	
	public Process(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs, HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages) {
		this.queues = queues;
		this.incomingMessages = incomingMessages;
		this.id = id;
		this.allProcesses = allProcesses;
		this.costs = costs;
	}

	public abstract void processMessage(Message m);
	
	public abstract void broadcast() throws InterruptedException;

	public void sendMessage(int id, Message m) throws InterruptedException {
		BlockingQueue<Message> queue = queues.get(id);
		queue.put(m);
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
