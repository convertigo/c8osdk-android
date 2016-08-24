package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nicolas on 04/03/2016.
 */
public class C8oFileTransfer extends C8oFileTransferBase {

    private boolean tasksDbCreated = false;
    private boolean alive = true;
    private int chunkSize = 1000 * 1024;
    private int[] _maxRunning;

    private C8o c8oTask;
    private Map<String, C8oFileTransferStatus> tasks = null;
    public EventHandler<C8oFileTransfer, C8oFileTransferStatus> raiseTransferStatus;
    public EventHandler<C8oFileTransfer, String> raiseDebug;
    public EventHandler<C8oFileTransfer, Throwable> raiseException;

    private Map<String, InputStream> streamToUpload;

    public C8oFileTransfer(C8o c8o) throws C8oException {
        this(c8o, null);
    }
    public C8oFileTransfer(C8o c8o, C8oFileTransferSettings c8oFileTransferSettings) throws C8oException {
        if (c8oFileTransferSettings != null)
        {
            copy(c8oFileTransferSettings);
        }
        _maxRunning = new int[] {maxRunning};
        c8oTask = new C8o(c8o.getContext(), c8o.getEndpointConvertigo() + "/projects/" + projectName, new C8oSettings(c8o).setDefaultDatabaseName(taskDb));
        streamToUpload = new HashMap<String, InputStream>();
    }

    public C8oFileTransfer raiseTransferStatus(EventHandler<C8oFileTransfer, C8oFileTransferStatus> handler) {
        raiseTransferStatus = handler;
        return this;
    }

    public C8oFileTransfer raiseDebug(EventHandler<C8oFileTransfer, String> handler) {
        raiseDebug = handler;
        return this;
    }

