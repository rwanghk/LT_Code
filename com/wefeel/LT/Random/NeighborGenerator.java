package com.wefeel.LT.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generate the list of source frames to XOR for each encoded frame. <br>
 * <br>
 * This class give, for each encoded frame, a list of indexes of source 
 * frame (the {@literal Neighbors}). All neighbors are XOR'ed together into 
 * the encoded frame<br>
 * <br>
 * This class keeps two random generators, a {@code RobustSolitonGenerator} 
 * for number of frames to include, and a {@code UniqueRNG} that generate 
 * a list of non-repeating random numbers (so that in no circumstances will 
 * the same source frame be added twice and XOR with itself). The seeds for 
 * two generators are related by simple XOR. <br>
 * <br>
 * The class also stores internally a list of all previously generated 
 * neighbor indexes as rewinding the random generator can be tricky. 
 * 
 * @author R Wang
 *
 */
public class NeighborGenerator implements AutoCloseable {
	/** XOR with seed to make RNG and Soliton RNG with different seeds */
	public static final long SOLITON_SEED = 0x3062470030624770l;
	public static final double DEFAULT_C = 0.2;
	public static final double DEFAULT_FAILURE_PROBABILITY = 0.02; //2%
	public static final int DEFAULT_SPIKE = 50;

	private final UniqueRNG uniformRNG;
	private final RobustSolitonGenerator solitonRNG;
	private final long seed;
	/** Total number of packets */
	private final int nPackets;
	/** Index of this neighbor packet */
	private long index = -1;
	
	/**
	 * List of all previously generated neighbors are stored for 
	 * later reference
	 */
	private List<int[]> neighbors = new ArrayList<>();
	
	/**
	 * Initialize with custom parameters
	 * @param seed
	 * @param nPackets Number of packets in the source
	 * @param spike Spike position is the second peak of probability 
	 * @param failureProbability
	 */
	public NeighborGenerator(long seed, int nPackets, int spike, double failureProbability) {
		this.seed = seed;
		this.nPackets = nPackets;
		uniformRNG = new UniqueRNG(seed);
		solitonRNG = new RobustSolitonGenerator(
				nPackets, 
				spike > 0 ? spike : 1, 
				failureProbability, 
				seed ^ SOLITON_SEED);
	}
	
	/**
	 * Initialize with all default parameters. Notably, the spike will 
	 * be at {@code (ln(nPackets))^2} which give a number that feels 
	 * about right
	 * @param seed 
	 * @param nPackets
	 */
	public NeighborGenerator(long seed, int nPackets) {
		this(
				seed, 
				nPackets, 
				(int) Math.ceil(Math.log(nPackets) * Math.log(nPackets)), // Quick and dirty way to get a spike that feels right
				DEFAULT_FAILURE_PROBABILITY);
	}
	
	/**
	 * 
	 * @return The seed / nonce used by this generator
	 */
	public long getSeed() {
		return seed;
	}
	
	/**
	 * 
	 * @return -1 if not started yet, else a 0-based index as current index for the 
	 * distribution just returned
	 */
	public long getIndex() {
		return index;
	}
	
	/**
	 * 
	 * @return Index of Source frames to be included in the next encoded frame
	 */
	public int[] getNext() {
		index++;
		int neighborCount = solitonRNG.next(); // If d = 1, this is entry point 
		int[] arr = uniformRNG.nextInts(nPackets, neighborCount);
		neighbors.add(arr);
		return arr;
	}
	
	/**
	 * Get all neighbors setting from index 0 to minTargetIndex. If there are more 
	 * blocks generated, later blocks are returned as well. 
	 * 
	 * DO NOT ATTEMPT TO MODIFY THE NEIGHBORS IT CAN CAUSE BUGS
	 * @param minTargetIndex
	 * @return
	 */
	public List<int[]> getAll(int minTargetIndex) {
		while (neighbors.size() <= minTargetIndex) {
			getNext();
		}
		return Collections.unmodifiableList(neighbors);
	}
	
	/**
	 * Get the neighbors of encoded frame at specific index
	 * @param index
	 * @return
	 */
	public int[] get(int index) {
		while (neighbors.size() <= index) {
			getNext();
		}
		return neighbors.get(index);
	}

	@Override
	public void close() {
		neighbors = null;
	}
	
}
