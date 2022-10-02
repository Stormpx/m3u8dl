package org.stormpx.dl.m3u8;

import org.stormpx.dl.kit.ByteRange;
import org.stormpx.dl.kit.DL;
import org.stormpx.dl.kit.Strs;
import org.stormpx.dl.m3u8.master.MasterList;
import org.stormpx.dl.m3u8.master.StreamInfo;
import org.stormpx.dl.m3u8.play.InitInfo;
import org.stormpx.dl.m3u8.play.MediaList;
import org.stormpx.dl.m3u8.play.Segment;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.*;

public class M3u8Parser {

    private final static Set<String> MEDIA_TAGS=Set.of(
            "#EXTINF",
            "#EXT-X-BYTERANGE",
            "#EXT-X-DISCONTINUITY",
            "#EXT-X-KEY",
            "#EXT-X-MAP",
            "#EXT-X-PROGRAM-DATE-TIME",
            "#EXT-X-DATERANGE",
            "#EXT-X-TARGETDURATION",
            "#EXT-X-MEDIA-SEQUENCE",
            "#EXT-X-DISCONTINUITY-SEQUENCE",
            "#EXT-X-ENDLIST",
            "#EXT-X-PLAYLIST-TYPE",
            "#EXT-X-I-FRAMES-ONLY"
    );

    private final static Set<String> MASTER_TAGS=Set.of(
            "#EXT-X-MEDIA",
            "#EXT-X-STREAM-INF",
            "#EXT-X-I-FRAME-STREAM-INF",
            "#EXT-X-SESSION-DATA",
            "#EXT-X-SESSION-KEY"
    );

    private Parser parser;

    private boolean extm3u;

    private String versionTag;

    private boolean isMediaTag(String line){
        if (!isComment(line))
            return false;
        int idx = line.indexOf(":");
        if (idx>=0){
            line=line.substring(0,idx);
        }
        return MEDIA_TAGS.contains(line);
    }

    private boolean isMasterTag(String line){
        if (!isComment(line))
            return false;
        int idx = line.indexOf(":");
        if (idx>=0){
            line=line.substring(0,idx);
        }
        return MASTER_TAGS.contains(line);
    }

    private boolean isComment(String line){
        return line!=null&&line.startsWith("#");
    }

    private String getTagValue(String line){

        return getTagValue(line,false);
    }

    private String getTagValue(String line,boolean ass){
        int indexOf = line.indexOf(":");
        if (indexOf !=-1){
            var str=line.substring(indexOf+1);
            if (str.isBlank()&&ass){
                throw new IllegalStateException(line.substring(0,indexOf)+"parameter is required");
            }
            return str;
        }
        if (ass){
            throw new IllegalStateException(line+" parameter is required");
        }
        return null;
    }

    private Map<String,String> parseAttr(String str){
        Map<String,String> attrMap=new HashMap<>();
//        String[] split = str.split(",");
//        for (String s : split) {
//            int idx = s.indexOf("=");
//            if (idx==-1){
//                throw new IllegalStateException("abnormal attr "+s);
//            }
//            attrMap.put(s.substring(0,idx),idx==s.length()-1?"":s.substring(idx+1));
//        }

        int start=0;
        char[] chars = str.toCharArray();
        boolean quote=false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c=='\"'){
                quote=!quote;
            }

            String kvPair=null;
            if (!quote&&c==','){
                kvPair = str.substring(start, i);
            }else if (i==chars.length-1&&start<str.length()){
                kvPair=str.substring(start);
            }

