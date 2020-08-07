package com.sty.app.build_channels_by_metainf;

import com.sty.app.build_channels_by_metainf.utils.FileUtils;
import com.sty.app.build_channels_by_metainf.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class AppRelease {
    private static final String BUILD_DIR = System.getProperty("user.dir")
            + File.separator + "build-channels-by-metainf" + File.separator + "build"
            + File.separator + "output" + File.separator + "channels";

    /**
     * 添加渠道信息到 apk 文件中
     * @param apkFilePath
     * @throws IOException
     */
    public static void addChannelToApk(String apkFilePath) {
        try {
            addChannelToApk(new ZipFile(apkFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getChannelFromApk(String apkFilePath) {
        return MetaInfProcessor.getChannelByMetaInf(new File(apkFilePath));
    }

    /**
     * 添加渠道信息到 apk 文件中（创建新的渠道apk文件）
     * @param apkFile
     */
    public static void addChannelToApk(ZipFile apkFile) {
        if(apkFile == null) {
            throw new NullPointerException("Apk file can not be null");
        }

        Map<String, String> channels = getAllChannels();
        Set<String> channelSet = channels.keySet();
        String srcApkName = apkFile.getName().replace(".apk", "");
        srcApkName = srcApkName.substring(srcApkName.lastIndexOf(File.separator) + 1);

        for (String channel : channelSet) {
            String channelId = channels.get(channel);
            ZipOutputStream zos = null;
            try {
                File channelFile = new File(BUILD_DIR, srcApkName + "_" + channel + "_" + channelId + ".apk");
                if(channelFile.exists()) {
                    channelFile.delete();
                }
                FileUtils.createNewFile(channelFile);
                zos = new ZipOutputStream(new FileOutputStream(channelFile));
                copyApkFile(apkFile, zos);

                MetaInfProcessor.addChannelFile(zos, channel, channelId);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(zos);
            }

        }
        IOUtils.closeQuietly(apkFile);
    }

    /**
     * 将源 apk 文件完全复制到新的输出流中
     * @param src
     * @param zos
     * @throws IOException
     */
    private static void copyApkFile(ZipFile src, ZipOutputStream zos) throws IOException {
        Enumeration<? extends ZipEntry> entries = src.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            ZipEntry copyZipEntry = new ZipEntry(zipEntry.getName());
            zos.putNextEntry(copyZipEntry);
            if(!zipEntry.isDirectory()) {
                InputStream in = src.getInputStream(zipEntry);
                int len;
                byte[] buffer = new byte[8 * 1024];
                while ((len = in.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }

    /**
     * 从 channel.properties 配置文件中读取所有渠道信息
     * @return
     */
    private static Map<String, String> getAllChannels() {
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream("channel.properties");
            p.load(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }

        Map<String, String> channels = new HashMap<>();
        Enumeration<Object> keys = p.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            channels.put(key, p.getProperty(key));
        }

        return channels;
    }

}
