package com.UFL;//package com.UFL;

public class PeerConfigModel {
    private int port;
    private int downLoadId;
    private int uploadId;

    public int getPort() {
        return port;
    }

    public int getDownLoadId() {
        return downLoadId;
    }

    public int getUploadId() {
        return uploadId;
    }

    public PeerConfigModel(int port, int downLoadId, int uploadId) {
        this.port = port;
        this.downLoadId = downLoadId;
        this.uploadId = uploadId;
    }
}
