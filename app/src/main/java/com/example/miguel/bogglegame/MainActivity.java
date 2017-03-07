package com.example.miguel.bogglegame;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    frontend frontend = null;
    private SensorManager sManager;
    private Sensor mAccelerometer;
    private ShakeListener mShakeListener;
    private int difficulty;
    private GameMode mode;
    private int currPosition = -1;
    CountDownTimer gameTimer = null;
    AlertDialog.Builder hsdialog;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //read words from dictionary.txt
        String text = "";
        final String []words;
        try {
            InputStream is = getAssets().open("dictionary.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            text = new String(buffer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        words = text.split("\r\n");

        mode = (GameMode) getIntent().getSerializableExtra("EXTRA_MODE");
        difficulty = getIntent().getIntExtra("EXTRA_DIFFICULTY", 0);

        frontend = new frontend(words, difficulty, mode, getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridView gridview = (GridView) findViewById(R.id.LetterGrid);
        final TextView timeview = (TextView) findViewById(R.id.TimeRemaining);
        final TextView scoreview = (TextView) findViewById(R.id.CurrentScore);
        final Button clearButton = (Button) findViewById(R.id.ClearButton);
        final Button submitButton = (Button) findViewById(R.id.SubmitButton);
        final Button resetButton = (Button) findViewById(R.id.ResetButton);
        final TextView currentWord = (TextView) findViewById(R.id.CurrentWord);
        final TextView foundWords = (TextView) findViewById(R.id.foundWords);
        final GridView wordList = (GridView) findViewById(R.id.WordList);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, frontend.get_letters());

        CreateHighScoreDialog();
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeListener = new ShakeListener();
        mShakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                gameTimer.cancel();
                frontend = new frontend(words, difficulty, mode, getApplicationContext());
                String[] letters = frontend.get_letters();
                for(int i = 0; i < gridview.getChildCount(); i++) {
                    TextView child = (TextView) gridview.getChildAt(i);
                    child.setText(letters[i]);
                }
                refresh(gridview, currentWord, scoreview, foundWords);
                gameTimer = start_timer(timeview, wordList, foundWords, currentWord, submitButton, clearButton);
                HideWordList(wordList, foundWords, currentWord, submitButton, clearButton);
                System.out.println("Restarted the game");
            }
        });

        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if(frontend.tile_click(position) == true) {
                    if(frontend.tile_state[position] == true) {
                        v.setBackgroundColor(Color.parseColor("#FF0000"));
                    } else {
                        v.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }
                    currentWord.setText(frontend.get_candidate_word());
                }
            }
        });

        gridview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int currX, currY;
                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        currX = (int) event.getX();
                        currY = (int) event.getY();
                        processDragPoints(gridview,gridview.pointToPosition(currX, currY), currentWord);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        final int historySize = event.getHistorySize();
                        for (int h = 0; h < historySize; h++) {
                            currX = (int) event.getHistoricalX(h);
                            currY = (int) event.getHistoricalY(h);
                            processDragPoints(gridview, gridview.pointToPosition(currX, currY), currentWord);
                        }
                        currX = (int) event.getX();
                        currY = (int) event.getY();
                        processDragPoints(gridview, gridview.pointToPosition(currX, currY), currentWord);
                        break;
                    }
                }
                return true;
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.clear_click()) {
                    refresh(gridview, currentWord, scoreview, foundWords);
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.submit_click()) {
                    refresh(gridview, currentWord, scoreview, foundWords);
                    Toast.makeText(getApplicationContext(), "Valid word submitted!",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        gameTimer = start_timer(timeview, wordList, foundWords, currentWord, submitButton, clearButton);
        HideWordList(wordList, foundWords, currentWord, submitButton, clearButton);
    }

    private void processDragPoints(GridView g, int pos, TextView currentWord){
        int posToChange;

        if(pos == -1) return;
        if(currPosition == -1 || pos != currPosition) {
            System.out.printf("currPosition = %d, pos = %d\n", currPosition, pos);
            System.out.printf("last_click = %d\n", frontend.last_click);
            if(frontend.last_click > 0 && pos == frontend.current_submission[frontend.last_click-1]){
                posToChange = currPosition;
                currPosition = pos;
            }
            else {
                posToChange = pos;
                currPosition = pos;
            }
            TextView child = (TextView) g.getChildAt(posToChange);
            if (frontend.tile_click(posToChange) == true) {
                if (frontend.tile_state[posToChange] == true) {
                    child.setBackgroundColor(Color.parseColor("#FF0000"));
                } else {
                    child.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
                currentWord.setText(frontend.get_candidate_word());
            }
        }
    }

    //refreshes the views according to current game state
    public void refresh(GridView gridview, TextView current_word, TextView current_score, TextView words_found) {
        for(int i = 0; i < gridview.getChildCount(); i++) {
            TextView child = (TextView) gridview.getChildAt(i);
            if(frontend.tile_state[i] == true) {
                child.setBackgroundColor(Color.parseColor("#FF0000"));
            } else {
                child.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        current_word.setText(frontend.get_candidate_word());
        current_score.setText("Score: " + Integer.toString(frontend.boggleBoard.getScore()));
        String found_string = "";
        for(String word : frontend.boggleBoard.validWordsFoundByUser) {
            found_string += word;
            found_string += "  ";
        }
        words_found.setText(found_string);
    }

    public void CreateNameDialog(){
        AlertDialog.Builder dialog;
        dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enter your name");
        final EditText nameInput = new EditText(this);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        dialog.setView(nameInput);

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                frontend.send_score_name(nameInput.getText().toString());
                ShowHighScores();
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.cancel();
            }
        });
        dialog.show();
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
        List<String> list = frontend.get_high_scores();
        ListView hsList = new ListView(this);
        hsdialog.setView(hsList);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        hsList.setAdapter(adapter);
        hsdialog.show();
    }

    public CountDownTimer start_timer(final TextView timeview, final GridView word_list, final TextView found_words, final TextView current_word, final Button submit_button, final Button clear_button) {
        return new CountDownTimer(18000, 1000) {

            public void onTick(long millisUntilFinished) {
                timeview.setText("Time Left: " + (millisUntilFinished / 1000) + " s");
            }

            public void onFinish() {
                timeview.setText("Game Over");
                ShowWordList(word_list, found_words, current_word, submit_button, clear_button);
                if(frontend.end_game()){
                    CreateNameDialog();
                }
            }
        }.start();
    }

    public void ShowWordList(final GridView word_list, final TextView found_words, final TextView current_word, final Button submit_button, final Button clear_button){
        word_list.setVisibility(View.VISIBLE);
        found_words.setVisibility(View.INVISIBLE);
        current_word.setVisibility(View.INVISIBLE);
        submit_button.setVisibility(View.INVISIBLE);
        clear_button.setVisibility(View.INVISIBLE);
        List<String> list = new ArrayList<>(frontend.boggleBoard.validWordsOnBoard);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        word_list.setAdapter(adapter);
    }

    public void HideWordList(final GridView word_list, final TextView found_words, final TextView current_word, final Button submit_button, final Button clear_button){
        word_list.setVisibility(View.INVISIBLE);
        found_words.setVisibility(View.VISIBLE);
        current_word.setVisibility(View.VISIBLE);
        submit_button.setVisibility(View.VISIBLE);
        clear_button.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the Session Manager Listener onResume
        sManager.registerListener(mShakeListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        // Unregister the Sensor Manager onPause
        sManager.unregisterListener(mShakeListener);
        super.onPause();
    }
}
