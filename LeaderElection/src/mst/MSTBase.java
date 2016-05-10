package mst;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.MessageContent;
import common.Process;
import common.Message.MessageType;

/**
 * This is an abstract subclass of Process simulating processes that require
 * computing an MST for leader election. This includes MSTProcess
 * {@link MSTProcess} and {@link ShortestPathProcess}.
 * 
 * This class contains all functions handling messages related to the
 * construction of the MST. Once the MST is constructed, a message is sent to
 * all processes, which is handled by the abstract function processFinish. This
 * allows the subclass to perform different actions upon constructing the MST.
 */
public abstract class MSTBase extends Process {
	// STATIC CONSTANTS
	// ///////////////////////////////////////////////////////////

	/**
	 * The state the process is in. SN_SLEEPING if in initial state before any
	 * messages have been received. SN_FIND if participating in a fragment's
	 * search for min edge. SN_FOUND otherwise.
	 */
	public static final int SN_SLEEPING = 1;
	public static final int SN_FIND = 2;
	public static final int SN_FOUND = 3;

	/**
	 * The state the edge is in for a process. The same edge may not always be
	 * in the same state at its two endpoints. SE_BRANCH if the edge is a branch
	 * in the current fragment. SE_REJECTED if the edge is not a branch but
	 * connects to another process in the same fragment. SE_BASIC otherwise.
	 */
	public static final int SE_BASIC = 1;
	public static final int SE_BRANCH = 2;
	public static final int SE_REJECTED = 3;

	// INSTANCE FIELDS
	// ////////////////////////////////////////////////////////////

	/**
	 * Indicates the level number of the process. All processes start at level
	 * 0. When two fragments of level n connect, they create a new fragment of
	 * level n+1. When a fragment finds its minimum outgoing edge, its level
	 * number and the level number of the other fragment determines what
	 * happens. These rules reduce the number of messages required, as described
	 * in the paper.
	 */
	int ln;

	/**
	 * The state the process is in, either SN_SLEEPING, SN_FIND, or SN_FOUND.
	 */
	int sn;

	/**
	 * The fragment number of the fragment the process is in. The weight of the
	 * core edge is used as the fragment number.
	 */
	double fn;

	/**
	 * A hashmap of the state edges are in. The key is the id of the process,
	 * and the value is either SE_BASIC, SE_BRANCH, or SE_REJECTED.
	 */
	protected HashMap<Integer, Integer> se;

	/**
	 * A count of the number of processes a response is still expected from. It
	 * is incremented when this process initializes another process to the FIND
	 * state and decremented when a REPORT message is received.
	 */
	int findCount = 0;

	/**
	 * The edge that is being tested. A process sends a test to its minimum
	 * weight basic edge (testEdge), and expects to receive either ACCEPT or
	 * REJECT. If rejected, testEdge is set to REJECTED and the process repeats.
	 * -1 is used as a nil value when no edge is being tested.
	 */
	int testEdge = -1;

	/**
	 * The minimum outgoing edge weight found so far by this process and its
	 * children.
	 */
	double bestWt = Double.MAX_VALUE;

	/**
	 * If bestWt is of an edge connecting to this process, the id of the other
	 * endpoint. Otherwise, the process that found the bestWt edge.
	 * 
	 */
	int bestEdge = -1;

	/**
	 * The process initiate was received from. Also the process this node will
	 * report to.
	 */
	protected int inBranch = -1;

	/**
	 * Once the MST is found, the number of children this process has, given
	 * that the core is the root.
	 */
	protected int numChildren = -1;

	/**
	 * The number of branch edges this process has. If it is the leader, it is
	 * equal to numChildren. Otherwise, numChildren + 1.
	 */
	protected int numBranch = -1;

	// CONSTRUCTOR ////////////////////////////////////////////////////////////
	/**
	 * Constructor. Calls the {@link Process} constructor, and also initalizes
	 * MST construction variables to defaults.
	 * 
	 * @param id
	 * @param allProcesses
	 * @param costs
	 * @param queues
	 * @param incomingMessages
	 * @param costTracker
	 */
	public MSTBase(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages,
			CostTracker costTracker) {
		super(id, allProcesses, costs, queues, incomingMessages, costTracker);
		this.ln = 0;
		this.sn = SN_SLEEPING;
		this.fn = -1;
		this.se = new HashMap<Integer, Integer>();
		Iterator<Integer> it = costs.get(id).keySet().iterator();
		while (it.hasNext()) {
			int nextId = it.next();
			this.se.put(nextId, SE_BASIC);
		}
	}

