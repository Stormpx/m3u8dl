package org.stormpx.dl;


import org.stormpx.dl.kit.DL;
import org.stormpx.dl.kit.Http;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static boolean SYS_IN=true;

    public static Integer getInt(String option,String[] args,int index){
        if (index>=args.length){
            DL.perrln(option+": requires argument");
            System.exit(128);
        }
        var str=args[index];
        try {
            int i = Integer.parseInt(str);
            if (i<=0){
                DL.perrln(option+": expected argument > 0");
            }
            return i;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option+": invalid number of "+str);
        }
    }

    public static String getString(String option,String[] args,int index){
        if (index>=args.length){
            DL.perrln(option + ": requires argument");
            System.exit(128);
        }
        return args[index];
    }

    private static void printHelps(){
        String helps= """
                Usage: m3u8dl [options] <url|file>
                    -b --baseUrl <baseUrl>
                    -d --workDir <directory> set directory to save segment
                    -t --thread <number> set download threads
                    -m --maxSegment <number> set maximum number of segment to download
                    --proxy <address> set proxy. eg http://127.0.0.1:8889
                    -r --reload set to enable live streaming download. default: false
                    --concat concat segment file & try remuxing to .mp4 file
                    --retry <number>
                    --raw
                    -ua --userAgent <userAgent> send custom ua to server
                """;

        DL.poutln(helps);
        System.exit(0);
    }

    public static void main(String[] args){
//        DL.DEBUG=true;
        String userAgent=null;
        URI baseUri=null;
        int thread=Runtime.getRuntime().availableProcessors();
        int maxSegment=Integer.MAX_VALUE;
        int retry=10;
        boolean reload=false;
        boolean concat=false;
        boolean nocheck=false;
        URI proxyAddr=null;
        Path workDir= Paths.get(System.getProperty("user.dir"));

        try {
            int readIdx=0;
            for (; readIdx < args.length ; readIdx++) {
                String arg = args[readIdx];
                switch (arg) {
                    case "-h", "--help" -> printHelps();
                    case "-b", "--baseUrl" -> {
                        String baseUrl = getString(arg, args, ++readIdx);
                        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
                        baseUri = URI.create(baseUrl);
                    }
                    case "--debug" -> DL.DEBUG = true;
                    case "-r", "--reload" -> reload = true;
                    case "-t", "--thread" -> thread = getInt(arg, args, ++readIdx);
                    case "--proxy" -> {
                        String proxyStr = getString(arg, args, ++readIdx);
                        proxyAddr = new URI(proxyStr);
                    }
                    case "--retry" -> retry = getInt(arg, args, ++readIdx);
                    case "-ua", "--userAgent" -> userAgent = getString(arg, args, ++readIdx);
                    case "-d", "--workDir" -> {
                        String path = getString(arg, args, ++readIdx);
                        workDir = Path.of(path);
                    }
                    case "-m", "--maxSegment" -> maxSegment = getInt(arg, args, ++readIdx);
                    case "--concat" -> concat = true;
                    case "--nocheck" -> nocheck = true;
                    default -> {
                        if (arg.startsWith("-")) DL.perrln("unrecognized option '" + arg + "'");
                        else if (readIdx == args.length - 1) SYS_IN = false;
                    }
                }
            }
            //            System.setProperty("jdk.httpclient.connectionPoolSize", String.valueOf(thread));
            DL.perrln("threads: "+thread);
            DL.perrln("workDir: "+workDir);
            ExecutorService threadPool = Executors.newFixedThreadPool(thread);
            Http.build(proxyAddr,userAgent,Executors.newSingleThreadExecutor(r-> {
                var t= new Thread(r,"HttpClient-Thread");
                t.setDaemon(true);
                return t;
            }));
            Downloader downloader = new Downloader(baseUri,workDir,threadPool)
                    .setRetry(retry)
                    .setReload(reload)
                    .setConcat(concat)
                    .setNocheck(nocheck)
                    .setMaximumSegment(maxSegment)
                    ;

            if (!SYS_IN){
                downloader.download(args[args.length-1]);
            }else {
                Scanner scan = new Scanner(System.in);
                while (scan.hasNextLine()) {
                    String input = scan.nextLine();
                    downloader.download(input);
                    DL.perrln("download "+input+" done.");
                }
            }
            //done
            threadPool.shutdown();
        } catch (Throwable e){
            if (DL.DEBUG) {
                e.printStackTrace();
            }else {
                DL.perrln(e.getMessage());
            }
            System.exit(1);
        }
    }
}
