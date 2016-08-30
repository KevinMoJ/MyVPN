package com.androapplite.shadowsocks;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import yyf.shadowsocks.service.ShadowsocksVpnService;
import yyf.shadowsocks.utils.Console;
import yyf.shadowsocks.utils.Constants;

/**
 * Created by jim on 16/4/29.
 */
public class ShadowsockServiceHelper {
    public static final void copyFile(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        byte buffer[] = new byte[1024];
        int read = in.read(buffer);
        while(read != -1){
            out.write(buffer, 0, read);
            read = in.read(buffer);
        }
    }

    public static final void checkAndCopyAsset(@NonNull AssetManager assetManager, @NonNull String path){
        String[] filenames = null;
        try {
            filenames = assetManager.list(path);
        } catch(IOException e) {
            Log.e("ss-error", e.getMessage());
        }
        if (filenames != null) {
            for (String filename:filenames) {

                File outFile = new File(Constants.Path.BASE, filename);
                if(!outFile.exists()) {
                    copyAsset(assetManager, path, filename, outFile);
                }else {
                    try {
                        InputStream assetInputStream = assetManager.open(path.isEmpty() ? filename : path + "/" + filename);
                        if(outFile.length() != assetInputStream.available()){
                            copyAsset(assetInputStream, outFile);
                        }
                    } catch (IOException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
                if (!(outFile.canRead() && outFile.canExecute())) {
                    Console.runCommand("chmod 755 " + outFile.getAbsolutePath());
                }
            }
        }
    }

    private static void copyAsset(@NonNull AssetManager assetManager, @NonNull String path, String filename, File outFile) {
        InputStream in = null;
        try {
            in = assetManager.open(path.isEmpty() ? filename : path + "/" + filename);
            copyAsset(in, outFile);

        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    private static void copyAsset(@NonNull InputStream in, @NonNull File outFile){
        try {
            OutputStream out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }



    public static final void startService(@NonNull Context context){
        Intent intent = new Intent(context, ShadowsocksVpnService.class);
        context.startService(intent);
    }

    public static final void bindService(@NonNull Context context, @NonNull ServiceConnection connection){
        Intent intent = new Intent(context, ShadowsocksVpnService.class);
        intent.setAction(Constants.Action.SERVICE);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
}
