package org.stormpx.dl.m3u8.master;

import org.stormpx.dl.m3u8.EncryptInfo;
import org.stormpx.dl.m3u8.PlayList;

import java.util.ArrayList;
import java.util.List;

public class MasterList extends PlayList {

    private List<EncryptInfo> encryptInfos;

    private List<StreamInfo> streams;


    public MasterList() {
        this.encryptInfos=new ArrayList<>();
        this.streams =new ArrayList<>();
    }

    @Override
    public boolean isMediaFile() {
        return false;
    }

    public void addStream(StreamInfo element){
        this.streams.add(element);
    }

    public List<StreamInfo> getStreams() {
        return streams;
    }

    public void addEncryptInfo(EncryptInfo encryptInfo){
        this.encryptInfos.add(encryptInfo);
    }

    public List<EncryptInfo> getEncryptInfos() {
        return encryptInfos;
    }
}
