package com.wefeel.LT.Random;

import java.util.Random;

/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Kevin Wellenzohn
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal 
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * @author k13n
 * {@link https://github.com/k13n/soliton_distribution}
 *
 */
public class RobustSolitonGenerator {
	
	private Random random;
	private int nrBlocks; // k
	private int spike; // M
	private double failureProbability; // delta
	private double normalizationFactor; // beta
	private double c;
	private double R;
	private final long seed;

	/**
	 * 
	 * @param nrBlocks Number of blocks in raw data, must be > 0
	 * @param c constant to derive R, must be positive
	 * @param failureProbability [0, 1] failure probability 
	 * @param seed nonce to recreate random stream
	 */
	public RobustSolitonGenerator(int nrBlocks, double c, double failureProbability, long seed) {
		this.nrBlocks = nrBlocks;
		this.c = c;
		this.failureProbability = failureProbability;
		this.seed = seed;
		random = new Random(seed);
		R = computeR();
		spike = computeSpikePosition();
		normalizationFactor = computeNormalizationFactor();
	}

	/**
	 * 
	 * @param nrBlocks Number of blocks in raw data, must be > 0
	 * @param c constant to derive R, must be positive
	 * @param failureProbability [0, 1] failure probability 
	 */
	public RobustSolitonGenerator(int nrBlocks, double c, double failureProbability) {
		this(nrBlocks, c, failureProbability, new Random().nextLong());
	}

	/**
	 * 
	 * @param nrBlocks Number of blocks in raw data, must be > 0
	 * @param spike Position of spike of probability 
	 * @param failureProbability [0, 1] failure probability 
	 */
	public RobustSolitonGenerator(int nrBlocks, int spike, double failureProbability) {
		this(nrBlocks, spike, failureProbability, new Random().nextLong());
	}

	/**
	 * 
	 * @param nrBlocks Number of blocks in raw data, must be > 0
	 * @param spike Position of spike of probability 
	 * @param failureProbability [0, 1] failure probability 
	 * @param seed nonce to recreate random stream
	 */
	public RobustSolitonGenerator(int nrBlocks, int spike, double failureProbability, long seed) {
		this.nrBlocks = nrBlocks;
		this.spike = spike;
		this.failureProbability = failureProbability;
		this.seed = seed;
		random = new Random(seed);
		R = nrBlocks / ((double) spike);
		normalizationFactor = computeNormalizationFactor();
	}
	
	/**
	 * 
	 * @return Next random integer, with Soliton distribution 
	 */
	public int next() {
		double u = random.nextDouble();
		return inverseTransformSampling(u);
	}

	/**
	 * Get the seed of this generator
	 * @return
	 */
	public long getSeed() {
		return seed;
	}

	private double computeR() {
		return c * Math.log(nrBlocks / failureProbability) * Math.sqrt(nrBlocks);
	}

	private int computeSpikePosition() {
		return (int) Math.floor(nrBlocks / R);
	}

	private double computeNormalizationFactor() {
		double sum = 0;
		for (int i = 1; i <= nrBlocks; i++)
			sum += idealSoliton(i) + unnormalizedRobustSoliton(i);
		return sum;
	}

	private int inverseTransformSampling(double u) {
		double sum = 0;
		int index = 1;
		while (sum <= u)
			sum += normalizedRobustSoliton(index++);
		return index - 1;
	}

	private double normalizedRobustSoliton(int i) {
		return (idealSoliton(i) + unnormalizedRobustSoliton(i)) / normalizationFactor;
	}

	private double unnormalizedRobustSoliton(int i) {
		if (1 <= i && i <= spike - 1)
			return 1.0 / (i * spike);
		else if (i == spike)
			return Math.log(R / failureProbability) / spike;
		else
			return 0;
	}

	private double idealSoliton(int i) {
		if (i == 1)
			return 1.0 / nrBlocks;
		else
			return 1.0 / (i * (i - 1));
	}
	
	public int getSpike() {
		return spike;
	}

}
