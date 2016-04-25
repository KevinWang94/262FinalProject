package mst;

import common.MessageContent;

public class MSTMessageContent implements MessageContent{
	public static final int MSG_CONNECT = 1;
	public static final int MSG_ACCEPT = 2;
	public static final int MSG_REJECT = 3;
	public static final int MSG_REPORT = 4;
	public static final int MSG_CHANGEROOT = 5;
	
	private int type;
	private double[] args;
	
	public MSTMessageContent(int type, double[] args) {
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
