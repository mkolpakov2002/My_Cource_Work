package com.mmkolpakov.mycourcework;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        isMusicPlaying = false;

    }
    public void retToMainActivityVoid(View v) {
        finish();
    }
    boolean isMusicPlaying;
    public void playMusicVoid(View v){
        if (!isMusicPlaying){
        Intent playMusicService=new Intent(this, MyService.class);
        startService(playMusicService);
        isMusicPlaying = true;
        Toast outputInfoToast = Toast.makeText(this, "Запуск пасхалки!",Toast.LENGTH_LONG);
        outputInfoToast.show();
        } else {
            stopService(new Intent(this, MyService.class));
            isMusicPlaying = false;
        }
    }
}
