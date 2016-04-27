package baseline;

import common.Message;
import common.MessageContent;

public class BaselineMessageContent implements MessageContent {	
	/* Baseline Message Types */
	
	// leader election
	public static final int MSG_ELECT_LEADER = 1;
	public static final int MSG_ACK_LEADER = 4;

	// workload specific
	public static final int MSG_LEADER_RESPONSE = 2;
	public static final int MSG_QUERY_LEADER = 3;
	public static final int MSG_LEADER_HELLO = 5;
	
	private int type;
	private int senderUuid;
	private String queryString;
		
	private BaselineMessageContent(int type, int senderUuid, String queryString) {
		this.type = type;
		this.senderUuid = senderUuid;
		this.queryString = queryString;
	}
	
	/* Factory functions */
	
	static BaselineMessageContent createBMCQueryLeader(String queryString) {
		return new BaselineMessageContent(
				BaselineMessageContent.MSG_QUERY_LEADER, 
				BaselineProcess.UUID_INVALID, 
				queryString);
	}
		
	static BaselineMessageContent createBMCLeaderResponse(String responseString) {
		return new BaselineMessageContent(
				BaselineMessageContent.MSG_LEADER_RESPONSE, 
				BaselineProcess.UUID_INVALID, 
				responseString);
	}
	
	static BaselineMessageContent createBMCAckLeader() {
		return new BaselineMessageContent(
				BaselineMessageContent.MSG_ACK_LEADER,
				BaselineProcess.UUID_INVALID, 
				null /* messageString*/);
	}
	
	static BaselineMessageContent createBMCLeaderHello() {
		final String helloString = "hey";

		return new BaselineMessageContent(
			BaselineMessageContent.MSG_LEADER_HELLO, 
			BaselineProcess.UUID_INVALID, 
			helloString);
	}
	
	static BaselineMessageContent createBMCElectLeader(int uuid) {
		return new BaselineMessageContent(BaselineMessageContent.MSG_ELECT_LEADER,
			uuid, 
			null /* messageString*/);
	}
	
	/* Accessors */

	public int getType() {
		return type;
	}
	
	public int getUuid() {
		return senderUuid;
	}
	
	public String getQueryString() {
		return queryString;
	}
}
