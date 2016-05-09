package common;

import baseline.BaselineProcess;

/**
 * This represents the content of a message. It is overridden by each separate
 * simulation to include custom contents required by the communication algorithm.
 * For example, @see {@link BaselineProcess}
 */
public class MessageContent {
	/**
	 * Message body
	 */
	private String body;

	/**
	 * Simple constructor without body, setting it to null
	 */
	public MessageContent() {
		body = null;
	}

	/**
	 * Simple constructor
	 * 
	 * @param body
	 *            the desired body
	 */
	public MessageContent(String body) {
		this.body = body;
	}

	/**
	 * Simple getter
	 * 
	 * @return body
	 */
	public String getBody() {
		return body;
	}
}
