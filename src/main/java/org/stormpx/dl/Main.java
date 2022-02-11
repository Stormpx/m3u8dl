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

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
//        DL.DEBUG=true;

//        int[] arr={1024*1024*1025,1025*1024,1025,1};
//        for (int i : arr) {
//            System.out.println(Strs.formatByteSize(i));
//        }
//

//        System.out.println(URI.create("https://hdtv.prod2.ioio.tv/broker/manifests/e529407a-cb61-45ce-a9ad-94f0ad5e0ad9/270/535680.m3u8")
//                .resolve(URI.create("https://hdtv.prod2.ioio.tv/sources/65d90a85-feec-418e-9076-68a0c82a7312/hls/90f95aa50ddbff24eec3164ef984cd9b_480x270p-0.4Mbps-400000_00032.ts?bw=400000&c_guid=e529407a-cb61-45ce-a9ad-94f0ad5e0ad9&c_name=Factory&h=270&s_dur=6000&s_pos=186000&s_time=1644599857796&v_dur=193777&v_guid=65d90a85-feec-418e-9076-68a0c82a7312&v_name=Daymaker%20LED%20Lighting&v_type=source"))
//                );

//        System.out.println(URI.create("https://hdtv.prod2.ioio.tv/sources/65d90a85-feec-418e-9076-68a0c82a7312/hls/90f95aa50ddbff24eec3164ef984cd9b_480x270p-0.4Mbps-400000_00032.ts?bw=400000&c_guid=e529407a-cb61-45ce-a9ad-94f0ad5e0ad9&c_name=Factory&h=270&s_dur=6000&s_pos=186000&s_time=1644599857796&v_dur=193777&v_guid=65d90a85-feec-418e-9076-68a0c82a7312&v_name=Daymaker%20LED%20Lighting&v_type=source").resolve("")
//                .relativize(URI.create("https://hdtv.prod2.ioio.tv/sources/65d90a85-feec-418e-9076-68a0c82a7312/hls/90f95aa50ddbff24eec3164ef984cd9b_480x270p-0.4Mbps-400000_00032.ts?bw=400000&c_guid=e529407a-cb61-45ce-a9ad-94f0ad5e0ad9&c_name=Factory&h=270&s_dur=6000&s_pos=186000&s_time=1644599857796&v_dur=193777&v_guid=65d90a85-feec-418e-9076-68a0c82a7312&v_name=Daymaker%20LED%20Lighting&v_type=source")).getPath());
//
//        System.exit(0);

//        String s="https://europe.olemovienews.com/hlstimeofffmp4/20210226/fICqcpqr/mp4/fICqcpqr.mp4/index-v1-a1.m3u8";
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
