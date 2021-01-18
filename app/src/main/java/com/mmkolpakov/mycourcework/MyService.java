package com.mmkolpakov.mycourcework;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MyService extends Service {

    MediaPlayer player;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //not implemented
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        player = MediaPlayer.create(this, R.raw.hse_anthem);
        player.setLooping(false);
        player.setVolume(100,100);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            player.start();
        }).start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        player.release(); //освобождение используемых проигрывателем ресурсов
    }

}

