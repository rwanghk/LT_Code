package com.wefeel.LT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.wefeel.LT.Random.NeighborGenerator;

public class Encoder extends AbstractLT implements Iterator<DefaultEncodedFrame>, AutoCloseable {
	
	/** Store the data as an array of frames, each with constant length */
	private byte[][] data;
	
	private volatile boolean closed;
	private volatile long idx;

	public Encoder(byte[] data, int frameSize) {
		this(data, frameSize, new Random().nextLong());
	}

	public Encoder(byte[] data, int frameSize, long nonce) {
		this(frameSize, data.length, nonce);
		int noFrame = calcNoFrame(data.length, frameSize);
		this.data = new byte[noFrame][frameSize];
		noFrame--; // should use a new variable but oh well
		for (int i = 0; i < noFrame; i++) { // Populate the byte[][]
			System.arraycopy(data, i * frameSize, this.data[i], 0, frameSize);
		}
		// Copy the last frame, which may has fewer bytes than a frame
		System.arraycopy(data, noFrame * frameSize, this.data[noFrame], 0, data.length - noFrame * frameSize);
	}
	
	private Encoder(int frameSize, long fileSize, long nonce) {
		this.frameSize = frameSize;
		this.filesize = fileSize;
		this.nonce = nonce;
		nPackets = (int) Math.ceil(((double) fileSize) / frameSize);
		random = new NeighborGenerator(nonce, calcNoFrame(fileSize, frameSize));
	}

	private int calcNoFrame(long totalBytes, int frameSize) {
		return (int) Math.ceil(totalBytes / (double) (frameSize));
	}

	public byte[][] getData() {
		return data;
	}
	
	/**
	 * The encoder can generate potentially infinite data from a source, 
	 * so just a reminder for users to actually clean that up...
	 */
	@Override
	public void close() {
		super.close();
		data = null;
		closed = true;
		// System.gc(); // probably don't need to go that far...
	}

	/**
	 * Will always have next as it is dynamically generated, unless it's 
	 * closed
	 */
	@Override
	public boolean hasNext() {
		return !closed;
	}

	@Override
	public DefaultEncodedFrame next() {
		int[] neighbors = random.getNext();
		byte[] arr = Arrays.copyOfRange(data[neighbors[0]], 0, data[neighbors[0]].length); // Copy of byte[] to store new frame data
		for (int i = 1; i < neighbors.length; i++) {
			DefaultEncodedFrame.xor(arr, data[neighbors[i]]); // Encode for each neighbor byte[]
		}
		DefaultEncodedFrame frame = new DefaultEncodedFrame(nonce, filesize, idx++, arr, neighbors);
		return frame;
	}

	@Override @Deprecated 
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Create encoder from InputStream
	 * @param in
	 * @param frameSize
	 * @param nonce
	 * @return
	 * @throws IOException
	 */
	public static Encoder get(InputStream in, int frameSize, long nonce) throws IOException {
		List<byte[]> temp = new ArrayList<>();
		int read;
		long fileSize = 0;
		do {
			byte[] b = new byte[frameSize];
			read = in.read(b);
			if (read != -1) { // In case the file fits into frame boundary, avoid adding empty frame
				temp.add(b);
				fileSize += read;
			}
		} while (read == frameSize); // If less than frameSize read, the file is completed
		Encoder encoder = new Encoder(frameSize, fileSize, nonce);
		encoder.data = temp.toArray(new byte[0][]);
		return encoder;
	}
	

}
