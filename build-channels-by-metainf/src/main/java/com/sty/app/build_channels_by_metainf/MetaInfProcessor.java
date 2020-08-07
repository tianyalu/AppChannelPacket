package com.sty.app.build_channels_by_metainf;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class MetaInfProcessor {
    private static final String META_INF_PATH = "META-INF" + File.separator;
    private static final String CHANNEL_PREFIX = "channel_";
    private static final String CHANNEL_PATH = META_INF_PATH + CHANNEL_PREFIX;

    public static void addChannelFile(ZipOutputStream zos, String channel, String channelId) throws IOException {
        // Add Channel file to META-INF
        ZipEntry emptyChannelFile = new ZipEntry(CHANNEL_PATH + channel + "_" + channelId);
        zos.putNextEntry(emptyChannelFile);
        zos.close();
    }

    /**
     * 从渠道 apk 文件中读取 META-INF 中的渠道信息
     * @param apkFile
     * @return
     */
    public static String getChannelByMetaInf(File apkFile) {
        String channel = "";
        if(apkFile == null || !apkFile.exists()) {
            return channel;
        }

        try {
            ZipFile zipFile = new ZipFile(apkFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if(name == null || name.trim().length() == 0 || !name.startsWith(META_INF_PATH)) {
                    continue;
                }
                name = name.replace(META_INF_PATH, "");
                if(name.startsWith(CHANNEL_PREFIX)) {
                    channel = name.replace(CHANNEL_PREFIX, "");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return channel;
    }

}
