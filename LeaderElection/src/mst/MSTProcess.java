package mst;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.Message.MessageType;
import common.MessageContent;

/**
 * This is a subclass of {@link MSTBase}. This class simulates a process that
 * implements leader election through constructing an MST, and selecting the
 * root (last core in the creation of the MST). Broadcast and query both
 * send messages along the edges of the MST.
 */
public class MSTProcess extends MSTBase {
	
	// INSTANCE FIELDS
	// ////////////////////////////////////////////////////////////

	/**
	 * Number of acks received. Only used for the leader. Used to 
	 * determine when leader election has been completed.
	 */
	int acksReceived = 0;

	// CONSTRUCTOR ////////////////////////////////////////////////////////////
	/**
	 * Just calls the constructor in {@link MSTBase}.
	 * 
	 * @param id
	 * @param allProcesses
	 * @param costs
	 * @param queues
	 * @param incomingMessages
	 * @param costTracker
	 */
	public MSTProcess(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
	}

	
	/**
	 * Processes the FINISH message, which is sent from the last core process
	 * when the MST construction is finished. The message content is expected to
	 * contain an array of one element, the leader id. Each process that receives
	 * the message should set its own leaderId and propagate the message to
	 * all its children.
	 */
	public void processFinish(Message m) {
		MSTMessageContent mContent = (MSTMessageContent) m.getContent();
		leaderId = (int) ((MSTMessageContent) mContent).getArgs()[0];
		if (DEBUG)
			System.out.println(m.getSender() + " to " + id);
		passMessageMST(m.getType(), m.getContent());
		ackLeader();
	}

	/**
	 * Broadcasts a message from the leader to all processes. The leader
	 * only sends the message to its children in the MST, which are expected
	 * to propagate the message further.
	 * 
	 * @param messageType: the type of the message
	 * @param mContent: the message content
	 */
	@Override
	public void broadcast(MessageType messageType, MessageContent mContent) {
		assert (id == this.leaderId);
		passMessageMST(messageType, mContent);
	}
	
	/**
	 * Processes a MSG_LEADER_BROADCAST_SIMPLE message. It calls the base
	 * processLeaderBroadcastSimpleForReceiver function in {@link Process}, but also
	 * propagates the message to its children.
	 */
	@Override
	protected void processLeaderBroadcastSimple(Message m) {
		assert(!isLeader);
		passMessageMST(m.getType(), m.getContent());
		super.processLeaderBroadcastSimpleForReceiver(m);
	}
	

	/**
	 * Sends a query to the leader with the given message content. This
	 * process just sends a message to the process's parent in the MST, 
	 * since the other processes are expected to propagate the message
	 * up the tree until the leader.
	 * 
	 * @param mContent: the message content to send.
	 */
	@Override
	public void queryLeader(MessageContent mContent) {
		sendMessage(new Message(id, inBranch, MessageType.MSG_QUERY_SIMPLE, mContent));
	}

	
	/**
	 * Processes a MSG_QUERY_SIMPLE message. If the process is the leader,
	 * execute the base processQuerySimpleForLeader in {@link Process}. 
	 * Otherwise, just pass the message to the parent.
	 * 
	 * @param m: the QUERY message
	 */
	protected boolean processQuerySimple(Message m) {
		if (id == leaderId) {
			return super.processQuerySimpleForLeader(m);
		} else {
			queryLeader(m.getContent());
			return false;
		}
	}

	
	/**
	 * Triggers leader election by calling {@link wakeup}.
	 */
	@Override
	public void triggerLeaderElection() {
		wakeup();
	}

	
	/**
	 * Called when an ack is received. Once a process has received all
	 * acks from its children, it sends an ACK_LEADER message to its parent
	 * unless it is the leader. If it is the leader, then all acks have been
	 * received, and the simple workload is started.
	 */
	@Override
	protected void ackLeader() {
		acksReceived++;
		if (acksReceived == numChildren + 1) {
			if (id != leaderId) {
				sendMessage(new Message(id, inBranch, MessageType.MSG_ACK_LEADER, null));
			} else {
				System.out.println("Leader acked!");
				startWorkloadSimple();
			}
		}
	}

	
	/**
	 * Processes the ACK_LEADER message by calling {@link ackLeader}.
	 */
	@Override
	protected void processMessageAckLeader() {
		ackLeader();
	}
}
