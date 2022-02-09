package org.stormpx.dl.m3u8;

import org.stormpx.dl.kit.ByteRange;
import org.stormpx.dl.kit.DL;
import org.stormpx.dl.m3u8.play.InitInfo;
import org.stormpx.dl.m3u8.play.Segment;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Consumer;

public class M3u8Parser {

    private Map<String, Consumer<String>> tagHandlers=Map.of(
            "#EXTM3U",this::m3u
    );


    //0:start 1:init 2:list
    private int state=0;

    private PlayListFile playListFile;

    private List<PlayListElement> elements=new ArrayList<>();

    private Segment previous;
    private Segment segment;

    private boolean isStart(){
        return state==0;
    }


    private boolean isInit(){
        return state==1;
    }

    private boolean isList(){
        return state==2;
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
        String[] split = str.split(",");
        Map<String,String> attrMap=new HashMap<>();
        for (String s : split) {
            int idx = s.indexOf("=");
            if (idx==-1){
                throw new IllegalStateException("abnormal attr "+s);
            }
            attrMap.put(s.substring(0,idx),idx==s.length()-1?"":s.substring(idx+1));
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

    private void addXMapTag(String tag){
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

    public PlayListFile parse(Reader input) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(input)){
            String line ;
            if (DL.VIEW){
                System.out.println();
            }
            while ((line=reader.readLine())!=null){
                if (DL.VIEW) {
                    System.out.println(line);
                }
                if (line.isBlank()){
                    continue;
                }
                line=line.trim();
                if (isStart()){

                    if (line.startsWith("#EXTM3U")){
                        this.playListFile=new PlayListFile();
                        state=1;
                        continue;
                    }else {
                        throw new IllegalStateException("#EXTM3U must be the first line of playlist file ");
                    }
                }
                if (isInit()){
                    if (line.startsWith("#EXT-X-VERSION")){
                        this.playListFile.setVersion(Integer.valueOf(Objects.requireNonNull(getTagValue(line))));
                        continue;
                    }else if (line.startsWith("#EXT-X-TARGETDURATION")){
                        this.playListFile.setTargetDuration(Integer.valueOf(Objects.requireNonNull(getTagValue(line))));
                        continue;
                    }else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")){
                        this.playListFile.setMediaSequence(Long.valueOf(Objects.requireNonNull(getTagValue(line))));
                        continue;
                    }else if (line.startsWith("#EXT-X-PLAYLIST-TYPE")){
                        this.playListFile.setType(PlayListType.valueOf(getTagValue(line,true).toUpperCase()));
                        continue;
                    }else if(line.startsWith("#EXT-X-I-FRAMES-ONLY")){
                        this.playListFile.setiFramesOnly(true);
                        continue;
                    }else if (line.startsWith("#EXTINF")||line.startsWith("#EXT-X-BYTERANGE")||line.startsWith("#EXT-X-MAP")||line.startsWith("#EXT-X-KEY")){
                        state=2;
                    }
                }
                if (isList()){
                    if (line.startsWith("#")){
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
                            continue;
                        }else if (line.startsWith("#EXT-X-BYTERANGE")){
                            if (this.segment==null){
                                this.segment=new Segment();
                            }
                            String value = getTagValue(line, true);
                            assert value!=null;
                            String[] byteRange = value.split("@");
                            this.segment.setByteRange(new ByteRange(byteRange.length <=1?null:Integer.valueOf(byteRange[1]),Integer.valueOf(byteRange[0])));
                            continue;
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
                            continue;
                        }else if (line.startsWith("#EXT-X-MAP")){
                            addXMapTag(line);
                            continue;
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

                        if (this.playListFile.getTargetDuration()!=null&& this.playListFile.getTargetDuration()<Math.round(this.segment.getDuration())){
                            throw new IllegalStateException("duration of media segment must be <= target duration");
                        }

                        this.playListFile.addElement(this.segment);
                        this.previous=this.segment;
                        this.segment =null;
                        continue;
                    }
                }

                if(line.startsWith("#EXT-X-ENDLIST")){
                    this.playListFile.setEndTag(true);
                    break;
                }else{
                    if (DL.DEBUG) {
                        System.err.println("ignore tag: " + line);
                    }
                }
            }

//            this.playListFile.setElements(elements);
            System.out.flush();
            System.err.flush();

            if (playListFile.getTargetDuration()==null){
                throw new IllegalStateException("the EXT-X-TARGETDURATION tag is REQUIRED");
            }

            return playListFile;
        }


    }


    private void m3u(String tag){

    }

}
