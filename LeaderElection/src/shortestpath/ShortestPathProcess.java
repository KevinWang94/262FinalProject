package shortestpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import util.Pair;
import util.PathInfo;
import common.CostTracker;
import common.Message;
import common.Message.MessageType;
import common.MessageContent;
import mst.MSTBase;
import mst.MSTProcess;

/**
 * This is a subclass of MSTBase. This class simulates a process that
 * implements leader election through constructing a matrix of all-pairs
 * shortest paths between nodes in the network, and selecting the
 * leader as the node with either: 1. minimal sum of shortest path cost
 * to any other node in the network, or 2. minimal max of shortest path
 * cost to any other node in the network. 
 * 
 * The current estimate of the shortest paths in the network are stored
 * in the partial distance matrix (called the pd matrix) at each node.
 * 
 * The class includes MSTBase because it first requires the construction of
 * an MST.
 * 
 * Broadcast operates via the connections of the MST, and query from a 
 * machine to the leader operates via the shortest path, according to the
 * all-pairs shortest path algorithm. 
 */
public class ShortestPathProcess extends MSTBase {
	// STATIC CONSTANTS
	// ///////////////////////////////////////////////////////////	
	/**
	 * Captures the state of a node in the network.
	 */
	public enum ShortestPathState {
		STATE_TRANSMIT, // Ready to transmit pd matrix
		STATE_RECEIVING, // Receiving pd matrix from neighbors 
		STATE_SATURATED, // Completed pd matrix and transmitting final matrix
		STATE_UNASSIGNED // State not assigned yet
	}
	
	/**
	 * Captures the leader election method
	 */
	public enum LeaderMethod {
		METHOD_MAX, // Choose leader based on the max of the shortest
					// paths from other nodes to the leader
		METHOD_SUM // Choose leader based on the sum of the shortest
					// paths from other nodes to the leader
	}

	// INSTANCE FIELDS
	// ////////////////////////////////////////////////////////////

	/**
	 * Indicates the state of the node in the network, choosing from
	 * the values in the ShortestPathState enum
	 */
	private ShortestPathState state;
	
	/**
	 * Contains the partial distance matrix assembled so far.
	 * 
	 * Pair is a pair of nodes, and PathInfo is our current estimate for the
	 * shortest path between these two nodes in the network
	 */
	HashMap<Pair, PathInfo> pd;
	
	/**
	 * This counts the number of pd matrices that have been sent from 
	 * the node's neighbors to it, keeping track of the point at which
	 * this node is ready to transmit its pd matrix further.
	 */
	int count;
	
	/**
	 * Captures the list of node ids that have transmitted their pd matrix
	 * to this node. Ultimately, when all but one neighbor has sent
	 * its pd matrix to this node, the node will be ready to
	 * transmit its pd matrix further.
	 */
	HashSet<Integer> seen;
	
	/**
	 * The method used to choose the leader based on the pd matrix, using
	 * the enum LeaderMethod described above.
	 */
	private LeaderMethod method;
	
	/**
	 * Counts the number of nodes that have acknowledged that they have
	 * chosen a leader. Used by the leader node only.
	 */
	int acksReceived = 0;
	
	
	// CONSTRUCTOR ////////////////////////////////////////////////////////////
	/**
	 * Constructs the shortest path node. All params are as used by 
	 * {@link MSTBase}. The initial node state is STATE_UNASSIGNED,
	 * and leader method is METHOD_SUM. 
	 * 
	 * @param id 
	 * @param allProcesses
	 * @param costs
	 * @param queues
	 * @param incomingMessages
	 * @param costTracker
	 */
	public ShortestPathProcess(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		state = ShortestPathState.STATE_UNASSIGNED;
		pd = new HashMap<Pair, PathInfo>();
		count = 0;
		seen = new HashSet<Integer>();
		method = LeaderMethod.METHOD_SUM;
	}

