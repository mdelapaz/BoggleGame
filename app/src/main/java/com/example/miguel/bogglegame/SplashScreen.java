package com.example.miguel.bogglegame;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.miguel.bogglegame.AppLogic.BoggleBoard;

import java.util.List;

public class SplashScreen extends AppCompatActivity {
    private GameMode mode;
    private int difficulty;
    private boolean is_multi_round;
    private AlertDialog.Builder hsdialog;
    private frontend f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        CreateHighScoreDialog();

        final RadioButton onePlayer = (RadioButton) findViewById(R.id.singlePlayerButton);
        final RadioButton twoPlayer = (RadioButton) findViewById(R.id.twoPlayerButton);
        final RadioButton easyButton = (RadioButton) findViewById(R.id.easyButton);
        final RadioButton mediumButton = (RadioButton) findViewById(R.id.mediumButton);
        final RadioButton hardButton = (RadioButton) findViewById(R.id.hardButton);
        final RadioButton basicButton = (RadioButton) findViewById(R.id.basicButton);
        final RadioButton cutthroatButton = (RadioButton) findViewById(R.id.cutthroatButton);
        final RadioGroup modeGroup = (RadioGroup) findViewById(R.id.modeGroup);
        final RadioButton singleRoundButton = (RadioButton) findViewById(R.id.singleRoundButton);
        final RadioButton multiRoundButton = (RadioButton) findViewById(R.id.multiRoundButton);
        final Button clientButton = (Button) findViewById(R.id.clientButton);
        final Button startButton = (Button) findViewById(R.id.startButton);
        final Button scoresButton = (Button) findViewById(R.id.highScoreButton);

        difficulty = 0;
        mode = GameMode.SinglePlayer;
        is_multi_round = false;

        easyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                difficulty = 0;
            }
        });

        mediumButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                difficulty = 1;
            }
        });

        hardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                difficulty = 2;
            }
        });

        basicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = GameMode.BasicTwoPlayer;
            }
        });

        cutthroatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = GameMode.CutThroatTwoPLayer;
            }
        });

        singleRoundButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_multi_round = false;
            }
        });

        multiRoundButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_multi_round = true;
            }
        });

        onePlayer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                modeGroup.setVisibility(View.INVISIBLE);
                mode = GameMode.SinglePlayer;
                startButton.setText("START");
                clientButton.setVisibility(View.INVISIBLE);
            }
        });

        twoPlayer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                modeGroup.setVisibility(View.VISIBLE);
                startButton.setText("HOST");
                clientButton.setVisibility(View.VISIBLE);
                if(cutthroatButton.isChecked()){
                    mode = GameMode.CutThroatTwoPLayer;
                }
                else{
                    mode = GameMode.BasicTwoPlayer;
                }
            }
        });

        clientButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mode == GameMode.BasicTwoPlayer || mode == GameMode.CutThroatTwoPLayer) {
                    System.out.println("Client button clicked");
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("EXTRA_DIFFICULTY", difficulty);
                    intent.putExtra("EXTRA_MODE", mode);
                    intent.putExtra("EXTRA_IS_HOST", false);
                    startActivity(intent);
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //if two player try to do bluetooth
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("EXTRA_DIFFICULTY", difficulty);
                    intent.putExtra("EXTRA_MODE", mode);
                    intent.putExtra("EXTRA_MULTIROUND", is_multi_round);
                    if(mode == GameMode.BasicTwoPlayer || mode == GameMode.CutThroatTwoPLayer) {
                        intent.putExtra("EXTRA_IS_HOST",true);
                    }
                    startActivity(intent);
            }
        });

        scoresButton.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               f = new frontend(difficulty, mode, getApplicationContext());
               switch(mode){
                   case SinglePlayer:
                       switch(difficulty){
                           case 0:
                               hsdialog.setMessage("Single Player - Easy");
                               break;
                           case 1:
                               hsdialog.setMessage("Single Player - Medium");
                               break;
                           case 2:
                               hsdialog.setMessage("Single Player - Hard");
                               break;
                       }
                       break;
                   case BasicTwoPlayer:
                       switch(difficulty){
                           case 0:
                               hsdialog.setMessage("Two Player Basic - Easy");
                               break;
                           case 1:
                               hsdialog.setMessage("Two Player Basic - Medium");
                               break;
                           case 2:
                               hsdialog.setMessage("Two Player Basic - Hard");
                               break;
                       }
                       break;
                   case CutThroatTwoPLayer:
                       switch(difficulty){
                           case 0:
                               hsdialog.setMessage("Two Player Cutthroat - Easy");
                               break;
                           case 1:
                               hsdialog.setMessage("Two Player Cutthroat - Medium");
                               break;
                           case 2:
                               hsdialog.setMessage("Two Player Cutthroat - Hard");
                               break;
                       }
                       break;
               }
               ShowHighScores();
           }
        });
    }

    public void CreateHighScoreDialog(){
        hsdialog = new AlertDialog.Builder(this);
        hsdialog.setTitle("High Scores");

        hsdialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface hsdialog, int which){
                hsdialog.dismiss();
            }
        });
    }

    public void ShowHighScores(){
        List<String> list = f.get_high_scores();
        ListView hsList = new ListView(this);
        hsdialog.setView(hsList);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        hsList.setAdapter(adapter);
        hsdialog.show();
    }
}
