package org.stormpx.dl.kit;

import java.io.*;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DL {
    public static boolean VIEW=false;
    public static boolean DEBUG=false;



    public static void poutf(String format, Object ... args){
        System.out.printf(format, args);
    }

    public static void pout(String s){
        System.out.print(s);
    }
    public static void poutln(String s){
        System.out.println(s);
    }

    public static void perr(String s){
        System.err.println(s);
    }


    public static URI resolve(URI base,String str){

        URI uri = URI.create(str);
        if (uri.isAbsolute())
            return uri;
        return base.resolve(base);
    }

    public static void createDir(Path path) throws IOException {
        if (Files.notExists(path)){
            Files.createDirectories(path);
        }else{
            if (!Files.isDirectory(path)){
                throw new FileAlreadyExistsException(path+" already exists and is not a dir.");
            }
        }
    }


    public static String getPlatform(){
        String osName = System.getProperty("os.name");
        if (osName.contains("windows")){
            return "windows";
        }else{
            return "linux";
        }
    }

//    public static void loadLib(){
//        load("avcodec");
//        load("avformat");
//        load("avutil");
//        load("dl");
//    }
//
//    public static void load(String lib){
//        try {
//            System.out.println(System.mapLibraryName(lib));
//            System.loadLibrary(lib);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }

    public static boolean remuxing(Path in,Path out){
        try {
            var process=Runtime.getRuntime()
                    .exec(new String[]{"ffmpeg", "-y","-i",in.toAbsolutePath().toString(),"-c","copy",out.toAbsolutePath().toString()});
            int exitCode=process.waitFor();
            if (exitCode!=0) {
                BufferedReader reader = process.errorReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            return exitCode==0;
        } catch (Exception e) {
            throw new RuntimeException("remuxing failed. "+e.getMessage(),e);
        }

    }

//    public native static void remuxing(String inPath,String outPath);

}
