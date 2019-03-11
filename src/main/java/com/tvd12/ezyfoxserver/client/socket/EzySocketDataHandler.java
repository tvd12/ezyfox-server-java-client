package com.tvd12.ezyfoxserver.client.socket;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import com.tvd12.ezyfox.callback.EzyCallback;
import com.tvd12.ezyfox.codec.EzyMessage;
import com.tvd12.ezyfox.codec.EzyMessageDataDecoder;
import com.tvd12.ezyfox.concurrent.EzyExecutors;
import com.tvd12.ezyfox.entity.EzyArray;
import com.tvd12.ezyfox.util.EzyLoggable;
import com.tvd12.ezyfox.util.EzyResettable;
import com.tvd12.ezyfoxserver.client.event.EzyDisconnectionEvent;
import com.tvd12.ezyfoxserver.client.event.EzyEvent;

/**
 * Created by tavandung12 on 9/21/18.
 */

public class EzySocketDataHandler extends EzyLoggable implements EzyResettable {
    protected SocketChannel socketChannel;
    protected volatile boolean disconnected;
    protected final EzyMessageDataDecoder decoder;
    protected final ExecutorService codecThreadPool;
    protected final EzySocketEventQueue eventQueue;
    protected final EzyCallback<EzyMessage> decodeBytesCallback;
    protected final EzyDisconnectionDelegate disconnectionDelegate;

    public EzySocketDataHandler(EzyMessageDataDecoder decoder,
                                EzySocketEventQueue eventQueue,
                                EzyDisconnectionDelegate disconnectionDelegate) {
        this.decoder = decoder;
        this.eventQueue = eventQueue;
        this.disconnectionDelegate = disconnectionDelegate;
        this.codecThreadPool = EzyExecutors.newSingleThreadExecutor("codec");
        this.decodeBytesCallback = new EzyCallback<EzyMessage>() {
            @Override
            public void call(EzyMessage message) {
                executeHandleReceivedMessage(message);
            }
        };
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void fireBytesReceived(byte[] bytes) throws Exception {
        try {
            decoder.decode(bytes, decodeBytesCallback);
        }
        catch(Exception throwable) {
            fireExceptionCaught(throwable);
        }
    }

    public void fireSocketDisconnected(int reason) {
        if(disconnected)
            return;
        disconnected = true;
        disconnectionDelegate.onDisconnected(reason);
        EzyEvent event = new EzyDisconnectionEvent(reason);
        EzySocketEvent socketEvent = new EzySimpleSocketEvent(
                EzySocketEventType.EVENT, event);
        fireSocketEvent(socketEvent);
    }

    public void fireSocketEvent(EzySocketEvent socketEvent) {
        eventQueue.add(socketEvent);
    }

    private void fireExceptionCaught(Exception e) {
    }

    private void executeHandleReceivedMessage(final EzyMessage message) {
        codecThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                handleReceivedMesssage(message);
            }
        });
    }

    private void handleReceivedMesssage(EzyMessage message) {
        try {
            Object data = decodeMessage(message);
            handleReceivedData(data);
        }
        catch (Exception e) {
            fireExceptionCaught(e);
        }
    }

    private Object decodeMessage(EzyMessage message) throws  Exception {
        Object answer = decoder.decode(message);
        return answer;
    }

    private void handleReceivedData(Object data) {
        EzyResponse response = newSocketResponse(data);
        EzySocketEvent event = new EzySimpleSocketEvent(EzySocketEventType.RESPONSE, response);
        boolean success = eventQueue.add(event);
        if(!success) {
            getLogger().warn("response queue is full, drop incomming response");
        }
    }

    private EzyResponse newSocketResponse(Object data) {
        return new EzySimpleResponse((EzyArray) data);
    }

    public void firePacketSend(EzyPacket packet, ByteBuffer writeBuffer) throws Exception {
        boolean canWriteBytes = canWriteBytes();
        if(canWriteBytes)
            writePacketToSocket(packet, writeBuffer);
        else
            packet.release();
    }

    private boolean canWriteBytes() {
        if(socketChannel == null)
            return false;
        return socketChannel.isConnected();
    }

    protected int writePacketToSocket(EzyPacket packet, Object writeBuffer) throws Exception {
        byte[] bytes = getBytesToWrite(packet);
        int bytesToWrite = bytes.length;
        ByteBuffer buffer = getWriteBuffer((ByteBuffer)writeBuffer, bytesToWrite);
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();
        int bytesWritten = socketChannel.write(buffer);
        if (bytesWritten < bytesToWrite) {
            byte[] remainBytes = getPacketFragment(buffer);
            packet.setFragment(remainBytes);
        }
        else {
            packet.release();
        }
        return bytesWritten;
    }

    private ByteBuffer getWriteBuffer(ByteBuffer fixed, int bytesToWrite) {
        return bytesToWrite > fixed.capacity() ? ByteBuffer.allocate(bytesToWrite) : fixed;
    }

    private byte[] getPacketFragment(ByteBuffer buffer) {
        byte[] remainBytes = new byte[buffer.remaining()];
        buffer.get(remainBytes);
        return remainBytes;
    }

    private byte[] getBytesToWrite(EzyPacket packet) {
        return (byte[])packet.getData();
    }

    @Override
    public void reset() {
        decoder.reset();
    }
}
