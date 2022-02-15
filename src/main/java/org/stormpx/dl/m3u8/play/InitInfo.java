package org.stormpx.dl.m3u8.play;

import org.stormpx.dl.kit.ByteRange;
import org.stormpx.dl.m3u8.PlayListElement;

import java.util.Objects;

public class InitInfo implements PlayListElement {

    private String uri;
    private ByteRange byteRange;

    public InitInfo(String uri, ByteRange byteRange) {
        Objects.requireNonNull(uri,"URI is required");
        this.uri = uri;
        this.byteRange = byteRange;
    }



    public String getUri() {
        return uri;
    }

    public InitInfo setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public ByteRange getByteRange() {
        return byteRange;
    }

    public InitInfo setByteRange(ByteRange byteRange) {
        this.byteRange = byteRange;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitInfo initInfo = (InitInfo) o;
        return Objects.equals(uri, initInfo.uri) && Objects.equals(byteRange, initInfo.byteRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, byteRange);
    }
}

