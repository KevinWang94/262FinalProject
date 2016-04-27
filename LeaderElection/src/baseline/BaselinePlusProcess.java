package baseline;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.Message;

public class BaselinePlusProcess extends BaselineProcess {
	
	private static final String[] magic = {"Oooh",
			"We're no strangers to love",
			"You know the rules and so do I",
			"A full commitment's what I'm thinking of",
			"You wouldn't get this from any other guy",
			"I just wanna tell you how I'm feeling",
			"Gotta make you understand",
			"Never gonna give you up",
			"Never gonna let you down",
			"Never gonna run around and desert you",
			"Never gonna make you cry",
			"Never gonna say goodbye",
			"Never gonna tell a lie and hurt you",
			"We've known each other for so long",
			"Your heart's been aching, but",
			"You're too shy to say it",
			"Inside, we both know what's been going on",
			"We know the game and we're gonna play it",
			"And if you ask me how I'm feeling",
			"Don't tell me you're too blind to see",
			"Never gonna give you up",
			"Never gonna let you down",
			"Never gonna run around and desert you",
			"Never gonna make you cry",
			"Never gonna say goodbye",
			"Never gonna tell a lie and hurt you"};
	
	public BaselinePlusProcess(int id, int[] allProcesses, 
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
	}
}
