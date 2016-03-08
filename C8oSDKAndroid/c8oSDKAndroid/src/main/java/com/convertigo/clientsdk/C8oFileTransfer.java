package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nicolas on 04/03/2016.
 */
public class C8oFileTransfer {

    private boolean tasksDbCreated = false;
    private boolean alive = true;

    private C8o c8oTask;
    private Map<String, C8oFileTransferStatus> tasks = null;
    public EventHandler<C8oFileTransfer, C8oFileTransferStatus> raiseTransferStatus;
    public EventHandler<C8oFileTransfer, String> raiseDebug;
    public EventHandler<C8oFileTransfer, Throwable> raiseException;

    public C8oFileTransfer(C8o c8o) throws C8oException {
        this(c8o, "lib_FileTransfer");
    }

    public C8oFileTransfer(C8o c8o, String projectName) throws C8oException {
        this(c8o, projectName, "c8ofiletransfer_tasks");
    }
    
    public C8oFileTransfer(C8o c8o, String projectName, String taskDb) throws C8oException {
        c8oTask = new C8o(c8o.getContext(), c8o.getEndpointConvertigo() + "/projects/" + projectName, new C8oSettings(c8o).setDefaultDatabaseName(taskDb));
    }

    public C8oFileTransfer raiseTransferStatus(EventHandler<C8oFileTransfer, C8oFileTransferStatus> handler) {
        this.raiseTransferStatus = handler;
        return this;
    }

    public C8oFileTransfer raiseDebug(EventHandler<C8oFileTransfer, String> handler) {
        this.raiseDebug = handler;
        return this;
    }

    public C8oFileTransfer raiseException(EventHandler<C8oFileTransfer, Throwable> handler) {
        this.raiseException = handler;
        return this;
    }

