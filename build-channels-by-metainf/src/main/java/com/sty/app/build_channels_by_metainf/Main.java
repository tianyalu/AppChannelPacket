package com.sty.app.build_channels_by_metainf;

public class Main {
    public static void main(String[] args) {
        String apkFilPath = "/Users/tian/NeCloud/xxt/workspace/AppChannelFlavor/app/release/app-release.apk";
        AppRelease.addChannelToApk(apkFilPath);

//        String channelApkPath = "/Users/tian/NeCloud/xxt/workspace/AppChannelFlavor/build-channels-by-metainf/build/output/channels/app-debug_xiaomi_04.apk";
//        String channelName = AppRelease.getChannelFromApk(channelApkPath);
//        System.out.println("channel Name: " + channelName);
    }
}
