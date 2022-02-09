package org.stormpx.dl.m3u8;


import org.stormpx.dl.m3u8.play.Segment;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PlayListFile {


    private Integer version;

    private Integer targetDuration;

    private long mediaSequence=0;

    private boolean iFramesOnly;

    private List<PlayListElement> elements;

    private int segmentSize=0;

    private PlayListType type;

    private boolean endTag;

    public PlayListFile() {
        this.elements=new ArrayList<>();
    }

    public void addElement(PlayListElement element){
        if (element instanceof Segment seq){
            seq.setSequence(mediaSequence+segmentSize);
            segmentSize++;
        }
        this.elements.add(element);
    }


    public Integer getVersion() {
        return version;
    }

    public PlayListFile setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public Integer getTargetDuration() {
        return targetDuration;
    }

    public PlayListFile setTargetDuration(Integer targetDuration) {
        this.targetDuration = targetDuration;
        return this;
    }

    public Long getMediaSequence() {
        return mediaSequence;
    }

    public PlayListFile setMediaSequence(Long mediaSequence) {
        this.mediaSequence = mediaSequence;
        return this;
    }

    public boolean isiFramesOnly() {
        return iFramesOnly;
    }

    public PlayListFile setiFramesOnly(boolean iFramesOnly) {
        this.iFramesOnly = iFramesOnly;
        return this;
    }

    public List<PlayListElement> getElements() {
        return elements;
    }

    public PlayListFile setElements(List<PlayListElement> elements) {
        this.elements = elements;
        return this;
    }

    public boolean isEndTag() {
        return endTag;
    }

    public PlayListFile setEndTag(boolean endTag) {
        this.endTag = endTag;
        return this;
    }

    public int getSegmentSize() {
        return segmentSize;
    }

    public PlayListFile setSegmentSize(int segmentSize) {
        this.segmentSize = segmentSize;
        return this;
    }

    public PlayListType getType() {
        return type;
    }

    public PlayListFile setType(PlayListType type) {
        this.type = type;
        return this;
    }
}
