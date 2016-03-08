package com.convertigo.clientsdk;

/**
 * Created by Nicolas on 04/03/2016.
 */
public class C8oFileTransferStatus {
    public static final DownloadState StateQueued = DownloadState.Queued;
    public static final DownloadState StateAuthenticated = DownloadState.Authenticated;
    public static final DownloadState StateReplicate = DownloadState.Replicate;
    public static final DownloadState StateAssembling = DownloadState.Assembling;
    public static final DownloadState StateCleaning = DownloadState.Cleaning;
    public static final DownloadState StateFinished = DownloadState.Finished;

    public enum DownloadState
    {
        Queued("queued"),
        Authenticated("authenticated"),
        Replicate("replicating"),
        Assembling("assembling"),
        Cleaning("cleaning"),
        Finished("finished");

        String toString;

        DownloadState(String toString) {
            this.toString = toString;
        }

        public String toString() {
            return toString;
        }
    }

    private DownloadState state = StateQueued;

    public DownloadState getState() {
        return state;
    }

    void setState(DownloadState state) {
        this.state = state;
    }

    private String uuid;

    public String getUuid() {
        return uuid;
    }

    private String filepath;

    public String getFilepath() {
        return filepath;
    }

    public int current;

    public int getCurrent() {
        return current;
    }

    void setCurrent(int current) {
        this.current = current;
    }

    public int total;

    public int getTotal() {
        return total;
    }

    public double getProgress() {
        return total > 0 ? current * 1.0f / total : 0;
    }

    C8oFileTransferStatus(String uuid, String filepath) {
        this.uuid = uuid;
        this.filepath = filepath;
        total = Integer.parseInt(uuid.substring(uuid.lastIndexOf('-') + 1));
    }
}
