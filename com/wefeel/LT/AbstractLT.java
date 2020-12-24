package com.wefeel.LT;

import com.wefeel.LT.Random.NeighborGenerator;

public abstract class AbstractLT implements AutoCloseable {
	protected int frameSize;
	protected long filesize;
	/** Unique identifier of the LT data stream, also used as seed for random generator */
	protected long nonce;
	protected int nPackets;
	protected NeighborGenerator random;
	
	protected int calcNumPacket(long totalBytes, int frameSize) {
		return (int) Math.ceil(totalBytes / (double) (frameSize));
	}

	public int getFrameSize() {
		return frameSize;
	}

	public long getFilesize() {
		return filesize;
	}

	public long getNonce() {
		return nonce;
	}

	public int getNPackets() {
		return nPackets;
	}
	
	/**
	 * 
	 * @throws Exception 
	 */
	@Override
	public void close() {
		random.close();
	}

}
