package common;

public abstract class MessageContent {
	private String body;
	
	protected MessageContent() {
		body = null;
	}
	
	protected MessageContent(String body) {
		this.body = body;
	}
	
	public String getBody() {
		return body;
	}
}
