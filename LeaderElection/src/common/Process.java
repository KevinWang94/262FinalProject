package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import baseline.BaselineMessageContent;

public abstract class Process implements Runnable {
	protected static final int ID_NONE = -1;

	protected HashMap<Integer, LinkedBlockingQueue<Message>> queues;
	protected int[] allProcesses;
	protected HashMap<Integer, HashMap<Integer, Double>> costs;
	protected LinkedBlockingQueue<Message> incomingMessages;
	protected int id;
	protected int leaderId;
	
	/* true iff this process has computed itself as leader */
	protected boolean isLeader;
	
	public Process(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs, HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages) {
		this.queues = queues;
		this.incomingMessages = incomingMessages;
		this.id = id;
		this.allProcesses = allProcesses;
		this.costs = costs;
		this.leaderId = this.ID_NONE;
		this.isLeader = false;
	}
	
	/* PUBLIC API */
	public abstract void triggerLeaderElection() throws InterruptedException;
	public abstract void broadcast(MessageContent mc) throws InterruptedException;
	public abstract void queryLeader(String queryString) throws InterruptedException;
	
	// Workload stuff
	protected abstract void broadcastLeaderHello() throws InterruptedException;
	
	/* Message handling */

	protected abstract void processMessageAckLeader(Message m) throws InterruptedException;
	protected abstract void processMessageQueryLeader(Message m) throws InterruptedException;
	protected abstract void processMessageFromLeader(Message m) throws InterruptedException;
	protected abstract void processMessageSpecial(Message m) throws InterruptedException;
	
	protected void processMessage(Message m) throws InterruptedException {		
		switch (m.getType()) {
         case Message.MSG_ACK_LEADER:
        	 processMessageAckLeader(m);
        	 break;
         case Message.MSG_QUERY_LEADER:
        	 processMessageQueryLeader(m);
        	 break;
         case Message.MSG_LEADER_RESPONSE:
        	 processMessageFromLeader(m);
        	 break;
         default: 
        	 processMessageSpecial(m);
        	 break;
		}
	}
	
	public void sendMessage(Message m) throws InterruptedException {
		BlockingQueue<Message> queue = queues.get(m.getReciever());
		queue.put(m);
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
