package org.stormpx.dl.kit;

import java.util.List;

public class Strs {

    public static boolean isBlank(String str){
        return str==null||str.isBlank();
    }

    public static List<String> split(String str,String regex){
        return isBlank(str)?List.of():List.of(str.split(regex));
    }

    public static String removeExt(String str){
        if(str!=null&&str.contains("."))
            return str.substring(0, str.lastIndexOf('.'));
        return str;
    }

    public static String formatByteSize(long size){
        String[] units={"GB","MB","KB"};
        for (int i = 0; i < units.length; i++) {
            double pow = Math.pow(1024, units.length - i);
            if (size>pow){
                return String.format("%.2f"+units[i],(double)size/pow);
            }
        }
        return size+"B";
    }

}
