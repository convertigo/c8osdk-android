package com.convertigo.clientsdk;

/**
 * Created by Nicolas on 04/03/2016.
 */
public class C8oFileTransferStatus {

    public static final C8oFileTransferState StateQueued = C8oFileTransferState.Queued;
    public static final C8oFileTransferState StateAuthenticated = C8oFileTransferState.Authenticated;
    public static final C8oFileTransferState StateSplitting = C8oFileTransferState.Splitting;
    public static final C8oFileTransferState StateReplicate = C8oFileTransferState.Replicate;
    public static final C8oFileTransferState StateAssembling = C8oFileTransferState.Assembling;
    public static final C8oFileTransferState StateCleaning = C8oFileTransferState.Cleaning;
    public static final C8oFileTransferState StateFinished = C8oFileTransferState.Finished;

    public enum C8oFileTransferState
    {
        Queued("queued"),
        Authenticated("authenticated"),
        Splitting("splitting"),
        Replicate("replicating"),
        Assembling("assembling"),
        Cleaning("cleaning"),
        Finished("finished");

        String toString;

        C8oFileTransferState(String toString) {
            this.toString = toString;
        }

        public String toString() {
            return toString;
        }
    }

    private C8oFileTransferState state = StateQueued;

    public C8oFileTransferState getState() {
        return state;
    }

    void setState(C8oFileTransferState state) {
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

    private String serverFilePath;

    public String getServerFilePath() { return serverFilePath; }

    public void setServerFilePath(String serverFilePath) { this.serverFilePath = serverFilePath; }

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

    private boolean download;

    public boolean isDownload() { return download; }

    public void setDownload(boolean download) {
        this.download = download;
        if(download){
            tot();
        }
    }

    C8oFileTransferStatus(String uuid, String filepath) {
        this.uuid = uuid;
        this.filepath = filepath;
        total = 0;
        //total = Integer.parseInt(uuid.substring(uuid.lastIndexOf('-') + 1));
    }

    private void tot(){
        total = Integer.parseInt(uuid.substring(uuid.lastIndexOf('-') + 1));
    }
}
