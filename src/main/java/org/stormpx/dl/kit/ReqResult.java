package org.stormpx.dl.kit;

import java.io.InputStream;
import java.net.URI;

public class ReqResult implements AutoCloseable {

    private boolean success;

    private URI targetUri;

    private boolean m3u8File;

    private InputStream inputStream;

    private Integer contentLength;

    public ReqResult(boolean success) {
        this.success = success;
    }

    public ReqResult setTargetUri(URI targetUri) {
        this.targetUri = targetUri;
        return this;
    }

    public ReqResult setM3u8File(boolean m3u8File) {
        this.m3u8File = m3u8File;
        return this;
    }

    public ReqResult setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public ReqResult setContentLength(Integer contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public URI getTargetUri() {
        return targetUri;
    }

    public boolean isM3u8File() {
        return m3u8File;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    @Override
    public void close() throws Exception {
        if (inputStream!=null)
            inputStream.close();
    }
}