	/**
	 * At the conclusion of finding the MST, this method initializes the
	 * partial distance matrix to include updated values for neighboring nodes,
	 * and sets states for this node based on whether it is a leaf (ready 
	 * to transmit the pd matrix) or interior node. Calls nodeAction to 
	 * continue finding shortest path matrix.
	 * 
	 * @param m: Message which will be passed on to other nodes in the network.
	 */
	@Override
	public void processFinish(Message m) {
		initializePDMatrix();
		boolean isLeaf = passMessageMST(m.getType(), m.getContent());
		if (isLeaf) {
			state = ShortestPathState.STATE_TRANSMIT;
		} else {
			state = ShortestPathState.STATE_RECEIVING;
		}
		nodeAction();
	}
	
	/**
	 * Partial distance matrix initialization.
	 * 
	 * Case 1: Initializes the partial distance matrix to include the distance
	 * 0 between identical nodes and an empty path. 
	 * 
	 * Case 2: Includes the distance of the edge cost between this node
	 * id and any other node in the network. 
	 * 
	 * Case 3: Includes the distance of a first node to this node id, plus
	 * this node id to a second node, for the distance between a first
	 * and second node.
	 */
	public void initializePDMatrix() {
		for (int i = 0; i < allProcesses.length; i++) {
			for (int j = 0; j < allProcesses.length; j++) {
				int idI = allProcesses[i];
				int idJ = allProcesses[j];
				Pair pair = new Pair(idI, idJ);
				ArrayList<Integer> path = new ArrayList<Integer>();
				// Case 1
				path.add(idI); 
				double cost = 0;
				if (i != j) {
					// Case 2
					if ((idI == id) || (idJ == id)) {
						cost = costs.get(idI).get(idJ);
						path.add(idJ);
					} else { // Case 3
						cost = costs.get(idI).get(id);
						cost += costs.get(idJ).get(id);
						path.add(id);
						path.add(idJ);
					}
				}
				PathInfo pathInfo = new PathInfo(path, cost); 
				pd.put(pair, pathInfo);
			}
		}
	}
	
	/** 
	 * At first, the only action taken is to transmit if we are currently
	 * a leaf node.
	 */
	public void nodeAction() {
		if (state == ShortestPathState.STATE_TRANSMIT) {
			transmittingNodeProcess();
		}
	}

	/**
	 * Transmit the partial distance matrix to the branch that has
	 * not yet sent us a partial distance matrix. This ensures that we 
	 * don't send partial distance matrix to nodes that are further
	 * towards the leaves in the MST (thus ensuring progress of the 
	 * algorithm).
	 */
	public void transmittingNodeProcess() {
		int sendId = -1;
		// choose sender to be one that hasn't sent us a pd matrix yet.
		for (Integer i : se.keySet()) {
			// only choose branch edges - edges of the MST
			if ((se.get(i) == MSTProcess.SE_BRANCH) && 
					(!seen.contains(i))) {
				sendId = i;
				break;
			}
		}
		// Send partial distance matrix to remaining neighbor
		System.out.println("Transmitting partial distance matrix.");
		this.sendMessage(new Message(id, sendId, 
				MessageType.MSG_PATH_PARTIAL,
				new ShortestPathMessageContent(pd)));
	}
	
	/**
	 * Augment the partial distance matrix to reflect the new
	 * partial distance matrix information received from a neighbor. 
	 * This update follows the pattern of a dynamic programming
	 * shortest paths algorithm approach.
	 * 
	 * @param newPd: new partial distance matrix from a neighbor
	 */
	public void augmentPd(HashMap<Pair, PathInfo> newPd) {
		// update any new costs that beat this pd matrix's path costs
		for (Pair pair : newPd.keySet()) {
			if (newPd.get(pair).getCost() < pd.get(pair).getCost()) {
				pd.put(pair, newPd.get(pair));
			}
		}
		// go through each pair of nodes, and see if the above
		// updates have led to a new better shortest path including
		// the updated node.
		for (Pair pair : pd.keySet()) {
			for (Integer i : allProcesses) {
				int fst = pair.getFst();
				int snd = pair.getSnd();
				Pair pair1 = new Pair(fst, i);
				Pair pair2 = new Pair(i, snd);
				double cost1 = pd.get(pair1).getCost();
				double cost2 = pd.get(pair2).getCost();
				double newCost = cost1 + cost2;
				if (newCost < pd.get(pair).getCost()) {
					ArrayList<Integer> newPath = pd.get(pair1).getPath();
					newPath.addAll(pd.get(pair2).getPath());
					pd.put(pair, new PathInfo(newPath, newCost));
				}
			}
		}
	}
	
