/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.filterinputstream;


import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * @author Daniele Sergio
 */
public class CheckFilterInputStreamTest {
    private static final String STRING_1 = "TEST_STRING_1";
    private static final int STRING_1_BYTE_SIZE = STRING_1.getBytes().length;
    private static final String STRING_2 = "TEST_STRING_2";
    private static final String MD5_STRING_1 = "f0fafb6e9be4b9c8c2fddd1babe6bf41";
    private static final String SHA1_STRING_1 = "d21b71393aa79dcb7d2569116c03f7ab31ac4487";

    private CheckFilterInputStream.FileCheckListener listener;

    private byte[] buffer;

    @BeforeMethod
    public void setup(){
        buffer = new byte[STRING_1_BYTE_SIZE];
        listener = mock(CheckFilterInputStream.FileCheckListener.class);
    }

    @Test
    public void testCorrectRead() throws Exception {
//        InputStream mockedInputStream = mock(InputStream.class);

        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

    }

    @Test
    public void testCorruptedFileRead() throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());
    }

    @Test
    public void testIgnoreMd5ValueIfNullOrEmptyRead()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(2)).onValidationResult(eq(true), any());


         checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(2)).onValidationResult(eq(false), any());
    }

    @Test
    public void testFileAlwaysValidWhenSha1AndMd5AreNullOrEmptyRead() throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value(null)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value(null)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(2)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(3)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(4)).onValidationResult(eq(true), any());
    }

    @Test
    public void testIgnoreSha1ValueIfNullOrEmptyRead()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(null)
                .build();

        setPrivateField(checkFilterInputStream, "md5Digest", null);

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(2)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(null)
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(2)).onValidationResult(eq(false), any());
    }

    @Test
    public void testIgnoreSha1DigestNullRead()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "sha1Digest", null);

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "sha1Digest", null);

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

    }

    @Test
    public void testIgnoreMd5DigestNullRead()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "md5Digest", null);

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "md5Digest", null);

        while(checkFilterInputStream.read() != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

    }

    @Test(expectedExceptions = IOException.class)
    public void testCorruptedFileOnIOExceptionThrowRead() throws Exception{
        InputStream inputStream = mock(BufferedInputStream.class);
        when(inputStream.read()).thenThrow(new IOException());
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(inputStream)
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        try {
            while (checkFilterInputStream.read() != -1) {
            }
        } catch (IOException e){
            verify(listener,times(1)).onValidationResult(eq(false), any());
            throw e;
        }

    }

    @Test
    public void testCorrectReadWithArg() throws Exception {
//        InputStream mockedInputStream = mock(InputStream.class);

        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());
    }

    @Test
    public void testCorruptedFileReadWithArg() throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());
    }

    @Test
    public void testIgnoreMd5ValueIfNullOrEmptyReadWithArg()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(2)).onValidationResult(eq(true), any());


        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value(SHA1_STRING_1)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(2)).onValidationResult(eq(false), any());
    }

    @Test
    public void testFileAlwaysValidWhenSha1AndMd5AreNullOrEmptyReadWithArg() throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value(null)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value(null)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(2)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(null)
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(3)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value("")
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(4)).onValidationResult(eq(true), any());
    }

    @Test
    public void testIgnoreSha1ValueIfNullOrEmptyReadWithArg()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(null)
                .build();

        setPrivateField(checkFilterInputStream, "md5Digest", null);

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(2)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(null)
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value("")
                .build();

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(2)).onValidationResult(eq(false), any());
    }

    @Test
    public void testIgnoreSha1DigestNullReadWithArg()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "sha1Digest", null);

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "sha1Digest", null);

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

    }

    @Test
    public void testIgnoreMd5DigestNullReadWithArg()throws Exception{
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_1.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "md5Digest", null);

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(true), any());

        checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(new ByteArrayInputStream(STRING_2.getBytes()))
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();

        setPrivateField(checkFilterInputStream, "md5Digest", null);

        while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}

        verify(listener,times(1)).onValidationResult(eq(false), any());

    }

    @Test(expectedExceptions = IOException.class)
    public void testCorruptedFileOnIOExceptionThrowReadWithArg() throws Exception{
        InputStream inputStream = mock(BufferedInputStream.class);
        when(inputStream.read(buffer, 0, STRING_1_BYTE_SIZE)).thenThrow(new IOException());
        CheckFilterInputStream checkFilterInputStream = CheckFilterInputStream.builder()
                .withStream(inputStream)
                .withListener(listener)
                .withMd5Value(MD5_STRING_1)
                .withSha1Value(SHA1_STRING_1)
                .build();


        try {
            while(checkFilterInputStream.read(buffer, 0, STRING_1_BYTE_SIZE) != -1){}
        } catch (IOException e){
            verify(listener,times(1)).onValidationResult(eq(false), any());
            throw e;
        }

    }

    private void setPrivateField(Object instance, String fieldName, Object valueToSet) throws NoSuchFieldException, IllegalAccessException {
        Field f = instance.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(instance, valueToSet);
        f.setAccessible(false);
    }

}