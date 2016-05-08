package mst;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.Message.MessageType;
import common.MessageContent;

public class MSTProcess extends MSTBase {

	int acksReceived = 0;

	public MSTProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
	}

	public void processFinish(Message m) {
		MSTMessageContent mContent = (MSTMessageContent) m.getContent();
		leaderId = (int) ((MSTMessageContent) mContent).getArgs()[0];
		System.out.println(m.getSender() + " to " + id);
		passMessage(m.getType(), m.getContent());
		ackLeader();
	}

	@Override
	public void broadcast(MessageType messageType, MessageContent mContent) {
		assert (id == this.leaderId);
		passMessage(messageType, mContent);
	}
	
	protected void processLeaderBroadcastSimple(Message m) {
		assert(!isLeader);
		passMessage(m.getType(), m.getContent());
		super.processLeaderBroadcastSimpleForReceiver(m);
	}
	

	@Override
	public void queryLeader(MessageContent mContent) {
		sendMessage(new Message(id, inBranch, MessageType.MSG_QUERY_SIMPLE, mContent));
	}

	protected void processQuerySimple(Message m) {
		if (id == leaderId) {
			super.processQuerySimpleForLeader(m);
		} else {
			queryLeader(m.getContent());
		}
	}

	public void processMessageSpecial(Message m) {
		switch (m.getType()) {
		  case MSG_MST_CONNECT:
			  processConnect(m);
			  break;
		  case MSG_MST_ACCEPT:
			  processAccept(m.getSender());
			  break;
		  case MSG_MST_REJECT:
			  processReject(m.getSender());
			  break;
		  case MSG_MST_REPORT:
			processReport(m);
			break;
		  case MSG_MST_CHANGEROOT:
			processChangeRoot();
			break;
		  case MSG_MST_INITIATE:
			processInitiate(m);
			break;
		  case MSG_MST_TEST:
			processTest(m);
			break;
		  case MSG_MST_FINISH:
			processFinish(m);
			break;
		  default:
			// TODO FAIL
		}
	}

	@Override
	public void triggerLeaderElection() {
		wakeup();
	}

	@Override
	protected void ackLeader() {
		acksReceived++;
		if (acksReceived == numChildren + 1) {
			if (id != leaderId) {
				sendMessage(new Message(id, inBranch, MessageType.MSG_ACK_LEADER, null));
			} else {
				System.out.println("Leader acked!");
				startRunningSimple();
			}
		}
	}

	@Override
	protected void processMessageAckLeader() {
		ackLeader();
	}
}
