package com.tencent.ilivedemo.demos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.model.Constants;
import com.tencent.ilivedemo.model.MessageObservable;
import com.tencent.ilivedemo.model.UserInfo;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivedemo.view.DemoEditText;
import com.tencent.ilivefilter.TILFilter;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.livesdk.ILVCustomCmd;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveConstants;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.livesdk.ILVLiveRoomOption;
import com.tencent.livesdk.ILVText;

/**
 * Created by xkazerzhang on 2017/5/24.
 */
public class DemoBtu extends Activity implements View.OnClickListener, ILVLiveConfig.ILVLiveMsgListener,
        SeekBar.OnSeekBarChangeListener {
    private final String TAG = "DemoBtu";
    private DemoEditText etRoom;
    private TextView tvMsg;
    private AVRootView arvRoot;
    private SeekBar sbBeauty, sbWhite;
    private LinearLayout llBeautyControl;

    private boolean bAvsdkBeauty = true;
    private TILFilter mUDFilter;    // 美颜处理器

    private boolean isCameraOn = true;
    private boolean isMicOn = true;
    private boolean isFlashOn = false;

    private String strMsg = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_btu);

        UserInfo.getInstance().getCache(getApplicationContext());

        arvRoot = (AVRootView) findViewById(R.id.arv_root);
        etRoom = (DemoEditText) findViewById(R.id.et_room);
