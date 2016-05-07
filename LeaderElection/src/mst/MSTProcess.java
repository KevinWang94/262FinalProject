package mst;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import common.CostTracker;
import common.Message;
import common.MessageContent;
import common.Process;

public class MSTProcess extends Process {
	public static final int SN_SLEEPING = 1;
	public static final int SN_FIND = 2;
	public static final int SN_FOUND = 3;

	public static final int SE_BASIC = 1;
	public static final int SE_BRANCH = 2;
	public static final int SE_REJECTED = 3;

	int ln;
	int sn;
	double fn;
	HashMap<Integer, Integer> se;
	int findCount = 0;
	int testEdge = -1; // -1 is used as nil value
	double bestWt = Double.MAX_VALUE;
	int bestEdge = -1;
	int inBranch = -1;

	public MSTProcess(int id, int[] allProcesses,
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
	 * @return
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

	public void wakeup() throws InterruptedException {
		System.out.println("wakeup " + this.id);
		int minEdge = getMinEdge();
		se.put(minEdge, SE_BRANCH);
		sn = SN_FOUND;
		ln = 0;
		double[] args = new double[1];
		args[0] = 0;
		this.sendMessage(new Message(id,
				minEdge, new MSTMessageContent(
						MSTMessageContent.MSG_CONNECT, args)));
	}

	public void processConnect(Message m) throws InterruptedException {
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
					new MSTMessageContent(MSTMessageContent.MSG_INITIATE,
							newargs)));
			if (sn == SN_FIND) {
				findCount++;
			}
		} else if (se.get(sender) == SE_BASIC) {
			this.incomingMessages.put(m);
		} else {
			double[] newargs = new double[3];
			newargs[0] = ln + 1;
			newargs[1] = costs.get(id).get(sender);
			newargs[2] = SN_FIND;
			this.sendMessage(new Message(id, sender,
					new MSTMessageContent(MSTMessageContent.MSG_INITIATE,
							newargs)));
		}
	}

	public void processAccept(int sender) {
		testEdge = -1;
		double newCost = costs.get(id).get(sender);
		if (newCost < bestWt) {
			bestEdge = sender;
			bestWt = newCost;
		}
		report();
	}

	public void processReject(int sender) {
		if (se.get(sender) == SE_BASIC) {
			se.put(sender, SE_REJECTED);
		}
		test();
	}

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
					System.out.println("Leader is " + this.leaderId);
				}
			}
		}
	}

	public void changeRoot() {
		if (se.get(bestEdge) == SE_BRANCH) {
			try {
				this.sendMessage(new Message(id, bestEdge,
						new MSTMessageContent(MSTMessageContent.MSG_CHANGEROOT,
								null)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		} else {
			double[] args = new double[1];
			args[0] = ln;
			try {
				this.sendMessage(new Message(id, bestEdge,
						new MSTMessageContent(MSTMessageContent.MSG_CONNECT,
								args)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
			se.put(bestEdge, SE_BRANCH);
		}
	}

	public void processChangeRoot() {
		changeRoot();
	}

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
				try {
					this.sendMessage(new Message(id, nextId,
							new MSTMessageContent(
									MSTMessageContent.MSG_INITIATE, newargs)));
					if (sn == SN_FIND) {
						findCount = findCount + 1;
					}
				} catch (InterruptedException e) {
					System.err.println("Failed to send message.\n");
				}
			}
		}
		if (sn == SN_FIND) {
			this.test();
		}
	}

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
			try {
				this.sendMessage(new Message(id, testEdge,
						new MSTMessageContent(MSTMessageContent.MSG_TEST,
								newargs)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		} else {
			testEdge = -1;
			this.report();
		}
	}

	public void processTest(Message m) throws InterruptedException {
		if (sn == SN_SLEEPING) {
			this.wakeup();
		}

		double[] args = ((MSTMessageContent) m.getContent()).getArgs();
		int l = (int) args[0];
		double f = args[1];

		if (l > ln) {
			incomingMessages.put(m);
		} else if (f != fn) {
			this.sendMessage(new Message(id, m.getSender(),
					new MSTMessageContent(MSTMessageContent.MSG_ACCEPT, null)));
		} else {
			if (se.get(m.getSender()) == SE_BASIC) {
				se.put(m.getSender(), SE_REJECTED);
				if (testEdge != m.getSender()) {
					this.sendMessage(
							new Message(id, m.getSender(),
									new MSTMessageContent(
											MSTMessageContent.MSG_REJECT, null)));
				} else {
					this.test();
				}
			}
		}
	}

	public void report() {
		if (findCount == 0 && testEdge == -1) {
			System.out.println("report " + inBranch + " " + id);
			sn = SN_FOUND;
			double[] args = new double[1];
			args[0] = bestWt;
			try {
				this.sendMessage(new Message(id, inBranch,
						new MSTMessageContent(MSTMessageContent.MSG_REPORT,
								args)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		} else {
			System.out.println("no report " + inBranch + " " + id);
		}
	}

	public void electLeader() throws InterruptedException {
		//TODO
	}

	@Override
	public void broadcast(MessageContent mContent) throws InterruptedException {
		//TODO
	}
	

	@Override
	public void queryLeader(String queryString) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
		
	public void processMessageSpecial(Message m) throws InterruptedException {
		MSTMessageContent msg = (MSTMessageContent) m.getContent();
		// TODO: costs need to be registered here
		if (msg.getType() == MSTMessageContent.MSG_CONNECT) {
			System.out.println("connect " + this.id);
			processConnect(m);
		} else if (msg.getType() == MSTMessageContent.MSG_ACCEPT) {
			System.out.println("accept " + this.id);
			processAccept(m.getSender());
		} else if (msg.getType() == MSTMessageContent.MSG_REJECT) {
			System.out.println("reject " + this.id);
			processReject(m.getSender());
		} else if (msg.getType() == MSTMessageContent.MSG_REPORT) {
			System.out.println("report " + this.id);
			processReport(m);
		} else if (msg.getType() == MSTMessageContent.MSG_CHANGEROOT) {
			System.out.println("changeroot " + this.id);
			processChangeRoot();
		} else if (msg.getType() == MSTMessageContent.MSG_INITIATE) {
			System.out.println("initiate " + this.id);
			processInitiate(m);
		} else if (msg.getType() == MSTMessageContent.MSG_TEST) {
			System.out.println("test " + this.id);
			processTest(m);
		}
	}

	@Override
	public void triggerLeaderElection() throws InterruptedException {
		// TODO Auto-generated method stub
	}
}
