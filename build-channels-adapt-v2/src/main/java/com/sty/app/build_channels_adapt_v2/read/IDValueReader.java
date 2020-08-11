package com.sty.app.build_channels_adapt_v2.read;

import com.sty.app.build_channels_adapt_v2.ApkUtil;
import com.sty.app.build_channels_adapt_v2.utils.IOUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Map;

public class IDValueReader {
    public static String getString(final File apkFile, final int id) {
        final byte[] bytes = get(apkFile, id);
        if(bytes == null) {
            return "";
        }
        try {
            return new String(bytes, ApkUtil.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static byte[] get(File apkFile, int id) {
        final Map<Integer, ByteBuffer> idValues = getAll(apkFile);
        if(idValues == null) {
            return null;
        }
        final ByteBuffer byteBuffer = idValues.get(id);
        if(byteBuffer == null) {
            return null;
        }

        return getBytes(byteBuffer);
    }

    private static byte[] getBytes(ByteBuffer byteBuffer) {
        final byte[] array = byteBuffer.array();
        final int arrayOffset = byteBuffer.arrayOffset();
        return Arrays.copyOfRange(array, arrayOffset + byteBuffer.position(), arrayOffset + byteBuffer.limit());

    }

    private static Map<Integer, ByteBuffer> getAll(File apkFile) {
        Map<Integer, ByteBuffer> idValues = null;
        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;
        try {
            randomAccessFile = new RandomAccessFile(apkFile, "r");
            fileChannel = randomAccessFile.getChannel();
            final ByteBuffer apkSigningBlock2 = ApkUtil.findApkSigningBlock(fileChannel).getmFirst(); //获取签名块
            idValues = ApkUtil.findIdValues(apkSigningBlock2); //获取键值对
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            IOUtils.closeQuietly(fileChannel);
            IOUtils.closeQuietly(randomAccessFile);
        }
        return idValues;
    }
}