    public void start() {
        if (tasks == null) {
            tasks = new HashMap<String, C8oFileTransferStatus>();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        checkTaskDb();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    int skip = 0;

                    Map<String, Object> param = new HashMap<String, Object>();
                    param.put("limit", 1);
                    param.put("include_docs", true);

                    while (alive) {
                        try {
                            param.put("skip", skip);
                            JSONObject res = c8oTask.callJson("fs://.all", param).sync();

                            JSONArray rows = res.getJSONArray("rows");
                            if (rows.length() > 0) {
                                JSONObject row = rows.getJSONObject(0);
                                JSONObject task = row.getJSONObject("doc");
                                if (task == null) {
                                    task = c8oTask.callJson("fs://.get",
                                            "docid", row.getString("id")
                                    ).sync();
                                }
                                String uuid = task.getString("_id");

                                if (!tasks.containsKey(uuid)) {
                                    String filePath = task.getString("filePath");

                                    C8oFileTransferStatus transferStatus = new C8oFileTransferStatus(uuid, filePath);
                                    tasks.put(uuid, transferStatus);
                                    C8oFileTransfer.this.notify(transferStatus);
                                    downloadFile(transferStatus, task);
                                    skip = 0;
                                } else {
                                    skip++;
                                }
                            } else {
                                synchronized (C8oFileTransfer.this) {
                                    C8oFileTransfer.this.wait();
                                    skip = 0;
                                }
                            }
                        } catch (Throwable e) {
                            e.toString();
                        }
                    }
                }
            }).start();
        }
    }

    private void checkTaskDb() throws Throwable {
        if (!tasksDbCreated) {
            c8oTask.callJson("fs://.create").sync();
            tasksDbCreated = true;
        }
    }

    /// <summary>
    /// Add a file to transfer to the download queue. This call must be done after getting a uuid from the Convertigo Server.
    /// the uuid is generated by the server by calling the RequestFile file Sequence.
    /// </summary>
    /// <param name="uuid">a uuid obtained by a call to the 'RequestFile' sequence on the server</param>
    /// <param name="filepath">a path where the file will be assembled when the transfer is finished</param>
    public void downloadFile(String uuid, String filePath) throws Throwable {
        checkTaskDb();

        c8oTask.callJson("fs://.post",
            "_id", uuid,
            "filePath", filePath,
            "replicated", false,
            "assembled", false,
            "remoteDeleted", false
        ).then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                synchronized (C8oFileTransfer.this) {
                    C8oFileTransfer.this.notify();
                }
                return null;
            }
        });
    }


    void downloadFile(C8oFileTransferStatus transferStatus, JSONObject task) {
        boolean needRemoveSession = false;
        C8o c8o = null;
        try {
            c8o = new C8o(c8oTask.getContext(), c8oTask.getEndpoint(), new C8oSettings(c8oTask).setFullSyncLocalSuffix("_" + transferStatus.getUuid()));
            String fsConnector = null;

            //
            // 0 : Authenticates the user on the Convertigo server in order to replicate wanted documents
            //
            if (!task.getBoolean("replicated") || !task.getBoolean("remoteDeleted")) {
                needRemoveSession = true;
                JSONObject json = c8o.callJson(".SelectUuid", "uuid", transferStatus.getUuid()).sync();

                debug("SelectUuid:\n" + json.toString());

                JSONObject doc = json.getJSONObject("document");
                if (!"true".equals(doc.getString("selected"))) {
                    if (!task.getBoolean("replicated")) {
                        throw new Exception("uuid not selected");
                    }
                } else {
                    fsConnector = doc.getString("connector");
                    transferStatus.setState(C8oFileTransferStatus.StateAuthenticated);
                    notify(transferStatus);
                }
            }

            //
            // 1 : Replicate the document discribing the chunks ids list
            //

            if (!task.getBoolean("replicated") && fsConnector != null) {
                final boolean[] locker = new boolean[] { false };

                c8o.callJson("fs://" + fsConnector + ".create").sync();

                needRemoveSession = true;
                c8o.callJson("fs://" + fsConnector + ".replicate_pull").then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        synchronized (locker) {
                            locker[0] = true;
                            locker.notify();
                        }
                        return null;
                    }
                });

                transferStatus.setState(C8oFileTransferStatus.StateReplicate);
                notify(transferStatus);

                Map<String, Object> allOptions = new HashMap<String, Object>();
                allOptions.put("startkey", transferStatus.getUuid() + "_");
                allOptions.put("endkey", transferStatus.getUuid() + "__");

                // Waits the end of the replication if it is not finished
                do {
                    try {
                        synchronized (locker) {
                            locker.wait(500);
                        }

                        JSONObject all = c8o.callJson("fs://" + fsConnector + ".all", allOptions).sync();
                        JSONArray rows = all.getJSONArray("rows");
                        if (rows != null) {
                            int current = rows.length();
                            if (current != transferStatus.getCurrent()) {
                                transferStatus.setCurrent(current);
                                notify(transferStatus);
                            }
                        }
                    } catch (Exception e) {
                        debug(e.toString());
                    }
                } while (!locker[0]);

                if (transferStatus.getCurrent() < transferStatus.getTotal()) {
                    throw new Exception("replication not completed");
                }
                task.put("replicated", true);
                JSONObject res = c8oTask.callJson("fs://" + fsConnector + ".post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", task.getString("_id"),
                    "replicated", true
                ).sync();
                debug("replicated true:\n" + res);
            }

            if (!task.getBoolean("assembled") && fsConnector != null) {
                transferStatus.setState(C8oFileTransferStatus.StateAssembling);
                notify(transferStatus);
                //
                // 2 : Gets the document describing the chunks list
                //
                OutputStream createdFileStream = new FileOutputStream(transferStatus.getFilepath());

                for (int i = 0; i < transferStatus.getTotal(); i++) {
                    JSONObject meta = c8o.callJson("fs://" + fsConnector + ".get", "docid", transferStatus.getUuid() + "_" + i).sync();
                    debug(meta.toString());

                    appendChunk(createdFileStream, meta.getJSONObject("_attachments").getJSONObject("chunk").getString("content_url"));
                }
                createdFileStream.close();

                task.put("assembled", true);
                JSONObject res = c8oTask.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", task.getString("_id"),
                    "assembled", true
                ).sync();
                debug("assembled true:\n" + res);
            }

            if (!task.getBoolean("remoteDeleted") && fsConnector != null) {
                transferStatus.setState(C8oFileTransferStatus.StateCleaning);
                notify(transferStatus);

                JSONObject res = c8o.callJson("fs://" + fsConnector + ".destroy").sync();
                debug("destroy local true:\n" + res);

                needRemoveSession = true;
                res = c8o.callJson(".DeleteUuid", "uuid", transferStatus.getUuid()).sync();
                debug("deleteUuid:\n" + res);

                task.put("remoteDeleted", true);
                res = c8oTask.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", task.getString("_id"),
                    "remoteDeleted", true
                ).sync();
                debug("remoteDeleted true:\n" + res);
            }

            if (task.getBoolean("replicated") && task.getBoolean("assembled") && task.getBoolean("remoteDeleted")) {
                JSONObject res = c8oTask.callJson("fs://.delete", "docid", transferStatus.getUuid()).sync();
                debug("local delete:\n" + res);

                transferStatus.setState(C8oFileTransferStatus.StateFinished);
                notify(transferStatus);
            }
        } catch (Throwable e) {
            notify(e);
        }

        if (needRemoveSession && c8o != null) {
            c8o.callJson(".RemoveSession");
        }

        tasks.remove(transferStatus.getUuid());

        synchronized (this) {
            this.notify();
        }
    }

    private void appendChunk(OutputStream createdFileStream, String contentPath) throws IOException {
        contentPath = contentPath.replaceFirst("^file:", "");
        InputStream chunkStream = new FileInputStream(contentPath);
        IOUtils.copy(chunkStream, createdFileStream);
        chunkStream.close();
    }

    private void notify(C8oFileTransferStatus transferStatus) {
        if (raiseTransferStatus != null) {
            raiseTransferStatus.on(this, transferStatus);
        }
    }

    private void notify(Throwable exception) {
        if (raiseException != null) {
            raiseException.on(this, exception);
        }
    }

    private void debug(String debug) {
        if (raiseDebug != null) {
            raiseDebug.on(this, debug);
        }
    }
}
