package com.wefeel.LT;

import java.util.Arrays;
import java.util.HashSet;
import com.wefeel.LT.Random.NeighborGenerator;

public class DefaultEncodedFrame extends EncodedFrame {

	private boolean inited;
	
	public DefaultEncodedFrame(byte[] encodedData) {
		super(deserializeNonce(encodedData), 
				deserializeFilesize(encodedData),
				deserializeIndex(encodedData), 
				deserializeData(encodedData));
	}

	public DefaultEncodedFrame(long nonce, long filesize, long index, byte[] data, int[] neighbors) {
		super(nonce, filesize, index, data, neighbors);
	}

	@Override
	public byte[] toByteArr() {
		byte[] encoded = new byte[data.length + 20];
		// Nonce
		encoded[0] = (byte) ((nonce >> 56) & 0xFF);
		encoded[1] = (byte) ((nonce >> 48) & 0xFF);
		encoded[2] = (byte) ((nonce >> 40) & 0xFF);
		encoded[3] = (byte) ((nonce >> 32) & 0xFF);
		encoded[4] = (byte) ((nonce >> 24) & 0xFF);
		encoded[5] = (byte) ((nonce >> 16) & 0xFF);
		encoded[6] = (byte) ((nonce >> 8) & 0xFF);
		encoded[7] = (byte) ((nonce) & 0xFF);
		// Filesize
		encoded[8] = (byte) ((filesize >> 40) & 0xFF);
		encoded[9] = (byte) ((filesize >> 32) & 0xFF);
		encoded[10] = (byte) ((filesize >> 24) & 0xFF);
		encoded[11] = (byte) ((filesize >> 16) & 0xFF);
		encoded[12] = (byte) ((filesize >> 8) & 0xFF);
		encoded[13] = (byte) ((filesize) & 0xFF);
		// Index
		encoded[14] = (byte) ((index >> 40) & 0xFF);
		encoded[15] = (byte) ((index >> 32) & 0xFF);
		encoded[16] = (byte) ((index >> 24) & 0xFF);
		encoded[17] = (byte) ((index >> 16) & 0xFF);
		encoded[18] = (byte) ((index >> 8) & 0xFF);
		encoded[19] = (byte) ((index) & 0xFF);
		System.arraycopy(data, 0, encoded, 20, data.length);
		return encoded;
	}
	
	/**
	 * Should be called if this frame is somehow received. It will populate the 
	 * neighbors array 
	 * @param ng
	 */
	public void received(NeighborGenerator ng) {
		this.intendedNeighbors = ng.get((int) this.index);
		if (neighbors == null) neighbors = new HashSet<>();
		inited = true;
	}
	
	public void neighborAvailable(int idx) {
		neighbors.add(idx);
	}
	
	
	/**
	 * If all neighbors are available to decode this message
	 * @return true if the message can be decoded into a new neighbor (unencoded data)
	 */
	public boolean readyToDecode() {
		return inited && (neighbors.size() == intendedNeighbors.length - 1); // Only one neighbor left
		// -> after XOR all other neighbors it becomes the remaining one
	}
	
	private static long deserializeNonce(byte[] encodedData) {
		return ((encodedData[0] & 0xFFl) << 56) 
				| ((encodedData[1] & 0xFFl) << 48) 
				| ((encodedData[2] & 0xFFl) << 40) 
				| ((encodedData[3] & 0xFFl) << 32)
				| ((encodedData[4] & 0xFFl) << 24) 
				| ((encodedData[5] & 0xFFl) << 16)
				| ((encodedData[6]) & 0xFFl) << 8
				| ((encodedData[7]) & 0xFFl);
	}
	
	private static long deserializeFilesize(byte[] encodedData) {
		return ((encodedData[8] & 0xFFl) << 40) 
				| ((encodedData[9] & 0xFFl) << 32) 
				| ((encodedData[10] & 0xFFl) << 24) 
				| ((encodedData[11] & 0xFFl) << 16) 
				| ((encodedData[12] & 0xFFl) << 8)
				| (encodedData[13] & 0xFFl);
	}
	
	private static long deserializeIndex(byte[] encodedData) {
		return ((encodedData[14] & 0xFFl) << 40) 
				| ((encodedData[15] & 0xFFl) << 32) 
				| ((encodedData[16] & 0xFFl) << 24)
				| ((encodedData[17] & 0xFFl) << 16)
				| ((encodedData[18] & 0xFFl) << 8)
				| (encodedData[19] & 0xFFl);
	}
	
	private static byte[] deserializeData(byte[] encodedData) {
		return Arrays.copyOfRange(encodedData, 20, encodedData.length);
	}
	
	/**
	 * 
	 * @return true if all constituent neighbors are already available
	 */
	public boolean shouldDiscard() {
		return !inited || (neighbors.size() == intendedNeighbors.length);
	}
	
	public static void xor(byte[] target, byte[] anotherArr) {
		for (int i = 0; i < target.length; i++) {
			target[i] ^= anotherArr[i];
		}
	}
}
