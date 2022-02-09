package org.stormpx.dl.m3u8;

import java.util.Arrays;
import java.util.Objects;

public class EncryptInfo implements PlayListElement {

    private EncryptMethod method;

    private String uri;

    private byte[] iv;

    private String  keyFormat;

    private String keyFormatVersions;

    public EncryptInfo(EncryptMethod method, String uri, byte[] iv, String keyFormat, String keyFormatVersions) {
        this.method = method;
        this.uri = uri;
        this.iv = iv;
        this.keyFormat = keyFormat;
        this.keyFormatVersions = keyFormatVersions;
    }

    public EncryptMethod getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public byte[] getIv() {
        return iv;
    }

    public String getKeyFormat() {
        return keyFormat;
    }

    public String getKeyFormatVersions() {
        return keyFormatVersions;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptInfo that = (EncryptInfo) o;
        return method == that.method && Objects.equals(uri, that.uri) && Arrays.equals(iv, that.iv) && Objects.equals(keyFormat, that.keyFormat) && Objects.equals(keyFormatVersions, that.keyFormatVersions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, uri, keyFormat, keyFormatVersions);
        result = 31 * result + Arrays.hashCode(iv);
        return result;
    }
}
