package com.wefeel.LT;

import java.io.IOException;
import java.io.InputStream;

public class ByteArrArrayInputStream extends InputStream {
	
	private byte[][] data;
	public final int total;
	private int available;
	private int x, y;
		
	public ByteArrArrayInputStream(byte[][] data) {
		this.data = data;
		int t = 0; 
		for (int i = 0; i < data.length; i++) {
			if (data[i] != null) {
				t += data[i].length;
			}
		}
		total = t;
		available = t;
	}
	
	public ByteArrArrayInputStream(byte[][] data, int len) {
		this.data = data;
		total = len;
		available = len;
	}

	@Override
	public int read() {
		if (available <= 0) {
			return -1;
		}
		while (y == data[x].length) {
			gotoNextRow();
		}
		available--;
		return data[x][y++];
	}
	
	@Override
    public int available() {
    	return available;
    }

	@Override
    public void close() {
    	data = null;
    	try {
			super.close();
		} catch (IOException e) {} // It shouldn't happen...
    }

	@Override
    public void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	@Override
    public boolean markSupported(){
        return false; 
    }

	@Override
    public int read(byte[] b) {
    	return read(b, 0, b.length);
    }

	@Override
    public int read(byte[] b, int off, int len) {
		if (available <= 0) {
			return -1;
		}
    	int read = 0;
    	int availableThisRow;
    	while (true) {
    		availableThisRow = availableThisRow();
    		if (availableThisRow > len) {
    			System.arraycopy(data[x], y, b, off, len);
    			// len = 0; off += len;
    			read += len;
    			y += len;
    			available -= len;
    			return read;
    		} else {
    			System.arraycopy(data[x], y, b, off, availableThisRow);
    			read += availableThisRow;
    			gotoNextRow();
    			available -= availableThisRow;
    			len -= availableThisRow;
    			off += availableThisRow;
    			if (available <= 0) {
    				// No more data available
    				return read;
    			}
    		}
    	}
    }

    public void reset() {
		throw new UnsupportedOperationException();
    }

    public long skip(long n) {
    	if (n >= available) {
    		long skipped = available;
    		available = 0;
    		return skipped;
    	}
    	long skipped = 0;
    	while (n > 0) {
    		int availableThisRow = availableThisRow();
    		if (n > availableThisRow) {
    			n -= availableThisRow;
    			skipped += availableThisRow;
    			available -= availableThisRow;
    			gotoNextRow();
    			if (available <= 0) {
    				// No more available
    				return skipped;
    			}
    		} else {
    			y += n;
    			available -= n;
    			skipped += n;
    			// n = 0;
    			return skipped;
    		}
    	}
    	// shouldn't need it
    	return skipped;
    }
    
    protected int availableThisRow() {
    	if (x >= data.length) {
    		return 0;
    	}
    	return data[x].length - y;
    }
    
    protected void gotoNextRow() {
    	x++;
    	y = 0;
    }


}
