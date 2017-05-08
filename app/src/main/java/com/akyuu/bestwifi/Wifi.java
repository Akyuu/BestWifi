package com.akyuu.bestwifi;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = AppDatabase.class)
public class Wifi extends BaseModel {
    @PrimaryKey(autoincrement = true)
    long id; // package-private recommended, not required

    @Column
    String SSID;

    @Column
    String BSSID;
}
