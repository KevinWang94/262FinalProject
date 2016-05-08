package test;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;
import common.MessageContent;
import common.Process;

public class TestProcess extends Process {
	
	public TestProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs, HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages) {
		// TODO: decide if we need TestPRocess
		// super(id, allProcesses, costs, queues, incomingMessages);
		assert(allProcesses != null);
	}

	@Override
	public void processMessage(Message m) {
		System.out.println(id + " receiving " + m.getSender());
	}


	@Override
	public void broadcast(MessageContent mContent) throws InterruptedException {
		System.out.println(id + " broadcasting");
	}
	

	@Override
	public void triggerLeaderElection() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void queryLeader(String queryString) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void broadcastLeaderHello() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processMessageAckLeader(Message m) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processMessageQueryLeader(Message m) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processMessageFromLeader(Message m) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processMessageSpecial(Message m) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
}
