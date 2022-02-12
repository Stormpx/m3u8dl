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
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
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

        try {
            SecretKeyHolder keyHolder = keyMap.computeIfAbsent(encryptInfo, k -> new SecretKeyHolder());
            CompletableFuture<SecretKeySpec> future = keyHolder.keySpecFuture;

            if (future==null){
                Semaphore semaphore = keyHolder.semaphore;
                semaphore.acquire();
                try {
                    future= keyHolder.keySpecFuture;
                    if (future==null||future.isCompletedExceptionally()||future.isCancelled()) {
                        future = Http.requestAsync(baseUri.resolve(encryptInfo.getUri()), null)
                                .thenApply(reqResult -> {
                                    try {
                                        if (!reqResult.isSuccess()) {
                                            throw new RuntimeException("req decrypt key failed");
                                        }
                                        byte[] bytes = reqResult.getInputStream().readAllBytes();
                                        reqResult.getInputStream().close();

                                        return new SecretKeySpec(bytes, "AES");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                                });
                        keyHolder.keySpecFuture=future;
                    }
                }finally {
                    semaphore.release();
                }
            }
            return future.thenApplyAsync(key-> createCipher(key,new IvParameterSpec(encryptInfo.getIv()!=null?encryptInfo.getIv():sequence2Iv(sequence))));
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(),e);
        }

    }

    private static class SecretKeyHolder {
        private Semaphore semaphore=new Semaphore(1);
        private volatile CompletableFuture<SecretKeySpec> keySpecFuture;

        public Semaphore getSemaphore() {
            return semaphore;
        }

        public CompletableFuture<SecretKeySpec> keyFuture() {
            return keySpecFuture;
        }

        public SecretKeyHolder setKeySpecFuture(CompletableFuture<SecretKeySpec> keySpecFuture) {
            this.keySpecFuture = keySpecFuture;
            return this;
        }
    }


}
