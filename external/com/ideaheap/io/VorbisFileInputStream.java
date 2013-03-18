package com.ideaheap.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * A native wrapper to the xiph.org vorbis file Input Stream. 
 * @author nwertzberger
 *
 */
public class VorbisFileInputStream implements Closeable {

	private final VorbisInfo info;
	
	public VorbisInfo getInfo() {
		return info;
	}

	private final int oggStreamIdx;
	
	static {
		System.loadLibrary("ogg");
		System.loadLibrary("vorbis");
		System.loadLibrary("vorbis-stream");
	}
	
	/**
	 * Opens a file for reading and parses any comments out of the file header.
	 */
	public VorbisFileInputStream(String fname) throws IOException {
		info = new VorbisInfo();
		oggStreamIdx = this.create(fname, info);
	}
	
	public void close() throws IOException {
		this.closeStreamIdx(oggStreamIdx);
	}
	
	/**
	 * Returns the interleaved PCM data from the vorbis stream.
	 * @param pcmBuffer
	 * @return
	 * @throws IOException
	 */
	public int read(short[] pcmBuffer) throws IOException {
		return this.readStreamIdx(oggStreamIdx, pcmBuffer, 0, pcmBuffer.length);
	}
	
	/**
	 * Returns interleaved PCM data from the vorbis stream.
	 * @param pcmBuffer
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public int read(short[] pcmBuffer, int offset, int length) throws IOException {
		return this.readStreamIdx(oggStreamIdx, pcmBuffer, offset, length);
	}
	
	/**
	 * Returns interleaved PCM data from the vorbis stream.
	 * @return
	 * @throws IOException
	 */
	public short read() throws IOException {
		short buf[] = new short[1];
		if (this.readStreamIdx(oggStreamIdx, buf, 0, 1) == -1) return -1;
		else return buf[0]; 
	}
	
	private native int create(String fname, VorbisInfo info) throws IOException;
	private native void closeStreamIdx(int sidx) throws IOException;
	
	/**
	 * This just returns all the channels interleaved together. I assume this
	 * is how android wants it.
	 * @param pcm
	 * @return
	 * @throws IOException
	 */
	private native int readStreamIdx(int sidx, short [] pcm, int offset, int size) throws IOException;
	
	/**
	 * Skips over the number of samples specified.  This skip doesn't
	 * account for channels.
	 * 
	 * @param samples
	 * @return
	 * @throws IOException
	 */
	private native long skipStreamIdx(int sidx, long samples) throws IOException;

}
