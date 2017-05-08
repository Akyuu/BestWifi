package com.akyuu.bestwifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WifiService extends Service {

    private WifiManager mWifiManager;
    private AlarmManager mAlarmManager;

    private boolean mIsRunning = false;
    private List<ScanResult> mAvailableResults = new ArrayList<>();
    private SharedPreferences mPreferences;

    private ScanReceiver mScanReceiver;
    private PendingIntent mPendingIntent;

    public WifiService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPreferences = getSharedPreferences(SettingActivity.PREFERENCE_NAME, MODE_PRIVATE);
        mPendingIntent = PendingIntent.getService(
                this, 0, new Intent(this, WifiService.class), 0);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new WifiBinder();
    }

    class WifiBinder extends Binder {
        boolean isAutoSwitchRunning() {
            return mIsRunning;
        }
        void startAutoSwitch() {
            startAuto();
        }
        void stopAutoSwitch() {
            stopAuto();
        }
    }

    private void startAuto() {
        mIsRunning = true;

        showNotification();

        if (mScanReceiver == null) {
            mScanReceiver = new ScanReceiver();
        }
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mScanReceiver, filter);

        int interval = mPreferences.getInt(SettingActivity.KEY_INTERVAL, 15);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + interval * 1000L, mPendingIntent);
    }

    private void stopAuto() {
        mIsRunning = false;
        stopForeground(true);
        unregisterReceiver(mScanReceiver);
        mAlarmManager.cancel(mPendingIntent);
    }

    private void showNotification() {
        Intent i = new Intent(WifiService.this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(WifiService.this, 0, i, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(WifiService.this);
        builder.setContentTitle(getString(R.string.app_name))
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi);
        startForeground(1, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                mWifiManager.startScan();
            }
        }
        if (mIsRunning) {
            int interval = mPreferences.getInt(SettingActivity.KEY_INTERVAL, 15);
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + interval * 1000L, mPendingIntent);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAuto();
    }

    private class ScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateResults();
            connectBestWifi();
        }

        private void updateResults() {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            mAvailableResults.clear();
            for (ScanResult result : scanResults) {
                if (SQLite.select().from(Wifi.class)
                        .where(Wifi_Table.SSID.eq(result.SSID))
                        .querySingle() != null
                        && isWifiConfigured(result)) {
                    mAvailableResults.add(result);
                }
            }
            Collections.sort(mAvailableResults, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult scanResult, ScanResult t1) {
                    return -(scanResult.level - t1.level);
                }
            });
        }

        private boolean isWifiConfigured(ScanResult result) {
            String SSID = '\"' + result.SSID + '\"';
            List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration config : configurations) {
                if (config.SSID.equals(SSID)) {
                    return true;
                }
            }
            return false;
        }

        private void connectBestWifi() {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                int minDifference = mPreferences.getInt(SettingActivity.KEY_DIFFERENCE, 5);
                int minRssi = -mPreferences.getInt(SettingActivity.KEY_STRENGTH, 60);

                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED
                        && wifiInfo.getRssi() < minRssi
                        && mAvailableResults.size() > 0
                        && mAvailableResults.get(0).level - wifiInfo.getRssi() > minDifference) {
                    ScanResult result = mAvailableResults.get(0);
                    String SSID = '\"' + result.SSID + '\"';
                    if (!wifiInfo.getSSID().equals(SSID)) {
                        List<WifiConfiguration> configurations =
                                mWifiManager.getConfiguredNetworks();
                        for (WifiConfiguration config : configurations) {
                            if (config.SSID.equals(SSID)) {
                                mWifiManager.disableNetwork(wifiInfo.getNetworkId());
                                mWifiManager.enableNetwork(config.networkId, true);
                                ToastUtil.showToast(WifiService.this,
                                        String.format("Connect to %s", config.SSID),
                                        Toast.LENGTH_SHORT);
                            }
                        }
                    }
                }
            }
        }
    }
}
