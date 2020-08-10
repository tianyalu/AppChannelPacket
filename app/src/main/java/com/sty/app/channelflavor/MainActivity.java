package com.sty.app.channelflavor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sty.app.channelflavor.utils.ChannelHelper;

public class MainActivity extends AppCompatActivity {
    private TextView tvChannel;
    private Button btnGetChannelByFlavor;
    private Button btnGetChannelByMetaInf;
    private Button btnGetChannelByFc;
    private Button btnGetChannelByIdValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        tvChannel = findViewById(R.id.tv_channel);
        btnGetChannelByFlavor = findViewById(R.id.btn_get_channel_by_flavor);
        btnGetChannelByMetaInf = findViewById(R.id.btn_get_channel_by_metainf);
        btnGetChannelByFc = findViewById(R.id.btn_get_channel_by_fc);
        btnGetChannelByIdValue = findViewById(R.id.btn_get_channel_by_id_value);

        btnGetChannelByFlavor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getChannel();
            }
        });
        btnGetChannelByMetaInf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channelName = ChannelHelper.getChannelByMetaInf(MainActivity.this);
                showChannel(channelName);
            }
        });
        btnGetChannelByFc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channelName = ChannelHelper.getChannelByFC(MainActivity.this);
                showChannel(channelName);
            }
        });
        btnGetChannelByIdValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channelName = ChannelHelper.getChannelByIdValue(MainActivity.this);
                showChannel(channelName);
            }
        });
    }

    //读取Manifest中的 meta-data 渠道信息
    private void getChannel() {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String channelName = appInfo.metaData.getString("CHANNEL_VALUE");

            showChannel(channelName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showChannel(String channel) {
        if(TextUtils.isEmpty(channel)) {
            channel = "null";
        }
        tvChannel.setText(channel);
    }
}
