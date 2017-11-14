/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.filterInputStream;

import com.kynetics.updatefactory.ddiclient.core.model.Hash;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * @author Daniele Sergio
 */
public class CheckFilterInputStream extends FilterInputStream {

    @FunctionalInterface
    public interface FileCheckListener {
        void onValidationResult(boolean isValid, Hash hash);
    }

    public static class Builder{
        private InputStream stream;
        private String md5Value;
        private String sha1Value;
        private FileCheckListener listener;

        private Builder() {
        }

        public Builder withStream(InputStream stream) {
            this.stream = stream;
            return this;
        }

        public Builder withMd5Value(String md5Value) {
            this.md5Value = md5Value;
            return this;
        }

        public Builder withSha1Value(String sha1Value) {
            this.sha1Value = sha1Value;
            return this;
        }

        public Builder withListener(FileCheckListener listener) {
            this.listener = listener;
            return this;
        }

        public CheckFilterInputStream build(){
            final String notNullTemplateString = "%s can't be null";
            Objects.requireNonNull(stream, String.format(notNullTemplateString, "stream"));
            Objects.requireNonNull(listener, String.format(notNullTemplateString, "listener"));
            DigestInputStream md5Digest, sha1Digest;
            md5Digest = getDigest(stream, md5Value, "MD5");
            stream = md5Digest == null ? stream : md5Digest;
            sha1Digest = getDigest(stream, sha1Value, "SHA-1");
            stream = sha1Digest == null ? stream : sha1Digest;
            return new CheckFilterInputStream(stream, sha1Value, md5Value, sha1Digest, md5Digest, listener);
        }

        private DigestInputStream getDigest(InputStream stream, String value, String algo) {
            DigestInputStream md5Digest = null;
            if(value != null){
                try {
                    MessageDigest md5 = MessageDigest.getInstance(algo);
                    md5Digest = new DigestInputStream(stream, md5);
                }catch (NoSuchAlgorithmException e){
                    e.printStackTrace();
                }
            }
            return md5Digest;
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    private CheckFilterInputStream(InputStream inputStream,
                                   String sha1Value,
                                   String md5Value,
                                   DigestInputStream sha1Digest,
                                   DigestInputStream md5Digest,
                                   FileCheckListener listener) {
        super(inputStream);
        this.hash = new Hash(md5Value, sha1Value);
        this.listener = listener;
        this.sha1Digest = sha1Digest;
        this.md5Digest = md5Digest;

    }

    @Override
    public int read() throws IOException {
        try {
            final int count = this.in.read();
            if (count == -1) {
                invokeListener();
            }
            return count;
        }catch (IOException e){
            listener.onValidationResult(false, null);
            throw e;
        }
    }

    @Override
    public int read(byte[] var1, int var2, int var3) throws IOException {
        try {
            final int count = this.in.read(var1, var2, var3);
            if (count == -1) {
                invokeListener();
            }
            return count;
        }catch (IOException e){
            listener.onValidationResult(false, null);
            throw e;
        }
    }

    private void invokeListener() {
        final Hash currentHash = buildHash();
        listener.onValidationResult(check(hash.getSha1(), currentHash.getSha1()) && check(hash.getMd5(), currentHash.getMd5()),
                currentHash);
    }

    //Used toHex function instead of DatatypeConverter.printHexBinary to be usable from Android
    private Hash buildHash(){
        String md5 = null;
        String sha1 = null;
        if(md5Digest != null){
//            md5  = DatatypeConverter.printHexBinary(md5Digest.getMessageDigest().digest());
            md5  = toHex(md5Digest.getMessageDigest().digest());
        }

        if(sha1Digest != null){
//            sha1  = DatatypeConverter.printHexBinary(sha1Digest.getMessageDigest().digest());
            sha1  = toHex(sha1Digest.getMessageDigest().digest());
        }
        return new Hash(md5, sha1);
    }

    private String toHex(byte[] arg) {
        return String.format("%x", new BigInteger(1, arg));
    }

    private boolean check(String correctValue, String valueToCheck){
        if( correctValue == null || correctValue.isEmpty() || valueToCheck == null){
            return true;
        }

        return valueToCheck.equalsIgnoreCase(correctValue);
    }

    private final DigestInputStream md5Digest;
    private final DigestInputStream sha1Digest;
    private final Hash hash;
    private final FileCheckListener listener;

}
