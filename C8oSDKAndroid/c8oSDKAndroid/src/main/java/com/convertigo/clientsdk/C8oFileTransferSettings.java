package com.convertigo.clientsdk;

/**
 * Created by charlesg on 12/07/2016.
 */
public class C8oFileTransferSettings extends C8oFileTransferBase {
    public C8oFileTransferSettings() {
    }

    public C8oFileTransferSettings(C8oFileTransferSettings c8oFileTransferSettings){
        copy(c8oFileTransferSettings);
    }

    public C8oFileTransferSettings setProjectName(String projectName) {
        if (projectName != null) {
            this.projectName = projectName;
        }
        return this;
    }

    public C8oFileTransferSettings setTaskDb(String taskDb) {
        if (taskDb != null) {
            this.taskDb = taskDb;
        }
        return this;
    }

    public C8oFileTransferSettings setMaxRunning(int maxRunning) {
        if (maxRunning > 0) {
            this.maxRunning = maxRunning;
        }
        return this;
    }

    public C8oFileTransferSettings SetMaxDurationForTransferAttempt(long maxDurationForTransferAttempt) {
        this.maxDurationForTransferAttempt = maxDurationForTransferAttempt;
        return this;
    }
}
