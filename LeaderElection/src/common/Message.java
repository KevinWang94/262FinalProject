package common;

public class Message {
	private int sender;
	private int receiver;
	private MessageContent content;
	
	public Message(int sender, int receiver, MessageContent content) {
		this.sender = sender;
		this.receiver = receiver;
		this.content = content;
	}

	public int getSender() {
		return sender;
	}
	
	public int getReciever() {
		return receiver;
	}
	
	public MessageContent getContent() {
		return content;
	}
}
