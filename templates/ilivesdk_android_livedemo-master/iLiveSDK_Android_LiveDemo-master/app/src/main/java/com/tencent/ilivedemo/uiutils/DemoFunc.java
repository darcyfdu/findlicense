package com.tencent.ilivedemo.uiutils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xkazerzhang on 2017/6/22.
 */
public class DemoFunc {

    public static String getTimeStr(long uTime){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(new Date(uTime));
    }
}
