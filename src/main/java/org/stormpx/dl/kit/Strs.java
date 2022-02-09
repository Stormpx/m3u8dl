package org.stormpx.dl.kit;

public class Strs {

    public static String removeExt(String str){
        if(str!=null&&str.contains("."))
            return str.substring(0, str.lastIndexOf('.'));
        return str;
    }
}
