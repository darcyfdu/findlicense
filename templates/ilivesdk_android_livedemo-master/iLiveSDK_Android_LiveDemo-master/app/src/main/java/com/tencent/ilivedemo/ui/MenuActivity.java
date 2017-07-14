package com.tencent.ilivedemo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.demos.DemoBtu;
import com.tencent.ilivedemo.demos.DemoCross;
import com.tencent.ilivedemo.demos.DemoGuest;
import com.tencent.ilivedemo.demos.DemoHost;
import com.tencent.ilivedemo.demos.DemoLiveGuest;
import com.tencent.ilivedemo.demos.DemoReplayList;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveSDK;

/**
 * 示例菜单
 */
public class MenuActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mainmenu);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.lmv_host:
                enterDemo(DemoHost.class);
                break;
            case R.id.lmv_guest:
                enterDemo(DemoGuest.class);
                break;
            case R.id.lmv_live_guest:
                enterDemo(DemoLiveGuest.class);
                break;
            case R.id.lmv_cross:
                DlgMgr.showMsg(this, R.string.tip_not_support);
                //enterDemo(DemoCross.class);
                break;
            case R.id.lmv_beauty:
                enterDemo(DemoBtu.class);
                break;
            case R.id.lmv_log:
                showLogDialog();
                break;
            case R.id.lmv_replay:
                enterDemo(DemoReplayList.class);
                break;
        }
    }


    private void enterDemo(Class clsActivity){
        startActivity(new Intent(this, clsActivity));
    }

    private Context getContext(){
        return this;
    }

    private void showLogDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.str_menu_log);
        builder.setView(LayoutInflater.from(getContext()).inflate(R.layout.dialog_log, null));

        final AlertDialog dialog = DlgMgr.showAlertDlg(getContext(), builder);
        final EditText etDate = (EditText)dialog.findViewById(R.id.et_date);
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int date = 0;
                try{
                    date = Integer.valueOf(etDate.getText().toString());
                }catch (NumberFormatException e){
                }
                ILiveSDK.getInstance().uploadLog("report log", date,  new ILiveCallBack(){
                    @Override
                    public void onSuccess(Object data) {
                        Toast.makeText(getContext(), "Log report succ!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        Toast.makeText(getContext(), "failed:"+module+"|"+errCode+"|"+errMsg, Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.dismiss();
            }
        });
    }
}
