package org.stormpx.dl;

public class DLThreadContext {

    private final static ThreadLocal<DLThreadContext> threadLocal=ThreadLocal.withInitial(DLThreadContext::new);

    private final byte[] buffer=new byte[8*1024];

    public DLThreadContext() {
    }

    public static DLThreadContext current(){
        return threadLocal.get();
    }

    public byte[] getBuffer() {
        return buffer;
    }
}
