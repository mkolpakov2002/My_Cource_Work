package com.mmkolpakov.mycourcework;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class MyService extends Service {

    MediaPlayer player;
    public IBinder onBind(Intent arg0) {
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        player.stop(); //остановка плеера
        player.release(); //освобождение используемых проигрывателем ресурсов
    }

}

