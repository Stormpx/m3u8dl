package org.stormpx.dl;

import org.stormpx.dl.kit.Strs;
import org.stormpx.dl.m3u8.EncryptInfo;
import org.stormpx.dl.m3u8.PlayListElement;
import org.stormpx.dl.m3u8.PlayListFile;
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

    private URI uri;

    private URI baseUri;

    private String dirName;

    private Context parent;

    private PlayListFile playListFile;

    private Queue<PlayListElement> playList;

    public Context(URI baseUri, String dirName, Context parent, PlayListFile playListFile) {
        Objects.requireNonNull(baseUri);
        this.baseUri = baseUri;
        this.dirName = dirName;
        this.parent = parent;
        this.playList=new LinkedList<>();
        append(playListFile);
    }

    public Context(URI uri, Context parent, PlayListFile playListFile) {
        Objects.requireNonNull(uri);
        this.uri = uri;
        this.baseUri = uri.resolve("");
        this.parent = parent;
        this.playList=new LinkedList<>();
        append(playListFile);
    }

    public boolean shouldReload(){
        return !playListFile.isEndTag()&&playListFile.getType()!= PlayListType.VOD;
    }

    public PlayListFile getPlayListFile() {
        return playListFile;
    }

    public boolean append(PlayListFile file){
        long maxSequence =-1;
        if (this.playListFile!=null){
            if (this.playListFile.getMediaSequence()>file.getMediaSequence()){
                return false;
            }
            maxSequence = this.playListFile.getMediaSequence() + this.playListFile.getSegmentSize()-1;
        }
        boolean change=false;
        for (PlayListElement element : file.getElements()) {
            if (element instanceof Segment seq){
                if (seq.getSequence()>maxSequence){
                    this.playList.add(seq);
                    change=true;
                }
            }else{
                this.playList.add(element);
            }
        }

        this.playListFile=file;
        return change;
    }

    public Path getPath(Path workPath){
        Objects.requireNonNull(workPath);
        String dirName=this.dirName;
        if (dirName==null) {
            if (this.uri!=null) {
                URI uri = baseUri.relativize(this.uri);
                dirName = Strs.removeExt(uri.getPath());

            }
            if (dirName == null || dirName.isBlank()) {
                dirName = UUID.randomUUID().toString().replaceAll("-", "");
            }
        }
        return workPath.resolve(dirName);
    }


    public int read(int maxSlices,Consumer<Entry> segmentHandler){
        int slices=0;
        EncryptInfo encryptInfo =null;
        MediaEntry prev=null;
        while (!playList.isEmpty()&&slices<maxSlices){
            PlayListElement element = playList.poll();
            if (element instanceof Segment seg){
                segmentHandler.accept(new MediaEntry(encryptInfo,seg,prev));
                slices++;
            }else if (element instanceof EncryptInfo encrypt){
                encryptInfo =encrypt;
            }else if (element instanceof InitInfo map){
                segmentHandler.accept(new MediaEntry(encryptInfo,map));
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

        //false: init true: segment
        private boolean segment;

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
            this.segment=true;
        }

        public MediaEntry(EncryptInfo encryptInfo,InitInfo initInfo){
            this.encryptInfo = encryptInfo;
            this.element=initInfo;
            this.uri=baseUri.resolve(initInfo.getUri());
            this.byteRange=initInfo.getByteRange();
            this.segment=false;

        }

        public boolean isSegment() {
            return segment;
        }

        public String getName() {
            if (this.name!=null){
                return this.name;
            }
            return baseUri.relativize(getUri()).getPath();
        }


        public URI getUri() {
            return baseUri.resolve(this.uri);
        }

        public Path getPath(Path path){
            String name = baseUri.relativize(getUri()).getPath();
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

