package baseline;

import common.Message;
import common.MessageContent;

public class BaselineMessageContent extends MessageContent {		
	private int senderUuid;
		
	private BaselineMessageContent(int senderUuid, String body) {
		super(body);
		this.senderUuid = senderUuid;
	}
	
	/* Factory functions */
	
	static BaselineMessageContent createBMCQueryLeader(String queryString) {
		return new BaselineMessageContent(
				BaselineProcess.UUID_INVALID, 
				queryString);
	}
		
	static BaselineMessageContent createBMCLeaderResponse(String responseString) {
		return new BaselineMessageContent(
				BaselineProcess.UUID_INVALID, 
				responseString);
	}
	
	static BaselineMessageContent createBMCAckLeader() {
		return new BaselineMessageContent(
				BaselineProcess.UUID_INVALID, 
				null /* messageString*/);
	}
	
	static BaselineMessageContent createBMCElectLeader(int uuid) {
		return new BaselineMessageContent(uuid, null /* messageString*/);
	}
	
	/* Accessors */

	public int getUuid() {
		return senderUuid;
	}
}
