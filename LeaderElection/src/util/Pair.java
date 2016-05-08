package util;

public class Pair {
	private int fst;
	private int snd;
	
	public Pair(int fst, int snd) {
		this.setFst(fst);
		this.setSnd(snd);
	}

	public int fst() {
		return fst;
	}

	public void setFst(int fst) {
		this.fst = fst;
	}

	public int snd() {
		return snd;
	}

	public void setSnd(int snd) {
		this.snd = snd;
	}
}
