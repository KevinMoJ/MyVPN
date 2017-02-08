package com.androapplite.shadowsocks.activity;

import android.app.AlarmManager;
import android.content.SharedPreferences;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;

import java.util.ArrayList;

public class CheckInActivity extends AppCompatActivity {
    private ArrayList<ImageView> mSmileImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        LinearLayout checkin = (LinearLayout)findViewById(R.id.checkin);
        mSmileImages = new ArrayList<>(checkin.getChildCount());
        for(int i=0; i<checkin.getChildCount(); i++){
            LinearLayout linearLayout = (LinearLayout)checkin.getChildAt(i);
            mSmileImages.add((ImageView)linearLayout.getChildAt(1));
        }

        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        int continousCheckIn = sharedPreferences.getInt(SharedPreferenceKey.CONITNUOUSE_CHECK_IN, 0);
        for(int i=0; i<continousCheckIn; i++){
            mSmileImages.get(i).setImageLevel(1);
        }

        TextView checkinSummaryTextView = (TextView)findViewById(R.id.checkin_summary);
        checkinSummaryTextView.setText(String.format("You have checked in continuously for %d day(s).", continousCheckIn));
        GAHelper.sendScreenView(this, "签到屏幕");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
