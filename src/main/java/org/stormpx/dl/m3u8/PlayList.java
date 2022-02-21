package org.stormpx.dl.m3u8;

public abstract class PlayList {

    protected Integer version;


    public Integer getVersion() {
        return version;
    }

    public PlayList setVersion(Integer version) {
        this.version = version;
        return this;
    }


    public abstract boolean isMediaFile();
}
