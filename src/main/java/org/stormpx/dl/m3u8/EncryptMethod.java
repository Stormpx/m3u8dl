package org.stormpx.dl.m3u8;

public enum EncryptMethod {

    NONE("NONE"),
    AES_128("AES-128"),
    SAMPLE_AES("SAMPLE-AES")
    ;

    private String value;

    EncryptMethod(String value) {
        this.value = value;
    }


    public static EncryptMethod of(String str){
        for (EncryptMethod method : values()) {
            if (method.value.equalsIgnoreCase(str)){
                return method;
            }
        }
        throw new IllegalArgumentException("unsupported encrypt method: "+ str);
    }
}
