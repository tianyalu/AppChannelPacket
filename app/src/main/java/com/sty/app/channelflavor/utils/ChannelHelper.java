package com.sty.app.channelflavor.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.sty.app.build_channels_by_fc.FileCommentProcessor;
import com.sty.app.build_channels_by_metainf.MetaInfProcessor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ChannelHelper {
    private static final String TAG = ChannelHelper.class.getSimpleName();

    /**
     * 获取Apk文件META-INF目录里的渠道号信息
     *
     * @param context context
     * @return 如果渠道文件存在，返回渠道号；否则返回空字符串
     */
    public static String getChannelByMetaInf(Context context) {
        String srcApkPath = ApkUtils.getSrcApkPath(context);
        if (srcApkPath == null) {
            return "";
        }
        return MetaInfProcessor.getChannelByMetaInf(new File(srcApkPath));
    }

    public static String getChannelByFC(Context context) {
        String srcApkPath = ApkUtils.getSrcApkPath(context);
        if (srcApkPath == null) {
            return "";
        }

        String channelJson = FileCommentProcessor.readFileComment(new File(srcApkPath));
        Log.i(TAG, channelJson);

        String str = "";
        try {
            JSONObject json = new JSONObject(channelJson);
            String channel = json.getString("channel");
            String channelId = json.getString("channel_id");
            str = channel + "_" + channelId;
        } catch (JSONException ignore) {
        }

        return str;
    }

    public static String getV1Channel(Context context) {
        ZipFile zipFile = null;
        StringBuilder channel = new StringBuilder();
        try {
            //当前APP的apk文件 zip文件
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            String sourceDir = appInfo.sourceDir;
            Log.d(TAG, "getV1Channel: " + sourceDir);

            //遍历apk中所有文件
            zipFile = new ZipFile(sourceDir);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                //读取 META-INF 中的信息（渠道信息）
                String entryName = entry.getName();
                if (entryName.startsWith("META_INF/channel")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        channel.append(line);
                    }
                    br.close();
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return channel.toString();
    }

}
