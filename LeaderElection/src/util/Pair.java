package util;

public class Pair {
	private int fst;
	private int snd;
	
	public Pair(int fst, int snd) {
		this.fst = fst;
		this.snd = snd;
	}

	public int fst() {
		return fst;
	}

	public int snd() {
		return snd;
	}
	
	public int hashCode() {
		return (fst * 31) ^ snd;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair other = (Pair) o;
			return ((fst == other.fst()) && (snd == other.snd()));
		}
		return false;
	}
}
