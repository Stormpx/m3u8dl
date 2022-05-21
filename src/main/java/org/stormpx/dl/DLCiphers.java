package org.stormpx.dl;

import org.stormpx.dl.kit.Http;
import org.stormpx.dl.kit.ReqResult;
import org.stormpx.dl.kit.Strs;
import org.stormpx.dl.m3u8.EncryptInfo;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class DLCiphers {

    private final static Map<EncryptInfo, SecretKeyHolder> keyMap =new ConcurrentHashMap<>();

    private Cipher createCipher(SecretKeySpec key, IvParameterSpec iv) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key,iv);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e.getMessage());
        }
        return cipher;
    }

    private byte[] sequence2Iv(long sequence){
        byte[] iv=new byte[16];
        ByteBuffer.wrap(iv)
                .putLong(8,sequence);
        return iv;
    }

    public SecretKeySpec load(URI baseUri,EncryptInfo encryptInfo){
        try {
            SecretKeyHolder keyHolder = keyMap.computeIfAbsent(encryptInfo, k -> new SecretKeyHolder());
            SecretKeySpec keySpec = keyHolder.keySpec;
            if (keySpec==null){
                var lock = keyHolder.lock;
                lock.lock();
                try {
                    keySpec=keyHolder.keySpec;
                    if (keySpec==null){
                        ReqResult result = Http.request(baseUri.resolve(encryptInfo.getUri()), null);
                        if (!result.isSuccess()){
                            throw new RuntimeException("req decrypt key failed");
                        }
                        byte[] bytes = result.getInputStream().readAllBytes();
                        result.getInputStream().close();

                        keyHolder.keySpec=new SecretKeySpec(bytes, "AES");
                    }

                }finally {
                    lock.unlock();
                }
            }
            return keyHolder.keySpec;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

//
//    public CompletableFuture<SecretKeySpec> load(URI baseUri,EncryptInfo encryptInfo){
//        try {
//            SecretKeyHolder keyHolder = keyMap.computeIfAbsent(encryptInfo, k -> new SecretKeyHolder());
//            CompletableFuture<SecretKeySpec> future = keyHolder.keySpecFuture;
//
//            if (future==null){
//                Semaphore semaphore = keyHolder.semaphore;
//                semaphore.acquire();
//                try {
//                    future= keyHolder.keySpecFuture;
//                    if (future==null||future.isCompletedExceptionally()||future.isCancelled()) {
//                        future = Http.requestAsync(baseUri.resolve(encryptInfo.getUri()), null)
//                                .thenApply(reqResult -> {
//                                    try {
//                                        if (!reqResult.isSuccess()) {
//                                            throw new RuntimeException("req decrypt key failed");
//                                        }
//                                        byte[] bytes = reqResult.getInputStream().readAllBytes();
//                                        reqResult.getInputStream().close();
//
//                                        return new SecretKeySpec(bytes, "AES");
//                                    } catch (IOException e) {
//                                        throw new RuntimeException(e.getMessage(), e);
//                                    }
//                                });
//                        keyHolder.keySpecFuture=future;
//                    }
//                }finally {
//                    semaphore.release();
//                }
//            }
//            return future;
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e.getMessage(),e);
//        }
//    }

    public Cipher createCipher(URI baseUri, long sequence, EncryptInfo encryptInfo){
        return createCipher(load(baseUri,encryptInfo),new IvParameterSpec(encryptInfo.getIv()!=null?encryptInfo.getIv():sequence2Iv(sequence)));
    }

//    public CompletableFuture<Cipher> createCipherAsync(URI baseUri, long sequence, EncryptInfo encryptInfo)  {
//        return load(baseUri,encryptInfo)
//                .thenApply(key-> createCipher(key,new IvParameterSpec(encryptInfo.getIv()!=null?encryptInfo.getIv():sequence2Iv(sequence))));
//
//    }

    private static class SecretKeyHolder {
//        private Semaphore semaphore=new Semaphore(1);
//        private volatile CompletableFuture<SecretKeySpec> keySpecFuture;
        private ReentrantLock lock=new ReentrantLock();
        private volatile SecretKeySpec keySpec;

    }


}
