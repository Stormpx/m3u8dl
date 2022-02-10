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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class DLCiphers {

    private final static ReentrantLock lock=new ReentrantLock();
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

    public Cipher getCipher(URI baseUri,long sequence,EncryptInfo encryptInfo) throws IOException {
        SecretKeyHolder keyHolder = keyMap.computeIfAbsent(encryptInfo, k -> new SecretKeyHolder());
        SecretKeySpec key = keyHolder.getSecretKey();
        if (key==null){
            lock.lock();
            try {
                key= keyHolder.getSecretKey();
                if (key==null){
                    ReqResult reqResult = Http.request(baseUri.resolve(encryptInfo.getUri()), null);
                    if (!reqResult.isSuccess()){
                        throw new RuntimeException("req decrypt key failed");
                    }
                    byte[] bytes = reqResult.getInputStream().readAllBytes();
                    reqResult.getInputStream().close();

                    key=new SecretKeySpec(bytes,"AES");
                    keyHolder.setSecretKey(key);
                }
            } finally {
                lock.unlock();
            }

        }
        byte[] iv=encryptInfo.getIv();
        if (iv==null){
            iv=new byte[16];
            ByteBuffer.wrap(iv)
                    .putLong(8,sequence);

        }
        return createCipher(key,new IvParameterSpec(iv));
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