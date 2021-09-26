package com.zmy.rtmp_pusher.lib.queue;

import junit.framework.TestCase;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class ByteQueueTest extends TestCase {
    private ByteBuffer createSrc(int capacity) {
        ByteBuffer src = ByteBuffer.allocateDirect(capacity);
        for (int i = 0; i < src.capacity(); i++) {
            src.put(i, (byte) i);
        }
        return src;
    }

    private boolean isContinuous(ByteBuffer byteBuffer, int size) {
        byte last = byteBuffer.get(0);
        for (int i = 0; i < byteBuffer.capacity(); i++) {
            if (i >= size) break;
            byte current = byteBuffer.get(i);
            if (current > last) {
                if (current - last != 1) {
                    return false;
                }
            }
            last = current;
        }
        return true;
    }

    @Test
    public void testEnqueue() {
        ByteQueue queue = new ByteQueue(10, 20);
        ByteBuffer src = createSrc(3);
        for (int i = 0; i < 5; i++) {
            src.limit(src.capacity());
            src.position(0);
            queue.enqueue(src);
        }
        assertEquals(queue.getCapacity(), 20);
        assertEquals(queue.getSize(), 15);
    }

    @Test
    public void testEnqueueBlock() {
        ByteQueue queue = new ByteQueue(10, 20);
        Thread writeThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer src = createSrc(3);
                for (int i = 0; i < 7; i++) {
                    src.limit(src.capacity());
                    src.position(0);
                    queue.enqueue(src);
                }
            }
        };
        writeThread.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(writeThread.getState() == Thread.State.BLOCKED || writeThread.getState() == Thread.State.WAITING);
        assertEquals(queue.getSize(), 20);
        assertEquals(queue.getCapacity(), 20);
    }

    @Test
    public void testDequeue() {
        ByteQueue queue = new ByteQueue(10, 20);
        ByteBuffer src = createSrc(3);
        for (int i = 0; i < 5; i++) {
            src.limit(src.capacity());
            src.position(0);
            queue.enqueue(src);
        }

        Thread readThread = new Thread() {
            @Override
            public void run() {
                super.run();
                int readBytes = 0;
                ByteBuffer output = ByteBuffer.allocateDirect(4);
                for (int i = 0; i < 6; i++) {
                    output.limit(output.capacity());
                    output.position(0);
                    int read = queue.dequeue(output);
                    assertEquals(read, output.remaining());
                    readBytes += read;
                    assertTrue(isContinuous(output, read));
                    if (i == 4) {
                        assertEquals(readBytes, 15);
                    }
                }
            }
        };
        readThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(readThread.getState() == Thread.State.BLOCKED || readThread.getState() == Thread.State.WAITING);
    }

    @Test
    public void testEnqueueAndDequeue() {
        ByteQueue queue = new ByteQueue(10, 20);

        Thread writeThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer src = createSrc(3);
                for (int i = 0; i < 1000; i++) {
                    src.limit(src.capacity());
                    src.position(0);
                    queue.enqueue(src);
                }
            }
        };
        writeThread.start();
        final int[] readBytes = {0};
        Thread readThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer output = ByteBuffer.allocateDirect(4);
                for (int i = 0; i < 100000; i++) {
                    output.limit(output.capacity());
                    output.position(0);
                    int read = queue.dequeue(output);
                    assertEquals(read, output.remaining());
                    readBytes[0] += read;
                    assertTrue(isContinuous(output, read));
                }
            }
        };
        readThread.start();
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(readBytes[0], 3000);
        assertTrue(readThread.getState() == Thread.State.BLOCKED || readThread.getState() == Thread.State.WAITING);
        assertSame(writeThread.getState(), Thread.State.TERMINATED);
    }

    @Test
    public void testFlush() {
        ByteQueue queue = new ByteQueue(10, 20);

        Thread writeThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer src = createSrc(3);
                for (int i = 0; i < 1000; i++) {
                    if (i == 500) {
                        queue.flush();
                    }
                    src.limit(src.capacity());
                    src.position(0);
                    queue.enqueue(src);

                }
            }
        };
        writeThread.start();
        final int[] readBytes = {0};
        Thread readThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer output = ByteBuffer.allocateDirect(4);
                do {
                    output.limit(output.capacity());
                    output.position(0);
                    int read = queue.dequeue(output);
                    assertEquals(read, output.remaining());
                    if (read > 0) {
                        readBytes[0] += read;
                        assertTrue(isContinuous(output, read));
                    }
                } while (!queue.isClosed());
            }
        };
        readThread.start();
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            readThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(readBytes[0], 1500);
    }


    @Test
    public void testWriteClose() {
        ByteQueue queue = new ByteQueue(10, 20);
        Thread writeThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer src = createSrc(3);
                for (int i = 0; i < 1000; i++) {
                    src.limit(src.capacity());
                    src.position(0);
                    queue.enqueue(src);
                }
            }
        };
        writeThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queue.close();
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(queue.getCapacity(), 20);
        assertEquals(queue.getSize(), 20);
    }

    @Test
    public void testReadClose() {
        ByteQueue queue = new ByteQueue(1024, 4096);
        ByteBuffer src = createSrc(1024);
        for (int i = 0; i < 4; i++) {
            src.limit(src.capacity());
            src.position(0);
            queue.enqueue(src);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Thread readThread = new Thread() {
            int readBytes = 0;

            @Override
            public void run() {
                super.run();
                ByteBuffer output = ByteBuffer.allocateDirect(4);
                while (true) {
                    int read = queue.dequeue(output);
                    if (read > 0) {
                        readBytes += read;
                        assertTrue(isContinuous(output, read));
                    }
                    if (queue.isClosed()) break;
                }
                assertEquals(readBytes, 4096);
            }
        };
        readThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(readThread.getState() == Thread.State.BLOCKED || readThread.getState() == Thread.State.WAITING);
        queue.close();
        try {
            readThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test()
    public void testWriteAndReadClose() {
        ByteQueue queue = new ByteQueue(1024, 4096);

        Thread writeThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer src = createSrc(36);
                while (true) {
                    if (queue.isClosed()) break;
                    src.limit(src.capacity());
                    src.position(0);
                    queue.enqueue(src);
                }
            }
        };
        writeThread.start();

        Thread readThread = new Thread() {
            @Override
            public void run() {
                super.run();
                ByteBuffer output = ByteBuffer.allocateDirect(4);
                while (true) {
                    output.limit(output.capacity());
                    output.position(0);
                    int read = queue.dequeue(output);
                    if (read > 0) {
                        assertTrue(isContinuous(output, read));
                    }
                    if (queue.isClosed()) break;
                }
            }
        };
        readThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queue.close();
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            readThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}