	/**
	 * Gets the index of the minimum adjacent edge.
	 * 
	 * @return id of minimum adjacent edge.
	 */
	public int getMinEdge() {
		int minEdge = -1;
		double minCost = Double.MAX_VALUE;
		HashMap<Integer, Double> edgeCosts = costs.get(id);
		Iterator<Map.Entry<Integer, Double>> it = edgeCosts.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Double> pair = it.next();
			if (pair.getValue() < minCost) {
				minEdge = pair.getKey();
				minCost = pair.getValue();
			}
		}
		return minEdge;
	}

	/**
	 * Wakes up current process. Is called on first process to trigger election,
	 * and is also called if a process receives a message while in SLEEPING
	 * state. This function sets its state to found, sets the minimum outgoing
	 * edge to BRANCH, and sends a CONNECT message to the process connected by
	 * the minimum outgoing edge.
	 */
	public void wakeup() {
		int minEdge = getMinEdge();
		se.put(minEdge, SE_BRANCH);
		sn = SN_FOUND;
		ln = 0;
		findCount = 0;
		double[] args = new double[1];
		args[0] = 0;
		this.sendMessage(new Message(id, minEdge, MessageType.MSG_MST_CONNECT,
				new MSTMessageContent(args)));
	}

	/**
	 * Processes the CONNECT message. 
	 * 
	 * 1. If the sender's level is lower than this process's, the sender's 
	 * fragment joins this fragment, so an INITIATE is sent to the sender. 
	 * 2. Otherwise, if the sender's edge is in a BASIC state, we make the 
	 * sender wait until the state has changed. 
	 * 3. Otherwise, the edge must be in the BRANCH state, since 
	 * a process would not send a CONNECT message to a process in the
	 * same fragment. Then, the two processes share the same minimum
	 * weight outgoing edge, so the process sends a new INITIATE message
	 * to combine the two fragments to form a new fragment with level 1 
	 * higher than the previous level.
	 * 
	 * @param m: the message being processed. Must be of type MSG_MST_CONNECT.
	 */
	public void processConnect(Message m) {
		int sender = m.getSender();
		double[] args = ((MSTMessageContent) m.getContent()).getArgs();

		if (sn == SN_SLEEPING) {
			wakeup();
		}

		if (args[0] < ln) {
			se.put(sender, SE_BRANCH);
			double[] newargs = new double[3];
			newargs[0] = ln;
			newargs[1] = fn;
			newargs[2] = sn;
			this.sendMessage(new Message(id, sender,
					MessageType.MSG_MST_INITIATE,
					new MSTMessageContent(newargs)));
			if (sn == SN_FIND) {
				findCount++;
			}
		} else if (se.get(sender) == SE_BASIC) {
			try {
				this.incomingMessages.put(m);
			} catch (InterruptedException e) {
				System.out
						.println("Couldn't add incoming message back to queue");
				e.printStackTrace();
			}
		} else {
			double[] newargs = new double[3];
			newargs[0] = ln + 1;
			newargs[1] = costs.get(id).get(sender);
			newargs[2] = SN_FIND;
			this.sendMessage(new Message(id, sender,
					MessageType.MSG_MST_INITIATE,
					new MSTMessageContent(newargs)));
		}
	}
	
	/**
	 * Processes an ACCEPT message. A process may receive an ACCEPT
	 * message from a process it has sent a TEST message to. If an ACCEPT 
	 * is received, the sender of the message is the minimum outgoing 
	 * BASIC edge from the process. If its cost is lower than the current 
	 * bestWt, it is recorded as bestEdge and bestWt.
	 * 
	 * @param sender: the sender of the ACCEPT message
	 */
	public void processAccept(int sender) {
		testEdge = -1;
		double newCost = costs.get(id).get(sender);
		if (newCost < bestWt) {
			bestEdge = sender;
			bestWt = newCost;
		}
		report();
	}

	/**
	 * Processes a REJECT message. A process may receive a REJECT
	 * message from a process it has sent a TEST message to. If a 
	 * REJECT message is received, the edge is set to the REJECTED
	 * state, and the process tests again.
	 * 
	 * @param sender: the sender of the REJECT message
	 */
	public void processReject(int sender) {
		if (se.get(sender) == SE_BASIC) {
			se.put(sender, SE_REJECTED);
		}
		test();
	}

	/**
	 * Processes a REPORT message. A process receives REPORT messages
	 * from processes that it initiated. If the sender is the process
	 * that initiated it (inBranch), and the process is not in state FIND,
	 * then a new core has been found. If the sender is not the process
	 * that initiated it, update bestWt and bestEdge if it is lower
	 * cost than the previous best, and subtract one from findCount.
	 * 
	 * @param m: the REPORT message
	 */
	public void processReport(Message m) {
		MSTMessageContent msg = (MSTMessageContent) m.getContent();
		double w = (msg.getArgs())[0];
		int sender = m.getSender();
		if (sender != inBranch) {
			findCount -= 1;
			if (w < bestWt) {
				bestWt = w;
				bestEdge = sender;
			}
			report();
		} else {
			if (sn == SN_FIND) {
				try {
					incomingMessages.put(m);
				} catch (InterruptedException e) {
					System.err.println("Failed to find incoming messages\n");
				}
			} else {
				if (w > bestWt) {
					changeRoot();
				} else if ((w == bestWt) && (w == Double.MAX_VALUE)) {
					this.leaderId = Math.min(id, sender);

					if (id == leaderId) {
						this.isLeader = true;
						System.out.println("MST Leader is " + this.leaderId);
						double[] newargs = new double[1];
						newargs[0] = leaderId;
						this.sendMessage(new Message(id, leaderId,
								MessageType.MSG_MST_FINISH,
								new MSTMessageContent(newargs)));
					}
				}
			}
		}
	}


	/**
	 * Changes the root of a fragment.
	 */
	public void changeRoot() {
		if (se.get(bestEdge) == SE_BRANCH) {
			this.sendMessage(new Message(id, bestEdge,
					MessageType.MSG_MST_CHANGEROOT, new MSTMessageContent(null)));
		} else {
			double[] args = new double[1];
			args[0] = ln;
			this.sendMessage(new Message(id, bestEdge,
					MessageType.MSG_MST_CONNECT, new MSTMessageContent(args)));
			se.put(bestEdge, SE_BRANCH);
		}
	}

	/**
	 * Processes the CHANGE_ROOT message, by calling the 
	 * {@link changeRoot} function.
	 */
	public void processChangeRoot() {
		changeRoot();
	}

	
	/**
	 * Processes the INITIATE message. INITIATE messages have level
	 * number, fragment number, and state as the arguments. The
	 * process sets its attributes according to the message. It
	 * also passes the INITIATE message along its branches,
	 * incrementing findCount each time (to keep track of which
	 * processes it is still waiting on a report from). If it
	 * is in the FIND state, it also calls {@link test} to find 
	 * its own minimum weight outgoing edge.
	 * 
	 * @param m
	 */
	public void processInitiate(Message m) {
		double[] args = ((MSTMessageContent) m.getContent()).getArgs();
		ln = (int) args[0];
		fn = args[1];
		sn = (int) args[2];
		inBranch = m.getSender();
		bestEdge = -1;
		bestWt = Double.MAX_VALUE;
		Iterator<Integer> it = se.keySet().iterator();
		while (it.hasNext()) {
			int nextId = it.next();
			if (nextId != m.getSender() && se.get(nextId) == SE_BRANCH) {
				double[] newargs = new double[3];
				newargs[0] = ln;
				newargs[1] = fn;
				newargs[2] = sn;
				this.sendMessage(new Message(id, nextId,
						MessageType.MSG_MST_INITIATE, new MSTMessageContent(
								newargs)));
				if (sn == SN_FIND) {
					findCount = findCount + 1;
				}
			}
		}
		if (sn == SN_FIND) {
			this.test();
		}
	}

	
	/**
	 * The function called to find this processes minimum weight
	 * outgoing edge. It sends a TEST message to the minimum
	 * weight BASIC edge, which will either ACCEPT or REJECT. If
	 * REJECT, test is called again, which will find the next
	 * smallest weight edge.
	 * If there does not exist a BASIC edge, {@link report} is called.
	 */
	public void test() {
		boolean hasBasic = false;
		Iterator<Integer> it = se.keySet().iterator();
		double weight = Double.MAX_VALUE;
		while (it.hasNext()) {
			int nextId = it.next();
			if (se.get(nextId) == SE_BASIC) {
				hasBasic = true;
				double currweight = costs.get(id).get(nextId);
				if (currweight < weight) {
					testEdge = nextId;
					weight = currweight;
				}
			}
		}
		if (hasBasic) {
			double[] newargs = new double[2];
			newargs[0] = ln;
			newargs[1] = fn;
			this.sendMessage(new Message(id, testEdge,
					MessageType.MSG_MST_TEST, new MSTMessageContent(newargs)));
		} else {
			testEdge = -1;
			this.report();
		}
	}

	/**
	 * Processes TEST messages. If the incoming process has a higher
	 * level number, wait. If the fragment numbers are not equal, send
	 * an ACCEPT message, since it is an outgoing edge. Otherwise, send 
	 * the REJECT message, since they are in the same fragment.
	 * 
	 * @param m: the TEST message being processed
	 */
	public void processTest(Message m) {
		if (sn == SN_SLEEPING) {
			this.wakeup();
		}

		double[] args = ((MSTMessageContent) m.getContent()).getArgs();
		int l = (int) args[0];
		double f = args[1];

		if (l > ln) {
			try {
				incomingMessages.put(m);
			} catch (InterruptedException e) {
				System.out.println("Failure putting message back in queue");
				e.printStackTrace();
			}
		} else if (f != fn) {
			this.sendMessage(new Message(id, m.getSender(),
					MessageType.MSG_MST_ACCEPT, null));
		} else {
			if (se.get(m.getSender()) == SE_BASIC) {
				se.put(m.getSender(), SE_REJECTED);
			}
			if (testEdge != m.getSender()) {
				this.sendMessage(new Message(id, m.getSender(),
						MessageType.MSG_MST_REJECT, null));
			} else {
				this.test();
			}
		}
	}

	
	/**
	 * Checks conditions for reporting and reports if met. If this
	 * process has found its own minimum weight outgoing edge and has
	 * received responses from all its children, then send a REPORT
	 * back to its parent. Otherwise, do nothing.
	 */
	public void report() {
		if (findCount == 0 && testEdge == -1) {
			sn = SN_FOUND;
			double[] args = new double[1];
			args[0] = bestWt;
			this.sendMessage(new Message(id, inBranch,
					MessageType.MSG_MST_REPORT, new MSTMessageContent(args)));
		}
	}

	
	/**
	 * Passes a message to all of its children in the MST. Allows propagation
	 * of messages along the edges of an MST. Also checks if this process is
	 * a leaf.
	 * 
	 * @param messageType: the type of the message
	 * @param m: the message content
	 * @return true if the process is a leaf, false otherwise.
	 */
	protected boolean passMessageMST(MessageType messageType, MessageContent m) {
		Iterator<Integer> it = se.keySet().iterator();
		boolean isLeaf = true;
		int count = 0;
		while (it.hasNext()) {
			int nextId = it.next();
			if ((id == leaderId || nextId != inBranch)
					&& se.get(nextId) == SE_BRANCH) {
				isLeaf = false;
				count = count + 1;
				this.sendMessage(new Message(id, nextId, messageType, m));
			}
		}
		if (numChildren < 0) {
			numChildren = count;
			numBranch = isLeader ? numChildren : numChildren + 1;
		}
		return isLeaf;
	}

	/**
	 * A function to process the FINISH message, which indicates the MST has been
	 * found.
	 * 
	 * @param m: the received FINISH message.
	 */
	public abstract void processFinish(Message m);


	/**
	 * Takes a message and calls the appropriate function. Returns whether
	 * the process should exit.
	 * 
	 * @param m: the message to process
	 * @return true if the process is finished and should terminate, 
	 * otherwise false.
	 */
	public boolean processMessageSpecial(Message m) {
		switch (m.getType()) {
		case MSG_MST_CONNECT:
			processConnect(m);
			return true;
		case MSG_MST_ACCEPT:
			processAccept(m.getSender());
			return true;
		case MSG_MST_REJECT:
			processReject(m.getSender());
			return true;
		case MSG_MST_REPORT:
			processReport(m);
			return true;
		case MSG_MST_CHANGEROOT:
			processChangeRoot();
			return true;
		case MSG_MST_INITIATE:
			processInitiate(m);
			return true;
		case MSG_MST_TEST:
			processTest(m);
			return true;
		case MSG_MST_FINISH:
			processFinish(m);
			return true;
		default:
			return false;
		}
	}
}
