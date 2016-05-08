package baseline;

import common.MessageContent;

public class BaselineMessageContent extends MessageContent {		
	private int senderUuid;
		
	public BaselineMessageContent(int senderUuid) {
		super();
		this.senderUuid = senderUuid;
	}

	/* Accessors */
	public int getUuid() {
		return senderUuid;
	}
}
