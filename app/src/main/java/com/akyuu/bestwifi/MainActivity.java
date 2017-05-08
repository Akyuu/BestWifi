package com.akyuu.bestwifi;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private List<ScanResult> mScanResults = new ArrayList<>();
    private List<Wifi> mWifiList = new ArrayList<>();

    // View
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WifiAdapter mAdapter;

    // Toolbar View
    private Switch mSwitch;

    private WifiService.WifiBinder mBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mWifiList = SQLite.select().from(Wifi.class).queryList();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_wifi);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new WifiAdapter();
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateResultsAndCheckPermission();
            }
        });
        updateResultsAndCheckPermission();

        bindService(new Intent(this, WifiService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mBinder = (WifiService.WifiBinder) iBinder;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }

        }, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        mSwitch = new Switch(this);
        mSwitch.setText(getString(R.string.auto_switch));
        mSwitch.setTextColor(0);
        menu.findItem(R.id.menu_switch).setActionView(mSwitch);
        mSwitch.setChecked(mBinder.isAutoSwitchRunning());
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mBinder.startAutoSwitch();
                } else {
                    mBinder.stopAutoSwitch();
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setting:
                Intent i = SettingActivity.newIntent(this);
                startActivity(i);
                break;
            default:
                break;
        }
        return true;
    }

    private void updateResultsAndCheckPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ) {
            updateResults();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length >= 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateResults();
            } else {
                ToastUtil.showToast(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT);
            }
        }
    }

    private void updateResults() {
        WifiManager manager =
                (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        manager.startScan();
        mScanResults = manager.getScanResults();
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    class WifiHolder extends RecyclerView.ViewHolder {
        private TextView mSsidTextView;
        private TextView mBssidTextView;
        private TextView mStrengthTextView;
        private CheckBox mAvailableCheckBox;

        WifiHolder(View itemView) {
            super(itemView);
            mSsidTextView = (TextView) itemView.findViewById(R.id.tv_ssid);
            mBssidTextView = (TextView) itemView.findViewById(R.id.tv_bssid);
            mStrengthTextView = (TextView) itemView.findViewById(R.id.tv_strength);
            mAvailableCheckBox = (CheckBox) itemView.findViewById(R.id.cb_available);
        }

        void bindView(final ScanResult result) {
            setIsRecyclable(false);
            mSsidTextView.setText(result.SSID);
            mBssidTextView.setText(String.format("BSSID: %s", result.BSSID));
            mStrengthTextView.setText(
                    String.format(Locale.getDefault(), "Strength: %d dbm", result.level));
            mAvailableCheckBox.setChecked(isWifiSelected(result));
            mAvailableCheckBox.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if (b) {
                                Wifi wifi = new Wifi();
                                wifi.SSID = result.SSID;
                                wifi.BSSID = result.BSSID;
                                wifi.save();
                                mWifiList.add(wifi);
                            } else {
                                Wifi wifi = SQLite.select().from(Wifi.class)
                                        .where(Wifi_Table.SSID.eq(result.SSID))
                                        .and(Wifi_Table.BSSID.eq(result.BSSID))
                                        .querySingle();
                                if (wifi != null) {
                                    for (Wifi oneWifi : mWifiList) {
                                        if (oneWifi.SSID.equals(wifi.SSID) &&
                                                oneWifi.BSSID.equals(wifi.BSSID)) {
                                            mWifiList.remove(oneWifi);
                                            break;
                                        }
                                    }
                                    wifi.delete();
                                }
                            }
                        }
                    });
        }
    }

    private boolean isWifiSelected(ScanResult result) {
        for (Wifi wifi : mWifiList) {
            if (result.SSID.equals(wifi.SSID) && result.BSSID.equals(wifi.BSSID)) {
                return true;
            }
        }
        return false;
    }

    private class WifiAdapter extends RecyclerView.Adapter<WifiHolder> {
        @Override
        public WifiHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_wifi, parent, false);
            return new WifiHolder(v);
        }

        @Override
        public void onBindViewHolder(WifiHolder holder, int position) {
            holder.bindView(mScanResults.get(position));
        }

        @Override
        public int getItemCount() {
            return mScanResults.size();
        }
    }
}
