package util;

/**
 * A simple utility class for pairs of integers
 *
 */
public class Pair {
	/**
	 * The fst and snd members of the pair
	 */
	private int fst;
	private int snd;

	/**
	 * Simple constructor
	 * 
	 * @param fst
	 * @param snd
	 */
	public Pair(int fst, int snd) {
		this.fst = fst;
		this.snd = snd;
	}

	/**
	 * Simple getter
	 * 
	 * @return fst
	 */
	public int getFst() {
		return fst;
	}

	/**
	 * Simple getter
	 * 
	 * @return snd
	 */
	public int getSnd() {
		return snd;
	}

	/**
	 * So this is hashable
	 */
	public int hashCode() {
		return (fst * 31) ^ snd;
	}

	/**
	 * Implement equals as expected
	 */
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair other = (Pair) o;
			return ((fst == other.getFst()) && (snd == other.getSnd()));
		}
		return false;
	}
}
