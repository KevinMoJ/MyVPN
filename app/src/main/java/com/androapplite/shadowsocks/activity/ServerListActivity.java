package com.androapplite.shadowsocks.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerListActivity extends BaseShadowsocksActivity implements
        SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener,
        DialogInterface.OnClickListener, AbsListView.OnScrollListener, View.OnClickListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SharedPreferences mPreferences;
    private ArrayList<String> mNations;
    private ArrayList<String> mFlags;
    private HashMap<String, Integer> mSignalResIds;
    private ListView mListView;
    private View mTransparentView;
    private String mNation;
    private int mSelectedIndex;
    private boolean mHasServerJson;
    private AlertDialog mDisconnectDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        actionBar.setHomeAsUpIndicator(upArrow);


        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_purple, android.R.color.holo_blue_bright, android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mTransparentView = findViewById(R.id.transparent_view);
        mTransparentView.setOnClickListener(this);

        mListView = (ListView)findViewById(R.id.vpn_server_list);
        mListView.setAdapter(new ServerListAdapter());
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);

        initForegroundBroadcastIntentFilter();
        initForegroundBroadcastReceiver();

        parseServerList();

        String serverList = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).getString(SharedPreferenceKey.FETCH_SERVER_LIST, null);
        if(serverList != null && serverList.length() > 2){
            mHasServerJson = true;
        }else{
            mHasServerJson = false;
        }
//        mHasServerJson = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).contains(SharedPreferenceKey.SERVER_LIST);
        addBottomAd(AdAppHelper.getInstance(this));
        Firebase.getInstance(this).logEvent("屏幕","服务器列表屏幕");
    }

    private void addBottomAd(AdAppHelper adAppHelper) {
        FrameLayout container = (FrameLayout)findViewById(R.id.ad_view_container);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
        try {
//            container.addView(adAppHelper.getNative(), params);
            adAppHelper.getNative(container, params);
            Firebase.getInstance(this).logEvent("NATIVE广告", "显示成功", "服务器列表底部");

        } catch (Exception ex) {
            ex.printStackTrace();
            Firebase.getInstance(this).logEvent("NATIVE广告", "显示失败", "服务器列表底部");

        }

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
        container.startAnimation(animation);
    }

    private void parseServerList() {
        mNations = new ArrayList<>();
        mFlags = new ArrayList<>();
        mSignalResIds = new HashMap<>();
        mPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mNation = mPreferences.getString(SharedPreferenceKey.VPN_NATION, getString(R.string.vpn_nation_opt));

        String serverListJson = mPreferences.getString(SharedPreferenceKey.FETCH_SERVER_LIST, null);
        ArrayList<ServerConfig> serverConfigs = null;
        if(serverListJson != null){
            serverConfigs = ServerConfig.createServerList(this, serverListJson);
        }

        if(serverConfigs != null && !serverConfigs.isEmpty()) {
            for (ServerConfig serverConfig : serverConfigs) {
                if (!mNations.contains(serverConfig.nation)) {
                    mNations.add(serverConfig.nation);
                    mFlags.add(serverConfig.flag);
                    mSignalResIds.put(serverConfig.nation, serverConfig.getSignalResId());
                }
            }
        }

        mSelectedIndex = mNations.indexOf(mNation);
        if(mSelectedIndex == -1) mSelectedIndex = 0;
        mListView.setItemChecked(mSelectedIndex, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            finish();
            return true;
        }else if(itemId == R.id.menu_repair){
            disconnectToRefresh("菜单");
        }
        return super.onOptionsItemSelected(item);
    }

    private void disconnectToRefresh(String position) {
        if (LocalVpnService.IsRunning) {
            showDissConnectDialog();
        } else {
            mSwipeRefreshLayout.setRefreshing(true);
            mTransparentView.setVisibility(View.VISIBLE);
            ServerListFetcherService.fetchServerListAsync(this);
        }
        Firebase.getInstance(this).logEvent("刷新服务器列表", position);
    }

    @Override
    public void onRefresh() {
        disconnectToRefresh("下拉刷新");
    }

    private void initForegroundBroadcastIntentFilter(){
        mForgroundReceiverIntentFilter = new IntentFilter();
        mForgroundReceiverIntentFilter.addAction(Action.SERVER_LIST_FETCH_FINISH);
    }

    private void disconnectVpnServiceAsync() {
        if (LocalVpnService.IsRunning) {
            VpnManageService.stopVpnByUser();
            DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).edit().putInt(SharedPreferenceKey.VPN_STATE, VpnState.Stopped.ordinal()).apply();
        }
    }

    private void showDissConnectDialog() {
        final Firebase firebase = Firebase.getInstance(this);

        mDisconnectDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.server_list_disconnect_title)
                .setPositiveButton(R.string.disconnect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        disconnectVpnServiceAsync();
                        mSwipeRefreshLayout.setRefreshing(true);
                        mTransparentView.setVisibility(View.VISIBLE);
                        ServerListFetcherService.fetchServerListAsync(ServerListActivity.this);
                        firebase.logEvent("服务器列表", "断开链接", "确定");
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        mTransparentView.setVisibility(View.GONE);
                        firebase.logEvent("服务器列表", "断开链接", "取消");
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mDisconnectDialog = null;
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.transparent_view:
                Toast.makeText(this, R.string.updating_please_try_it_later, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initForegroundBroadcastReceiver(){
        mForgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch(action){
                    case Action.SERVER_LIST_FETCH_FINISH:
                        mSwipeRefreshLayout.setRefreshing(false);
                        mTransparentView.setVisibility(View.GONE);
                        parseServerList();
                        String serverList = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context).getString(SharedPreferenceKey.FETCH_SERVER_LIST, null);
                        if(serverList != null && serverList.length() > 2){
                            mHasServerJson = true;
                        }else{
                            mHasServerJson = false;
                        }
                        ((BaseAdapter)mListView.getAdapter()).notifyDataSetChanged();
                        break;
                }
            }
        };
    }

    class ServerListAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return mNations != null ? mNations.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            final Context context = parent.getContext();
            if(convertView != null){
                view = convertView;
                holder = (ViewHolder)view.getTag();
            }else{
                view = View.inflate(context, R.layout.item_popup_vpn_server, null);
                holder = new ViewHolder();
                holder.mFlagImageView = (ImageView)view.findViewById(R.id.vpn_icon);
                holder.mNationTextView = (TextView)view.findViewById(R.id.vpn_name);
                holder.mItemView = view.findViewById(R.id.vpn_server_list_item);
                holder.mSignalImageView =(ImageView)view.findViewById(R.id.signal);
                view.setTag(holder);
            }
            String flag = mFlags.get(position);
            int resid = context.getResources().getIdentifier(flag, "drawable", getPackageName());
            holder.mFlagImageView.setImageResource(resid);
            String nation = mNations.get(position);
            holder.mNationTextView.setText(nation);
