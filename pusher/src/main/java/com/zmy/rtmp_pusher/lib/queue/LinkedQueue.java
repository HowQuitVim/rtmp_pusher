package com.zmy.rtmp_pusher.lib.queue;

import java.util.LinkedList;

public class LinkedQueue<T> extends Queue<T, T[]> {
    private LinkedList<T> list = new LinkedList<>();

    public LinkedQueue(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    protected int doDequeue(T[] output) {
        if (!canRead) {
            return 0;
        }
        while (list.size() == 0) {
            block();
            if (!canRead) return 0;
        }
        int dequeueCount = Math.min(list.size(), output.length);

        for (int i = 0; i < dequeueCount; i++) {
            output[i] = list.removeFirst();
            wakeUp();
        }
        return dequeueCount;
    }

    @Override
    protected void doEnqueue(T data) {
        if (!canWrite) return;
        while (list.size() >= maxCapacity) {
            block();
            if (!canWrite) return;
        }
        list.add(data);
        wakeUp();
    }

    @Override
    protected int resize(int newSize) {
        return 0;
    }

    @Override
    public int getCapacity() {
        return maxCapacity;
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public synchronized void close() {
        super.close();
    }
}
