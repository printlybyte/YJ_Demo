package com.mj.regist;

/**
 * Created by Administrator on 2017/12/22.
 */


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Administrator on 2017/4/13.
 */

public class RegUtil {
    private Context context;
    private Dialog dialog_reg;
    private ProgressDialog p;
    private String input;
    boolean isoverTime = true;
    private String code = "";
    private final SharedPreferences sp;
    private String strreg;
    private DialogCancelInterface dcif;


    public RegUtil(Context context) {
        this.context = context;
        sp = context.getSharedPreferences("RegCode", MODE_PRIVATE);
        regist();
    }

    public void regist() {

        try {
            strreg = sp.getString("REGCODE", "");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (strreg == null || TextUtils.isEmpty(strreg)) {
            showRegDialog();
        }

    }

    private void showRegDialog() {

        View v = LayoutInflater.from(context).inflate(R.layout.reg_dialog, null);
        dialog_reg = new Dialog(context, R.style.DialogStyle);
        dialog_reg.setCanceledOnTouchOutside(false);
        dialog_reg.show();
        dialog_reg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dcif.ToFinishActivity();
                dialog_reg.dismiss();
            }
        });
        Window window = dialog_reg.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        lp.width = dip2px(context, 290); // 宽度
        lp.height = dip2px(context, 200); // 高度
        lp.alpha = 0.7f; // 透明度
        window.setAttributes(lp);
        window.setContentView(v);
        final TextView reg = (TextView) v.findViewById(R.id.editTextReg);
        ImageButton ib = (ImageButton) v.findViewById(R.id.imageButtonReg);
        ImageButton.OnClickListener listener = new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isConnected(context)) {
                    Toast.makeText(context, "无网络，请稍后重试",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                input = reg.getText().toString();
                if (input == null || TextUtils.isEmpty(input)) {
                    Toast.makeText(context, "请输入验证码",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // 网络验证中
                p = ProgressDialog.show(context, "请稍候",
                        "注册码验证中请不要进行其他操作", true);
                new CountDownTimer(8000, 1000) {

                    @Override
                    public void onTick(long arg0) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onFinish() {


                        if (isoverTime) {
                            mMessageHandler.sendEmptyMessage(1);
                        }
                    }
                };

                new Thread() {
                    @Override
                    public void run() {

                        // Message m = new Message();
                        // m.what = 0;

                        TelephonyManager telephonyManager = (TelephonyManager) context
                                .getSystemService(Context.TELEPHONY_SERVICE);
                        String IMEI = telephonyManager.getDeviceId();

                        String sURL = "http://218.246.35.74:5050/PC/Default.aspx?Number="
                                + input + "&Onlycode=" + IMEI;

                        java.net.URL l_url = null;
                        try {
                            l_url = new java.net.URL(sURL);
                        } catch (MalformedURLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            // Toast.makeText(context,
                            // "网络验证异常",Toast.LENGTH_SHORT).show();

                            p.dismiss();

                            mMessageHandler
                                    .sendEmptyMessage(3);
                        }
                        java.net.HttpURLConnection l_connection = null;
                        try {
                            l_connection = (java.net.HttpURLConnection) l_url
                                    .openConnection();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            // Toast.makeText(context, "网络验证（建立网络）异常",
                            // Toast.LENGTH_SHORT).show();

                            p.dismiss();

                            mMessageHandler
                                    .sendEmptyMessage(4);
                        }
                        try {
                            l_connection.connect();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            // Toast.makeText(context, "网络验证（连接网络）异常",
                            // Toast.LENGTH_SHORT).show();

                            p.dismiss();

                            mMessageHandler
                                    .sendEmptyMessage(5);

                        }
                        InputStream l_urlStream = null;
                        try {
                            l_urlStream = l_connection.getInputStream();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            p.dismiss();

                            mMessageHandler
                                    .sendEmptyMessage(6);
                            return;

                        }

                        java.io.BufferedReader l_reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(l_urlStream));
                        String sCurrentLine = "";
                        code = "";
                        try {
                            while ((sCurrentLine = l_reader.readLine()) != null) {
                                code += sCurrentLine;
                            }
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            // Toast.makeText(context, "网络验证（解析数据）异常",
                            // Toast.LENGTH_SHORT).show();

                            p.dismiss();

                            mMessageHandler
                                    .sendEmptyMessage(7);
                        }

                        p.dismiss();

                        mMessageHandler.sendEmptyMessage(0);

                    }
                }.start();

            }
        };

        ib.setOnClickListener(listener);

    }

    Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:// time out
                    Toast.makeText(context, "网络连接超时，注册码验证失败",
                            Toast.LENGTH_SHORT).show();


                    if (p != null) {
                        p.dismiss();
                    }


                    break;
            /* 当取得识别为 离开运行线程时所取得的短信 */
                case 0:


                    if (code == null || TextUtils.isEmpty(code)) {
                        Toast.makeText(context, "注册码验证失败，请重试",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (code.equalsIgnoreCase("1")) {

                        Toast.makeText(context, "注册码验证成功",
                                Toast.LENGTH_SHORT).show();
                        isoverTime = false;
                        SharedPreferences.Editor et = sp.edit();
                        et.putString("REGCODE", input);
                        et.commit();


                        dialog_reg.dismiss();
                        // onConfirm.OK();

                    } else if (code.equalsIgnoreCase("11")) {

                        Toast.makeText(context, "注册码超过有效使用次数",
                                Toast.LENGTH_SHORT).show();

                    } else if (code.equalsIgnoreCase("12")) {

                        Toast.makeText(context, "注册码已过期",
                                Toast.LENGTH_SHORT).show();

                    } else if (code.equalsIgnoreCase("13")) {

                        Toast.makeText(context, "注册码超过有效使用次数或已过期",
                                Toast.LENGTH_SHORT).show();

                    } else if (code.equalsIgnoreCase("14")) {

                        Toast.makeText(context, "此注册码未授权在此机器使用",
                                Toast.LENGTH_SHORT).show();

                    } else if (code.equalsIgnoreCase("15")) {

                        Toast.makeText(context, "注册码已被禁用",
                                Toast.LENGTH_SHORT).show();

                    } else if (code.equalsIgnoreCase("16")) {

                        Toast.makeText(context, "注册码不存在",
                                Toast.LENGTH_SHORT).show();

                    } else if (code.equalsIgnoreCase("17")) {

                        Toast.makeText(context, "注册中发生未知异常,注册失败",
                                Toast.LENGTH_SHORT).show();

                    } else {

                        Toast.makeText(context, "注册码错误，请重新输入",
                                Toast.LENGTH_SHORT).show();

                    }

                    break;
                case 3:
                    Toast.makeText(context, "网络验证异常", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case 4:
                    Toast.makeText(context, "网络验证（建立连接网络）异常",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    Toast.makeText(context, "网络验证（连接网络）异常",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 6:
                    Toast.makeText(context, "网络验证（获取数据）异常",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 7:
                    Toast.makeText(context, "网络验证（解析数据）异常",
                            Toast.LENGTH_SHORT).show();
                    break;
                case 10:

                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

        ;
    };

    public void SetDialogCancelCallBack(DialogCancelInterface dcif) {
        this.dcif = dcif;
    }

    public interface DialogCancelInterface {
        void ToFinishActivity();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 判定是否有网络
     *
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        boolean isOk = true;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiNetInfo != null && !wifiNetInfo.isConnectedOrConnecting()) {
                if (mobNetInfo != null && !mobNetInfo.isConnectedOrConnecting()) {
                    NetworkInfo info = connectivityManager
                            .getActiveNetworkInfo();
                    if (info == null) {
                        isOk = false;
                    }
                }
            }
            mobNetInfo = null;
            wifiNetInfo = null;
            connectivityManager = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOk;
    }
}