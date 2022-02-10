package org.stormpx.dl;

import org.stormpx.dl.kit.Http;
import org.stormpx.dl.kit.ProgressBar;
import org.stormpx.dl.kit.ReqResult;
import org.stormpx.dl.kit.Strs;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class Downloader {

    private URI baseUri;
    private Path workPath;
    private boolean reload =false;
    private boolean merge=false;
    private int maximumSegment =Integer.MAX_VALUE;
    private DLCiphers ciphers;

    public Downloader(URI baseUri, Path workPath) {
        this.ciphers=new DLCiphers();
        this.baseUri = baseUri;
        this.workPath = workPath;
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

    public void download(String target) throws IOException, InterruptedException {
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
            downloadByFile(Path.of(target).toFile());
        }

    }

    public void downloadByUri(URI uri) throws IOException, InterruptedException {


        URI base=uri.resolve("");
        URI relativeUri = base.relativize(uri);
        String dirName = Strs.removeExt(relativeUri.toString());

//        reqResult.getInputStream().transferTo(new FileOutputStream(workPath.resolve(dirName+".m3u8").toFile()));

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


    public void downloadByFile(File file) throws IOException, InterruptedException {
        Objects.requireNonNull(this.baseUri,"baseUri is required");

        String dirName=Strs.removeExt(file.getName());

        download(baseUri,dirName,()->{
            FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
            return new M3u8Parser().parse(reader);
        });
    }

    private void download(URI baseUri, String dirName, PlayFileProvider playFileProvider) throws IOException, InterruptedException {
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

        int readSlices=0;
        List<Context.Entry> entries=new ArrayList<>();
        do {
            long now=System.currentTimeMillis();
            URI finalBaseUri = baseUri;
            readSlices+=context.read(this.maximumSegment, entry -> {
                try {
                    if (entry instanceof Context.MediaEntry media) {
                        System.out.println();
                        URI uri = media.getUri();
                        Path filePath = media.getPath(downloadPath);

//                        System.out.println(media.getName());
//                        System.out.println(media.getUri());
//                        System.out.println(filePath);
//                        System.out.println(media.getByteRange());
//                        System.out.println(media.getElement());

                        ReqResult reqResult = Http.request(uri, media.getByteRange());
                        if (!reqResult.isSuccess()) {
                            System.err.printf("req %s failed%n", uri);
                            return;
                        }
                        if (reqResult.isM3u8File()) {
                            System.err.println(".m3u8 file detected pass.");
                            return;
                        }

                        Integer contentLength = reqResult.getContentLength();
                        InputStream inputStream = reqResult.getInputStream();
                        File targetFile = filePath.toFile();

                        EncryptInfo encryptInfo = entry.getEncryptInfo();
                        if (encryptInfo !=null){
                            if (encryptInfo.getMethod() != EncryptMethod.NONE){
                                Cipher cipher = ciphers.getCipher(finalBaseUri, ((Segment) media.getElement()).getSequence(), encryptInfo);
                                inputStream=new CipherInputStream(inputStream,cipher);
                            }
                        }

                        ProgressBar progressBar = new ProgressBar(contentLength, 50, targetFile.getName() + ": ");

                        byte[] buffer=new byte[16*1024];
                        try (InputStream in=inputStream;
                             FileOutputStream out = new FileOutputStream(targetFile, false)){

                            int dataRead=-1;
                            while ((dataRead= in.read(buffer))!=-1){
                                out.write(buffer,0,dataRead);
                                out.flush();
                                progressBar.stepBy(dataRead).printf();
                            }
                            if (!progressBar.isDone()){
                                if (progressBar.getTotal()- progressBar.getCurrent()<16)
                                    progressBar.complete().printf();
                            }

                        }

                        entries.add(media);

                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (context.shouldReload()&&this.reload &&readSlices< maximumSegment){
                PlayListFile playListFile = context.getPlayListFile();
                long sleepMillis=((long) (playListFile.getTargetDuration()*1000)-(System.currentTimeMillis()-now));
                if (sleepMillis>0)
                    Thread.sleep(sleepMillis);
                boolean change=context.append(playFileProvider.getFile());
                while (!change){
                    Thread.sleep(playListFile.getTargetDuration()*1000/2);
                    change=context.append(playFileProvider.getFile());
                }
            }


        }while (context.shouldReload()&&this.reload &&readSlices< maximumSegment);

        System.out.println();
        if (this.merge)
            merge(entries,downloadPath);


    }

    private void merge(List<Context.Entry> entries,Path dirPath) throws IOException {
        File allInOneFile=dirPath.getParent().resolve(dirPath.getFileName()+".ts").toFile();
        byte[] buffer=new byte[16*1024];
        try (OutputStream out = new FileOutputStream(allInOneFile)){
            System.out.println("segment merging..");
            for (Context.Entry entry : entries) {
                File file = entry.getPath(dirPath).toFile();
                try (InputStream in=new FileInputStream(file)){
                    int dataRead=-1;
                    while ((dataRead=in.read(buffer))!=-1) {
                        out.write(buffer,0,dataRead);
                    }
                }
            }
            System.out.println("merge done..");
        }


    }

    private interface PlayFileProvider{

        PlayListFile getFile() throws IOException;

    }

    private class MediaWriter{
        private byte[] buffer=new byte[16*1024];


        public void write(Context.MediaEntry entry,InputStream inputStream, File targetFile, ProgressBar progressBar) throws IOException {


        }

    }


}
