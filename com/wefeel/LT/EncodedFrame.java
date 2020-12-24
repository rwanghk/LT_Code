package com.wefeel.LT;

import java.util.Set;

public abstract class EncodedFrame {
	
	/** 64 bit nonce which happens to be the seed used to generate random numbers */
	protected final long nonce;
	/** 48 bit filesize should be large enough */
	protected final long filesize;
	/** 48 bit index */
	protected final long index;
	/** Encoded data stored in this frame */
	protected final byte[] data;
	
	protected Set<Integer> neighbors; // Neighbors already included
	protected int[] intendedNeighbors; // Neighbors that should be included
	
	protected EncodedFrame(long nonce, long filesize, long index, byte[] data) {
		this.nonce = nonce;
		this.filesize = filesize; 
		this.index = index; 
		this.data = data;
	}
	
	protected EncodedFrame(long nonce, long filesize, long index, byte[] data, int[] neighbors) {
		this(nonce, filesize, index, data);
		this.intendedNeighbors = neighbors;
	}
		
	public byte[] getData() {
		return data;
	}

	public long getFilesize() {
		return filesize;
	}

	public int[] getNeighbors() {
		return intendedNeighbors;
	}
	
	public Set<Integer> getAvailableNeighbors() {
		return neighbors;
	}
	
	public long getIndex() {
		return index;
	}
	
	public long getNonce() {
		return nonce;
	}
	
	public int getFrameSize() {
		return data.length;
	}
	
	/**
	 * Convert this encoded frame into a single byte[]
	 * @return serialized 
	 */
	public abstract byte[] toByteArr();
}
