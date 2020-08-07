package com.sty.app.build_channels_by_metainf.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * IO工具类
 */
public class IOUtils {

    public static void close(Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new RuntimeException("IOException occurred. ", e);
            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }
}
