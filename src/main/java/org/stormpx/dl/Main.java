package org.stormpx.dl;


import org.stormpx.dl.kit.DL;
import org.stormpx.dl.kit.Http;
import org.stormpx.dl.kit.ProgressBar;
import org.stormpx.dl.kit.ProgressGroup;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    public static Integer getInt(String[] args,int index){
        if (index>=args.length){
            return null;
        }
        var str=args[index];
        int i = 0;
        try {
            i = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid number of "+str);
        }
        return i;
    }

    public static String getString(String[] args,int index){
        if (index>=args.length){
            return null;
        }
        return args[index];
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
//        DL.DEBUG=true;


        Http.build(null,null);

        String s="http://kia.stormx.link/0ac338785b5e4e36a91c1cff6c24f6d2/video/7920a5df00b64c14bc30382b6196bbef-7d33de1855564bd1b66b777a7dd11efb-video-ld.m3u8";
//        String s="https://europe.olemovienews.com/hlstimeofffmp4/20210226/fICqcpqr/mp4/fICqcpqr.mp4/index-v1-a1.m3u8";
//        String s="https://1258712167.vod2.myqcloud.com/fb8e6c92vodtranscq1258712167/e223ace85285890805202470684/drm/voddrm.token.dWluPTE5MTUyMDQ2MDc7c2tleT1AcHRCRUFXc2lwO3Bza2V5PXRlWnFIVWpxcXJ1WFBibE9DRXdXNC1FUDIyVHllaDMzQW5mMmFTVUxkNElfO3Bsc2tleT0wMDA0MDAwMDlhNGM0NmNhYWZkZDI0MGEwNjdlMzU4NWEzMGIyYjQyYWNhMGM3ZmRlMTA2NTdmY2QyN2M4ZDc0NmJkZjZmN2ZkYjVkZWQxMjY4ZmNlNjBlO2V4dD07dWlkX3R5cGU9MDt1aWRfb3JpZ2luX3VpZF90eXBlPTA7dWlkX29yaWdpbl9hdXRoX3R5cGU9MDtjaWQ9Mjk3NDYxO3Rlcm1faWQ9MTAwMzUyNTQ2O3ZvZF90eXBlPTA=.v.f30739.m3u8?exper=0&sign=25db63d3dd16c7ae7f17b70aa788f30f&t=620E0940&us=4899204661372339825";
        new Downloader(null,Path.of("Z:\\m3u8\\download"))
//                .setMaximumSlices(3)
                .download(s);


        //        boolean debug=true;
//        String userAgent="m3u8dl";
//        int thread=Runtime.getRuntime().availableProcessors();
//        boolean recursion=false;
//        URI proxyAddr=null;
//        Path downloadDir= Paths.get(System.getProperty("user.dir"));
//
//        try {
//            if (args.length!=0) {
//                for (int i = 0; i < args.length ; i++) {
//                    String arg = args[i];
//                    switch (arg) {
//                        case "-r":
//                        case "--recursion":
//                            recursion = true;
//                            break;
//                        case "-t":
//                        case "--thread":
//                            Integer threadN = getInt(args, ++i);
//                            if (threadN==null){
//                                System.err.println(arg+ " requires argument");
//                                System.exit(128);
//                            }
//                            if (threadN>0){
//                                thread=threadN;
//                            }
//                            break;
//                        case "--proxy":
//                            String proxyStr=getString(args,++i);
//                            if (proxyStr==null){
//                                System.err.println(arg+" requires argument");
//                                System.exit(1);
//                            }
//                            proxyAddr=new URI(proxyStr);
//                            break;
//                        case "-ua":
//                        case "--user-agent":
//                            String s = getString(args, ++i);
//                            if (s!=null){
//                                userAgent=s;
//                            }
//                            break;
//                        case "-dr":
//                        case "--downloadDir":
//                            try {
//                                String path = getString(args, ++i);
//                                if (path==null){
//                                    System.err.println(arg+" requires argument");
//                                    System.exit(1);
//                                }
//                                downloadDir=Path.of(path);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            break;
//                    }
//                }
//            }
//            File dFile = downloadDir.toFile();
//            if (!dFile.exists()&&!dFile.mkdirs()){
//                System.err.println("mkdirs failed");
//                System.exit(1);
//            }
//            if (!dFile.canWrite()||!dFile.canRead()){
//                System.err.println(dFile+":permission denied");
//                System.exit(1);
//            }
//
//            Http.build(proxyAddr,userAgent);
//
//            Scanner scan = new Scanner(System.in);
//            while (scan.hasNextLine()) {
//                String url = scan.nextLine();
////                System.out.println(url);
//                URI uri = new URI(url);
//
//
//            }
//        } catch (Throwable e){
//            if (debug) {
//                e.printStackTrace();
//            }else {
//                System.err.println(e.getLocalizedMessage());
//            }
//            System.exit(1);
//        }
    }
}
