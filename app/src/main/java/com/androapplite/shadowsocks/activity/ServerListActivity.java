package com.androapplite.shadowsocks.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ServerListFetcherService;

import java.util.ArrayList;
import java.util.HashMap;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;

public class ServerListActivity extends BaseShadowsocksActivity implements
        SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener,
        ServiceConnection, DialogInterface.OnClickListener{
    private IShadowsocksService mShadowsocksService;


    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SharedPreferences mPreferences;
    private ArrayList<String> mNations;
    private ArrayList<String> mFlags;
    private HashMap<String, Integer> mSignalResIds;
    private ListView mListView;
    private String mNation;
    private int mSelectedIndex;
    private boolean mHasServerJson;
    private IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_purple, android.R.color.holo_blue_bright, android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mListView = (ListView)findViewById(R.id.vpn_server_list);
        mListView.setAdapter(new ServerListAdapter());
        mListView.setOnItemClickListener(this);

        initForegroundBroadcastIntentFilter();
        initForegroundBroadcastReceiver();

        parseServerList();
        ShadowsockServiceHelper.bindService(this, this);
        mShadowsocksServiceCallbackBinder = createShadowsocksServiceCallbackBinder();

        mHasServerJson = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).contains(SharedPreferenceKey.SERVER_LIST);
        GAHelper.sendScreenView(this, "服务器列表屏幕");
    }

    private void parseServerList() {
        mNations = new ArrayList<>();
        mFlags = new ArrayList<>();
        mSignalResIds = new HashMap<>();
        mPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mNation = mPreferences.getString(SharedPreferenceKey.VPN_NATION, getString(R.string.vpn_nation_opt));

        ArrayList<ServerConfig> serverConfigs = null;
        String serverListJson = mPreferences.getString(SharedPreferenceKey.SERVER_LIST, null);
        if(serverListJson != null){
            serverConfigs = ServerConfig.createServerList(this, serverListJson);
        }
        if(serverConfigs == null || serverConfigs.isEmpty()){
            serverConfigs = ServerConfig.createDefaultServerList(getResources());
        }
        for(ServerConfig serverConfig:serverConfigs){
            if(!mNations.contains(serverConfig.nation)){
                mNations.add(serverConfig.nation);
                mFlags.add(serverConfig.flag);
                mSignalResIds.put(serverConfig.nation, serverConfig.getSignalResId());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            finish();
            return true;
        }else if(itemId == R.id.menu_repair){
            if(mShadowsocksService != null){
                try {
                    int s = mShadowsocksService.getState();
                    Constants.State state = Constants.State.values()[s];
                    if(state == Constants.State.CONNECTED){
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.disconnect_to_refresh)
                                .setPositiveButton(R.string.disconnect, this)
                                .setNegativeButton(android.R.string.cancel, this)
                                .show();
                    }else{
                        mSwipeRefreshLayout.setRefreshing(true);
                        ServerListFetcherService.fetchServerListAsync(this);
                    }
                    GAHelper.sendEvent(this, "刷新服务器列表", "菜单", state.name());
                } catch (RemoteException e) {
                    ShadowsocksApplication.handleException(e);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {

        ServerListFetcherService.fetchServerListAsync(this);
    }

    private void initForegroundBroadcastIntentFilter(){
        mForgroundReceiverIntentFilter = new IntentFilter();
        mForgroundReceiverIntentFilter.addAction(Action.SERVER_LIST_FETCH_FINISH);
    }

    private void initForegroundBroadcastReceiver(){
        mForgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch(action){
                    case Action.SERVER_LIST_FETCH_FINISH:
                        mSwipeRefreshLayout.setRefreshing(false);
                        parseServerList();
                        mHasServerJson = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context).contains(SharedPreferenceKey.SERVER_LIST);
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
            if(nation.equals(mNation)) {
                holder.mItemView.setSelected(true);
                mSelectedIndex = position;
            }else{
                holder.mItemView.setSelected(false);
            }
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
        View selectedView = parent.getChildAt(mSelectedIndex);
        ServerListAdapter.ViewHolder holder = (ServerListAdapter.ViewHolder) selectedView.getTag();
        holder.mItemView.setSelected(false);

        String nation = mNations.get(position);
        String flag = mFlags.get(position);
        mPreferences.edit().putString(SharedPreferenceKey.VPN_NATION, nation)
                .putString(SharedPreferenceKey.VPN_FLAG, flag)
                .apply();
        setResult(RESULT_OK);
        finish();
        GAHelper.sendEvent(this, "选择国家", nation);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mShadowsocksService = IShadowsocksService.Stub.asInterface(service);

        try {
            mShadowsocksService.registerCallback(mShadowsocksServiceCallbackBinder);
            int s = mShadowsocksService.getState();
            Constants.State state = Constants.State.values()[s];
            if(state == Constants.State.INIT || state == Constants.State.STOPPED || state == Constants.State.ERROR){
                ServerListFetcherService.fetchServerListAsync(this);
            }
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mShadowsocksService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mShadowsocksService != null){
            try {
                mShadowsocksService.unregisterCallback(mShadowsocksServiceCallbackBinder);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        unbindService(this);

    }

    private IShadowsocksServiceCallback.Stub createShadowsocksServiceCallbackBinder(){
        return new IShadowsocksServiceCallback.Stub(){
            @Override
            public void stateChanged(int state, String msg) throws RemoteException {
                Constants.State s = Constants.State.values()[state];
                if(s == Constants.State.INIT || s == Constants.State.STOPPED || s == Constants.State.ERROR){
                    ServerListFetcherService.fetchServerListAsync(ServerListActivity.this);
                }
            }

            @Override
            public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
            }
        };
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Toast.makeText(this, "which " + which, Toast.LENGTH_SHORT).show();
    }
}
