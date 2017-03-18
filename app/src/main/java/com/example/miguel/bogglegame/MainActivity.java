package com.example.miguel.bogglegame;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.ViewGroup;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    frontend frontend = null;
    private SensorManager sManager;
    private Sensor mAccelerometer;
    private ShakeListener mShakeListener;
    private int difficulty;
    private GameMode mode;
    private boolean is_multi_round;
    private int currPosition = -1;
    CountDownTimer gameTimer = null;
    long timeLeft = -1;
    static final long maxTime = 180000;

    //multiplayer things
    private boolean is_host;
    private BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devices;
    private BluetoothService btService;
    private Handler mHandler;
    private String mConnectedDeviceName = null;
    private BoggleMessage boardFromHost;
    private String []words;

    private boolean waiting = false;
    private boolean opponentDone = false;
    private long savedTime;
    private int savedTotalScore;


    // gui objects declared here
    private GridView gridview;
    private TextView scoreview;
    private TextView currentWord;
    private TextView foundWords;
    private TextView timeview;
    private Button clearButton;
    private Button submitButton;
    private Button resetButton;
    private GridView wordList;

    private ArrayAdapter<String> adapter;


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        is_multi_round = getIntent().getBooleanExtra("EXTRA_MULTIROUND", false);



        System.out.println("At least things are printing...");
        //if 2p, try to set up multiplayer connection
        if(mode != GameMode.SinglePlayer) {
            initializeBluetooth();
        }

        // board setup...for multiplayer, this is a dummy board until the connection is established
        frontend = new frontend(words, difficulty, mode, getApplicationContext());

        gridview = (GridView) findViewById(R.id.LetterGrid);
        timeview = (TextView) findViewById(R.id.TimeRemaining);
        scoreview = (TextView) findViewById(R.id.CurrentScore);
        clearButton = (Button) findViewById(R.id.ClearButton);
        submitButton = (Button) findViewById(R.id.SubmitButton);
        resetButton = (Button) findViewById(R.id.ResetButton);
        currentWord = (TextView) findViewById(R.id.CurrentWord);
        foundWords = (TextView) findViewById(R.id.foundWords);
        wordList = (GridView) findViewById(R.id.WordList);

        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeListener = new ShakeListener();
        mShakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                if(mode != GameMode.SinglePlayer) return;
                gameTimer.cancel();
                frontend = new frontend(words, difficulty, mode, getApplicationContext());
                String[] letters = frontend.get_letters();
                redrawBoard(letters);
                refresh();
                gameTimer = start_timer(maxTime);
                HideWordList();
                System.out.println("Restarted the game");
            }
        });

        //if multiround use the menu button as an end round button
        if(is_multi_round) {
            resetButton.setText("End Round");
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, frontend.get_letters());
        gridview.setAdapter(adapter);

        gridview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(waiting) {
                    return true;
                }
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
                            processDragPoints(currX, currY);
                        }
                        currX = (int) event.getX();
                        currY = (int) event.getY();
                        processDragPoints(currX, currY);
                        break;
                    }
                }
                return true;
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(waiting) {
                    return;
                }
                if(frontend.clear_click()) {
                    refresh();
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(waiting) {
                    return;
                }
                if(mode == GameMode.SinglePlayer || mode == GameMode.BasicTwoPlayer) {
                    int submissionLength = frontend.last_click+1;
                    int[] submission = frontend.current_submission;
                    if (frontend.submit_click()) {
                        if (mode == GameMode.BasicTwoPlayer){
                            BoggleMessage submit_msg = new BoggleMessage(MessageType.SubmitWord, submission, submissionLength);
                            btService.write(submit_msg.output());
                        }
                        refresh();
                        Toast.makeText(getApplicationContext(), "Valid word submitted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                    }
                } else {  // CutthroatTwoPlayer
                    if(is_host) {
                        int submissionLength = frontend.last_click+1;
                        int[] submission = frontend.current_submission;
                        if (frontend.submit_click()) {
                            if(submission != null) {
                                BoggleMessage submit_msg = new BoggleMessage(MessageType.SubmitWord, submission, submissionLength);
                                btService.write(submit_msg.output());
                            }
                            refresh();
                            Toast.makeText(getApplicationContext(), "Valid word submitted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                        }
                    } else { //client
                        int submissionLength = frontend.last_click + 1;
                        int[] submission = frontend.submit_click_client();
                        if (submission != null) {
                            refresh();
                            //send this to host to verify
                            BoggleMessage submit_msg = new BoggleMessage(MessageType.SubmitWord, submission, submissionLength);
                            btService.write(submit_msg.output());
                        }
                    }
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(waiting) {
                    return;
                }
                if(is_multi_round && !frontend.game_over) {
                    //can't stop until you get 5 words
                    if(frontend.boggleBoard.validWordsFoundByUser.size() < 5) {
                        return;
                    }
                    savedTime = timeLeft + (frontend.boggleBoard.getRoundScore()*1000);
                    gameTimer.cancel();
                    savedTotalScore = frontend.boggleBoard.getScore();

                    if(mode == GameMode.SinglePlayer) {
                        startNewRound();
                    } else {
                        if(is_host) { //if host make new board and sent it to client
                            frontend = new frontend(words, difficulty, mode, getApplicationContext());
                            boardFromHost = new BoggleMessage(MessageType.HostRoundDone, frontend.get_letters());
                            btService.write(boardFromHost.output());
                        }
                        else{  // if client, let the host know we're done
                            BoggleMessage clientDone = new BoggleMessage(MessageType.ClientRoundDone);
                            btService.write(clientDone.output());
                        }

                        if(opponentDone){
                            startNewRound();
                        }
                        else{
                            waiting = true;  //freeze the buttons until the other guy finishes
                        }
                    }
                } else { //in single round go back to splash
                    finish();
                }
            }
        });
        HideWordList();
        if(mode == GameMode.SinglePlayer) startGame();
    }

    private void startNewRound() {

        if(mode == GameMode.SinglePlayer) { //single player
            frontend = new frontend(words, difficulty, mode, getApplicationContext());
            frontend.boggleBoard.add_prev_rounds_score(savedTotalScore);
            redrawBoard(frontend.get_letters());
            startGame(savedTime);
        } else { //multiplayer
            if(is_host) {  //make a new board and send it to client
                frontend.boggleBoard.add_prev_rounds_score(savedTotalScore);
                redrawBoard(frontend.get_letters());
                startGame(savedTime);
            } else { //client should have new board
                String[] letters = boardFromHost.letters;
                frontend = new frontend(letters, words, difficulty, mode, getApplicationContext());
                frontend.boggleBoard.add_prev_rounds_score(savedTotalScore);
                redrawBoard(letters);
                startGame(savedTime);
            }
        }
        waiting = false;
        opponentDone = false;
    }

    private void processDragPoints(int x, int y){
        int posToChange;

        int pos = gridview.pointToPosition(x, y);

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
            TextView child = (TextView) gridview.getChildAt(posToChange);

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

    private void ShowScoreDialog(final boolean highScore){
        AlertDialog.Builder dialog;

        dialog = new AlertDialog.Builder(this);
        switch(mode){
            case SinglePlayer:
                dialog.setMessage(String.format("Your Final Score: %d", frontend.boggleBoard.getScore()));
                break;
            case BasicTwoPlayer:
            case CutThroatTwoPLayer:
                dialog.setMessage(String.format("Your Final Score: %d\nYour Opponent's Score: %d", frontend.boggleBoard.getScore(), frontend.boggleBoard.getClientScore()));
                break;
        }
        if(mode == GameMode.SinglePlayer) {

        }
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
                if(highScore) CreateNameDialog();
            }
        });
        dialog.show();
    }

    private void ShowWinnerDialog(final boolean winner){
        AlertDialog.Builder dialog;

        dialog = new AlertDialog.Builder(this);
        if(winner){
            dialog.setTitle("You win!");
            dialog.setMessage("Your opponent ran out of time!");
        }
        else{
            dialog.setTitle("You lose!");
            dialog.setMessage("Sorry, you ran out of time!");
        }

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void CreateNameDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
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

    public void ShowHighScores(){
        AlertDialog.Builder hsdialog = new AlertDialog.Builder(this);
        hsdialog.setTitle("High Scores");

        hsdialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface hsdialog, int which){
                hsdialog.dismiss();
            }
        });
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

    public CountDownTimer start_timer(long startTime) {
        return new CountDownTimer(startTime, 1000) {

            public void onTick(long millisUntilFinished) {
                timeview.setText("Time Left: " + (millisUntilFinished / 1000) + " s");
                timeLeft = millisUntilFinished;
            }

            public void onFinish() {
                timeview.setText("Game Over");
                resetButton.setText("Menu");
                ShowWordList();
                if(mode == GameMode.SinglePlayer){
                    ShowScoreDialog(frontend.end_game());
                }
                else{
                    BoggleMessage score_msg = new BoggleMessage(MessageType.SendScore, frontend.boggleBoard.getScore());
                    btService.write(score_msg.output());
                    if(is_multi_round){
                      /* In multiple round mode if your timer runs out, that means you lose the game */
                      waiting = false;
                      ShowWinnerDialog(false);
                    }
                }
            }
        }.start();
    }

    public void ShowWordList(){
        wordList.setVisibility(View.VISIBLE);
        foundWords.setVisibility(View.INVISIBLE);
        currentWord.setVisibility(View.INVISIBLE);
        submitButton.setVisibility(View.INVISIBLE);
        clearButton.setVisibility(View.INVISIBLE);
        List<String> list = new ArrayList<>(frontend.boggleBoard.validWordsOnBoard);
        final List<String> myWords = new ArrayList<>(frontend.boggleBoard.validWordsFoundByUser);
        final List<String> opponentsWords = new ArrayList<>(frontend.boggleBoard.wordsFoundByOpponent);
        Collections.sort(list);

        wordList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                if(myWords.contains(text.getText()) && opponentsWords.contains(text.getText())){
                    text.setTextColor(Color.parseColor("#0000FF"));
                }
                else if(myWords.contains(text.getText())){
                    text.setTextColor(Color.parseColor("#00FF00"));
                }
                else if(opponentsWords.contains(text.getText())){
                    text.setTextColor(Color.parseColor("#FF0000"));
                }
                else{
                    text.setTextColor(Color.parseColor("#000000"));
                }

                return view;
            }
        });
    }

    public void HideWordList(){
        wordList.setVisibility(View.INVISIBLE);
        foundWords.setVisibility(View.VISIBLE);
        currentWord.setVisibility(View.VISIBLE);
        submitButton.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.VISIBLE);
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

    //new round
    private void startGame(long startTime){
        gameTimer = start_timer(startTime);
        refresh();
    }

    //new game
    private void startGame() {
        gameTimer = start_timer(maxTime);
        refresh();
    }

    private void initializeBluetooth(){
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
                    case BluetoothService.MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        dealWithMessage(readBuf);
                        break;
                    case BluetoothService.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothService.MESSAGE_TOAST:
                        Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST), Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        };
        btService = new BluetoothService(mHandler);
        if(is_host) { //WE ARE THE HOST
            btService.start();
                /*
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
                */
        } else {  //WE ARE THE CLIENT
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
                    BoggleMessage readyMessage = new BoggleMessage(MessageType.ReadyToStart);
                    btService.write(readyMessage.output());
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

    private void redrawBoard(String[] letters){
        for(int i = 0; i < gridview.getChildCount(); i++) {
            TextView child = (TextView) gridview.getChildAt(i);
            child.setText(letters[i]);
        }
    }

    //method for acting on recieved messages
    private void dealWithMessage(byte[] input) {
        BoggleMessage message = new BoggleMessage(input);
        switch (message.type) {
            case MessageType.SupplyBoard:
                boardFromHost = message;
                mode = boardFromHost.mode;
                String[] letters = boardFromHost.letters;
                difficulty = boardFromHost.difficulty;
                is_multi_round = boardFromHost.is_multiround;
                frontend = new frontend(letters, words, difficulty, mode, getApplicationContext());
                resetButton.setText("End Round");
                redrawBoard(letters);
                startGame();
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
                String word = frontend.tiles_to_word(message.word_submission);
                if(mode == GameMode.CutThroatTwoPLayer) {
                    if (is_host) {
                        int result = frontend.boggleBoard.checkWordAndUpdateScoreCutThroat(word, message.word_submission, true);
                        BoggleMessage reply;
                        if (result > 0) {
                            reply = new BoggleMessage(MessageType.AcceptWord, message.word_submission, message.word_length);
                            btService.write(reply.output());
                            frontend.boggleBoard.addOpponentWord(word);
                            Toast.makeText(getApplicationContext(), String.format("Opponent found: %s", word), Toast.LENGTH_SHORT).show();
                        } else if (result == -1) {
                            reply = new BoggleMessage(MessageType.RejectWordIllegal);
                            btService.write(reply.output());
                        } else {
                            reply = new BoggleMessage(MessageType.RejectWorldAlreadyFound);
                            btService.write(reply.output());
                        }
                    } else { // Client
                        frontend.boggleBoard.addOpponentWord(word);
                        Toast.makeText(getApplicationContext(), String.format("Opponent found: %s", word), Toast.LENGTH_SHORT).show();
                    }
                }
                else { // BasicTwoPlayer
                    frontend.boggleBoard.addOpponentWord(word);
                    Toast.makeText(getApplicationContext(), String.format("Opponent found a word!"), Toast.LENGTH_SHORT).show();
                }
                break;
            case MessageType.RejectWordIllegal:
                Toast.makeText(getApplicationContext(), "Word not valid!", Toast.LENGTH_SHORT).show();
                break;
            case MessageType.RejectWorldAlreadyFound:
                Toast.makeText(getApplicationContext(), "Word already found!", Toast.LENGTH_SHORT).show();
                break;
            case MessageType.SendScore:
                /* This is sent from either the host or the client when their timer runs out
                   for multiple round mode, this means that the opponent has run out of time and
                   lost the game.
                 */
                if(is_multi_round){
                    waiting = false;
                    timeview.setText("Game Over");
                    resetButton.setText("Menu");
                    ShowWordList();
                    gameTimer.cancel();
                    ShowWinnerDialog(true);
                }
                else {
                    frontend.boggleBoard.setClientScore(message.score);
                    ShowScoreDialog(frontend.end_game());
                }
                break;
            case MessageType.ReadyToStart: // Host receives this from client
                frontend = new frontend(words, difficulty, mode, getApplicationContext());
                boardFromHost = new BoggleMessage(frontend.get_letters(), mode, difficulty, is_multi_round);
                btService.write(boardFromHost.output());
                redrawBoard(frontend.get_letters());
                startGame();
                break;
            case MessageType.HostRoundDone: //client recieves this when host ends his round
                //save board
                boardFromHost = message;
                if(waiting) { //client is finished an waiting for host board
                    startNewRound();
                } else {
                    opponentDone = true;
                }
                break;
            case MessageType.ClientRoundDone: //host recieves this when client ends his round
                if(waiting) {
                    startNewRound();
                } else {
                    opponentDone = true;
                }
                break;


            //add more message types
        }
    }


}
