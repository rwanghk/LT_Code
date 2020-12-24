package com.wefeel.LT.Random;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Generate an array of non-repeating random integer
 * @author RWang
 * 
 *
 */
public class UniqueRNG extends Random {
	
	public UniqueRNG() {
		super();
	}
	
	public UniqueRNG(long seed) {
		super(seed);
	}
	
	/**
	 * Get an array of random but unique int from 0 (inclusive) to {@code max} exclusive 
	 * @param max Maximum value of integer to return
	 * @param count Number of ints to return
	 * @return int[]
	 */
	public int[] nextInts(int max, int count) {
		if (count > max) {
			throw new RuntimeException("More random value required than possible");
		}
		// Below a few hundreds the time taken for iterating and checking for 
		// uniqueness should be negligible on most platforms
		// If numbers to get is only a tiny fraction of the max value, it's 
		// likely much faster to simply repeat only when duplicates are found
		if (count < 200 || (((double) count) / max) < 0.1) {
			int[] c = new int[count];
			int i = 0; 
			outer:
			while (i < count) {
				c[i] = nextInt(max);
				for (int j = 0; j < i; j++) {
					if (c[j] == c[i]) continue outer; // retry if any duplicate found
				}
				i++; // Only increment if there's no duplicate
			}
			return c;
		} else { /*if (count * 2 >= max) {*/ 
			// probably asking too many unique numbers here, use a list of unique number
			// and shuffle. This may cause problem if max is too large but we're using 
			// int here, wouldn't be more than a few GiB...
			int[] rnd = new int[max];
			for (int i = 1; i < max; i++) {
				int j = nextInt(i);
				rnd[i] = rnd[j];
				rnd[j] = i;
			}
			int[] arr = new int[max];
			for (int i = 0; i < max; i++) {
				arr[i] = i;
			}
			shuffle(arr);
			return Arrays.copyOfRange(arr, 0, count);
		} 
//		else { 
//			// DEBUG 
//			// Set only allow unique values but doesn't normally retain the order of 
//			// which elements are added, so the outcome may be different on different
//			// platforms!!
//			Set<Integer> set = new HashSet<>();
//			while (set.size() < count) {
//				set.add(nextInt(max));
//			}
//			Integer[] arr0 = set.toArray(new Integer[0]);
//			int[] arr = new int[count];
//			for (int i = 0; i < count; i++) {
//				arr[i] = arr0[i];
//			}
//			shuffle(arr);
//			return arr;
//		}
	}
	
	/**
	 * Get an array of random but unique int from {@code min} (inclusive) to {@code max} exclusive 
	 * @param min Minimum value of integer to return
	 * @param max Maximum value of integer to return
	 * @param count Number of ints to return
	 * @return int[]
	 */
	public int[] nextInts(int min, int max, int count) {
		int maxTemp = max - min;
		int[] arr = nextInts(maxTemp, count); // Could be faster but that's more complicated
		for (int i = 0; i < arr.length; i++) {
			arr[i] += min;
		}
		return arr;
	}

	/**
	 * Shuffle the given array using {@code this} as the random generator, and 
	 * similar method as Java built in, except using primitive array directly
	 * @param arr
	 */
	protected void shuffle(int[] arr) {
		int index, temp;
		for (int i = arr.length - 1; i > 0; i--) {
			index = nextInt(i + 1);
			temp = arr[i];
			arr[i] = arr[index];
			arr[index] = temp;
		}
	}
}
