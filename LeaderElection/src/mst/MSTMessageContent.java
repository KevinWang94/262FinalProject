package mst;

import common.MessageContent;

public class MSTMessageContent extends MessageContent{
	private double[] args;
	
	public MSTMessageContent(double[] args) {
		super();
		this.args = args;
	}
	
	public double[] getArgs() {
		return args;
	}
}
