package mst;

import common.MessageContent;

public class MSTMessageContent extends MessageContent{
	public static final int MSG_CONNECT = 1;
	public static final int MSG_ACCEPT = 2;
	public static final int MSG_REJECT = 3;
	public static final int MSG_REPORT = 4;
	public static final int MSG_CHANGEROOT = 5;
	public static final int MSG_INITIATE = 6;
	public static final int MSG_TEST = 7;
	
	private int type;
	private double[] args;
	
	public MSTMessageContent(int type, double[] args) {
		super();
		this.type = type;
		this.args = args;
	}
	
	public int getType() {
		return type;
	}
	
	public double[] getArgs() {
		return args;
	}
}
