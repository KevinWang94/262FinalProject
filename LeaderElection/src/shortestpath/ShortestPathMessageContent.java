package shortestpath;

import java.util.HashMap;

import util.Pair;
import util.PathInfo;
import common.MessageContent;

/**
 * Implementation of message content for use in the shortest path
 * leader election algorithm, allowing messages to pass along 
 * currently assembled shortest path information.
 */
public class ShortestPathMessageContent extends MessageContent {
	
	/**
	 * The distance matrix used by {@link ShortestPathProcess}.
	 * 
	 * Pair is the pair of node ids, and path info is the cost of the
	 * current shortest path between these nodes, along with the
	 * actual path between these nodes (a list of IDs).
	 */
	private HashMap<Pair, PathInfo> paths;

	/**
	 * Constructor for ShortestPathMesageContent.
	 * @param paths: The paths stored in this.paths.
	 */
	public ShortestPathMessageContent(
			HashMap<Pair, PathInfo> paths) {
		super();
		this.paths = paths;
	}
	
	/**
	 * Getter for paths
	 * 
	 * @return partial distance matrix.
	 */
	public HashMap<Pair, PathInfo> getPaths() {
		return paths;
	}
}
