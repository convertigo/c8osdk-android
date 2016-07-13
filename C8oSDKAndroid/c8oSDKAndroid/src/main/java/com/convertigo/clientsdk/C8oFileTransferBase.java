package com.convertigo.clientsdk;

/**
 * Created by charlesg on 12/07/2016.
 */
public class C8oFileTransferBase {
    protected int[] maxRunning = { 4 };

    public int[] getMaxRunning(){
        return maxRunning;
    }

    public void Copy(C8oFileTransferSettings c8oFileTransferSettings){
        maxRunning = c8oFileTransferSettings.getMaxRunning();
    }

}
