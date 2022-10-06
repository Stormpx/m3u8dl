package org.stormpx.dl.kit;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class BufferPool {
    private static int BUFFER_SIZE = 8 * 1024;
    private Semaphore quota;
    private Semaphore mutex;
    private IdentityHashMap<byte[],Boolean> identityMap;


    public BufferPool(int size) {
        this.quota = new Semaphore(size);
        this.mutex=new Semaphore(1);
        this.identityMap=new IdentityHashMap<>();
    }

    private byte[] getOrCreateBuffer() throws InterruptedException {
        mutex.acquire();
        byte[] buffer = identityMap.entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .findFirst()
                .map(Map.Entry::getKey)
                .orElseGet(()->new byte[BUFFER_SIZE]);
//        if (bufferQueue.isEmpty()){
//            return new byte[8*1024];
//        }
//        return bufferQueue.poll();
        identityMap.put(buffer,false);
        mutex.release();
        return buffer;
    }

    private void releaseBuffer(byte[] buffer) throws InterruptedException {
        mutex.acquire();
        if (!identityMap.containsKey(buffer)){
            throw new IllegalArgumentException("nope.");
        }
        identityMap.put(buffer,true);
        mutex.release();
    }

    public byte[] acquire() throws InterruptedException {
        quota.acquire();
        return getOrCreateBuffer();
    }

    public void release(byte[] buffer) throws InterruptedException {
        releaseBuffer(buffer);
        quota.release();
    }
}