//            if(nation.equals(mNation)) {
//                holder.mItemView.setSelected(true);
//                mSelectedIndex = position;
//            }else{
//                holder.mItemView.setSelected(false);
//            }
            if(mHasServerJson) {
                holder.mSignalImageView.setImageResource(mSignalResIds.get(nation));
                holder.mSignalImageView.setVisibility(View.VISIBLE);
            }else{
                holder.mSignalImageView.setVisibility(View.INVISIBLE);
            }
            return view;
        }

        class ViewHolder{
            ImageView mFlagImageView;
            TextView mNationTextView;
            View mItemView;
            ImageView mSignalImageView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_server_list, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        mListView.setItemChecked(mSelectedIndex, false);
        mSelectedIndex = position;
        int resid = mSignalResIds.get(mNations.get(position));
        if (resid == R.drawable.server_signal_full) {
            Toast.makeText(this, R.string.server_list_full_toast, Toast.LENGTH_SHORT).show();
        } else {
            mListView.setItemChecked(position, true);
            String nation = mNations.get(position);
            String flag = mFlags.get(position);
            mPreferences.edit().putString(SharedPreferenceKey.VPN_NATION, nation)
                    .putString(SharedPreferenceKey.VPN_FLAG, flag)
                    .apply();
            setResult(RESULT_OK);
            finish();
            Firebase.getInstance(this).logEvent("选择国家", nation);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE){
            mSwipeRefreshLayout.setRefreshing(true);
            Firebase.getInstance(this).logEvent("刷新服务器列表", "断开");
        }else if(which == DialogInterface.BUTTON_NEGATIVE){
            mSwipeRefreshLayout.setRefreshing(false);
            Firebase.getInstance(this).logEvent("刷新服务器列表", "取消");

        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mSwipeRefreshLayout.setEnabled(scrollState == SCROLL_STATE_IDLE && view.getFirstVisiblePosition() == 0);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }
}
