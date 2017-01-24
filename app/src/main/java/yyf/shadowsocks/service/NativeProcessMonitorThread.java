package yyf.shadowsocks.service;

import com.androapplite.vpn3.BuildConfig;
import com.androapplite.shadowsocks.ShadowsocksApplication;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by jim on 16/8/20.
 */

public class NativeProcessMonitorThread extends Thread {
    private static final int CODE = 1437;
    private static final String CMD = "ps | grep /" + BuildConfig.APPLICATION_ID;
    private final Shell.OnCommandResultListener mCommandResultListener;
    private BaseService mService;
    private boolean isRunning = true;

    public NativeProcessMonitorThread(BaseService  service) {
        super(NativeProcessMonitorThread.class.getSimpleName());
        mService = service;
        mCommandResultListener = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if(output.size() != 4){
                    mService.stopRunner();
                }
            }
        };
    }

    public void stopThread() {
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning){
            try{
                sleep(1000);
                Shell.Builder builder = new Shell.Builder();
                Shell.Interactive shell = builder.useSH().setWatchdogTimeout(10).open();
                shell.addCommand(CMD, CODE, mCommandResultListener);
                shell.waitForIdle();
                shell.close();
            }catch (InterruptedException e){
                isRunning = false;
            }

        }
    }
}
