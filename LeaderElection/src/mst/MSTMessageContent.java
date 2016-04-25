package mst;

import common.MessageContent;

public class MSTMessageContent implements MessageContent{
	public static final int MSG_CONNECT = 1;
	
	private int type;
	private float[] args;
	
	public MSTMessageContent(int type, float[] args) {
		this.type = type;
		this.args = args;
	}
	
	public int getType() {
		return type;
	}
	
	public float[] getArgs() {
		return args;
	}
}
