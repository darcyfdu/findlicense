package com.tencent.ilivedemo;

import android.app.Application;

import com.tencent.ilivedemo.model.MessageObservable;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.qalsdk.sdk.MsfSdkUtils;

/**
 * Created by xkazerzhang on 2017/5/23.
 */
public class DemoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if(MsfSdkUtils.isMainProcess(this)){
            // 初始化LiveSDK
            ILiveSDK.getInstance().initSdk(this, 1400028096, 11851);
            ILVLiveManager.getInstance().init(new ILVLiveConfig()
                    .setLiveMsgListener(MessageObservable.getInstance()));
        }
    }
}