//        etRoom.setText("" + UserInfo.getInstance().getRoom());
        tvMsg = (TextView) findViewById(R.id.tv_msg);
        llBeautyControl = (LinearLayout) findViewById(R.id.ll_btu_control);

        sbBeauty = (SeekBar) findViewById(R.id.sb_beauty);
        sbWhite = (SeekBar)findViewById(R.id.sb_white);
        sbBeauty.setOnSeekBarChangeListener(this);
        sbWhite.setOnSeekBarChangeListener(this);

        ILVLiveManager.getInstance().setAvVideoView(arvRoot);
        MessageObservable.getInstance().addObserver(this);

        initILiveBeauty();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ILVLiveManager.getInstance().onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ILVLiveManager.getInstance().onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageObservable.getInstance().deleteObserver(this);
        ILVLiveManager.getInstance().onDestory();

        if (null != mUDFilter) {
            mUDFilter.setFilter(-1);
            mUDFilter.destroyFilter();
            mUDFilter = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_create:
                createRoom();
                break;
            case R.id.iv_camera:
                isCameraOn = !isCameraOn;
                ILiveRoomManager.getInstance().enableCamera(ILiveRoomManager.getInstance().getCurCameraId(),
                        isCameraOn);
                ((ImageView) findViewById(R.id.iv_camera)).setImageResource(
                        isCameraOn ? R.mipmap.ic_camera_off : R.mipmap.ic_camera_on);
                break;
            case R.id.iv_switch:
                ILiveRoomManager.getInstance().switchCamera(1 - ILiveRoomManager.getInstance().getCurCameraId());
                break;
            case R.id.iv_flash:
                toggleFlash();
                break;
            case R.id.iv_mic:
                isMicOn = !isMicOn;
                ILiveRoomManager.getInstance().enableMic(isMicOn);
                ((ImageView) findViewById(R.id.iv_mic)).setImageResource(
                        isMicOn ? R.mipmap.ic_mic_off : R.mipmap.ic_mic_on);
                break;
            case R.id.iv_beauty:
                if (View.VISIBLE == llBeautyControl.getVisibility())
                    llBeautyControl.setVisibility(View.INVISIBLE);
                else
                    llBeautyControl.setVisibility(View.VISIBLE);
                break;
            case R.id.iv_switch_btu:
                showBtuTypeDlg();
                break;
            case R.id.iv_return:
                finish();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()){
            return;
        }
        switch (seekBar.getId()) {
            case R.id.sb_beauty:
                if (bAvsdkBeauty)
                    ILiveRoomManager.getInstance().enableBeauty(progress);
                else
                    mUDFilter.setBeauty(progress);
                break;
            case R.id.sb_white:
                if (bAvsdkBeauty)
                    ILiveRoomManager.getInstance().enableWhite(progress);
                else
                    mUDFilter.setWhite(progress);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
        addMessage(SenderId, text.getText());
    }

    @Override
    public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {
        switch (cmd.getCmd()) {
            case ILVLiveConstants.ILVLIVE_CMD_LINKROOM_REQ:     // 跨房邀请
                linkRoomReq(id);
                break;
        }
    }

    @Override
    public void onNewOtherMsg(TIMMessage message) {

    }

    private Context getContenxt() {
        return this;
    }

    // 添加消息
    private void addMessage(String sender, String msg) {
        strMsg += "[" + sender + "]  " + msg + "\n";
        tvMsg.setText(strMsg);
    }

    // 加入房间
    private void createRoom() {
        ILVLiveRoomOption option = new ILVLiveRoomOption(ILiveLoginManager.getInstance().getMyUserId())
                .controlRole(Constants.ROLE_MASTER)
                .autoFocus(true);
        ILVLiveManager.getInstance().createRoom(Integer.valueOf(etRoom.getText().toString()),
                option, new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {
                        afterCreate();
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        DlgMgr.showMsg(getContenxt(), "create failed:" + module + "|" + errCode + "|" + errMsg);
                    }
                });
    }

    private void afterCreate() {
        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        etRoom.setEnabled(false);
        findViewById(R.id.tv_create).setVisibility(View.INVISIBLE);
        findViewById(R.id.ll_controller).setVisibility(View.VISIBLE);
        Log.v(TAG, "afterCreate->show control");
    }

    private void toggleFlash() {
        if (ILiveConstants.BACK_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()) {
            return;
        }
        AVVideoCtrl videoCtrl = ILiveSDK.getInstance().getAvVideoCtrl();
        if (null == videoCtrl) {
            return;
        }

        final Object cam = videoCtrl.getCamera();
        if ((cam == null) || (!(cam instanceof Camera))) {
            return;
        }
        final Camera.Parameters camParam = ((Camera) cam).getParameters();
        if (null == camParam) {
            return;
        }

        Object camHandler = videoCtrl.getCameraHandler();
        if ((camHandler == null) || (!(camHandler instanceof Handler))) {
            return;
        }

        //对摄像头的操作放在摄像头线程
        if (isFlashOn == false) {
            ((Handler) camHandler).post(new Runnable() {
                public void run() {
                    try {
                        camParam.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        ((Camera) cam).setParameters(camParam);
                        isFlashOn = true;
                    } catch (RuntimeException e) {
                        Log.d(TAG, "setParameters->RuntimeException");
                    }
                }
            });
        } else {
            ((Handler) camHandler).post(new Runnable() {
                public void run() {
                    try {
                        camParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        ((Camera) cam).setParameters(camParam);
                        isFlashOn = false;
                    } catch (RuntimeException e) {
                        Log.d(TAG, "setParameters->RuntimeException");
                    }
                }
            });
        }
    }

    // 拒绝跨房连麦
    private void refuseLink(String id) {
        ILVLiveManager.getInstance().refuseLinkRoom(id, null);
    }

    // 同意跨房连麦
    private void acceptLink(String id) {
        ILVLiveManager.getInstance().acceptLinkRoom(id, null);
    }

    private void linkRoomReq(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.live_title_link);
        builder.setMessage("[" + id + "]" + getString(R.string.link_req_tips));
        builder.setNegativeButton(R.string.str_btn_refuse, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                refuseLink(id);
            }
        });
        builder.setPositiveButton(R.string.str_btn_agree, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                acceptLink(id);
            }
        });
        DlgMgr.showAlertDlg(this, builder);
    }

    private void initILiveBeauty(){
        if (null == mUDFilter){
            Log.d(TAG, "FILTER->created");
            mUDFilter = new TILFilter(this);
            mUDFilter.setFilter(1);
            mUDFilter.setBeauty(0);
            mUDFilter.setWhite(0);
        }
    }

    private void changeBeauty(){
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()){
            return;
        }
        sbBeauty.setProgress(0);
        sbWhite.setProgress(0);
        mUDFilter.setBeauty(0);
        mUDFilter.setWhite(0);
        ILiveRoomManager.getInstance().enableBeauty(0);
        ILiveRoomManager.getInstance().enableWhite(0);
        if (!bAvsdkBeauty) {
            ILiveSDK.getInstance().getAvVideoCtrl().setLocalVideoPreProcessCallback(new AVVideoCtrl.LocalVideoPreProcessCallback() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void onFrameReceive(AVVideoCtrl.VideoFrame var1) {
                    mUDFilter.processData(var1.data, var1.dataLen, var1.width, var1.height, var1.srcType);
                }
            });
        }else{
            ILiveSDK.getInstance().getAvVideoCtrl().setLocalVideoPreProcessCallback(null);
        }
    }

    private void showBtuTypeDlg(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContenxt())
                .setTitle(R.string.str_title_btu_type)
                .setSingleChoiceItems(R.array.str_btu_type, bAvsdkBeauty?0:1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (bAvsdkBeauty != (0==i)){
                            bAvsdkBeauty = (0==i);
                            changeBeauty();
                            Log.v(TAG, "showBtuTypeDlg->Change beauty: "+bAvsdkBeauty);
                        }
                    }
                });
        DlgMgr.showAlertDlg(getContenxt(), builder);
    }
}
