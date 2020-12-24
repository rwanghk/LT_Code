package com.wefeel.LT;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.wefeel.LT.Random.NeighborGenerator;

public class Decoder extends AbstractLT implements AutoCloseable {
	
	public static boolean DEBUG = false;
	
	private volatile boolean initialized = false;
	
	/** Keep a weak reference of all frames received. Probably won't need them anyway */
	private Map<Long, WeakReference> receivedEncodedFrames;
	/** This is the collection of original frames that we want to get, and we'll populate this 2D array as we get new data */
	private byte[][] rawFrames;
	/** We keep a list of frames that can't be decoded yet as the required raw frame hasn't been available, and will pop from the list for decoding when the data can be decoded */
	private ArrList waitingRawFrameList;
	
	private int decodedCount = 0;
	
	// SECTION Initialization. The decoder is normally initialized when receiving the first frame, but can be initialized manually as required
	
	/**
	 * Initialize the decoder with the first frame received. Be very careful not to mix different fountain streams as it won't decode
	 * @param frameOne First frame received. All data will be modeled after this frame
	 */
	void init(DefaultEncodedFrame frameOne) {
		init(
				frameOne.getNonce(), 
				frameOne.getFilesize(), 
				(int) Math.ceil(((double) frameOne.getFilesize()) / frameOne.getFrameSize()), 
				frameOne.getFrameSize()
			);
	}
	
	/**
	 * Manually initialize the decoder by giving all parameters directly. Good 
	 * when you know the parameters exactly
	 * @param nonce
	 * @param filesize
	 * @param nPackets
	 * @param frameSize
	 */
	public void init(long nonce, long filesize, int nPackets, int frameSize) {
		this.nonce = nonce; 
		this.filesize = filesize;
		this.nPackets = nPackets;
		this.frameSize = frameSize;
		random = new NeighborGenerator(nonce, nPackets);
		receivedEncodedFrames = new HashMap<>();
		waitingRawFrameList = new ArrList(nPackets);
		rawFrames = new byte[nPackets][];
		initialized = true;
	}
	
	public void frameReceived(byte[] encoded) {
		frameReceived(new DefaultEncodedFrame(encoded));
	}
	
	public void frameReceived(DefaultEncodedFrame frame) {
		if (!initialized) {
			init(frame);
		}
		if (nonce != frame.getNonce()) {
			// this frame is not what we want at all... Either throw an exception, or silently discard it 
			return;
		}
		if (DEBUG) {
			System.out.println("[LT] Frame " + frame.getIndex() + " received");
		}
		receivedEncodedFrames.put(frame.getIndex(), new WeakReference(frame));
		frame.received(random); // Initialize the frame
		// Check if it can be decoded / discarded before further processing
		for (int i : frame.getNeighbors()) {
			if (rawFrames[i] != null) { // Available 
				frame.neighborAvailable(i);
			} 
		}
		if (frame.shouldDiscard()) {
			// discard
		} else if (frame.readyToDecode()) {
			decodeFrame(frame);
		} else { // Need to wait
			for (int i : frame.getNeighbors()) {
				if (rawFrames[i] != null) continue;
				waitingRawFrameList.put(i, frame);
			}
		}
		return;
	}
	
	public int numFrameDecoded() {
		return decodedCount;
	}
	
	public boolean finished() {
		return decodedCount == rawFrames.length;
	}

	public byte[] getDecoded() {
		if (!finished()) {
			throw new RuntimeException("Not finished decoding");
		}
		byte[] b = new byte[(int) filesize];
		int ll = rawFrames.length - 1;
		for (int i = 0; i < ll; i++) {
			System.arraycopy(rawFrames[i], 0, b, i * frameSize, frameSize);
		}
		System.arraycopy(rawFrames[ll], 0, b, ll * frameSize, (int) (filesize - ll * frameSize));
		return b;
	}
	
	public InputStream getDecodedAsStream() {
		if (!finished()) {
			throw new RuntimeException("Not finished decoding");
		}
		InputStream in = new ByteArrArrayInputStream(rawFrames, (int) filesize);
		return in;
	}
	
