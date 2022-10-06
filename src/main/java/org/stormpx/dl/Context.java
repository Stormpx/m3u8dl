package org.stormpx.dl;

import org.stormpx.dl.m3u8.EncryptInfo;
import org.stormpx.dl.m3u8.PlayListElement;
import org.stormpx.dl.m3u8.play.MediaList;
import org.stormpx.dl.kit.ByteRange;
import org.stormpx.dl.m3u8.PlayListType;
import org.stormpx.dl.m3u8.play.InitInfo;
import org.stormpx.dl.m3u8.play.Segment;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

public class Context {

    private URI baseUri;

    private String dirName;

    private Context parent;

    private MediaList playListFile;

    private Queue<PlayListElement> playList;

    private EncryptInfo encryptInfo =null;

    private InitInfo initInfo=null;

    private MediaEntry prev=null;

    public Context(URI baseUri, String dirName, Context parent, MediaList playListFile) {
        Objects.requireNonNull(baseUri);
        this.baseUri = baseUri;
        this.dirName = dirName;
        this.parent = parent;
        this.playList=new LinkedList<>();
        append(playListFile);
    }


    public boolean shouldReload(){
        return !playListFile.isEnd()&&playListFile.getType()!= PlayListType.VOD;
    }

    public MediaList getPlayListFile() {
        return playListFile;
    }

    public boolean append(MediaList file){
        long maxSequence =-1;
        if (this.playListFile!=null){
            if (this.playListFile.getMediaSequence()>file.getMediaSequence()){
                return false;
            }
            maxSequence = this.playListFile.getMediaSequence() + this.playListFile.getSegmentSize()-1;
        }
        boolean append=false;
        for (PlayListElement element : file.getElements()) {
            if (element instanceof Segment seq){
                if (seq.getSequence()>maxSequence){
                    this.playList.add(seq);
                    append=true;
                }
            }else{
                this.playList.add(element);
            }
        }

        this.playListFile=file;
        return append;
    }



    public int read(int maxSlices,Consumer<MediaEntry> segmentHandler){
        if (maxSlices<=0)
            return 0;
        int slices=0;

        while (!playList.isEmpty()&&slices<maxSlices){
            PlayListElement element = playList.poll();
            if (element instanceof Segment seg){
                MediaEntry entry = new MediaEntry(encryptInfo, seg, prev);
                segmentHandler.accept(entry);
                prev=entry;
                slices++;
            }else if (element instanceof EncryptInfo encrypt){
                encryptInfo =encrypt;
            }else if (element instanceof InitInfo map){
                if (!Objects.equals(initInfo,map)){
                    initInfo=map;
                    segmentHandler.accept(new MediaEntry(encryptInfo,map));
                }

            }
        }
        return slices;
    }


    public interface Entry{

        String getName();

        URI getUri();

        Path getPath(Path workPath);

        EncryptInfo getEncryptInfo();

        PlayListElement getElement();
    }

    public class MediaEntry implements Entry {
        private final EncryptInfo encryptInfo;
        private final PlayListElement element;

        private String name;
        private URI uri;
        private ByteRange byteRange;


        public MediaEntry(EncryptInfo encryptInfo,Segment segment, MediaEntry prevEntry) {
            this.encryptInfo = encryptInfo;
            this.element=segment;
            if (segment.getByteRange()!=null){
                if (segment.getByteRange().offset()!=null||prevEntry==null||  prevEntry.getByteRange() ==null){
                    this.byteRange=segment.getByteRange();
                }else {
                    ByteRange prevSegByteRange = prevEntry.getByteRange();
                    this.byteRange = new ByteRange(prevSegByteRange.offset() + prevSegByteRange.size(), segment.getByteRange().size());
                }
            }
            this.name=segment.getTitle();
            this.uri=baseUri.resolve(segment.getUri());
        }

        public MediaEntry(EncryptInfo encryptInfo,InitInfo initInfo){
            this.encryptInfo = encryptInfo;
            this.element=initInfo;
            this.uri=baseUri.resolve(initInfo.getUri());
            this.byteRange=initInfo.getByteRange();
        }

        public boolean isSegment() {
            return element instanceof Segment;
        }

        public String getName() {
            if (this.name!=null){
                return this.name;
            }
            return baseUri.relativize(getUri()).getPath();
        }


        public URI getUri() {
            //return or resolve?
            if (this.uri.isAbsolute()){
                return uri;
            }
            return baseUri.resolve(this.uri);
        }

        public Path getPath(Path path){
            URI uri = getUri();
            String name="";
            if (uri.isAbsolute()){
                name= uri.resolve("").relativize(uri).getPath();
            }else {
                name = baseUri.relativize(uri).getPath();
            }
            return path.resolve(!isSegment()?name:((Segment)element).getSequence()+"_"+name);
        }

        public ByteRange getByteRange(){
            return this.byteRange;
        }


        public EncryptInfo getEncryptInfo() {
            return encryptInfo;
        }

        @Override
        public PlayListElement getElement() {
            return element;
        }


    }

}

