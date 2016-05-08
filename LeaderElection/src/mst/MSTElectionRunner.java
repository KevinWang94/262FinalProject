package mst;

import common.ElectionRunner;
import common.Message;
import common.Process;
import mst.MSTProcess;

public class MSTElectionRunner extends ElectionRunner {
	
	public static void main(String[] args) {
		int[] ids = genIds(10);
		instantiateAndRun(ids, genCosts(ids));
	}

}
