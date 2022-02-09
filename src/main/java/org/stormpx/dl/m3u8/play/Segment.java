package org.stormpx.dl.m3u8.play;

import org.stormpx.dl.kit.ByteRange;
import org.stormpx.dl.m3u8.PlayListElement;

public class Segment implements PlayListElement {

    private long sequence=0;

    private String title;
    private Double duration;

    private ByteRange byteRange;
    private String uri;

    public long getSequence() {
        return sequence;
    }

    public Segment setSequence(long sequence) {
        this.sequence = sequence;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Segment setTitle(String title) {
        this.title = title;
        return this;
    }

    public Double getDuration() {
        return duration;
    }

    public Segment setDuration(Double duration) {
        this.duration = duration;
        return this;
    }


    public String getUri() {
        return uri;
    }

    public Segment setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public ByteRange getByteRange() {
        return byteRange;
    }

    public Segment setByteRange(ByteRange byteRange) {
        this.byteRange = byteRange;
        return this;
    }
}
