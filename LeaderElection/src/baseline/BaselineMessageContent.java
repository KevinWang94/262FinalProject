package baseline;

import common.MessageContent;

public class BaselineMessageContent implements MessageContent{
	public static final int UUID_NONE = -1;
	
	public static final int MSG_ELECT_LEADER = 1;
	
	private int type;
	private int senderUuid;
	
	public BaselineMessageContent(int type) {
		this.type = type;
		this.senderUuid = this.UUID_NONE;
	}
	
	public BaselineMessageContent(int type, int senderUuid) {
		this.type = type;
		this.senderUuid = senderUuid;
	}
	
	public int getType() {
		return type;
	}
	
	public int getUuid() {
		return senderUuid;
	}
}
