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
import org.stormpx.dl.task.Progress;
import org.stormpx.dl.task.TaskManager;
import org.stormpx.dl.task.TaskUnit;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

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

        DL.perrln("uri: "+uri);
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
            DL.perrln(path + " is not exists.");
            return;
        }
        if (!Files.isRegularFile(path)){
            DL.perrln(path + " is not regular file.");
            return;
        }
        String dirName=Strs.removeExt(path.getFileName().toString());
        DL.perrln("filePath: "+ path);

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
            DL.perrln("master list file detected..");
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
                DL.perrln("try download variant stream: " + uri);
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
                DL.perrln("live streaming file detected..");
            }

            int readSlices=0;
            List<Context.Entry> entries=new ArrayList<>();
            TaskManager taskManager = new TaskManager(executor);

            do {
                long now=System.currentTimeMillis();
                URI finalBaseUri = baseUri;

                readSlices+=context.read(this.maximumSegment-readSlices, media -> {
                    Path filePath = media.getPath(downloadPath);
                    taskManager.execute(filePath.getFileName().toString(),new MediaDownloader(finalBaseUri, filePath, media));
                    entries.add(media);
                });


                if (context.shouldReload()&&this.reload &&readSlices< maximumSegment){
                    MediaList mediaList = context.getPlayListFile();
                    long sleepMillis=((long) (mediaList.getTargetDuration()*1000)-(System.currentTimeMillis()-now));
                    if (sleepMillis>0) {
                        sleepWithShowProgress(taskManager,sleepMillis);
                    }
                    boolean change=context.append((MediaList) playListProvider.getFile());
                    while (!change){
                        sleepWithShowProgress(taskManager, (long) (mediaList.getTargetDuration()*1000/2));
                        change=context.append((MediaList) playListProvider.getFile());
                    }
                }


            }while (context.shouldReload()&&this.reload &&readSlices< maximumSegment);

            awaitComplete(taskManager);

            System.out.println();
            if (this.concat)
                concat(entries,downloadPath);
        }


    }

    private void printProgress(TaskManager taskManager){
        Progress progress = taskManager.getProgress();
        Progress latestProgress = taskManager.generateProgress();

        if (progress!=null){
            DL.perr("\r%s", progress.placeHolder());
        }

        DL.perr("\r%s",latestProgress.getPretty());

    }

    private void assertNoException(TaskManager taskManager) throws Exception {
        Collection<Exception> exceptions = taskManager.exceptions();
        if (!exceptions.isEmpty())
            throw exceptions.iterator().next();
    }


    private void awaitComplete(TaskManager taskManager) throws Exception {

        while (!taskManager.isAllDone()){
            printProgress(taskManager);
            assertNoException(taskManager);
            Thread.sleep(100);
        }
        printProgress(taskManager);

    }

    private void sleepWithShowProgress(TaskManager taskManager, long millis) throws Exception {
        long delay=System.currentTimeMillis()+millis;
        while (System.currentTimeMillis()<delay){
            printProgress(taskManager);
            assertNoException(taskManager);
            Thread.sleep(200);
        }

    }


    private void concat(List<Context.Entry> entries, Path dirPath) throws IOException {
        Path tsPath = dirPath.getParent().resolve(dirPath.getFileName() + ".ts");
        File allInOneFile= tsPath.toFile();
        byte[] buffer=new byte[16*1024];
        try (OutputStream out = new FileOutputStream(allInOneFile)){
            DL.perrln("try concat & remuxing");
            for (Context.Entry entry : entries) {
                File file = entry.getPath(dirPath).toFile();
                try (InputStream in=new FileInputStream(file)){
                    int dataRead=-1;
                    while ((dataRead=in.read(buffer))!=-1) {
                        out.write(buffer,0,dataRead);
                    }
                }
            }
            DL.perrln("concat done..");
        }

        if (DL.remuxing(tsPath, dirPath.getParent().resolve(dirPath.getFileName()+".mp4"))){
            DL.perrln("remuxing success..");
            allInOneFile.delete();
        }

    }

    private interface PlayListProvider {

        PlayList getFile() throws IOException;

    }

    private class MediaDownloader implements Consumer<TaskUnit> {
        private URI baseUri;
        private Path filePath;
        private Context.MediaEntry media;

        public MediaDownloader(URI baseUri, Path downloadPath, Context.MediaEntry media) {
            this.baseUri = baseUri;
            this.filePath = downloadPath;
            this.media = media;
        }

        @Override
        public void accept(TaskUnit taskUnit) {
            try {
                start(0,taskUnit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void start(int num,TaskUnit taskUnit) throws Exception {
            try (ReqResult reqResult = Http.request(media.getUri(), media.getByteRange());){
                if (!reqResult.isSuccess()) {
                    taskUnit.complete(new RuntimeException("request ts failed..."));
                }
                if (reqResult.isM3u8File()) {
                    // pass...
                    taskUnit.complete(new RuntimeException("unexpect .m3u8 file detected."));
                }
                taskUnit.setMessage(null);
                Integer contentLength = reqResult.getContentLength();
                InputStream inputStream = reqResult.getInputStream();

                taskUnit.setTotal(contentLength);

                EncryptInfo encryptInfo = media.getEncryptInfo();
                if (encryptInfo !=null){
                    if (encryptInfo.getMethod() != EncryptMethod.NONE){
                        Cipher cipher = ciphers.createCipher(this.baseUri, ((Segment) media.getElement()).getSequence(), encryptInfo);
                        inputStream=new CipherInputStream(inputStream,cipher);
                    }
                }

                write2File(inputStream,filePath,taskUnit);
            } catch (Exception e) {
                if (num < retry) {
                    taskUnit.setMessage("retry %d/%d".formatted(num,retry));
                    start(num + 1, taskUnit);
                } else {
                    throw e;
                }

            }
        }


        private void write2File(InputStream inputStream, Path targetFile,TaskUnit unit) throws IOException {
            byte[] buffer = DLThreadContext.current().getBuffer();
            try (InputStream in=inputStream;
                 OutputStream out=Files.newOutputStream(targetFile, StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING)){

                int dataRead;
                while ((dataRead= in.read(buffer))!=-1){
                    out.write(buffer,0,dataRead);
                    out.flush();
                    unit.stepBy(dataRead);
                }
                if (unit.getTotal()-unit.getCurrent()<=16) {
                    unit.complete(null);
                }else{
                    unit.complete(new RuntimeException("stream end.."));
                }
            }
        }


    }


}