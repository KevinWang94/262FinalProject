package baseline;

import common.MessageContent;

/**
 * Specific message content for baseline simulation. It is relatively barebones,
 * with just one field for the sender's UUID. This is all that is needed for
 * baseline.
 *
 */
public class BaselineMessageContent extends MessageContent {
	/*
	 * The UUID of the message's sender
	 */
	private int senderUuid;

	/**
	 * Simple constructor
	 * 
	 * @param senderUuid
	 */
	public BaselineMessageContent(int senderUuid) {
		super();
		this.senderUuid = senderUuid;
	}

	/**
	 * Simple getter
	 * 
	 * @return senderUUID
	 */
	public int getUuid() {
		return senderUuid;
	}
}
