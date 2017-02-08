package com.androapplite.shadowsocks.activity;

import android.app.AlarmManager;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.androapplite.shadowsocks.R;

import java.util.ArrayList;

public class CheckInActivity extends AppCompatActivity {
    private ArrayList<ImageView> mSmileImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        LinearLayout checkin = (LinearLayout)findViewById(R.id.checkin);
        mSmileImages = new ArrayList<>(checkin.getChildCount());
        for(int i=0; i<checkin.getChildCount(); i++){
            LinearLayout linearLayout = (LinearLayout)checkin.getChildAt(i);
            mSmileImages.add((ImageView)linearLayout.getChildAt(1));
        }

        mSmileImages.get(1).setImageLevel(1);
    }
}
