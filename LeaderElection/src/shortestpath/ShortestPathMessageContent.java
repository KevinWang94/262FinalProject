package shortestpath;

import java.util.ArrayList;
import java.util.HashMap;

import util.Pair;
import common.MessageContent;

public class ShortestPathMessageContent extends MessageContent {
	HashMap<Pair, ArrayList<Integer>> paths;
}