            if (kvPair!=null) {
                int idx = kvPair.indexOf("=");
                if (idx == -1) {
                    throw new IllegalStateException("abnormal attr " + kvPair);
                }
                attrMap.put(kvPair.substring(0, idx), kvPair.substring(idx + 1));
                start = i + 1;
            }
        }

        return attrMap;
    }

    private byte[] parseHex(String hex){
        if (hex==null)
            return null;
        if (hex.startsWith("0x")||hex.startsWith("0X")){
            hex=hex.substring(2);
        }
        return HexFormat.of().parseHex(hex);
    }
    private String parseQuotedString(String quotedStr){
        return quotedStr==null||quotedStr.length()==1?null:quotedStr.substring(1,quotedStr.length()-1);
    }



    public PlayList parse(Reader input) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(input)){
            String line ;
            while ((line=reader.readLine())!=null){
                if (DL.VIEW) {
                    DL.poutln(line);
                }
                if (line.isBlank()){
                    continue;
                }
                line=line.trim();
                if (!extm3u){
                    if (!line.startsWith("#EXTM3U")) {
                        throw new IllegalStateException("EXTM3U must be the first line of playlist file ");
                    }
                    extm3u=true;
                    continue;
                }

                if (!isComment(line)) {
                    //uri
                    if (this.parser==null){
                        throw new IllegalStateException("");
                    }
                    this.parser.parse(line);
                    continue;
                }
                if (this.parser==null) {
                    if (line.startsWith("#EXT-X-VERSION")) {
                        if (this.versionTag!=null){
                            throw new IllegalStateException("playlist file must not contain more than one EXT-X-VERSION tag");
                        }
                        this.versionTag=line;
                    } else if (isMediaTag(line)){
                        this.parser=new MediaParser();
                    } else if (isMasterTag(line)){
                        this.parser=new MasterParser();
                    }

                    if (this.parser!=null){
                        if (this.versionTag!=null){
                            this.parser.parse(versionTag);
                            this.versionTag=null;
                        }

                        if (this.parser.parse(line)){
                            break;
                        }
                    }
                }else{
                    if ((this.parser instanceof MediaParser && isMasterTag(line)) ||
                            (this.parser instanceof MasterParser && isMediaTag(line))){
                        throw new IllegalStateException("playlist must be either a media playlist or a master playlist");
                    }

                    if (this.parser.parse(line)){
                        break;
                    }
                }


            }

            System.out.flush();
            System.err.flush();


            return this.parser.getPlayListFile();
        }


    }


    private interface Parser{
        /**
         *
         * @param line
         * @return 有结果了就返回true
         */
        boolean parse(String line);

        PlayList getPlayListFile();

    }


    private class MediaParser implements Parser{
        private MediaList playListFile;

        private Segment previous;
        private Segment segment;

        public MediaParser() {
            this.playListFile=new MediaList();
        }

        private void parseXMapTag(String tag){
            String attributes = getTagValue(tag,true);
            assert attributes!=null;
            Map<String, String> attr = parseAttr(attributes);
            ByteRange range = null;
            String str = parseQuotedString(attr.get("BYTERANGE"));
            if (str!=null&&!str.isBlank()) {
                String[] byteRange = str.split("@");
                range = new ByteRange(byteRange.length <= 1 ? null : Integer.valueOf(byteRange[1]), Integer.valueOf(byteRange[0]));
            }
            this.playListFile.addElement(new InitInfo(parseQuotedString(attr.get("URI")),range));
        }

        @Override
        public boolean parse(String line) {
            if (isComment(line)) {
                if (line.startsWith("#EXTINF")){
                    if (this.segment!=null&&this.segment.getDuration() != null){
                        throw new IllegalStateException("");
                    }
                    if (this.segment==null){
                        this.segment=new Segment();
                    }
                    String value = getTagValue(line,true);
                    assert value!=null;
                    String[] infos = value.split(",");
                    this.segment.setDuration(Double.valueOf(infos[0]));
                    if (infos.length>1){
                        this.segment.setTitle(infos[1]);
                    }
                }else if (line.startsWith("#EXT-X-BYTERANGE")){
                    if (this.segment==null){
                        this.segment=new Segment();
                    }
                    String value = getTagValue(line, true);
                    assert value!=null;
                    String[] byteRange = value.split("@");
                    this.segment.setByteRange(new ByteRange(byteRange.length <=1?null:Integer.valueOf(byteRange[1]),Integer.valueOf(byteRange[0])));
                }else if (line.startsWith("#EXT-X-KEY")){
                    String attributes = getTagValue(line,true);
                    assert attributes!=null;
                    Map<String, String> attr = parseAttr(attributes);
                    String methodString = attr.get("METHOD");
                    String uri = parseQuotedString(attr.get("URI"));
                    byte[] iv = parseHex(attr.get("IV"));
                    String keyFormat = parseQuotedString(attr.get("KEYFORMAT"));
                    String keyFormatVersions = parseQuotedString(attr.get("KEYFORMATVERSIONS"));

                    if (methodString==null)
                        throw new IllegalStateException("EXT-X-KEY attribute 'method' is required");
                    EncryptMethod method = EncryptMethod.of(methodString);
                    if (uri==null&&method!=EncryptMethod.NONE){
                        throw new IllegalStateException("EXT-X-KEY attribute 'uri' is required unless unless the 'method' is none ");
                    }
                    EncryptInfo tag = new EncryptInfo(method,uri,iv,keyFormat,keyFormatVersions);
                    this.playListFile.addElement(tag);
                }else if (line.startsWith("#EXT-X-MAP")){
                    parseXMapTag(line);
                }else if (line.startsWith("#EXT-X-ENDLIST")){
                    this.playListFile.setEnd(true);
                    return true;
                } else if (line.startsWith("#EXT-X-VERSION")){
                    this.playListFile.setVersion(Integer.valueOf(Objects.requireNonNull(getTagValue(line))));
                }else if (line.startsWith("#EXT-X-TARGETDURATION")){
                    this.playListFile.setTargetDuration(Double.valueOf(Objects.requireNonNull(getTagValue(line))));
                }else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")){
                    this.playListFile.setMediaSequence(Long.parseLong(Objects.requireNonNull(getTagValue(line))));
                }else if (line.startsWith("#EXT-X-PLAYLIST-TYPE")){
                    this.playListFile.setType(PlayListType.valueOf(getTagValue(line,true).toUpperCase()));
                }else if(line.startsWith("#EXT-X-I-FRAMES-ONLY")){
                    this.playListFile.setiFramesOnly(true);
                }else {
                    if (DL.DEBUG)
                        System.err.println("ignore tag: " + line);
                }

            }else{
                //uri
                if (segment ==null||segment.getDuration()==null){
                    throw new IllegalStateException("");
                }
                if (this.segment.getByteRange()!=null){
                    if ((this.previous == null||this.previous.getByteRange()==null) && this.segment.getByteRange().offset() == null){
                        throw new IllegalStateException("");
                    }
                }
                this.segment.setUri(line);

                if (this.playListFile.getTargetDuration()!=null&& this.playListFile.getTargetDuration()<this.segment.getDuration()){
                    throw new IllegalStateException("duration of media segment must be <= target duration");
                }

                this.playListFile.addElement(this.segment);
                this.previous=this.segment;
                this.segment =null;
                return false;
            }

            return false;
        }

        @Override
        public PlayList getPlayListFile() {
            return playListFile;
        }
    }

    private class MasterParser implements Parser{

        private MasterList masterFile;


        private StreamInfo streamInfo;

        public MasterParser() {
            this.masterFile=new MasterList();
        }



        @Override
        public boolean parse(String line) {
            if (isComment(line)) {
                if (this.streamInfo!=null){
                    throw new IllegalStateException("");
                }
                if (line.startsWith("#EXT-X-STREAM-INF")){

                    String value = getTagValue(line, true);
                    Map<String, String> attr = parseAttr(value);
                    Integer bandwidth = Integer.valueOf(Objects.requireNonNull(attr.get("BANDWIDTH"),"EXT-X-STREAM-INF attribute 'BANDWIDTH' is required"));
                    String avgBandwidthStr = attr.get("AVERAGE-BANDWIDTH");
                    String codecs = parseQuotedString(attr.get("CODECS"));
                    String resolution = attr.get("RESOLUTION");
                    String frameRate = attr.get("FRAME-RATE");
                    String hdcpLevel = attr.get("HDCP-LEVEL");

                    this.streamInfo=new StreamInfo()
                            .setBandwidth(bandwidth)
                            .setAverageBandwidth(Strs.isBlank(avgBandwidthStr)?null:Integer.valueOf(avgBandwidthStr))
                            .setCodecs(Strs.isBlank(codecs)?List.of():List.of(codecs.split(",")))
                            .setResolution(resolution)
                            .setFramerate(Strs.isBlank(frameRate)?null:Double.valueOf(frameRate))
                            .setHdcpLevel(hdcpLevel)
                            .setAudio(attr.get("AUDIO"))
                            .setVideo(attr.get("VIDEO"))
                            .setSubTitles(attr.get("SUBTITLES"))
                            .setClosedCaptions(attr.get("CLOSED-CAPTIONS"))
                    ;
                }else if (line.startsWith("#EXT-X-SESSION-KEY")){
                    String attributes = getTagValue(line,true);
                    assert attributes!=null;
                    Map<String, String> attr = parseAttr(attributes);
                    String methodString = attr.get("METHOD");
                    String uri = parseQuotedString(attr.get("URI"));
                    byte[] iv = parseHex(attr.get("IV"));
                    String keyFormat = parseQuotedString(attr.get("KEYFORMAT"));
                    String keyFormatVersions = parseQuotedString(attr.get("KEYFORMATVERSIONS"));

                    if (methodString==null)
                        throw new IllegalStateException("EXT-X-KEY attribute 'method' is required");
                    EncryptMethod method = EncryptMethod.of(methodString);
                    if (method==EncryptMethod.NONE){
                        throw new IllegalStateException("master file EXT-X-KEY attribute 'method' must not be null");
                    }
                    if (uri==null){
                        throw new IllegalStateException("master file EXT-X-KEY attribute 'uri' is required");
                    }
                    EncryptInfo tag = new EncryptInfo(method,uri,iv,keyFormat,keyFormatVersions);
                    this.masterFile.addEncryptInfo(tag);
                }
            }else{
                if (this.streamInfo==null){
                    throw new IllegalStateException();
                }
                this.streamInfo.setUri(line);
                this.masterFile.addStream(this.streamInfo);
                this.streamInfo=null;
            }

            return false;
        }

        @Override
        public PlayList getPlayListFile() {
            return masterFile;
        }
    }

}
