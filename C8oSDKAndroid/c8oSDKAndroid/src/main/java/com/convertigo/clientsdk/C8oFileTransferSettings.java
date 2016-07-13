package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oException;

/**
 * Created by charlesg on 12/07/2016.
 */
public class C8oFileTransferSettings extends C8oFileTransferBase{
    public C8oFileTransferSettings(){

    }

    public C8oFileTransferSettings(C8oFileTransferSettings c8oFileTransferSettings){
        Copy(c8oFileTransferSettings);
    }

    public C8oFileTransferSettings Clone(){
        return new C8oFileTransferSettings(this);
    }

    public C8oFileTransferSettings SetMaxRunning(int maxRunning)throws C8oException{
        if (maxRunning <= 0 || maxRunning > 4){
            throw new C8oException("maxRunning must be between 1 and 4");
        }
        else{
            this.maxRunning[0] = maxRunning;
        }
        return this;
    }
}
