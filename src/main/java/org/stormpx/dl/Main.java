package org.stormpx.dl;


import org.stormpx.dl.kit.DL;
import org.stormpx.dl.kit.Http;

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
import java.util.concurrent.ExecutionException;

public class Main {
    private static boolean SYS_IN=true;

    public static Integer getInt(String option,String[] args,int index){
        if (index>=args.length){
            System.err.println(option+": requires argument");
            System.exit(128);
        }
        var str=args[index];
        int i = 0;
        try {
            i = Integer.parseInt(str);
            if (i<=0){
                System.err.println(option+": expected argument > 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option+": invalid number of "+str);
        }
        return i;
    }

    public static String getString(String option,String[] args,int index){
        if (index>=args.length){
            System.err.println(option+": requires argument");
            System.exit(128);
        }
        return args[index];
    }

    private static void printHelps(){
        String helps= """
                Usage: m3u8dl [options] url
                    -b --baseUrl <baseUrl>
                    -w --workDir <directory> set directory to save segment
                    -m --maxSegment <number> set maximum number of segment to download
                    --proxy <address> set proxy. eg http://127.0.0.1:8889
                    -r --reload set to enable live streaming download. default: false
                    -ua --userAgent <userAgent> send custom ua to server
                """;

        System.out.println(helps);
        System.exit(0);
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
//        DL.DEBUG=true;

//        System.out.println(URI.create("http://kia.stormx.link/fuck").resolve(""));
//        System.out.println(URI.create("http://kia.stormx.link/0ac338785b5e4e36a91c1cff6c24f6d2/video/").resolve("shit"));

//        System.exit(0);

//        Http.build(null,null);
        String s="http://kia.stormx.link/0ac338785b5e4e36a91c1cff6c24f6d2/video/7920a5df00b64c14bc30382b6196bbef-7d33de1855564bd1b66b777a7dd11efb-video-ld.m3u8";
//        String s="https://europe.olemovienews.com/hlstimeofffmp4/20210226/fICqcpqr/mp4/fICqcpqr.mp4/index-v1-a1.m3u8";
//        String s="https://1258712167.vod2.myqcloud.com/fb8e6c92vodtranscq1258712167/e223ace85285890805202470684/drm/voddrm.token.dWluPTE5MTUyMDQ2MDc7c2tleT1AcHRCRUFXc2lwO3Bza2V5PXRlWnFIVWpxcXJ1WFBibE9DRXdXNC1FUDIyVHllaDMzQW5mMmFTVUxkNElfO3Bsc2tleT0wMDA0MDAwMDlhNGM0NmNhYWZkZDI0MGEwNjdlMzU4NWEzMGIyYjQyYWNhMGM3ZmRlMTA2NTdmY2QyN2M4ZDc0NmJkZjZmN2ZkYjVkZWQxMjY4ZmNlNjBlO2V4dD07dWlkX3R5cGU9MDt1aWRfb3JpZ2luX3VpZF90eXBlPTA7dWlkX29yaWdpbl9hdXRoX3R5cGU9MDtjaWQ9Mjk3NDYxO3Rlcm1faWQ9MTAwMzUyNTQ2O3ZvZF90eXBlPTA=.v.f30739.m3u8?exper=0&sign=25db63d3dd16c7ae7f17b70aa788f30f&t=620E0940&us=4899204661372339825";
//        new Downloader(null,Path.of("Z:\\m3u8\\download"))
//                .setMaximumSlices(3)
//                .download(s);

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
                    case "-w":
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
                            System.err.println("unrecognized option '"+arg+"'");
                        else if (readIdx== args.length-1)
                            SYS_IN=false;

                }
            }

            Http.build(proxyAddr,userAgent);
            Downloader downloader = new Downloader(baseUri,workDir)
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

                }
            }
        } catch (Throwable e){
            if (DL.DEBUG) {
                e.printStackTrace();
            }else {
                System.err.println(e.getMessage());
            }
            System.exit(1);
        }
    }
}