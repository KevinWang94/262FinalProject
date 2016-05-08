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

public abstract class MSTBase extends Process {
	public static final int SN_SLEEPING = 1;
	public static final int SN_FIND = 2;
	public static final int SN_FOUND = 3;

	public static final int SE_BASIC = 1;
	public static final int SE_BRANCH = 2;
	public static final int SE_REJECTED = 3;

	int ln;
	int sn;
	double fn;
	protected HashMap<Integer, Integer> se;
	int findCount = 0;
	int testEdge = -1; // -1 is used as nil value
	double bestWt = Double.MAX_VALUE;
	int bestEdge = -1;
	protected int inBranch = -1;
	protected int numChildren = -1;

	public MSTBase(int id, int[] allProcesses, HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues, LinkedBlockingQueue<Message> incomingMessages,
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
	 * @return
	 */
	public int getMinEdge() {
		int minEdge = -1;
		double minCost = Double.MAX_VALUE;
		HashMap<Integer, Double> edgeCosts = costs.get(id);
		Iterator<Map.Entry<Integer, Double>> it = edgeCosts.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Double> pair = it.next();
			if (pair.getValue() < minCost) {
				minEdge = pair.getKey();
				minCost = pair.getValue();
			}
		}
		return minEdge;
	}

	public void wakeup() {
		System.out.println(this.id + " " + this.fn + " wakeup");
		int minEdge = getMinEdge();
		se.put(minEdge, SE_BRANCH);
		sn = SN_FOUND;
		ln = 0;
		findCount = 0;
		double[] args = new double[1];
		args[0] = 0;
		this.sendMessage(new Message(id, minEdge, MessageType.MSG_MST_CONNECT, new MSTMessageContent(args)));
	}

	public void processConnect(Message m) {
		System.out.println(this.id + " " + this.fn + " process connect from " + m.getSender());
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
			this.sendMessage(new Message(id, sender, MessageType.MSG_MST_INITIATE, new MSTMessageContent(newargs)));
			if (sn == SN_FIND) {
				findCount++;
			}
		} else if (se.get(sender) == SE_BASIC) {
			try {
				this.incomingMessages.put(m);
			} catch (InterruptedException e) {
				System.out.println("Couldn't add incoming message back to queue");
				e.printStackTrace();
			}
		} else {
			double[] newargs = new double[3];
			newargs[0] = ln + 1;
			newargs[1] = costs.get(id).get(sender);
			newargs[2] = SN_FIND;
			this.sendMessage(new Message(id, sender, MessageType.MSG_MST_INITIATE, new MSTMessageContent(newargs)));
		}
	}

	public void processAccept(int sender) {
		System.out.println(this.id + " " + this.fn + " process accept");
		testEdge = -1;
		double newCost = costs.get(id).get(sender);
		if (newCost < bestWt) {
			bestEdge = sender;
			bestWt = newCost;
		}
		report();
	}

	public void processReject(int sender) {
		System.out.println(this.id + " " + this.fn + " process reject");
		if (se.get(sender) == SE_BASIC) {
			se.put(sender, SE_REJECTED);
		}
		test();
	}

	public void processReport(Message m) {
		System.out.println(this.id + " " + this.fn + " process report " + this.inBranch);
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
						for (int i = 0; i < allProcesses.length; i++) {
							for (int j = i + 1; j < allProcesses.length; j++) {
								System.out.println(allProcesses[i] + " " + allProcesses[j] + ": "
										+ costs.get(allProcesses[i]).get(allProcesses[j]));
							}
						}

						System.out.println("Leader is " + this.leaderId);
						double[] newargs = new double[1];
						newargs[0] = leaderId;
						this.sendMessage(new Message(id, leaderId, MessageType.MSG_MST_FINISH, new MSTMessageContent(newargs)));
					}
				}
			}
		}
	}

	public void changeRoot() {
		System.out.println(this.id + " " + this.fn + " changeRoot");
		if (se.get(bestEdge) == SE_BRANCH) {
			this.sendMessage(new Message(id, bestEdge, MessageType.MSG_MST_CHANGEROOT, new MSTMessageContent(null)));
		} else {
			double[] args = new double[1];
			args[0] = ln;
			this.sendMessage(new Message(id, bestEdge, MessageType.MSG_MST_CONNECT, new MSTMessageContent(args)));
			se.put(bestEdge, SE_BRANCH);
		}
	}

	public void processChangeRoot() {
		System.out.println(this.id + " " + this.fn + " process changeRoot");
		changeRoot();
	}

	public void processInitiate(Message m) {
		System.out.println(this.id + " " + this.fn + " process initiate");
		double[] args = ((MSTMessageContent) m.getContent()).getArgs();
		ln = (int) args[0];
		fn = args[1];
		sn = (int) args[2];
		System.out.println(id + " initiate from " + m.getSender() + ": " + ln + " " + fn + " " + sn);
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
				this.sendMessage(new Message(id, nextId, MessageType.MSG_MST_INITIATE, new MSTMessageContent(newargs)));
				if (sn == SN_FIND) {
					findCount = findCount + 1;
				}
			}
		}
		if (sn == SN_FIND) {
			this.test();
		}
	}

	public void test() {
		System.out.println(this.id + " " + this.fn + " test");
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
			this.sendMessage(new Message(id, testEdge, MessageType.MSG_MST_TEST, new MSTMessageContent(newargs)));
		} else {
			testEdge = -1;
			this.report();
		}
	}

	public void processTest(Message m) {
		System.out.println(this.id + " " + this.fn + " process test");
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
			// TODO remove MessageContent here w/o causing null ptr

			this.sendMessage(new Message(id, m.getSender(), MessageType.MSG_MST_ACCEPT, new MSTMessageContent(null)));
		} else {
			if (se.get(m.getSender()) == SE_BASIC) {
				se.put(m.getSender(), SE_REJECTED);
			}
			if (testEdge != m.getSender()) {
				// TODO remove messageContent here in a way which doesn't
				// include null ptrs
				this.sendMessage(
						new Message(id, m.getSender(), MessageType.MSG_MST_REJECT, new MSTMessageContent(null)));
			} else {
				this.test();
			}
		}
	}

	public void report() {
		System.out.println(this.id + " " + this.fn + " report");
		if (findCount == 0 && testEdge == -1) {
			System.out.println(id + " report " + inBranch);
			sn = SN_FOUND;
			double[] args = new double[1];
			args[0] = bestWt;
			this.sendMessage(new Message(id, inBranch, MessageType.MSG_MST_REPORT, new MSTMessageContent(args)));
		} else {
			System.out.println(id + " no report " + inBranch);
		}
	}
	
	protected boolean passMessage(MessageType messageType, MessageContent m) {
		Iterator<Integer> it = se.keySet().iterator();
		boolean isLeaf =  true;
		int count = 0;
		while (it.hasNext()) {
			int nextId = it.next();
			if ((id == leaderId || nextId != inBranch) && se.get(nextId) == SE_BRANCH) {
				isLeaf = false;
				count = count + 1;
				this.sendMessage(new Message(id, nextId, messageType, m));
			}
		}
		if (numChildren < 0)
			numChildren = count;
		return isLeaf;
	}

	public abstract void processFinish(Message m);

}