	/**
	 * Choose a leader based on the assembled partial distance matrix, 
	 * such that the leader has low cost of query to other nodes in the
	 * network based on the shortest paths.
	 * 
	 * Either uses the sum of shortest path distances or the max
	 * of shortest path distances, depending on the leader method specified.
	 */
	public void chooseLeader() {
		double bestVal = Double.MAX_VALUE; 
		int bestId = -1;
		for (Integer i : allProcesses) {
			double val = (method == LeaderMethod.METHOD_MAX) ? -1 : 0; 
			for (Pair pair : pd.keySet()) {
				if (pair.getFst() == i) {
					double newCost = pd.get(pair).getCost();
					if (method == LeaderMethod.METHOD_MAX) {
						if (newCost > val) {
							val = newCost;
						}
					} else {
						val += newCost;
					}
				}
			}
			if (val < bestVal) {
				bestVal = val;
				bestId = i;
			}
 		}
		// update leader value
		leaderId = bestId;
		isLeader = (leaderId == id);
	}
	
	/**
	 * Prints useful debug information on the network communication costs
	 * and current partial distance matrix shortest paths costs.
	 */
	public void printDebugInfo() {
		System.out.println("Leader: " + leaderId);
		System.out.println("Costs: ");
		for (Integer i : costs.keySet()) {
			for (Integer j : costs.get(i).keySet()) {
				System.out.println(i + " " + j + " " + costs.get(i).get(j));
			}
		}
		System.out.println("Path Costs: ");
		for (Integer i : costs.keySet()) {
			for (Integer j : costs.get(i).keySet()) {
				Pair pair = new Pair(i,j);
				System.out.println(i + " " + j + " " + pd.get(pair).getCost());
			}
		}
	}
	
	/**
	 * Here, a node sends a final pd matrix onto its neighbors, and updates
	 * its leader according to this matrix. It also acks the leader, indicating
	 * that it has completed a new leader choice.
	 * 
	 * @param noSendId: this id is a node id which this message is 
	 * not passed onto. This is useful for avoiding passing the message
	 * back to the sender message of the final pd matrix, thus avoiding loops of 
	 * message passing.
	 */
	public void sendFinalPaths(int noSendId) {
		// send to all MST edge neighbors, except for noSendId
		for (Integer i : se.keySet()) {
			if ((se.get(i) == MSTProcess.SE_BRANCH) && (i != noSendId)) {
				this.sendMessage(new Message(id, i, MessageType.MSG_PATH_FINAL,
						new ShortestPathMessageContent(pd)));				
			}
		}
		// choose a leader, and let the leader know that we have chosen
		chooseLeader();
		ackLeader();
	}
	
	/**
	 * Receive a partial distance matrix from a neighboring node. 
	 * Update our pd matrix with this augmented information.
	 * If we have received all but one neighbor's partial distance matrix, 
	 * begin transmitting our updated pd matrix to our neighbor.
	 * If we have received all our partial distance matrices, we have
	 * the final matrix and can submit a final pd matrix across the network.
	 * 
	 * @param m: message including update for pd matrix
	 */
	public void processPathPartial(Message m) {
		ShortestPathMessageContent mContent = (ShortestPathMessageContent) m.getContent();
		augmentPd(mContent.getPaths());
		count++;
		seen.add(m.getSender());
		if (count == numBranch - 1) {
			state = ShortestPathState.STATE_TRANSMIT;
			transmittingNodeProcess();
		}
		if (count == numBranch) {
			state = ShortestPathState.STATE_SATURATED;
			if (DEBUG) 
				printDebugInfo();
			sendFinalPaths(-1);
		}
	}
	
