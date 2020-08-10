package com.sty.app.build_channels_adapt_v2;

import com.sty.app.build_channels_adapt_v2.read.IDValueReader;
import com.sty.app.build_channels_adapt_v2.write.IDValueWriter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChannelProcessor {
    private static final String CHARSET = "utf-8";

    public static void write(final File apkFile, String string) {
        try {
            final byte[] bytes = string.getBytes(CHARSET);
            final ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put(bytes, 0, bytes.length);
            byteBuffer.flip(); //重置position等状态

            Map<Integer, ByteBuffer> idValues = new LinkedHashMap<>();
            idValues.put(ApkUtil.APK_CHANNEL_BLOCK_ID, byteBuffer);

            IDValueWriter.writeApkSigningBlock(apkFile, idValues);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SignatureNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String read(final File apkFile) {
        return IDValueReader.getString(apkFile, ApkUtil.APK_CHANNEL_BLOCK_ID);
    }
}
