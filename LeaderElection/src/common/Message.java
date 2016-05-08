package common;

public class Message {
	////// TYPES
	public static final int MSG_ACK_LEADER = 10001;
	
	public static final int MSG_START_SIMPLE = 10002;
	public static final int MSG_LEADER_BROADCAST_SIMPLE = 10003;
	public static final int MSG_QUERY_SIMPLE = 10004;
	
	// baseline only 
	public static final int MSG_ELECT_LEADER = 101;
	
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
