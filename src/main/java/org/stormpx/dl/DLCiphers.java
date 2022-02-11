package org.stormpx.dl;

import org.stormpx.dl.kit.Http;
import org.stormpx.dl.kit.ReqResult;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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

    public Cipher getCipher(URI baseUri,long sequence,EncryptInfo encryptInfo)  {
        try {
            return getCipherAsync(baseUri, sequence, encryptInfo).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    public CompletableFuture<Cipher> getCipherAsync(URI baseUri, long sequence, EncryptInfo encryptInfo)  {
        return Http.requestAsync(baseUri.resolve(encryptInfo.getUri()), null)
                .thenApply(reqResult -> {
                    try {
                        if (!reqResult.isSuccess()){
                            throw new RuntimeException("req decrypt key failed");
                        }
                        byte[] bytes = reqResult.getInputStream().readAllBytes();
                        reqResult.getInputStream().close();
                        //                                System.out.println("?????");

                        SecretKeySpec newKey=new SecretKeySpec(bytes,"AES");
                        return createCipher(newKey,new IvParameterSpec(encryptInfo.getIv()!=null?encryptInfo.getIv():sequence2Iv(sequence)));
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(),e);
                    }
                });

//        SecretKeyHolder keyHolder = keyMap.computeIfAbsent(encryptInfo, k -> new SecretKeyHolder());
//        SecretKeySpec key = keyHolder.getSecretKey();
//        ReentrantLock lock = keyHolder.getLock();
//        if (key==null){
//            lock.lock();
//            key= keyHolder.getSecretKey();
//            if (key!=null) {
//                lock.unlock();
//            }else {
//                return Http.requestAsync(baseUri.resolve(encryptInfo.getUri()), null)
//                        .thenApply(reqResult -> {
//                            try {
//                                if (!reqResult.isSuccess()){
//                                    throw new RuntimeException("req decrypt key failed");
//                                }
//                                byte[] bytes = reqResult.getInputStream().readAllBytes();
//                                reqResult.getInputStream().close();
////                                System.out.println("?????");
//
//                                SecretKeySpec newKey=new SecretKeySpec(bytes,"AES");
//                                keyHolder.setSecretKey(newKey);
//                                return createCipher(newKey,new IvParameterSpec(encryptInfo.getIv()!=null?encryptInfo.getIv():sequence2Iv(sequence)));
//                            } catch (IOException e) {
//                                throw new RuntimeException(e.getMessage(),e);
//                            }
//                        })
//                        .whenComplete((v,t)->lock.unlock());
//
//            }
//        }
//        return CompletableFuture.completedFuture(createCipher(key,new IvParameterSpec(encryptInfo.getIv()!=null?encryptInfo.getIv():sequence2Iv(sequence))));

    }

    private static class SecretKeyHolder {
        private ReentrantLock lock=new ReentrantLock();
        private volatile SecretKeySpec secretKey;

        public ReentrantLock getLock() {
            return lock;
        }


        public SecretKeySpec getSecretKey() {
            return secretKey;
        }

        public SecretKeyHolder setSecretKey(SecretKeySpec secretKey) {
            this.secretKey = secretKey;
            return this;
        }
    }


}
