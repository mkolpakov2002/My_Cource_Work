package com.miem.mmkolpakov.coursework;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class MusicService extends Service {

    //Необязательный сервис - пасхалка, проигрывающая гимн ВШЭ
    static MediaPlayer player;

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
        player.setLooping(true); // Set looping
        player.setVolume(100,100);

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release(); //освобождение используемых проигрывателем ресурсов
    }

}

