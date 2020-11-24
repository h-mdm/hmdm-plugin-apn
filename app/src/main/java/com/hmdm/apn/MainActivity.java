package com.hmdm.apn;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.data.ApnSetting;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hmdm.MDMService;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MainActivity extends AppCompatActivity implements MDMService.ResultHandler {

    private MDMService mdmService;
    private boolean mdmConnected = false;
    private List<ApnSetting> apns = new LinkedList<>();
    private TextView textView;
    private TextView refreshButton;

    private final String PREFIX_APN_DEFAULT = "";

    private BroadcastReceiver configUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Const.NOTIFICATION_CONFIG_UPDATED)) {
                Log.i(Const.LOG_TAG, "Refreshing configuration by a server signal");
                if (mdmConnected) {
                    loadSettings();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text);
        refreshButton = findViewById(R.id.refresh);

        textView.setText(R.string.please_wait);
        refreshButton.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.version_warning)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
            return;
        }

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSettings();
            }
        });

        mdmService = MDMService.getInstance();
        registerReceiver(configUpdateReceiver, new IntentFilter(Const.NOTIFICATION_CONFIG_UPDATED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        if (mdmConnected) {
            loadSettings();
        } else {
            if (!mdmService.connect(this, this)) {
                textView.setText(R.string.error_hmdm_connect);
                refreshButton.setVisibility(View.GONE);
            }
        }
    }

    private void loadSettings() {
        if (needClearApn(PREFIX_APN_DEFAULT)) {
            apns.clear();
            setupApns();
            return;
        }

        ApnSetting apnSetting = loadApn(PREFIX_APN_DEFAULT, ApnSetting.TYPE_DEFAULT);
        if (apnSetting == null) {
            textView.setText(R.string.apn_helper_settings);
            refreshButton.setVisibility(View.VISIBLE);
            return;
        }
        apns.clear();
        apns.add(loadApn(PREFIX_APN_DEFAULT, ApnSetting.TYPE_DEFAULT));
        setupApns();
    }

    private boolean needClearApn(String cfgPrefix) {
        String clear = MDMService.Preferences.get(cfgPrefix + Const.PREF_APN_CLEAR, "").trim();
        return clear.equalsIgnoreCase("true") || clear.equalsIgnoreCase("1");
    }

    private ApnSetting loadApn(String cfgPrefix, int apnType) {
        String entryName = MDMService.Preferences.get(cfgPrefix + Const.PREF_APN_DESC, "").trim();
        if (entryName.equals("")) {
            return null;
        }
        String apnName = MDMService.Preferences.get(cfgPrefix + Const.PREF_APN_NAME, "").trim();
        if (apnName.equals("")) {
            return null;
        }
        String mcc = MDMService.Preferences.get(cfgPrefix + Const.PREF_MCC, "").trim();
        if (mcc.equals("")) {
            return null;
        }
        String mnc = MDMService.Preferences.get(cfgPrefix + Const.PREF_MNC, "").trim();
        if (mcc.equals("")) {
            return null;
        }
        String proxyAddr = MDMService.Preferences.get(cfgPrefix + Const.PREF_PROXY_ADDR, "").trim();
        String proxyPort = MDMService.Preferences.get(cfgPrefix + Const.PREF_PROXY_PORT, "").trim();
        String user = MDMService.Preferences.get(cfgPrefix + Const.PREF_USER, "").trim();
        String password = MDMService.Preferences.get(cfgPrefix + Const.PREF_PASSWORD, "").trim();

        ApnSetting.Builder builder = new ApnSetting.Builder()
                .setEntryName(entryName)
                .setApnName(apnName)
                .setApnTypeBitmask(apnType)
                .setOperatorNumeric(mcc + mnc)
                .setCarrierEnabled(true)
                .setProtocol(ApnSetting.PROTOCOL_IPV4V6)
                .setRoamingProtocol(ApnSetting.PROTOCOL_IPV4V6);

        if (!proxyAddr.equalsIgnoreCase("")) {
            builder.setProxyAddress(proxyAddr);
        }
        if (!proxyPort.equalsIgnoreCase("")) {
            try {
                int port = Integer.parseInt(proxyPort);
                if (port > 0) {
                    builder.setProxyPort(port);
                }
            } catch (NumberFormatException e) {
            }
        }
        if (!user.equalsIgnoreCase("")) {
            builder.setUser(user);
        }
        if (!password.equalsIgnoreCase("")) {
            builder.setPassword(password);
        }

        return builder.build();
    }

    private void reportError(String error) {
        Log.w(Const.LOG_TAG, error);
        textView.setText(getString(R.string.apn_setup_fail, error));
        refreshButton.setVisibility(View.GONE);
    }

    private String getProxyAddress(ApnSetting apnSetting) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            InetAddress inetAddress = apnSetting.getProxyAddress();
            if (inetAddress != null) {
                try {
                    return inetAddress.getHostAddress();
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return apnSetting.getProxyAddressAsString();
        }
        return null;
    }

    private void reportSuccess(ApnSetting apnSetting) {
        String s = getString(R.string.apn_setup);
        s += "\n\n";
        s += "entryName: " + apnSetting.getEntryName() + "\n";
        s += "apnName: " + apnSetting.getApnName() + "\n";
        s += "operatorNumeric: " + apnSetting.getOperatorNumeric() + "\n";
        if (getProxyAddress(apnSetting) != null) {
            s += "proxyAddr: " + apnSetting.getProxyAddressAsString() + "\n";
        }
        if (apnSetting.getProxyPort() != -1) {
            s += "proxyPort: " + apnSetting.getProxyPort() + "\n";
        }
        if (apnSetting.getUser() != null) {
            s += "user: " + apnSetting.getUser() + "\n";
        }
        if (apnSetting.getPassword() != null) {
            s += "password: " + apnSetting.getPassword() + "\n";
        }
        textView.setText(s);
        refreshButton.setVisibility(View.VISIBLE);
    }

    private void setupApns() {
        List<ApnSetting> legacyApns;

        try {
            ComponentName cn = new ComponentName("com.hmdm.launcher", "com.hmdm.launcher.AdminReceiver");
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            do {
                legacyApns = dpm.getOverrideApns(cn);
                for (ApnSetting apn : legacyApns) {
                    Log.i(Const.LOG_TAG, "Got overridden APN: " + apn.getEntryName() + "," + apn.getApnName() + "," + apn.getApnTypeBitmask() + "," + apn.getOperatorNumeric());
                }

                if (legacyApns.size() == 0) {
                    break;
                }
                if (!dpm.removeOverrideApn(cn, legacyApns.get(0).getId())) {
                    reportError("Cannot remove " + legacyApns.get(0).getEntryName());
                    return;
                }
            } while (true);

            if (apns.size() == 0) {
                dpm.setOverrideApnsEnabled(cn, false);
                textView.setText(R.string.apn_cleared);
                refreshButton.setVisibility(View.VISIBLE);
                return;
            }

            // Current version supports one APN only
            ApnSetting apnSetting = apns.get(0);
            int n = dpm.addOverrideApn(cn, apnSetting);
            if (n != -1) {
                reportSuccess(apnSetting);
            } else {
                reportError("Not allowed to add APN " + apnSetting.getEntryName());
                return;
            }

            dpm.setOverrideApnsEnabled(cn, true);

        } catch (Exception e) {
            e.printStackTrace();
            reportError(e.getLocalizedMessage());
        }

    }

    @Override
    public void onMDMConnected() {
        mdmConnected = true;
        loadSettings();
    }

    @Override
    public void onMDMDisconnected() {
        mdmConnected = false;
        new Handler().postDelayed(new MDMReconnectRunnable(), Const.HMDM_RECONNECT_DELAY_FIRST);
    }

    public class MDMReconnectRunnable implements Runnable {
        @Override
        public void run() {
            if (!mdmService.connect(MainActivity.this, MainActivity.this)) {
                // Retry in 1 minute
                new Handler().postDelayed(this, Const.HMDM_RECONNECT_DELAY_NEXT);
            }
        }
    }

}
