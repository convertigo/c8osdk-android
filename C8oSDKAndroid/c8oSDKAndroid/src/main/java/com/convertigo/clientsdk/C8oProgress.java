package com.convertigo.clientsdk;

/**
 * Created by nicolasa on 04/12/2015.
 */
public class C8oProgress {
    boolean finished;
    boolean pull;
    long current;
    long total;
    String taskInfo;
    String status;
    Object raw;

    C8oProgress()
    {
    }

    @Override
    public String toString()
    {
        return "" + current + "/" + total + " (" + (finished ? "done" : "running") + ")";
    }

    public long getCurrent()
    {
        return current;
    }

    public long getTotal()
    {
        return total;
    }

    public String getDirection()
    {
        return pull ?
                C8oFullSyncTranslator.FULL_SYNC_RESPONSE_VALUE_DIRECTION_PULL :
                C8oFullSyncTranslator.FULL_SYNC_RESPONSE_VALUE_DIRECTION_PUSH;
    }

    public String getTaskInfo()
    {
        return taskInfo;
    }

    public String getStatus()
    {
        return status;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public boolean isPull()
    {
        return pull;
    }

    public boolean isPush()
    {
        return !pull;
    }

    public Object getRaw()
    {
        return raw;
    }
}
