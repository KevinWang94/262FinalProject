package shortestpath;

import java.util.HashMap;

import util.Pair;
import util.PathInfo;
import common.MessageContent;

public class ShortestPathMessageContent extends MessageContent {
	
	private HashMap<Pair, PathInfo> paths;

	public ShortestPathMessageContent(
			HashMap<Pair, PathInfo> paths) {
		super();
		this.paths = paths;
	}
	
	public HashMap<Pair, PathInfo> getPaths() {
		return paths;
	}
}
