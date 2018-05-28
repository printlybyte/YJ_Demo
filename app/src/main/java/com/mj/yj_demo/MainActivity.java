package com.mj.yj_demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Config;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//import com.android.internal.app.ToolbarActionBar;
import com.mj.regist.Loading_view;
import com.mj.regist.RegUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.os.Build.VERSION.SDK_INT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 热点名称:
     */
    private TextView mHotname;
    /**
     * 热点密码:
     */
    private TextView mHotpassword;
    /**
     * 请输入热点名称
     */
    private EditText mEditName;
    /**
     * 请输入热点密码(最低8位)
     */
    private EditText mEditPassword;
    /**
     * 确认
     */
    private Button mConConfim;

    private Loading_view loading;
    ;

    public static final int MESSAGE_AP_STATE_ENABLED = 1;
    //接收message，做处理
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_AP_STATE_ENABLED:
                    if (isWifiApEnabled()) {
                        Toast.makeText(MainActivity.this, "热点开启成功", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MainActivity.this, "热点开启失败", Toast.LENGTH_SHORT).show();
                    }
                    dismisDialog();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        initRegUtil();
        initView();
        if (SDK_INT >25) {
            Toast.makeText(this, "8888", Toast.LENGTH_SHORT).show();
        }
    }


    private void showDialog() {
        loading = new Loading_view(this, R.style.CustomDialog);
        loading.show();

    }

    private void dismisDialog() {
        loading.dismiss();//3秒后调用关闭加载的方法
    }

    /**
     * @author liu3364575
     * 注册码验证
     * @Time 2017/12/22 13:39
     */
    private void initRegUtil() {
        RegUtil regUtil = new RegUtil(this);
        regUtil.SetDialogCancelCallBack(new RegUtil.DialogCancelInterface() {
            @Override
            public void ToFinishActivity() {
                finish();
            }
        });
    }

    private void initView() {

        mHotname = (TextView) findViewById(R.id.hotname);
        mHotpassword = (TextView) findViewById(R.id.hotpassword);
        mEditName = (EditText) findViewById(R.id.edit_name);
        mEditPassword = (EditText) findViewById(R.id.edit_password);
        mConConfim = (Button) findViewById(R.id.con_confim);
        mConConfim.setOnClickListener(this);
        getWifiAPConfig();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.con_confim:
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                if (isHasPermissions()) {

                    boolean isY = setWifiApEnabled(true);

                    if (isY) {
                        mHandler.sendEmptyMessageDelayed(MESSAGE_AP_STATE_ENABLED, 4000);

                    }
                }
                break;

        }
    }

    /**
     * @author liuguodong
     * 手动开启权限
     * @Time 2017/12/22 11:06
     */
    private boolean isHasPermissions() {
        boolean result = false;
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "打开热点需要启用“修改系统设置”权限，请手动开启", Toast.LENGTH_SHORT).show();

                //清单文件中需要android.permission.WRITE_SETTINGS，否则打开的设置页面开关是灰色的
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                //判断系统能否处理，部分ROM无此action，如魅族Flyme
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    //打开应用详情页
                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            } else {
                result = true;
            }
        } else {
            result = true;
        }
        return result;
    }

    /**
     * 自定义wifi热点
     *
     * @param enabled 开启or关闭
     * @return
     */
    private boolean setWifiApEnabled(boolean enabled) {
        boolean result = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }
        if (enabled) {
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
        }
        String one = mEditName.getText().toString().trim();
        String two = mEditPassword.getText().toString().trim();
        if (one.length() == 0 || TextUtils.isEmpty(one) || TextUtils.isEmpty(two) || two.length() == 0) {
            Toast.makeText(this, "请检查输入是否有错误", Toast.LENGTH_SHORT).show();
            return false;
        }
        showDialog();
        try {
            //热点的配置类
            WifiConfiguration apConfig = new WifiConfiguration();
            //配置热点的名称
            apConfig.SSID = one;
            //配置热点的密码，至少八位
            apConfig.preSharedKey = two;
            //必须指定allowedKeyManagement，否则会显示为无密码
            //指定安全性为WPA_PSK，在不支持WPA_PSK的手机上看不到密码
            //apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            //指定安全性为WPA2_PSK，（官方值为4，小米为6，如果指定为4，小米会变为无密码热点）
            int indexOfWPA2_PSK = 4;

            //从WifiConfiguration.KeyMgmt数组中查找WPA2_PSK的值
            for (int i = 0; i < WifiConfiguration.KeyMgmt.strings.length; i++) {
                if (WifiConfiguration.KeyMgmt.strings[i].equals("WPA2_PSK")) {
                    indexOfWPA2_PSK = i;
                    break;
                }
            }
            apConfig.allowedKeyManagement.set(indexOfWPA2_PSK);
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            //返回热点打开状态
            result = (Boolean) method.invoke(wifiManager, apConfig, enabled);
            if (!result) {
                Toast.makeText(this, "热点创建失败，请手动创建！1", Toast.LENGTH_SHORT).show();
                openAPUI();
            }
        } catch (Exception e) {
            Toast.makeText(this, "热点创建失败，请手动创建！2", Toast.LENGTH_SHORT).show();
            openAPUI();
        }
        return result;
    }

    /**
     * 打开网络共享与热点设置页面
     */
    private void openAPUI() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity");
        intent.setComponent(comp);
        startActivity(intent);
    }


    /**
     * 读取热点配置信息
     */
    private void getWifiAPConfig() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return;
            }
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration apConfig = (WifiConfiguration) method.invoke(wifiManager);
            if (apConfig == null) {
                mEditName.setText("未配置热点");
                mEditPassword.setText("未配置热点");
                return;
            }
            mEditName.setHint(String.format(" %s\r\n", apConfig.SSID));
            mEditPassword.setHint(String.format(" %s\n", apConfig.preSharedKey));

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * 判断是否已打开WiFi热点
     *
     * @return
     */
    private boolean isWifiApEnabled() {
        boolean isOpen = false;
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return false;
            }
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            isOpen = (boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return isOpen;
    }
}
