package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Process implements Runnable {
	protected static final int LEADER_ID_NONE = -1;

	protected HashMap<Integer, LinkedBlockingQueue<Message>> queues;
	protected int[] allProcesses;
	protected HashMap<Integer, HashMap<Integer, Double>> costs;
	protected LinkedBlockingQueue<Message> incomingMessages;
	protected int id;
	protected int leaderId;
	
	public Process(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs, HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages) {
		this.queues = queues;
		this.incomingMessages = incomingMessages;
		this.id = id;
		this.allProcesses = allProcesses;
		this.costs = costs;
		this.leaderId = this.LEADER_ID_NONE;
	}

	public abstract void processMessage(Message m) throws InterruptedException;
	
	public abstract void broadcast() throws InterruptedException;

	// TODO: remove id because it's redundant
	public void sendMessage(int id, Message m) throws InterruptedException {
		BlockingQueue<Message> queue = queues.get(id);
		queue.put(m);
	}
	
	public void queryLeader(MessageContent mContent) throws InterruptedException {
		// no-op if no leader elected yet. TODO: should this throw an error?
		if (this.leaderId != this.LEADER_ID_NONE) {
			sendMessage(leaderId, new Message(id, leaderId, mContent));
		}
	}
		
	public void checkForMessages() throws InterruptedException {		
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
			try {
				checkForMessages();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
