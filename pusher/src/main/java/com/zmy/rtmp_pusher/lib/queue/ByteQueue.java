package com.zmy.rtmp_pusher.lib.queue;


import androidx.annotation.NonNull;

import java.nio.ByteBuffer;


public class ByteQueue extends Queue<ByteBuffer, ByteBuffer> {

    private ByteBuffer buffer;


    private int readCursor = 0;
    private int writeCursor = 0;

    private int totalDequeueBytes = 0;
    private int totalEnqueueBytes = 0;
    private int initialCapacity = 0;

    public ByteQueue() {
        this(2, Integer.MAX_VALUE);
    }

    public ByteQueue(int initialCapacity, int maxCapacity) {
        super(maxCapacity);
        if (maxCapacity < initialCapacity || initialCapacity <= 0)
            throw new IllegalArgumentException("initialCapacity must be less than maxCapacity ,and must be more than 0 ");
        this.initialCapacity = initialCapacity;
        buffer = ByteBuffer.allocateDirect(this.initialCapacity);
    }

    public synchronized int getCapacity() {
        return buffer.capacity();
    }

    @Override
    public int getSize() {
        return Math.max(totalEnqueueBytes - totalDequeueBytes, 0);
    }

    private int getWriteableSize() {
        return buffer.capacity() - getSize();
    }

    private Area getReadableArea() {
        if (getSize() == 0) return null;
        Area area;
        if (writeCursor > readCursor) {
            area = new Area(readCursor, writeCursor - readCursor);
        } else {
            area = new Area(readCursor, buffer.capacity() - readCursor);
        }
        return area;
    }

    private Area getWriteableArea() {
        if (getSize() == buffer.capacity()) return null;
        Area area;
        if (writeCursor >= readCursor) {
            area = new Area(writeCursor, buffer.capacity() - writeCursor);
        } else {
            area = new Area(writeCursor, readCursor - writeCursor);
        }
        return area;
    }

    @Override
    public void doEnqueue(ByteBuffer src) {
        if (!canWrite) return;
        if (src == null) throw new IllegalArgumentException("src is null");
        int srcLimit = src.limit();
        while (src.remaining() > 0) {
            if (!canWrite) return;
            while (getWriteableSize() == 0 && resize(computeNewCapacity(getSize() + src.remaining())) == getSize()) {
                block();
                if (!canWrite) return;
            }
            Area area = getWriteableArea();
            if (area == null) throw new IllegalStateException("no space to store data");
            int writeBytes = Math.min(area.getSize(), src.remaining());
            buffer.limit(area.getEnd());
            buffer.position(area.getStart());
            src.limit(src.position() + writeBytes);
            buffer.put(src);
            writeCursor = (writeCursor + writeBytes) % buffer.capacity();
            totalEnqueueBytes += writeBytes;
            src.limit(srcLimit);
            wakeUp();
        }
    }

    private int computeNewCapacity(int expectedNewCapacity) {
        return Math.max(getCapacity() * 2, expectedNewCapacity);
    }

    private boolean isFlushing() {
        return !canWrite && canRead;
    }

    @Override
    protected int doDequeue(ByteBuffer output) {
        if (output == null || output.remaining() <= 0) throw new IllegalArgumentException("illegal output buffer");
        if (!canRead) return 0;
        while (getSize() == 0) {
            if (!canWrite) {
                canRead = false;
                return 0;
            }
            block();
            if (!canRead) return 0;
        }
        int maxSize = output.remaining();
        int readSize = Math.min(maxSize, getSize());
        int start = output.position();
        int totalReadSize = 0;
        while (readSize > 0) {
            if (!canRead) return totalReadSize;
            Area area = getReadableArea();
            if (area == null || area.getSize() <= 0 || getSize() <= 0) {
                break;
            }
            int currentRead = Math.min(area.getSize(), output.remaining());
            totalDequeueBytes += currentRead;
            readCursor = (readCursor + currentRead) % buffer.capacity();
            readSize -= currentRead;
            buffer.limit(area.getStart() + currentRead);
            buffer.position(area.getStart());
            output.put(buffer);
            totalReadSize += currentRead;
            wakeUp();
        }
        output.limit(start + totalReadSize);
        output.position(start);
        if (getSize() == 0 && !canWrite) {
            canRead = false;
        }
        return totalReadSize;
    }

    @Override
    protected int resize(int expectedCapacity) {
        if (buffer.capacity() < maxCapacity) {
            int newCapacity = Math.min(maxCapacity, expectedCapacity);//计算新的容量
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
            int migrateSize = getSize();
            int newWriteCursor = migrateSize;
            do {
                Area area = getReadableArea();
                if (area == null || area.getSize() == 0) break;
                buffer.limit(area.getEnd());
                buffer.position(area.getStart());
                newBuffer.put(buffer);
                migrateSize -= area.getSize();
                readCursor = (readCursor + area.getSize()) % buffer.capacity();
            } while (migrateSize > 0);
            writeCursor = newWriteCursor;
            readCursor = 0;
            buffer = newBuffer;
        }
        return buffer.capacity();
    }

    @Override
    public void close() {
        super.close();
        wakeUp();
    }

    @Override
    public synchronized void flush() {
        super.flush();
        wakeUp();
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    private static class Area {
        private final int start;
        private final int size;

        public Area(int start, int size) {
            this.start = start;
            if (size <= 0)
                throw new IllegalArgumentException("size must be more than 0");
            this.size = size;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return start + size;
        }


        public int getSize() {
            return size;
        }

        @NonNull
        @Override
        public String toString() {
            return "Area{" +
                    "start=" + start +
                    ", size=" + size +
                    ", end=" + getEnd() +
                    '}';
        }
    }
}
