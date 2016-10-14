package yyf.shadowsocks.utils;

import android.os.Environment;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsocksApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by jim on 16/8/24.
 */

public class GuardedProcess extends Process {
    private static final String TAG = GuardedProcess.class.getSimpleName();
    private volatile Thread guardThread;
    private volatile boolean isDestroyed;
    private volatile Process process;
    private volatile boolean isRestart;
    private List<String> cmdPars;
    private String cmd;

    public interface OnRestartCallback{
        void callback();
    }

    public GuardedProcess(String cmd){
        cmdPars = Arrays.asList(cmd.split("\\s+"));
        this.cmd = cmd;
    }

    public GuardedProcess(String[] cmds){
        cmdPars = Arrays.asList(cmds);
        StringBuilder sb = new StringBuilder();
        for(String s:cmds){
            sb.append(s).append(" ");
        }
        cmd = sb.toString();
    }

    public GuardedProcess(List<String> cmds){
        cmdPars = cmds;
        StringBuilder sb = new StringBuilder();
        for(String s:cmds){
            sb.append(s).append(" ");
        }
        cmd = sb.toString();
    }

    public GuardedProcess start(final OnRestartCallback callback){
        final Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();
            guardThread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
//                            if(cmdPars == null || cmdPars.isEmpty()) return;
                            try {
                                while (!isDestroyed) {
                                    Log.i(TAG, "start process: " + cmd);
                                    String username = System.getProperty("user.name");
                                    String userId = System.getProperty("userid");
                                    System.out.println(System.getenv());
                                    Log.i(TAG, username + " " + userId);
                                    long startTime = System.currentTimeMillis();

                                    final ProcessBuilder processBuilder = new ProcessBuilder(cmdPars).redirectErrorStream(true);
                                    process = processBuilder.start();
                                    new StreamGobbler(process.getInputStream(), "OUTPUT").start();
                                    new StreamGobbler(process.getErrorStream(), "ERROR").start();
                                    if (callback != null) {
                                        callback.callback();
                                    }
                                    semaphore.release();
                                    int exitValue = process.waitFor();
                                    Log.i(TAG, "exitValue " +  exitValue + " " + cmd);

                                    if(isRestart){
                                        isRestart = false;
                                    }else{
                                        if(System.currentTimeMillis() - startTime < 1000){
                                            Log.w(TAG, "process exit too fast, stop guard: " + cmd);
                                            isDestroyed = true;
                                        }
                                    }
                                }
                            }catch (InterruptedException e) {
                                Log.i(TAG, "thread interrupt, destroy process: " + cmd);
//                                destroyProcess();
                                process.destroy();
                            }catch (IOException e){
                                ShadowsocksApplication.handleException(e);
                            }finally {
                                semaphore.release();
                            }
                        }
                    }
            , "GuardThread-" + cmd);
            guardThread.start();
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    private void destroyProcess(){
        new StreamGobbler(process.getInputStream(), "OUTPUT").start();
        new StreamGobbler(process.getErrorStream(), "ERROR").start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            ShadowsocksApplication.handleException(e);
        }
        process.destroy();
    }

    @Override
    public void destroy() {
        isDestroyed = true;

        guardThread.interrupt();
//        destroyProcess();
        process.destroy();
        try {
            guardThread.join();
        } catch (InterruptedException e) {
            ShadowsocksApplication.handleException(e);
        }
        Log.i(TAG, "GuardProcess Destroy " + cmd);
    }

    public void restart(){
        isRestart = true;
        process.destroy();
    }

    @Override
    public int exitValue() {
        return 0;
    }

    @Override
    public InputStream getErrorStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int waitFor() throws InterruptedException {
        guardThread.join();
        return 0;
    }

    class StreamGobbler extends Thread {
        InputStream is;
        String type;

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null)
                    System.out.println(type + ">" + line);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }finally {
                try {
                    is.close();
                }catch (IOException e){
                    ShadowsocksApplication.handleException(e);
                }
            }
        }
    }
}
