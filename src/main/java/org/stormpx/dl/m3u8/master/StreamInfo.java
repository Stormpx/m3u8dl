package org.stormpx.dl.m3u8.master;

import org.stormpx.dl.m3u8.PlayListElement;

import java.util.List;

public class StreamInfo implements PlayListElement {

    private Integer bandwidth;

    private Integer averageBandwidth;

    private List<String> codecs;

    private String resolution;

    private Double framerate;

    private String hdcpLevel;

    private String audio;

    private String video;

    private String subTitles;

    private String closedCaptions;

    private String uri;


    public Integer getBandwidth() {
        return bandwidth;
    }

    public StreamInfo setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
        return this;
    }

    public Integer getAverageBandwidth() {
        return averageBandwidth;
    }

    public StreamInfo setAverageBandwidth(Integer averageBandwidth) {
        this.averageBandwidth = averageBandwidth;
        return this;
    }

    public List<String> getCodecs() {
        return codecs;
    }

    public StreamInfo setCodecs(List<String> codecs) {
        this.codecs = codecs;
        return this;
    }

    public String getResolution() {
        return resolution;
    }

    public StreamInfo setResolution(String resolution) {
        this.resolution = resolution;
        return this;
    }

    public Double getFramerate() {
        return framerate;
    }

    public StreamInfo setFramerate(Double framerate) {
        this.framerate = framerate;
        return this;
    }

    public String getHdcpLevel() {
        return hdcpLevel;
    }

    public StreamInfo setHdcpLevel(String hdcpLevel) {
        this.hdcpLevel = hdcpLevel;
        return this;
    }

    public String getAudio() {
        return audio;
    }

    public StreamInfo setAudio(String audio) {
        this.audio = audio;
        return this;
    }

    public String getVideo() {
        return video;
    }

    public StreamInfo setVideo(String video) {
        this.video = video;
        return this;
    }

    public String getSubTitles() {
        return subTitles;
    }

    public StreamInfo setSubTitles(String subTitles) {
        this.subTitles = subTitles;
        return this;
    }

    public String getClosedCaptions() {
        return closedCaptions;
    }

    public StreamInfo setClosedCaptions(String closedCaptions) {
        this.closedCaptions = closedCaptions;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public StreamInfo setUri(String uri) {
        this.uri = uri;
        return this;
    }
}
