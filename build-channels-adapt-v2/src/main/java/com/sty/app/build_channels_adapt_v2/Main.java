package com.sty.app.build_channels_adapt_v2;

import com.sty.app.build_channels_adapt_v2.write.AppRelease;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String apkFilPath = "/Users/tian/NeCloud/xxt/workspace/AppChannelFlavor/app/build/outputs/apk/release/app-release.apk";
        AppRelease.addChannelToApk(new File(apkFilPath));

//        String channelApkFilePath = "/Users/tian/NeCloud/xxt/workspace/AppChannelFlavor/build-channels-adapt-v2/build/output/channels/app-release_xiaomi_04.apk";
//        String channelName = AppRelease.readChannelFromApk(new File(channelApkFilePath));
//        System.out.println("channelName: " + channelName);
    }
}
