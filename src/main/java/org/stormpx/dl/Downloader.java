package org.stormpx.dl;

import org.stormpx.dl.kit.*;
import org.stormpx.dl.m3u8.EncryptInfo;
import org.stormpx.dl.m3u8.EncryptMethod;
import org.stormpx.dl.m3u8.M3u8Parser;
import org.stormpx.dl.m3u8.PlayListFile;
import org.stormpx.dl.m3u8.play.Segment;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
    private int retry=0;
    private boolean reload =false;
    private boolean merge=false;
    private int maximumSegment =Integer.MAX_VALUE;
    private DLCiphers ciphers;

    public Downloader(URI baseUri, Path workPath,Executor executor) {
        this.ciphers=new DLCiphers();
        this.baseUri = baseUri;
        this.workPath = workPath;
        this.executor=executor;
    }

    public Downloader setReload(boolean reload) {
        this.reload = reload;
        return this;
    }

    public Downloader setMerge(boolean merge) {
        this.merge = merge;
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

//        reqResult.getInputStream().transferTo(new FileOutputStream(workPath.resolve(dirName+".m3u8").toFile()));
        DL.poutln("uri: "+uri);
        download(base,dirName,()->{
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
        File file = path.toFile();
        if (!file.exists()||!file.isFile()){
            DL.perr(path + " not exists.");
        }
        String dirName=Strs.removeExt(path.getFileName().toString());
        DL.poutln("filePath: "+ path);

        download(baseUri,dirName,()->{
            FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
            return new M3u8Parser().parse(reader);
        });
    }

    private void download(URI baseUri, String dirName, PlayFileProvider playFileProvider) throws Throwable {
        if (this.baseUri!=null)
            baseUri=this.baseUri;
        if (dirName.length()>255)
            dirName=dirName.substring(0,255);
        Context context = new Context(baseUri, dirName,null, playFileProvider.getFile());

        Path downloadPath = context.getPath(workPath);
        File file = downloadPath.toFile();
        if (!file.mkdirs()){
            if (!file.exists()){
                throw new IOException("mkdirs "+file+" failed");
            }
        }

        DL.poutln("workDir: "+downloadPath);
        int readSlices=0;
        List<Context.Entry> entries=new ArrayList<>();
        List<CompletableFuture<Void>> futures=new ArrayList<>();

        do {
            long now=System.currentTimeMillis();
            URI finalBaseUri = baseUri;

            readSlices+=context.read(this.maximumSegment-readSlices, entry -> {
                try {
                    if (entry instanceof Context.MediaEntry media) {
//                        System.out.println();
//                        URI uri = media.getUri();
                        Path filePath = media.getPath(downloadPath);

                        ProgressBar progressBar = new ProgressBar( 50, filePath.getFileName() + ": ");
                        progressBar.setMessage("request...");
//                        DL.GROUP.clear();
                        DL.GROUP.addBar(progressBar);
//                        DL.GROUP.show(false);
                        DL.GROUP.report();
                        MediaDownload mediaDownload = new MediaDownload(finalBaseUri, filePath, media, progressBar);

                        var future=CompletableFuture.runAsync(Lambdas.rethrowRunnable(()->mediaDownload.start(0)),executor);

                        futures.add(future);
                        entries.add(media);

                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(),e);
                }
            });

            tryThrow(futures);

            if (context.shouldReload()&&this.reload &&readSlices< maximumSegment){
                PlayListFile playListFile = context.getPlayListFile();
                long sleepMillis=((long) (playListFile.getTargetDuration()*1000)-(System.currentTimeMillis()-now));
                if (sleepMillis>0) {
                    sleepWithShowProgress(sleepMillis);
                }
                boolean change=context.append(playFileProvider.getFile());
                while (!change){
                    sleepWithShowProgress(playListFile.getTargetDuration()*1000/2);
                    change=context.append(playFileProvider.getFile());
                }
            }

            tryThrow(futures);

        }while (context.shouldReload()&&this.reload &&readSlices< maximumSegment);

        tryThrow(futures);

        DL.GROUP.reportAwait();
        System.out.println();
        if (this.merge)
            merge(entries,downloadPath);

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

    private void sleepWithShowProgress(long millis) throws InterruptedException {
        long delay=System.currentTimeMillis()+millis;
//        DL.GROUP.clear();
//        DL.GROUP.show(false);
        while (System.currentTimeMillis()<delay){
            DL.GROUP.report();
            Thread.sleep(200);
        }

    }

    private void merge(List<Context.Entry> entries,Path dirPath) throws IOException {
        File allInOneFile=dirPath.getParent().resolve(dirPath.getFileName()+".ts").toFile();
        byte[] buffer=new byte[16*1024];
        try (OutputStream out = new FileOutputStream(allInOneFile)){
            DL.poutln("segment merging..");
            for (Context.Entry entry : entries) {
                File file = entry.getPath(dirPath).toFile();
                try (InputStream in=new FileInputStream(file)){
                    int dataRead=-1;
                    while ((dataRead=in.read(buffer))!=-1) {
                        out.write(buffer,0,dataRead);
                    }
                }
            }
            DL.poutln("merge done..");
        }


    }

    private interface PlayFileProvider{

        PlayListFile getFile() throws IOException;

    }

    private class MediaDownload {
        private URI baseUri;
        private Path filePath;
        private Context.MediaEntry media;
        private ProgressBar progressBar;

        public MediaDownload(URI baseUri, Path downloadPath, Context.MediaEntry media, ProgressBar progressBar) {
            this.baseUri = baseUri;
            this.filePath = downloadPath;
            this.media = media;
            this.progressBar = progressBar;
        }

        public void start(int num) throws IOException {
            try {
                ReqResult reqResult = Http.request(media.getUri(), media.getByteRange());
                if (!reqResult.isSuccess()) {
                    throw new RuntimeException("request ts failed...");
                }
                if (reqResult.isM3u8File()) {
                    progressBar.failed(".m3u8 file detected. pass... ");
                    return;
                }
                progressBar.setMessage(null);
                Integer contentLength = reqResult.getContentLength();
                InputStream inputStream = reqResult.getInputStream();
                File targetFile = filePath.toFile();

                EncryptInfo encryptInfo = media.getEncryptInfo();
                if (encryptInfo !=null){
                    if (encryptInfo.getMethod() != EncryptMethod.NONE){
                        Cipher cipher = ciphers.getCipher(this.baseUri, ((Segment) media.getElement()).getSequence(), encryptInfo);
                        inputStream=new CipherInputStream(inputStream,cipher);
                    }
                }

                progressBar.setTotal(contentLength);

                byte[] buffer = DLThreadContext.current().getBuffer();
                try (InputStream in=inputStream;
                     FileOutputStream out = new FileOutputStream(targetFile, false)){

                    int dataRead=-1;
                    while ((dataRead= in.read(buffer))!=-1){
                        out.write(buffer,0,dataRead);
                        out.flush();
                        progressBar.stepBy(dataRead);
                    }
                    if (!progressBar.isDone()){
                        if (progressBar.getTotal()- progressBar.getCurrent()<16)
                            progressBar.complete();
                    }
                }
            } catch (IOException e) {
                if (num<0){
                    progressBar.setMessage(String.format("retry %d/%d",num+1,retry));
                    start(num+1);
                }else{
                    progressBar.failed("download failed...");
                    throw new RuntimeException(e.getMessage(),e);
                }

            }
        }

    }


}
