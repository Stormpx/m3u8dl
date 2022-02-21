package org.stormpx.dl.kit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DL {
    public final static ProgressGroup GROUP=new ProgressGroup();
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

}
