package mst;

import common.MessageContent;

/**
 * Specific message content for MST simulation.
 */
public class MSTMessageContent extends MessageContent{
	/**
	 * Values used to determine the MST of the cost network
	 */
	private double[] args;
	
	/**
	 * Simple constructor
	 * 
	 * @param args
	 */
	public MSTMessageContent(double[] args) {
		super();
		this.args = args;
	}
	
	/**
	 * Simple getter
	 * 
	 * @return {@code args}
	 */
	public double[] getArgs() {
		return args;
	}
}
