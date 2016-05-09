package util;

/**
 * A simple utility class for ordered pairs of integers
 *
 */
public class Pair {
	/**
	 * The two integers in the pair
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
	 * Simple getter for first value of pair
	 * 
	 * @return {@code fst}
	 */
	public int getFst() {
		return fst;
	}

	/**
	 * Simple getter for second value of pair
	 * 
	 * @return {@code snd}
	 */
	public int getSnd() {
		return snd;
	}

	/**
	 * Simple hash of pair's values; needed for this class to be hashable.
	 */
	public int hashCode() {
		return (fst * 31) ^ snd;
	}

	/**
	 * Implement equals as expected
	 * 
	 * @param o	object to compare this to
	 */
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair other = (Pair) o;
			return ((fst == other.getFst()) && (snd == other.getSnd()));
		}
		return false;
	}
}
