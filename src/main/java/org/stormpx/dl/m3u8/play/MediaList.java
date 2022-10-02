package org.stormpx.dl.m3u8.play;


import org.stormpx.dl.m3u8.PlayList;
import org.stormpx.dl.m3u8.PlayListElement;
import org.stormpx.dl.m3u8.PlayListType;

import java.util.ArrayList;
import java.util.List;

public class MediaList extends PlayList {

    private Double targetDuration;

    private long mediaSequence=0;

    private long discontinuitySequence;

    private boolean iFramesOnly;

    private List<PlayListElement> elements;

    private int segmentSize=0;

    private PlayListType type;

    private boolean endTag;

    public MediaList() {
        this.elements=new ArrayList<>();
    }

    public void addElement(PlayListElement element){
        if (element instanceof Segment seq){
            seq.setSequence(mediaSequence+segmentSize);
            segmentSize++;
        }
        this.elements.add(element);
    }


    @Override
    public boolean isMediaFile() {
        return true;
    }

    public Double getTargetDuration() {
        return targetDuration;
    }

    public MediaList setTargetDuration(Double targetDuration) {
        this.targetDuration = targetDuration;
        return this;
    }

    public long getMediaSequence() {
        return mediaSequence;
    }

    public MediaList setMediaSequence(long mediaSequence) {
        this.mediaSequence = mediaSequence;
        return this;
    }

    public boolean isiFramesOnly() {
        return iFramesOnly;
    }

    public MediaList setiFramesOnly(boolean iFramesOnly) {
        this.iFramesOnly = iFramesOnly;
        return this;
    }

    public List<PlayListElement> getElements() {
        return elements;
    }

    public MediaList setElements(List<PlayListElement> elements) {
        this.elements = elements;
        return this;
    }

    public boolean isEnd() {
        return endTag;
    }

    public MediaList setEnd(boolean endTag) {
        this.endTag = endTag;
        return this;
    }

    public int getSegmentSize() {
        return segmentSize;
    }

    public MediaList setSegmentSize(int segmentSize) {
        this.segmentSize = segmentSize;
        return this;
    }

    public PlayListType getType() {
        return type;
    }

    public MediaList setType(PlayListType type) {
        this.type = type;
        return this;
    }

    public long getDiscontinuitySequence() {
        return discontinuitySequence;
    }

    public MediaList setDiscontinuitySequence(long discontinuitySequence) {
        this.discontinuitySequence = discontinuitySequence;
        return this;
    }
}
