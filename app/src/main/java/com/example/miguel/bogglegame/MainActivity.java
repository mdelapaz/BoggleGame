package com.example.miguel.bogglegame;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.Set;

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

    //multiplayer things
    private boolean is_host;
    private BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devices;
    private BluetoothService btService;
    private Handler mHandler;
    private String mConnectedDeviceName = null;
    private boolean requestingBoard = false;
    private BoggleMessage boardFromHost;
    private boolean readyToPlay = false;
    private String []words;

    //gui objects declared here for refresh
    GridView gridview;
    TextView scoreview;
    TextView currentWord;
    TextView foundWords;

    private ArrayAdapter<String> adapter;


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //read words from dictionary.txt
        String text = "";
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

        System.out.println("At least things are printing...");
        //if 2p, try to set up multiplayer connection
        if(mode != GameMode.SinglePlayer) {
            System.out.println("Multiplayer game initiated");
            //are we hosting this game?
            is_host = getIntent().getBooleanExtra("EXTRA_IS_HOST", false);
            //is bluetooth enabled?
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter == null) {
                Toast.makeText(getApplicationContext(), "This device is not bluetooth capable.", Toast.LENGTH_SHORT).show();
                finish();
            }
            if(!btAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 1);
            }
            //make handler
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case BluetoothService.MESSAGE_STATE_CHANGE:
                            switch (msg.arg1) {
                                case BluetoothService.STATE_CONNECTED:
                                    break;
                                case BluetoothService.STATE_CONNECTING:
                                    break;
                                case BluetoothService.STATE_LISTEN:
                                case BluetoothService.STATE_NONE:
                                    break;
                            }
                            break;
                        case BluetoothService.MESSAGE_WRITE:
                            //byte[] writeBuf = (byte[]) msg.obj;
                            // construct a string from the buffer
                            //String writeMessage = new String(writeBuf);
                            // Toast.makeText(getApplicationContext(), "Message Sent:  " + writeMessage, Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.MESSAGE_READ:
                            byte[] readBuf = (byte[]) msg.obj;
                            dealWithMessage(readBuf);
                            // construct a string from the valid bytes in the buffer
                            //String readMessage = new String(readBuf, 0, msg.arg1);
                            //Toast.makeText(getApplicationContext(), "Message Received: " + readMessage, Toast.LENGTH_SHORT).show();

                            break;
                        case BluetoothService.MESSAGE_DEVICE_NAME:
                            // save the connected device's name
                            mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                            Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.MESSAGE_TOAST:
                                Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            };
            btService = new BluetoothService(getApplicationContext(),mHandler);
            if(is_host) { //WE ARE THE HOST
                btService.start();
                //need to wait here until client connects
                int lazytimeout = 1000000000;
                while(lazytimeout > 0 && btService.getState() != BluetoothService.STATE_CONNECTED) {
                    lazytimeout--;
                }
                if(btService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(getApplicationContext(), "Could not connect to host", Toast.LENGTH_SHORT).show();
                    btService.stop();
                    finish();
                }
            } else {  //WE ARE THE CLIENT
                requestingBoard = true;
                //look for paired devices
                devices = btAdapter.getBondedDevices();
                //try to connect
                System.out.println("Client trying to connect");
                for(BluetoothDevice device:devices){
                    btService.connect(device);
                    //wait to see what happens
                    while(btService.getState() == BluetoothService.STATE_CONNECTING) {}
                    //connected, we're done
                    if(btService.getState() == BluetoothService.STATE_CONNECTED) {
                        break;
                    }
                }
                //if no connection go back
                if(btService.getState() != BluetoothService.STATE_CONNECTED) {
                    System.out.println("Unable to connect");
                    Toast.makeText(getApplicationContext(), "Could not connect to host", Toast.LENGTH_SHORT).show();
                }
            }
        }

        //board setup
        if(mode == GameMode.SinglePlayer) {
            frontend = new frontend(words, difficulty, mode, getApplicationContext());
        } else {
            if(is_host) {
                frontend = new frontend(words, difficulty, mode, getApplicationContext());
                //send board to client
                boardFromHost = new BoggleMessage(frontend.get_letters(), mode, difficulty);
                btService.write(boardFromHost.output());
            } else { //client
                //create dummy front end until we get response from host
                frontend = new frontend(words, difficulty, mode, getApplicationContext());
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridview = (GridView) findViewById(R.id.LetterGrid);
        final TextView timeview = (TextView) findViewById(R.id.TimeRemaining);
        scoreview = (TextView) findViewById(R.id.CurrentScore);
        final Button clearButton = (Button) findViewById(R.id.ClearButton);
        final Button submitButton = (Button) findViewById(R.id.SubmitButton);
        final Button resetButton = (Button) findViewById(R.id.ResetButton);
        currentWord = (TextView) findViewById(R.id.CurrentWord);
        foundWords = (TextView) findViewById(R.id.foundWords);
        final GridView wordList = (GridView) findViewById(R.id.WordList);

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
                refresh();
                gameTimer = start_timer(timeview, wordList, foundWords, currentWord, submitButton, clearButton);
                HideWordList(wordList, foundWords, currentWord, submitButton, clearButton);
                System.out.println("Restarted the game");
            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, frontend.get_letters());
        gridview.setAdapter(adapter);

        gridview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int currX, currY, position;
                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        position = gridview.pointToPosition((int) event.getX(), (int) event.getY());
                        if(position == -1) return true;
                        TextView child = (TextView) gridview.getChildAt(position);
                        if(frontend.tile_click(position) == true) {
                            if(frontend.tile_state[position] == true) {
                                child.setBackgroundColor(Color.parseColor("#FF0000"));
                            } else {
                                child.setBackgroundColor(Color.parseColor("#FAFAFA"));
                            }
                            currentWord.setText(frontend.get_candidate_word());
                            currPosition = position;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        final int historySize = event.getHistorySize();
                        for (int h = 0; h < historySize; h++) {
                            currX = (int) event.getHistoricalX(h);
                            currY = (int) event.getHistoricalY(h);
                            processDragPoints(gridview, currX, currY, currentWord);
                        }
                        currX = (int) event.getX();
                        currY = (int) event.getY();
                        processDragPoints(gridview, currX, currY, currentWord);
                        break;
                    }
                }
                return true;
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(frontend.clear_click()) {
                    refresh();
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mode == GameMode.SinglePlayer) {
                    if (frontend.submit_click()) {
                        refresh();
                        Toast.makeText(getApplicationContext(), "Valid word submitted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if(is_host) {
                        if (frontend.submit_click()) {
                            refresh();
                            Toast.makeText(getApplicationContext(), "Valid word submitted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                        }
                        //TODO send client word to add to opponent list
                    } else { //client
                        int[] submission = frontend.submit_click_client();
                        if(submission != null) {
                            refresh();
                            //send this to host to verify
                            BoggleMessage submit_msg = new BoggleMessage(MessageType.SubmitWord, submission);
                            btService.write(submit_msg.output());
                        }
                    }
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

    private void processDragPoints(GridView g, int x, int y, TextView currentWord){
        int posToChange;

        int pos = g.pointToPosition(x, y);

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
                    child.setBackgroundColor(Color.parseColor("#FAFAFA"));
                }
                currentWord.setText(frontend.get_candidate_word());
            }
        }
    }

    //refreshes the views according to current game state
    public void refresh() {
        //if we haven't initialized these things dont try to do anything
        if((gridview == null) || (scoreview == null) || (currentWord == null) || (foundWords == null)) {
            return;
        }
        for(int i = 0; i < gridview.getChildCount(); i++) {
            TextView child = (TextView) gridview.getChildAt(i);
            if(frontend.tile_state[i] == true) {
                child.setBackgroundColor(Color.parseColor("#FF0000"));
            } else {
                child.setBackgroundColor(Color.parseColor("#FAFAFA"));
            }
        }
        currentWord.setText(frontend.get_candidate_word());
        scoreview.setText("Score: " + Integer.toString(frontend.boggleBoard.getScore()));
        String found_string = "";
        for(String word : frontend.boggleBoard.validWordsFoundByUser) {
            found_string += word;
            found_string += "  ";
        }
        foundWords.setText(found_string);
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
        List<String> list = frontend.get_high_scores();
        ListView hsList = new ListView(this);
        hsdialog.setView(hsList);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        hsList.setAdapter(adapter);
        hsdialog.show();
    }

    public CountDownTimer start_timer(final TextView timeview, final GridView word_list, final TextView found_words, final TextView current_word, final Button submit_button, final Button clear_button) {
        return new CountDownTimer(180000, 1000) {

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btService != null) {
            btService.stop();
        }
    }

    //method for acting on recieved messages
    private void dealWithMessage(byte[] input) {
        BoggleMessage message = new BoggleMessage(input);
        switch (message.type) {
            case MessageType.SupplyBoard:
                boardFromHost = message;
                requestingBoard = false;
                mode = boardFromHost.mode;
                String[] letters = boardFromHost.letters;
                difficulty = boardFromHost.difficulty;
                frontend = new frontend(letters, words, difficulty, mode, getApplicationContext());
                for(int i = 0; i < gridview.getChildCount(); i++) {
                    TextView child = (TextView) gridview.getChildAt(i);
                    child.setText(letters[i]);
                }
                refresh();
                break;
            case MessageType.AcceptWord: //host accepted word
                if(!is_host) { //client
                    if (frontend.submit_click_client_accepted(message.word_submission)) {
                        refresh();
                        Toast.makeText(getApplicationContext(), "Valid word submitted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case MessageType.SubmitWord: //client submitted a word
                if(is_host) {
                    //TODO code to verify client word and then message back
                }
                break;

            //add more message types
        }
    }


}
