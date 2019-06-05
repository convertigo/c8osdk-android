package com.convertigo.clientsdk;

/**
 * Created by nicolasa on 04/12/2015.
 */
public class C8oProgress {
    private boolean changed = false;
    private boolean continuous = false;
    private boolean finished = false;
    private boolean pull = true;
    private long current = -1;
    private long total = -1;
    private String status = "";
    private String taskInfo = "";
    private Object raw;

    public C8oProgress() {
    }

    public C8oProgress(C8oProgress progress) {
        continuous = progress.continuous;
        finished = progress.finished;
        pull = progress.pull;
        current = progress.current;
        total = progress.total;
        status = progress.status;
        taskInfo = progress.taskInfo;
        raw = progress.raw;
        changed = progress.changed;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public String toString() {
        return getDirection() + ": " + current + "/" + total + " (" + (finished ? (continuous ? "live" : "done") : "running") + ")";
    }

    public boolean isContinuous() {
        return continuous;
    }

    void setContinuous(boolean continuous) {
        if (this.continuous != continuous) {
            changed = true;
            this.continuous = continuous;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    void setFinished(boolean finished) {
        if (this.finished != finished) {
            changed = true;
            this.finished = finished;
        }
    }

    public boolean isPull() {
        return pull;
    }

    void setPull(boolean pull) {
        if (this.pull != pull) {
            changed = true;
            this.pull = pull;
        }
    }

    public boolean isPush() {
        return !pull;
    }

    public long getCurrent() {
        return current;
    }

    void setCurrent(long current) {
        if (this.current != current) {
            changed = true;
            this.current = current;
        }
    }

    public long getTotal() {
        return total;
    }

    void setTotal(long total) {
        if (this.total != total) {
            changed = true;
            this.total = total;
        }
    }

    public String getDirection() {
        return pull ?
                C8oFullSyncTranslator.FULL_SYNC_RESPONSE_VALUE_DIRECTION_PULL :
                C8oFullSyncTranslator.FULL_SYNC_RESPONSE_VALUE_DIRECTION_PUSH;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        if (!this.status.equals(status)) {
            changed = true;
            this.status = status;
        }
    }

    public String getTaskInfo() {
        return taskInfo;
    }

    void setTaskInfo(String taskInfo) {
        if (!this.taskInfo.equals(taskInfo)) {
            changed = true;
            this.taskInfo = taskInfo;
        }
    }

    public Object getRaw() {
        return raw;
    }


    public void setRaw(Object raw) {
        if (this.raw != raw) {
            changed = true;
            this.raw = raw;
        }
    }
}
