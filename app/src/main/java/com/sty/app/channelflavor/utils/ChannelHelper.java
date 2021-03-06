package com.sty.app.channelflavor.utils;

import android.content.Context;
import android.util.Log;

import com.sty.app.build_channels_adapt_v2.ChannelProcessor;
import com.sty.app.build_channels_by_fc.FileCommentProcessor;
import com.sty.app.build_channels_by_metainf.MetaInfProcessor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

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

    public static String getChannelByIdValue(Context context) {
        String srcApkPath = ApkUtils.getSrcApkPath(context);
        if (srcApkPath == null) return "";

        String channelJson = ChannelProcessor.read(new File(srcApkPath));
        Log.i(TAG, channelJson);

        String str = "";
        try {
            JSONObject json = new JSONObject(channelJson);
            String channel = json.getString("channel");
            String channelId = json.getString("channel_id");
            str = channel + "_" + channelId;
        } catch (JSONException ignore) {}

        return str;
    }

}
