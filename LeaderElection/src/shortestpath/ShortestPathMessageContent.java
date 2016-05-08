package shortestpath;

import java.util.ArrayList;
import java.util.HashMap;

import util.Pair;
import common.MessageContent;

public class ShortestPathMessageContent extends MessageContent {
	private HashMap<Pair, ArrayList<Integer>> paths;

	public ShortestPathMessageContent(
			HashMap<Pair, ArrayList<Integer>> paths) {
		super();
		this.paths = paths;
	}
	
	public HashMap<Pair, ArrayList<Integer>> getPaths() {
		return paths;
	}
}
