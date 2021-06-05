package com.tvd12.ezyfoxserver.client.socket;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import com.tvd12.ezyfox.callback.EzyCallback;
import com.tvd12.ezyfox.codec.EzyByteToObjectDecoder;
import com.tvd12.ezyfox.codec.EzyMessage;


public class EzySimpleSocketDataDecoder implements EzySocketDataDecoder {

	protected ByteBuffer buffer;
	protected volatile boolean active;
	private final Queue<EzyMessage> queue;
	private final EzyByteToObjectDecoder decoder;

	public EzySimpleSocketDataDecoder(Object decoder) {
		this.active = true;
		this.queue = new LinkedList<>();
		this.decoder = (EzyByteToObjectDecoder)decoder;
	}
	
	@Override
	public Object decode(EzyMessage message, byte[] encryptionKey) throws Exception {
		Object answer = decoder.decode(message, encryptionKey);
		return answer;
	}
	
	@Override
	public void decode(
			byte[] bytes, EzyCallback<EzyMessage> callback) throws Exception {
		predecode(bytes);
		decoder.decode(buffer, queue);
		handleQueue(callback);
		postdecode();
	}
	
	private void handleQueue(EzyCallback<EzyMessage> callback) throws Exception {
		while(!queue.isEmpty() && active) {
			do {
				callback.call(queue.poll());
			}
			while(!queue.isEmpty());
			
			if(buffer.hasRemaining()) {
				decoder.decode(buffer, queue);
			}
		}
	}
	
	private void predecode(byte[] bytes) {
		if(buffer == null)
			buffer = newBuffer(bytes);
		else
			buffer = mergeBytes(bytes);
	}
	
	private void postdecode() {
		buffer = getRemainBytes(buffer);
	}
	
	private ByteBuffer newBuffer(byte[] bytes) {
		return ByteBuffer.wrap(bytes);
	}
	
	private ByteBuffer mergeBytes(byte[] bytes) {
		int capacity = buffer.remaining() + bytes.length;
		ByteBuffer merge = ByteBuffer.allocate(capacity).put(buffer).put(bytes);
		merge.flip();
		return merge;
	}
	
	private ByteBuffer getRemainBytes(ByteBuffer old) {
		if(!old.hasRemaining())
			return null;
		byte[] bytes = new byte[old.remaining()];
		old.get(bytes);
		return ByteBuffer.wrap(bytes);
	}

}
