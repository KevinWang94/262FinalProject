package common;

public class Message {
	////// TYPES
	public static final int MSG_ACK_LEADER = 10001;
	
	public static final int MSG_START_SIMPLE = 10002;
	public static final int MSG_LEADER_BROADCAST_SIMPLE = 10003;
	public static final int MSG_QUERY_SIMPLE = 10004;
	
	// baseline only 
	public static final int MSG_BASELINE_ELECT_LEADER = 101;
	
	// MST only
	public static final int MSG_MST_CONNECT = 1;
	public static final int MSG_MST_ACCEPT = 2;
	public static final int MSG_MST_REJECT = 3;
	public static final int MSG_MST_REPORT = 4;
	public static final int MSG_MST_CHANGEROOT = 5;
	public static final int MSG_MST_INITIATE = 6;
	public static final int MSG_MST_TEST = 7;
	public static final int MSG_MST_LEADER = 8;
	public static final int MSG_MST_QUERY_LEADER = 9;
	
	
	private int sender;
	private int receiver;
	private int type;
	
	private MessageContent content;

	public Message(int sender, int receiver, int type, MessageContent content) {
		this.sender = sender;
		this.receiver = receiver;
		this.content = content;
		this.type = type;
	}

	public int getSender() {
		return sender;
	}
	
	public int getReciever() {
		return receiver;
	}
	
	public int getType() {
		return type;
	}
	
	public MessageContent getContent() {
		return content;
	}
}
