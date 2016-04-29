package com.androapplite.shadowsocks;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
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
    public static final void copyFile(InputStream in,OutputStream out) throws IOException {
        byte buffer[] = new byte[1024];
        int read = in.read(buffer);
        while(read != -1){
            out.write(buffer, 0, read);
            read = in.read(buffer);
        }
    }

    public static final void checkAndCopyAsset(AssetManager assetManager, String path){
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
                    InputStream in = null;
                    try {
                        if (!path.isEmpty()) {
                            in = assetManager.open(path + "/" + filename);
                        } else {
                            in = assetManager.open(filename);
                        }

                        OutputStream out = new FileOutputStream(outFile);
                        copyFile(in, out);
                        in.close();
                        in = null;
                        out.flush();
                        out.close();
                        out = null;

                    } catch (Exception e) {
                        Log.e("ss-error", e.getMessage());
                    }
                }
                if (!(outFile.canRead() && outFile.canExecute())) {
                    Console.runCommand("chmod 755 " + outFile.getAbsolutePath());
                }
            }
        }
    }

    public static final void startService(Context context){
        context.startService(new Intent(context, ShadowsocksVpnService.class));
    }

    public static final void bindService(Context context, ServiceConnection connection){
        Intent intent = new Intent(context, ShadowsocksVpnService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
}
