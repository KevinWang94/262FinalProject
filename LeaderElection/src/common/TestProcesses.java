package common;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestProcesses {
	public static void main(String[] args) {
		int n = 4;
		HashMap<Integer, LinkedBlockingQueue<Message>> queues = new HashMap<Integer, LinkedBlockingQueue<Message>>();
		HashMap<Integer, Process> processes = new HashMap<Integer, Process>();
		int[] ids = new int[n];
		
		for (int i = 0; i < n; i++) {
			//TODO: better generation of random id
			int id = (int) (Math.random()*1000);
			while (queues.containsKey(id)) {
				id = (int) (Math.random()*1000);
			}

			ids[i] = id;
			queues.put(id, new LinkedBlockingQueue<Message>());
		}
		
		for (int i = 0; i < n; i++) {
			Process curr = new Process(ids[i], ids, queues, queues.get(ids[i]));
			(new Thread(curr)).start();
			processes.put(ids[i], curr);
		}
		
		for (int i = 0; i < n; i++) {
			Process curr = new Process(ids[i], ids, queues, queues.get(ids[i]));
			try {
				curr.broadcast();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
