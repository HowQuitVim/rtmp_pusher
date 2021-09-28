package com.zmy.rtmp_pusher.lib.util;

import android.util.Log;

import androidx.annotation.NonNull;

public abstract class WorkerThread extends Thread {
    private boolean exitFlag = false;

    public WorkerThread(@NonNull String name) {
        super(name);
    }

    @Override
    public void run() {
        super.run();
        doOnStart();
        while (true) {
            synchronized (this) {
                if (exitFlag) {
                    break;
                }
            }
            if (doMain()) {
                break;
            }
        }
        synchronized (this) {
            exitFlag = true;
        }
        doOnExit();
        Log.d("zmy", getName() + " exit");
    }

    protected abstract boolean doMain();

    protected void doOnStart() {
    }

    protected void doOnExit() {
    }

    private synchronized void setExitFlag() {
        exitFlag = true;
    }

    public void exit() {
        setExitFlag();
        if (getId() == Thread.currentThread().getId()) return;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
