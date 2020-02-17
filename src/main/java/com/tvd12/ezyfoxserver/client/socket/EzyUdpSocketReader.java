package com.tvd12.ezyfoxserver.client.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;

import com.tvd12.ezyfox.codec.EzyMessage;
import com.tvd12.ezyfox.codec.EzyMessageReaders;
import com.tvd12.ezyfox.entity.EzyArray;
import com.tvd12.ezyfoxserver.client.concurrent.EzySynchronizedQueue;
import com.tvd12.ezyfoxserver.client.constant.EzySocketConstants;
import com.tvd12.ezyfoxserver.client.util.EzyQueue;

public class EzyUdpSocketReader extends EzySocketAdapter {

	protected ByteBuffer buffer;
	protected final int readBufferSize;
	protected EzyQueue<EzyArray> dataQueue;
	protected EzySocketDataDecoder decoder;
	protected DatagramChannel datagramChannel;
	
	public EzyUdpSocketReader() {
		super();
		this.readBufferSize = EzySocketConstants.MAX_READ_BUFFER_SIZE;
	}
	
	@Override
	protected void loop() {
		this.dataQueue = new EzySynchronizedQueue<>();
		this.buffer = ByteBuffer.allocateDirect(readBufferSize);
		super.loop();
	}

	@Override
	protected void update() {
		while (true) {
			try {
				if(!active)
					return;
				this.buffer.clear();
				int bytesToRead = readSocketData();
				if(bytesToRead <= 0)
					return;
				buffer.flip();
				byte[] binary = new byte[buffer.limit()];
				buffer.get(binary);
				handleReceivedBytes(binary);
			}
			catch (InterruptedException e) {
				logger.warn("socket reader interrupted", e);
				return;
			}
			catch (IOException e) {
				logger.warn("I/O error at socket-reader", e);
				return;
			}
			catch (Exception e) {
				logger.warn("I/O error at socket-reader", e);
			}
		}
	}

	protected int readSocketData() throws Exception {
		datagramChannel.receive(buffer);
		int readBytes = buffer.position();
		return readBytes;
	}
	
	protected void handleReceivedBytes(byte[] bytes) {
		EzyMessage message = EzyMessageReaders.bytesToMessage(bytes);
		if(message == null)
			return;
		onMesssageReceived(message);
	}
	
	@Override
	protected void clear() {
		if(dataQueue != null)
			dataQueue.clear();
	}

	public void popMessages(List<EzyArray> buffer) {
		dataQueue.pollAll(buffer);
	}

	private void onMesssageReceived(EzyMessage message) {
		try {
			Object data = decoder.decode(message);
			dataQueue.add((EzyArray) data);
		}
		catch (Exception e) {
			logger.warn("decode error at socket-reader", e);
		}
	}
	
	public void setDecoder(EzySocketDataDecoder decoder) {
		this.decoder = decoder;
	}
	
	public void setDatagramChannel(DatagramChannel datagramChannel) {
		this.datagramChannel = datagramChannel;
	}
	
	@Override
	protected String getThreadName() {
		return "udp-socket-reader";
	}
	
}