    public C8oFileTransfer raiseException(EventHandler<C8oFileTransfer, Throwable> handler) {
        raiseException = handler;
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

                                // If this document id is not already in the tasks list
                                if (!tasks.containsKey(uuid) && (task.has("download") || task.has("upload"))) {

                                    /*c8oTask.callJson("fs://.delete", "docid", uuid).sync();
                                    if (5*3 == 15)
                                        continue;*/

                                    String filePath = task.getString("filePath");

                                    // Add the document id to the tasks list
                                    C8oFileTransferStatus transferStatus = new C8oFileTransferStatus(uuid, filePath);
                                    tasks.put(uuid, transferStatus);
                                    C8oFileTransfer.this.notify(transferStatus);

                                    if (task.has("download")) {
                                        transferStatus.setDownload(true);
                                        downloadFile(transferStatus, task);
                                    } else if (task.has("upload")) {
                                        transferStatus.setDownload(false);
                                        uploadFile(transferStatus, task);
                                    }

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
            "remoteDeleted", false,
            "download", 0
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
            synchronized (_maxRunning) {
                if (_maxRunning[0] <= 0) {
                    _maxRunning.wait();
                }
                _maxRunning[0]--;
            }
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
                JSONObject res = c8oTask.callJson("fs://.post",
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
                FileChannel createdFileStream = new FileOutputStream(transferStatus.getFilepath()).getChannel();

                for (int i = 0; i < transferStatus.getTotal(); i++) {
                    JSONObject meta = c8o.callJson("fs://" + fsConnector + ".get", "docid", transferStatus.getUuid() + "_" + i).sync();
                    debug(meta.toString());
                    String adr = meta.getJSONObject("_attachments").getJSONObject("chunk").getString("content_url");
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
        finally {
            synchronized (_maxRunning) {
                _maxRunning[0]++;
                _maxRunning.notify();
            }
        }

        if (needRemoveSession && c8o != null) {
            c8o.callJson(".RemoveSession");
        }

        tasks.remove(transferStatus.getUuid());

        synchronized (this) {
            this.notify();
        }
    }

    private void appendChunk(FileChannel createdFileStream, String contentPath) throws IOException {
        contentPath = contentPath.replaceFirst("^file:", "");
        FileChannel fromChannel = new FileInputStream(contentPath).getChannel();
        fromChannel.transferTo(0, fromChannel.size(), createdFileStream);
        fromChannel.close();
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

    /// <summary>
    /// Add a file to transfer to the upload queue.
    /// </summary>
    /// <param name="fileName"></param>
    /// <param name="fileStream"></param>
    public void uploadFile(String fileName, InputStream fileStream) throws Throwable {

        if (!fileStream.markSupported()) {
            throw new Exception("The stream to upload does not support the mark() and reset() methods.");
        }

        // Creates the task database if it doesn't exist
        checkTaskDb();

        // Initializes the uuid ending with the number of chunks
        String uuid = UUID.randomUUID().toString();

        c8oTask.callJson("fs://.post",
                "_id", uuid,
                "filePath", fileName,
                "splitted", false,
                "replicated", false,
                "localDeleted", false,
                "assembled", false,
                "upload", 0
        ).then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                synchronized (C8oFileTransfer.this) {
                    C8oFileTransfer.this.notify();
                }
                return null;
            }
        });

        streamToUpload.put(uuid, fileStream);
    }

    void uploadFile(C8oFileTransferStatus transferStatus, final JSONObject task) {

        try {
            synchronized (_maxRunning) {
                if (_maxRunning[0] <= 0) {
                    _maxRunning.wait();
                }
                _maxRunning[0]--;
            }
            JSONObject res = null;
            String fileName = transferStatus.getFilepath();
            final boolean[] locker = new boolean[] { false };

            // Creates a c8o instance with a specific fullsync local suffix in order to store chunks in a specific database
            C8o c8o = new C8o(c8oTask.getContext(), c8oTask.getEndpoint(), new C8oSettings(c8oTask).setFullSyncLocalSuffix("_" + transferStatus.getUuid()).setDefaultDatabaseName("c8ofiletransfer"));

            // Creates the local db
            c8o.callJson("fs://.create").sync();
            // res = c8o.callJson("fs://.all").sync();

            // If the file is not already splitted and stored in the local database
            if (!task.getBoolean("splitted")) {
                transferStatus.setState( C8oFileTransferStatus.StateSplitting);
                notify(transferStatus);

                // Checks if the stream is still stored
                if (!streamToUpload.containsKey(transferStatus.getUuid())) {
                    // Removes the local database
                    c8o.callJson("fs://.reset").sync();
                    // Removes the task doc
                    c8oTask.callJson("fs://.delete", "docid", transferStatus.getUuid()).sync();
                    throw new Exception("The file '" + task.get("filePath") + "' can't be upload because it was stopped before the file content was handled");
                }

                InputStream fileStream = null;
                InputStream chunk = null;
                //
                // 1 : Split the file and store it locally
                //
                try {
                    fileStream = streamToUpload.get(transferStatus.getUuid());
                    byte[] buffer = new byte[chunkSize];
                    String uuid = transferStatus.getUuid();
                    int countTot = -1;
                    int read = 0;
                    while (read >= 0) {
                        countTot ++;
                        read = fileStream.read(buffer);
                        if(read >= 0){
                            String docid = uuid + "_" + countTot;
                            c8o.callJson("fs://.post",
                                    "_id", docid,
                                    "fileName", fileName,
                                    "type", "chunk",
                                    "uuid", uuid).sync();

                            chunk = new ByteArrayInputStream(buffer, 0, read);
                            c8o.callJson("fs://.put_attachment",
                                    "docid", docid,
                                    "name", "chunk",
                                    "content_type", "application/octet-stream",
                                    "content", chunk).sync();

                            chunk.close();
                        }
                        else{

                        }

                    }

                    transferStatus.total = countTot;

                } catch (Exception e) {
                    throw e;
                } finally {
                    if (fileStream != null) {
                        fileStream.close();
                    }
                    if (chunk != null) {
                        chunk.close();
                    }
                }


                // Updates the state document in the c8oTask database
                res = c8oTask.callJson("fs://.post",
                        C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                        "_id", task.getString("_id"),
                        "splitted", task.put("splitted", true).getBoolean("splitted")
                ).sync();
                debug("splitted true:\n" + res.toString());
            }

            // res = c8o.callJson("fs://.all").sync();
            streamToUpload.remove(transferStatus.getUuid());

            // If the local database is not replecated to the server
            if (!task.getBoolean("replicated")) {
                //
                // 2 : Authenticates
                //
                res = c8o.callJson(".SetAuthenticatedUser", "userId", transferStatus.getUuid()).sync();
                debug("SetAuthenticatedUser:\n" + res.toString());

                transferStatus.setState(C8oFileTransferStatus.StateAuthenticated);
                notify(transferStatus);

                //
                // 3 : Replicates to server
                //
                transferStatus.setState(C8oFileTransferStatus.StateReplicate);
                notify(transferStatus);

                locker[0] = false;
                c8o.callJson("fs://.replicate_push").then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        synchronized (locker) {
                            locker[0] = true;
                            locker.notify();
                        }
                        return null;
                    }
                });

                // Waits the end of the replication if it is not finished
                do {
                    try {
                        synchronized (locker) {
                            locker.wait(500);
                        }

                        // Asks how many documents are in the server database with this uuid
                        JSONObject json = c8o.callJson(".c8ofiletransfer.GetViewCountByUuid", "_use_key", transferStatus.getUuid()).sync();
                        Object rows = json.getJSONObject("document").getJSONObject("couchdb_output").get("rows");// json.SelectToken("document.couchdb_output.rows.item");
                        if (rows != null && rows instanceof JSONObject) {
                            String currentStr = ((JSONObject) rows).getJSONObject("item").getString("value");
                            int current = Integer.parseInt(currentStr);
                            if (current != transferStatus.getCurrent()) {
                                transferStatus.setCurrent(current);
                                notify(transferStatus);
                            }
                        }
                    } catch (Exception e) {
                        debug(e.toString());
                    }
                } while (!locker[0]);

                // Updates the state document in the task database
                res = c8oTask.callJson("fs://.post",
                        C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                        "_id", task.getString("_id"),
                        "replicated", task.put("replicated", true).getBoolean("replicated")
                ).sync();
                debug("replicated true:\n" + res);
            }

