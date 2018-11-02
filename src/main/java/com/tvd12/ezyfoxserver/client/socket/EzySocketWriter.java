package com.tvd12.ezyfoxserver.client.socket;

import java.nio.ByteBuffer;

public class EzySocketWriter extends EzyAbstractSocketEventHandler {

	protected final ByteBuffer writeBuffer;
    protected final EzyPacketQueue packetQueue;
    protected final EzySocketDataHandler dataHandler;

	public EzySocketWriter(EzyPacketQueue packetQueue,
						   EzySocketDataHandler dataHandler) {
		this.packetQueue = packetQueue;
		this.dataHandler = dataHandler;
		this.writeBuffer = ByteBuffer.allocate(32768);
	}

    @Override
    public void handleEvent() {
		try {
            EzyPacket packet = packetQueue.peek();
            dataHandler.firePacketSend(packet, writeBuffer);
            if(packet.isReleased())
                packetQueue.take();
            else
            	packetQueue.wakeup();
		}
		catch (InterruptedException e) {
			getLogger().error("socket-writer thread interrupted: " + Thread.currentThread());
		}
		catch(Throwable throwable) {
			getLogger().error("problems in socket-writer main loop, thread: " + Thread.currentThread(), throwable);
		}
	}
	
	@Override
	public void destroy() {
		packetQueue.clear();
	}

    @Override
    public void reset() {
        writeBuffer.clear();
    }
	
}
