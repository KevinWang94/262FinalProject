package mst;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
	int messagesSent = 0;
	double messagesSentCost = 0;

	public MSTProcess(int id, int[] allProcesses,
			HashMap<Integer, HashMap<Integer, Double>> costs,
			HashMap<Integer, LinkedBlockingQueue<Message>> queues,
			LinkedBlockingQueue<Message> incomingMessages) {
		super(id, allProcesses, costs, queues, incomingMessages);
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

	@Override
	public void sendMessage(int sendId, Message m) throws InterruptedException {
		super.sendMessage(sendId, m);
		if (((MSTMessageContent)m.getContent()).getType() < 8) {
			messagesSent++;
			messagesSentCost += costs.get(id).get(sendId);
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
		System.out.println(this.id + " " + this.fn + " wakeup");
		int minEdge = getMinEdge();
		se.put(minEdge, SE_BRANCH);
		sn = SN_FOUND;
		ln = 0;
		findCount = 0;
		double[] args = new double[1];
		args[0] = 0;
		this.sendMessage(minEdge, new Message(id,
				minEdge, new MSTMessageContent(
						MSTMessageContent.MSG_CONNECT, args)));
	}

	public void processConnect(Message m) throws InterruptedException {
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
			this.sendMessage(sender, new Message(id, sender,
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
			this.sendMessage(sender, new Message(id, sender,
					new MSTMessageContent(MSTMessageContent.MSG_INITIATE,
							newargs)));
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
						System.out.println("Leader is " + this.leaderId);
						System.out.println(id + ": " + messagesSent + " " + messagesSentCost);
						
						double[] newargs = new double[1];
						newargs[0] = leaderId;
						passMessage(new MSTMessageContent(
								MSTMessageContent.MSG_LEADER, newargs), -1);		
						
					}
				}
			}
		}
	}

	public void changeRoot() {
		System.out.println(this.id + " " + this.fn + " changeRoot");
		if (se.get(bestEdge) == SE_BRANCH) {
			try {
				this.sendMessage(bestEdge, new Message(id, bestEdge,
						new MSTMessageContent(MSTMessageContent.MSG_CHANGEROOT,
								null)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		} else {
			double[] args = new double[1];
			args[0] = ln;
			try {
				this.sendMessage(bestEdge, new Message(id, bestEdge,
						new MSTMessageContent(MSTMessageContent.MSG_CONNECT,
								args)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
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
				try {
					this.sendMessage(nextId, new Message(id, nextId,
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
			try {
				this.sendMessage(testEdge, new Message(id, testEdge,
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
		System.out.println(this.id + " " + this.fn + " process test");
		if (sn == SN_SLEEPING) {
			this.wakeup();
		}

		double[] args = ((MSTMessageContent) m.getContent()).getArgs();
		int l = (int) args[0];
		double f = args[1];

		if (l > ln) {
			incomingMessages.put(m);
		} else if (f != fn) {
			this.sendMessage(m.getSender(), new Message(id, m.getSender(),
					new MSTMessageContent(MSTMessageContent.MSG_ACCEPT, null)));
		} else {
			if (se.get(m.getSender()) == SE_BASIC) {
				se.put(m.getSender(), SE_REJECTED);
			}
			if (testEdge != m.getSender()) {
				this.sendMessage(
						m.getSender(),
						new Message(id, m.getSender(),
								new MSTMessageContent(
										MSTMessageContent.MSG_REJECT, null)));
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
			try {
				this.sendMessage(inBranch, new Message(id, inBranch,
						new MSTMessageContent(MSTMessageContent.MSG_REPORT,
								args)));
			} catch (InterruptedException e) {
				System.err.println("Failed to send message.\n");
			}
		} else {
			System.out.println(id + " no report " + inBranch);
		}
	}
	
	public void processLeader(Message m) {
		MSTMessageContent mContent = (MSTMessageContent) m.getContent();
		leaderId = (int) ((MSTMessageContent) mContent).getArgs()[0];
		System.out.println(m.getSender() + " to " + id);
		System.out.println(id + ": " + messagesSent + " " + messagesSentCost);
		double[] newargs = new double[1];
		newargs[0] = leaderId;
		passMessage(new MSTMessageContent(
				MSTMessageContent.MSG_LEADER, newargs), m.getSender());		
	}
	
	private void passMessage(MessageContent m, int originalSender) {
		Iterator<Integer> it = se.keySet().iterator();
		while (it.hasNext()) {
			int nextId = it.next();
			if (nextId != originalSender && se.get(nextId) == SE_BRANCH) {
				try {
					this.sendMessage(nextId, new Message(id, nextId, m));
				} catch (InterruptedException e) {
					System.err.println("Failed to send message.\n");
				}
			}
		}
	}

	public void electLeader() throws InterruptedException {
		//TODO
	}

	@Override
	public void broadcast(MessageContent mContent) throws InterruptedException {
		assert(id == this.leaderId);
	}
	

	@Override
	public void queryLeader(String queryString) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
		
	@Override
	public void processMessage(Message m) throws InterruptedException {
		MSTMessageContent msg = (MSTMessageContent) m.getContent();
		if (msg.getType() == MSTMessageContent.MSG_CONNECT) {
			processConnect(m);
		} else if (msg.getType() == MSTMessageContent.MSG_ACCEPT) {
			processAccept(m.getSender());
		} else if (msg.getType() == MSTMessageContent.MSG_REJECT) {
			processReject(m.getSender());
		} else if (msg.getType() == MSTMessageContent.MSG_REPORT) {
			processReport(m);
		} else if (msg.getType() == MSTMessageContent.MSG_CHANGEROOT) {
			processChangeRoot();
		} else if (msg.getType() == MSTMessageContent.MSG_INITIATE) {
			processInitiate(m);
		} else if (msg.getType() == MSTMessageContent.MSG_TEST) {
			processTest(m);
		} else if (msg.getType() == MSTMessageContent.MSG_LEADER) {
			processLeader(m);
		}
	}

	@Override
	public void triggerLeaderElection() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void leaderRoutine() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
}
