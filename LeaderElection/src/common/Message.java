package common;

public class Message {
	
	public enum MessageType {
		/* common */
		MSG_ACK_LEADER,
		MSG_LEADER_BROADCAST_SIMPLE,
		MSG_QUERY_SIMPLE,
		
		/* baseline */
		MSG_BASELINE_ELECT_LEADER,
		
		/* mst */
		MSG_MST_CONNECT,
		MSG_MST_ACCEPT,
		MSG_MST_REJECT,
		MSG_MST_REPORT,
		MSG_MST_CHANGEROOT,
		MSG_MST_INITIATE,
		MSG_MST_TEST,
		MSG_MST_FINISH,
		
		/* path */
		MSG_PATH_PARTIAL,
		MSG_PATH_FINAL,
	}
	
	private int sender;
	private int receiver;
	private MessageType type;
	
	private MessageContent content;

	public Message(int sender, int receiver, MessageType type, MessageContent content) {
		this.sender = sender;
		this.receiver = receiver;
		this.content = content;
		this.type = type;
	}

	public int getSender() {
		return sender;
	}
	
	public int getReceiver() {
		return receiver;
	}
	
	public boolean isMSTInitialization() {
		return (type == MessageType.MSG_MST_CONNECT ||
				type == MessageType.MSG_MST_ACCEPT ||
				type == MessageType.MSG_MST_REJECT ||
				type == MessageType.MSG_MST_REPORT ||
				type == MessageType.MSG_MST_CHANGEROOT ||
				type == MessageType.MSG_MST_INITIATE ||
				type == MessageType.MSG_MST_TEST);
	}
	
	public MessageType getType() {
		return type;
	}
	
	public MessageContent getContent() {
		return content;
	}
}
