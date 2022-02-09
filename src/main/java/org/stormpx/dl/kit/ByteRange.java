package org.stormpx.dl.kit;

public record ByteRange(Integer offset, Integer size) {


    public int start(){
        return offset==null?0:offset;
    }
    public int end(){
        return offset+size;
    }
}
