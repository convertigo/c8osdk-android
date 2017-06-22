package com.convertigo.clientsdk;

/**
 * Created by charlesg on 12/07/2016.
 */
class C8oFileTransferBase {
    protected String projectName = "lib_FileTransfer";
    protected String taskDb = "c8ofiletransfer_tasks";
    protected long maxDurationForTransferAttempt = 1000 * 60 * 20; // 20 minutes
    protected int maxRunning = 4;

    public String getProjectName() {
        return projectName;
    }

    public String getTaskDb() {
        return taskDb;
    }

    public int getMaxRunning() {
        return maxRunning;
    }

    public long getMaxDurationForTransferAttempt() {
        return maxDurationForTransferAttempt;
    }

    protected void copy(C8oFileTransferBase settings) {
        projectName = settings.projectName;
        taskDb = settings.taskDb;
        maxRunning = settings.maxRunning;
        maxDurationForTransferAttempt = settings.maxDurationForTransferAttempt;
    }
}