            // If the local database containing chunks is not deleted
            locker[0] = true;
            if (!task.getBoolean("localDeleted"))
            {
                transferStatus.setState(C8oFileTransferStatus.StateCleaning);
                notify(transferStatus);

                locker[0] = false;
                //
                // 4 : Delete the local database containing chunks
                //
                c8o.callJson("fs://.reset").then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {

                        c8oTask.callJson("fs://.post",
                                C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                                "_id", task.getString("_id"),
                                "localDeleted", task.put("localDeleted", true).getBoolean("localDeleted")
                        ).then(new C8oOnResponse<JSONObject>() {
                            @Override
                            public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                                debug("localDeleted true:\n" + response);
                                return null;
                            }
                        });

                        synchronized (locker)
                        {
                            locker[0] = true;
                            locker.notify();
                        }

                        return null;
                    }
                });
            }

            // If the file is not assembled in the server
            if (!task.getBoolean("assembled"))
            {
                transferStatus.setState(C8oFileTransferStatus.StateAssembling);
                notify(transferStatus);

                //
                // 5 : Request the server to assemble chunks to the initial file
                //
                res = c8o.callJson(".StoreDatabaseFileToLocal",
                                        "uuid", transferStatus.getUuid(),
                                        "numberOfChunks", transferStatus.total
                ).sync();
                JSONObject document = res.getJSONObject("document");
                if (document.get("serverFilePath") == null)
                {
                    throw new Exception("Can't find the serverFilePath in JSON response : " + res);
                }
                String serverFilePath = document.getString("serverFilePath");
                res = c8oTask.callJson("fs://.post",
                        C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                        "_id", task.getString("_id"),
                        "assembled", task.put("assembled", true).getBoolean("assembled"),
                        "serverFilePath", task.put("serverFilePath", serverFilePath).getString("serverFilePath")
                ).sync();
                debug("assembled true:\n" + res);
            }

            transferStatus.setServerFilePath(task.getString("serverFilePath"));

            // Waits the local database is deleted
            do
            {
                synchronized (locker)
                {
                    locker.wait(500);
                }
            } while (!locker[0]);

            //
            // 6 : Remove the task document
            //
            res = c8oTask.callJson("fs://.delete", "docid", transferStatus.getUuid()).sync();
            debug("local delete:\n" + res.toString());

            transferStatus.setState(C8oFileTransferStatus.StateFinished);
            notify(transferStatus);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        finally {
            synchronized (_maxRunning) {
                _maxRunning[0]++;
                _maxRunning.notify();
            }
        }
    }
}
