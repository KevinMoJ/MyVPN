package yyf.shadowsocks;

import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.Config;
interface IShadowsocksService {
  int getMode();
  int getState();

  oneway void registerCallback(IShadowsocksServiceCallback cb);
  oneway void unregisterCallback(IShadowsocksServiceCallback cb);

  oneway void start(in Config config);
  oneway void stop();

  long getTxTotalMonthly();
  long getRxTotalMonthly();

  oneway void enableNotification(boolean enable);
  oneway void setRemainTime(int remain);
}