	/**
	 * Processes the signal that we have received a final pd matrix, and 
	 * sends matrix on to future nodes.
	 * @param m - includes the final pd matrix.
	 */
	public void processPathFinal(Message m) {
		ShortestPathMessageContent mContent = (ShortestPathMessageContent) m.getContent();
		HashMap<Pair, PathInfo> paths = mContent.getPaths();
		pd = paths;
		sendFinalPaths(m.getSender());
	}
	
	/**
	 * Switch statement for messages involved in shortest path
	 * leader election.
	 */
	public boolean processMessageSpecial(Message m) {
		boolean done = super.processMessageSpecial(m);
		if (done) 
			return true;
		switch (m.getType()) {
			case MSG_PATH_PARTIAL:
				processPathPartial(m);
				System.out.println("Processing path partial from " + m.getSender() + " to " + m.getReceiver());
				return true;
			case MSG_PATH_FINAL:
				processPathFinal(m);
				System.out.println("Processing path final from " + m.getSender() + " to " + m.getReceiver());
				return true;
			default:
				return false;
		}
	}

	/**
	 * To trigger leader election, begin the MST discovery process.
	 */
	@Override
	public void triggerLeaderElection() {
		wakeup();
	}
	
	/**
	 * Pass message to other MST edges, skipping the sender node.
	 * 
	 * @param messageType - message type to pass
	 * @param m - message content to pass
	 * @param sender - node to avoid
	 */
	protected void passMessagePath(MessageType messageType, MessageContent m, int sender) {
		for (Integer nextId : se.keySet()) {
			if ((nextId != sender) && se.get(nextId) == SE_BRANCH) {
				this.sendMessage(new Message(id, nextId, messageType, m));
			}
		}
	}

	/** 
	 * Broadcasts a message from the leader to other nodes in the network
	 */
	@Override
	public void broadcast(MessageType messageType, MessageContent mc) {
		assert (id == this.leaderId);
		passMessagePath(messageType, mc, -1);
	}

	/**
	 * Queries the leader by moving one step in the direction of the leader on the
	 * shortest path to the leader.
	 */
	@Override
	public void queryLeader(MessageContent mc) {
		assert(!isLeader);
		
		ArrayList<Integer> path = pd.get(new Pair(id, leaderId)).getPath();
		sendMessage(new Message(id, path.get(1), MessageType.MSG_QUERY_SIMPLE, mc));
	}
	
	/**
	 * Send a message to the leader by moving one step in the direction of the leader
	 * on the shortest path to the leader.
	 * 
	 * As the leader, count the number of acks that we have received, and 
	 * end when all of our neighbors have acknowledged the completion of leader election.
	 */
	@Override
	protected void ackLeader() {
		if (!isLeader) {
			ArrayList<Integer> path = pd.get(new Pair(id, leaderId)).getPath();
			sendMessage(new Message(id, path.get(1), MessageType.MSG_ACK_LEADER, null));
		} else {
			acksReceived++;
			if (acksReceived == numBranch) {
				System.out.println("Leader acked!");
				// next step of the workload, implemented in {@link Process}
				startWorkloadSimple();			
			}
		}
	}

	/**
	 * Ack the choice of a leader.
	 */
	@Override
	protected void processMessageAckLeader() {
		ackLeader();
	}

	/**
	 * Process a broadcast message as an intermediate node, 
	 * passing the broadcast to further nodes via the MST connections.
	 */
	@Override
	protected void processLeaderBroadcastSimple(Message m) {
		assert(!isLeader);
		passMessagePath(m.getType(), m.getContent(), m.getSender());
		super.processLeaderBroadcastSimpleForReceiver(m);
	}

	/** 
	 * Process a query to the leader, calling the method implemented in
	 * {@link Process} if a leader, and otherwise passing the
	 * message further in the direction of the leader.
	 */
	@Override
	protected boolean processQuerySimple(Message m) {
		if (id == leaderId) {
			return super.processQuerySimpleForLeader(m);
		} else {
			queryLeader(m.getContent());
			return false;
		}
	}
}