	/**
	 * Check which frames are properly received and decoded
	 * @return boolean[] with respective location set to true if that frame was received
	 */
	public boolean[] checkFramesReceived() {
		boolean[] arr = new boolean[rawFrames.length];
		for (int i = 0; i < rawFrames.length; i++) {
			arr[i] = rawFrames[i] != null;
		}
		return arr;
	}
	
	private void decodeFrame(DefaultEncodedFrame frame) {
		// It's possible that a new frame is available between queuing 
		// for decode and actual decoding
		if (frame.shouldDiscard()) {
			// Remove from list if ever happens. This should happen at most once
			waitingRawFrameList.remove(frame, frame.getNeighbors());
			return;
		}
		int[] neighbors = frame.getNeighbors();	
		byte[] data = frame.getData();
		int missing = -1;
		for (int i : neighbors) { 
			// Normally only one neighbor is missing, so if the neighbor is not 
			// missing, XOR to remove neighborhood
			if (rawFrames[i] != null) {
				DefaultEncodedFrame.xor(data, rawFrames[i]);
			} else { // This frame is not available yet, found the missing frame
				missing = i;
			}
		}
		if (missing == -1) { // shouldn't happen, this frame should've been discarded
			assert false;
		} else {
			assert rawFrames[missing] == null;
			rawFrames[missing] = data;
			decodedCount++;
			notifyNewRawFrame(missing);
		}
		waitingRawFrameList.remove(frame, neighbors);
		if (DEBUG) {
			System.out.println("[LT] Raw frame " + missing + " decoded");
		}
	}
	
	private void notifyNewRawFrame(int idx) {
		Iterator<DefaultEncodedFrame> itr = waitingRawFrameList.get(idx);
		List<DefaultEncodedFrame> toRemove = new ArrayList<>();
		List<DefaultEncodedFrame> toDecode = new ArrayList<>();
		while (itr.hasNext()) {
			DefaultEncodedFrame frame = itr.next();
			frame.neighborAvailable(idx);
			if (frame.shouldDiscard()) {
				toRemove.add(frame);
			} else if (frame.readyToDecode()) {
				toDecode.add(frame);
			} // else continue;
		}
		// Separate checking vs performing list operation to avoid concurrent modification
		for (DefaultEncodedFrame f : toRemove) {
			waitingRawFrameList.remove(f, f.getNeighbors());
		}
		for (DefaultEncodedFrame f : toDecode) {
			decodeFrame(f); // Theoretical potential for stack overflow here...
		}
	}

	/**
	 * A helper class to store the frames that are waiting for other frames before 
	 * they can be decoded
	 * @author R Wang
	 *
	 */
	@SuppressWarnings("rawtypes")
	class ArrList {
		final LinkedList[] arr;
		public ArrList(int size) {
			arr = new LinkedList[size];
		}
		
		@SuppressWarnings("unchecked")
		void put(int idx, DefaultEncodedFrame frame) {
			if (arr[idx] == null) {
				arr[idx] = new LinkedList<DefaultEncodedFrame>();
			}
			arr[idx].add(frame);
		}
		
		void finished(int idx) {
			arr[idx] = null;
		}
		
		void remove(DefaultEncodedFrame frame, int ... ints) {
			for (int i : ints) {
				if (arr[i] != null) {
					arr[i].remove(frame);
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		Iterator<DefaultEncodedFrame> get(int idx) {
			if (arr[idx] == null) {
				return new Iterator<DefaultEncodedFrame>() {

					@Override
					public boolean hasNext() {
						return false;
					}

					@Override
					public DefaultEncodedFrame next() {
						return null;
					}

					@Override
					public void remove() {						
					}};
			} else {
				return (Iterator<DefaultEncodedFrame>) arr[idx].iterator();
			}
		}
	}

	@Override
	public void close() {
		super.close();
		receivedEncodedFrames = null;
		waitingRawFrameList = null;
	}
}
