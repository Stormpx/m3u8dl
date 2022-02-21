package org.stormpx.dl;


import org.stormpx.dl.kit.DL;
import org.stormpx.dl.kit.Http;
import org.stormpx.dl.kit.Lambdas;
import org.stormpx.dl.kit.Strs;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static boolean SYS_IN=true;

    public static Integer getInt(String option,String[] args,int index){
        if (index>=args.length){
            DL.perr(option+": requires argument");
            System.exit(128);
        }
        var str=args[index];
        int i = 0;
        try {
            i = Integer.parseInt(str);
            if (i<=0){
                DL.perr(option+": expected argument > 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option+": invalid number of "+str);
        }
        return i;
    }

    public static String getString(String option,String[] args,int index){
        if (index>=args.length){
            DL.perr(option + ": requires argument");
            System.exit(128);
        }
        return args[index];
    }

    private static void printHelps(){
        String helps= """
                Usage: m3u8dl [options] url
                    -b --baseUrl <baseUrl>
                    -d --workDir <directory> set directory to save segment
                    -t --thread <number> set download threads
                    -m --maxSegment <number> set maximum number of segment to download
                    --proxy <address> set proxy. eg http://127.0.0.1:8889
                    -r --reload set to enable live streaming download. default: false
                    -ua --userAgent <userAgent> send custom ua to server
                """;

        System.out.println(helps);
        System.exit(0);
    }

    public static void main(String[] args){
//        DL.DEBUG=true;
        String userAgent=null;
        URI baseUri=null;
        int thread=Runtime.getRuntime().availableProcessors();
        int maxSegment=Integer.MAX_VALUE;
        boolean reload=false;
        boolean merge=false;
        URI proxyAddr=null;
        Path workDir= Paths.get(System.getProperty("user.dir"));

        try {
            int readIdx=0;
            for (; readIdx < args.length ; readIdx++) {
                String arg = args[readIdx];
                switch (arg) {
                    case "-h":
                    case "--help":
                        printHelps();
                        break;
                    case "-b":
                    case "--baseUrl":
                        String baseUrl = getString(arg,args, ++readIdx);
                        if (!baseUrl.endsWith("/"))
                            baseUrl=baseUrl+"/";
                        baseUri=URI.create(baseUrl);
                        continue;
                    case "--debug":
                        DL.DEBUG=true;
                        continue;
                    case "-r":
                    case "--reload":
                        reload = true;
                        continue;
                    case "-t":
                    case "--thread":
                        Integer threadN = getInt(arg,args, ++readIdx);
                        if (threadN!=null){
                            thread=threadN;
                        }
                        continue;
                    case "--proxy":
                        String proxyStr=getString(arg,args,++readIdx);
                        proxyAddr=new URI(proxyStr);
                        continue;
                    case "-ua":
                    case "--userAgent":
                        userAgent= getString(arg,args, ++readIdx);
                        continue;
                    case "-d":
                    case "--workDir":
                        try {
                            String path = getString(arg,args, ++readIdx);
                            workDir=Path.of(path);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    case "-m":
                    case "--maxSegment":
                        Integer m = getInt(arg,args, ++readIdx);
                        if (m!=null){
                            maxSegment=m;
                        }
                        continue;
                    case "--merge":
                        merge=true;
                        continue;
                    default:
                        if (arg.startsWith("-"))
                            DL.perr("unrecognized option '"+arg+"'");
                        else if (readIdx== args.length-1)
                            SYS_IN=false;

                }
            }

            System.out.println("threads: "+thread);
            DL.poutln("workDir: "+workDir);
            ExecutorService threadPool = Executors.newFixedThreadPool(thread);

            Http.build(proxyAddr,userAgent,threadPool);
            Downloader downloader = new Downloader(baseUri,workDir,threadPool)
                    .setReload(reload)
                    .setMerge(merge)
                    .setMaximumSegment(maxSegment)
                    ;

            if (!SYS_IN){
                downloader.download(args[args.length-1]);
            }else {
                Scanner scan = new Scanner(System.in);
                while (scan.hasNextLine()) {
                    String input = scan.nextLine();
                    //                System.out.println(url);
                    downloader.download(input);
                    DL.poutln("download "+input+" done.");
                }
            }
            //done
            threadPool.shutdown();
        } catch (Throwable e){
            if (DL.DEBUG) {
                e.printStackTrace();
            }else {
                DL.perr(e.getMessage());
            }
            System.exit(1);
        }
    }
}
