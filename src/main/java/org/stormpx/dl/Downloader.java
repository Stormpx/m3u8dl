package org.stormpx.dl;

import org.stormpx.dl.kit.*;
import org.stormpx.dl.m3u8.EncryptInfo;
import org.stormpx.dl.m3u8.EncryptMethod;
import org.stormpx.dl.m3u8.M3u8Parser;
import org.stormpx.dl.m3u8.PlayList;
import org.stormpx.dl.m3u8.master.MasterList;
import org.stormpx.dl.m3u8.master.StreamInfo;
import org.stormpx.dl.m3u8.play.MediaList;
import org.stormpx.dl.m3u8.play.Segment;

import javax.crypto.CipherInputStream;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class Downloader {

    private Executor executor;
    private URI baseUri;
    private Path workPath;
    private int retry=10;
    private boolean reload =false;
    private boolean concat =false;
    private int maximumSegment =Integer.MAX_VALUE;
    private DLCiphers ciphers;

    public Downloader(URI baseUri, Path workPath,Executor executor) {
        this.ciphers=new DLCiphers();
        this.baseUri = baseUri;
        this.workPath = workPath;
        this.executor=executor;
    }

    public Downloader setRetry(int retry) {
        this.retry = retry;
        return this;
    }

    public Downloader setReload(boolean reload) {
        this.reload = reload;
        return this;
    }

    public Downloader setConcat(boolean concat) {
        this.concat = concat;
        return this;
    }

    public Downloader setMaximumSegment(int maximumSegment) {
        this.maximumSegment = maximumSegment;
        return this;
    }

    public void download(String target) throws Throwable {
        boolean http=false;
        URI uri=null;
        try {
            uri = URI.create(target);
            http=uri.isAbsolute()&&uri.getScheme().startsWith("http");
        } catch (Exception e) {
        }
        if (http){
            downloadByUri(uri);
        }else{
            downloadByFile(Path.of(target));
        }

    }

    public void downloadByUri(URI uri) throws Throwable {


        URI base=uri.resolve("");
        URI relativeUri = base.relativize(uri);
        String dirName = Strs.removeExt(relativeUri.toString());

        DL.poutln("uri: "+uri);
        download(this.baseUri!=null?this.baseUri:base,null,dirName,()->{
            ReqResult reqResult = Http.request(uri,null);
            if (!reqResult.isSuccess()){
                throw new IOException(String.format("req uri: %s failed",uri));
            }
            if (!reqResult.isM3u8File()){
                throw new IllegalStateException(String.format("'%s' is not a m3u8 file", uri));
            }
            return new M3u8Parser().parse(new InputStreamReader(reqResult.getInputStream()));
        });

    }


    public void downloadByFile(Path path) throws Throwable{
        Objects.requireNonNull(this.baseUri,"baseUri is required");
        if (!Files.exists(path)){
            DL.perr(path + " is not exists.");
            return;
        }
        if (!Files.isRegularFile(path)){
            DL.perr(path + " is not regular file.");
            return;
        }
        String dirName=Strs.removeExt(path.getFileName().toString());
        DL.poutln("filePath: "+ path);

        download(baseUri,null,dirName,()->{
            return new M3u8Parser().parse(Files.newBufferedReader(path));
        });
    }

    private void download(URI baseUri,Path workPath, String dirName, PlayListProvider playListProvider) throws Throwable {
        if (baseUri==null)
            baseUri=this.baseUri;
        if (dirName.length()>128)
            dirName=dirName.substring(0,128);

        if (workPath==null)
            workPath=this.workPath;

        Path downloadPath = workPath.resolve(dirName);
        DL.createDir(downloadPath);

        PlayList playList = playListProvider.getFile();
        if (!playList.isMediaFile()){
            DL.poutln("master list file detected..");
            //master
            MasterList masterList= (MasterList) playList;

            List<StreamInfo> streams = masterList.getStreams();
            for (int i = 0; i < streams.size(); i++) {
                StreamInfo stream = streams.get(i);
                if (Strs.isBlank(stream.getUri())) {
                    continue;
                }
                URI uri = URI.create(stream.getUri());
                if (!uri.isAbsolute()) {
                    uri = baseUri.resolve(uri);
                }
                URI base = uri.resolve("");
                String dir = Strs.removeExt(base.relativize(uri).getPath());

                URI finalUri = uri;
                //download stream
                DL.poutln("try download variant stream: " + uri);
                download(base, downloadPath, i+"_"+dir, () -> {
                    ReqResult reqResult = Http.request(finalUri, null);
                    if (!reqResult.isSuccess()) {
                        throw new IOException(String.format("req uri: %s failed", finalUri));
                    }
                    if (!reqResult.isM3u8File()) {
                        throw new IllegalStateException(String.format("'%s' is not a m3u8 file", finalUri));
                    }
                    return new M3u8Parser().parse(new InputStreamReader(reqResult.getInputStream()));
                });

            }

        } else{

            //media
            Context context = new Context(baseUri, dirName,null, (MediaList) playList);

            if (context.shouldReload()){
                DL.poutln("live streaming file detected..");
            }

            int readSlices=0;
            List<Context.Entry> entries=new ArrayList<>();
            List<CompletableFuture<Void>> futures=new ArrayList<>();
            ProgressGroup group=new ProgressGroup();

            do {
                long now=System.currentTimeMillis();
                URI finalBaseUri = baseUri;

                readSlices+=context.read(this.maximumSegment-readSlices, media -> {
                    Path filePath = media.getPath(downloadPath);

                    ProgressBar progressBar = new ProgressBar( 50, filePath.getFileName() + "");
                    progressBar.setMessage("request...");
                    group.addBar(progressBar);
                    group.report();
                    MediaDownloader mediaDownload = new MediaDownloader(finalBaseUri, filePath, media, progressBar);

                    var future=mediaDownload.start(0);

                    futures.add(future);
                    entries.add(media);
                });

                tryThrow(futures);

                if (context.shouldReload()&&this.reload &&readSlices< maximumSegment){
                    MediaList mediaList = context.getPlayListFile();
                    long sleepMillis=((long) (mediaList.getTargetDuration()*1000)-(System.currentTimeMillis()-now));
                    if (sleepMillis>0) {
                        sleepAndShowProgress(group,sleepMillis);
                    }
                    boolean change=context.append((MediaList) playListProvider.getFile());
                    while (!change){
                        sleepAndShowProgress(group,mediaList.getTargetDuration()*1000/2);
                        change=context.append((MediaList) playListProvider.getFile());
                    }
                }

                tryThrow(futures);

            }while (context.shouldReload()&&this.reload &&readSlices< maximumSegment);

            tryThrow(futures);

            group.reportAwait();

            tryThrow(futures);

            System.out.println();
            if (this.concat)
                concat(entries,downloadPath);
        }


    }

    private void tryThrow(List<CompletableFuture<Void>> futures) throws Exception {
        Iterator<CompletableFuture<Void>> iterator = futures.iterator();
        while (iterator.hasNext()){
            CompletableFuture<Void> future = iterator.next();
            if (future.isDone()){
                //throw
                future.get(3, TimeUnit.SECONDS);
                iterator.remove();
            }
        }
    }


    private void sleepAndShowProgress(ProgressGroup group,long millis) throws InterruptedException {
        long delay=System.currentTimeMillis()+millis;
        while (System.currentTimeMillis()<delay){
            group.report();
            Thread.sleep(200);
        }

    }

    private void concat(List<Context.Entry> entries, Path dirPath) throws IOException {
        Path tsPath = dirPath.getParent().resolve(dirPath.getFileName() + ".ts");
        File allInOneFile= tsPath.toFile();
        byte[] buffer=new byte[16*1024];
        try (OutputStream out = new FileOutputStream(allInOneFile)){
            DL.poutln("try concat & remuxing");
            for (Context.Entry entry : entries) {
                File file = entry.getPath(dirPath).toFile();
                try (InputStream in=new FileInputStream(file)){
                    int dataRead=-1;
                    while ((dataRead=in.read(buffer))!=-1) {
                        out.write(buffer,0,dataRead);
                    }
                }
            }
            DL.poutln("concat done..");
        }

        if (DL.remuxing(tsPath, dirPath.getParent().resolve(dirPath.getFileName()+".mp4"))){
            System.out.println("remuxing success..");
            allInOneFile.delete();
        }

    }

    private interface PlayListProvider {

        PlayList getFile() throws IOException;

    }

    private class MediaDownloader {
        private URI baseUri;
        private Path filePath;
        private Context.MediaEntry media;
        private ProgressBar progressBar;

        public MediaDownloader(URI baseUri, Path downloadPath, Context.MediaEntry media, ProgressBar progressBar) {
            this.baseUri = baseUri;
            this.filePath = downloadPath;
            this.media = media;
            this.progressBar = progressBar;
        }

        public CompletableFuture<Void> start(int num)  {
            return Http.requestAsync(media.getUri(), media.getByteRange())
                    .thenCompose(reqResult -> {
                            if (!reqResult.isSuccess()) {
                                return CompletableFuture.failedFuture(new RuntimeException("request ts failed..."));
                            }
                            if (reqResult.isM3u8File()) {
                                progressBar.failed(".m3u8 file detected. pass... ");
                                return CompletableFuture.completedFuture(null);
                            }
                            progressBar.setMessage(null);
                            Integer contentLength = reqResult.getContentLength();
                            InputStream inputStream = reqResult.getInputStream();

                            progressBar.setTotal(contentLength);

                            EncryptInfo encryptInfo = media.getEncryptInfo();
                            if (encryptInfo !=null){
                                if (encryptInfo.getMethod() != EncryptMethod.NONE){
                                    return ciphers.createCipherAsync(this.baseUri, ((Segment) media.getElement()).getSequence(), encryptInfo)
                                            .thenCompose(cipher -> write2FileFuture(new CipherInputStream(inputStream,cipher),filePath));
                                }
                            }

                            return write2FileFuture(inputStream,filePath);

                    })
                    .exceptionallyCompose(t->{
                        if (num<retry){
                            progressBar.setMessage(String.format("retry %d/%d",num+1,retry));
                            return start(num+1);
                        }else{
                            progressBar.failed("download failed...");
                            return CompletableFuture.failedFuture(t);
                        }
                    });

        }

        private CompletableFuture<Void> write2FileFuture(InputStream inputStream, Path targetFile){
            try {
                write2File(inputStream,targetFile);
                return CompletableFuture.completedFuture(null);
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        private void write2File(InputStream inputStream, Path targetFile) throws IOException {
            byte[] buffer = DLThreadContext.current().getBuffer();
            try (InputStream in=inputStream;
                 OutputStream out=Files.newOutputStream(targetFile, StandardOpenOption.CREATE)){

                int dataRead;
                while ((dataRead= in.read(buffer))!=-1){
                    out.write(buffer,0,dataRead);
                    out.flush();
                    progressBar.stepBy(dataRead);
                }

                if (!progressBar.isDone()){
                    if (progressBar.getTotal()- progressBar.getCurrent()<=16)
                        progressBar.complete();
                }
            }
        }

    }


}