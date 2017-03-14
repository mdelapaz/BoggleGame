package com.example.miguel.bogglegame;

import android.content.Context;

import com.example.miguel.bogglegame.AppLogic.BoggleBoard;
import com.example.miguel.bogglegame.AppLogic.User;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by tpatecky on 2/8/2017.
 */

public class frontend {

    BoggleBoard boggleBoard;
    int boggleBoardLength = 4;
    int difficulty_level = 0;
    GameMode game_mode;

    //data members
    boolean game_over; //true if game has ended (timeout)
    //*************************BackendDummy backend;
    //BoggleBoard backend;  //real backend
    int current_submission[];
    int last_click;
    boolean tile_state[];
    String[] tile_letters;

    //for each grid position, gives an array of adjacent grid positions
    static final int adjacency[][] = {
            {1,4,5},{0,2,4,5,6},{1,3,5,6,7},{2,6,7},
            {0,1,5,8,9},{0,1,2,4,6,8,9,10},{1,2,3,5,7,9,10,11},{2,3,6,10,11},
            {4,5,9,12,13},{4,5,6,8,10,12,13,14},{5,6,7,9,11,13,14,15},{6,7,10,14,15},
            {8,9,13},{8,9,10,12,14},{9,10,11,13,15},{10,11,14}
    };

    public frontend(String[] wordsInDictionary, int difficulty, GameMode mode, Context context){

        game_over = false;
        game_mode = mode;
        difficulty_level = difficulty;
        current_submission = new int[16];
        last_click = -1;
        tile_state = new boolean[16];
        for(int i = 0; i < 16; i++) {
            tile_state[i] = false;
        }

        boggleBoard = new BoggleBoard(boggleBoardLength, wordsInDictionary, difficulty_level, context, mode);
        tile_letters = boggleBoard.exportBoard();
        //backend = new BoggleBoard(boggleBoardLength, wordsInDictionary, difficulty_level);
        //backend = new BackendDummy(boggleBoardLength, wordsInDictionary, difficulty_level);
        //tile_letters = backend.exportBoard();
    }

    //client frontend constructor
    public frontend(String[] letters, String[] wordsInDictionary, int difficulty, GameMode mode, Context context){

        game_over = false;
        game_mode = mode;
        difficulty_level = difficulty;
        current_submission = new int[16];
        last_click = -1;
        tile_state = new boolean[16];
        for(int i = 0; i < 16; i++) {
            tile_state[i] = false;
        }

        //boggleBoard = new BoggleBoard(boggleBoardLength, wordsInDictionary, difficulty_level, context, mode);
        //TODO construct multiplayer client backend using letters param as grid
        char[][] board = new char[4][4];
        for(int i = 0; i < 16; i++) {
            board[i/4][i%4] = letters[i].charAt(0);
        }
        boggleBoard = new BoggleBoard(boggleBoardLength, context, board, difficulty, mode, wordsInDictionary);
        tile_letters = boggleBoard.exportBoard();
        //backend = new BoggleBoard(boggleBoardLength, wordsInDictionary, difficulty_level);
        //backend = new BackendDummy(boggleBoardLength, wordsInDictionary, difficulty_level);
        //tile_letters = backend.exportBoard();
    }

    public frontend(int difficulty, GameMode mode, Context context){
        game_mode = mode;
        difficulty_level = difficulty;
        boggleBoard = new BoggleBoard(context, difficulty_level, game_mode);
    }

    //Public Methods

    public String[] get_letters() {
        return tile_letters;
    }

    //returns the current candidate word as a string
    public String get_candidate_word() {
        String word = "";
        for(int i = 0; i <= last_click; i++) {
            word += tile_letters[current_submission[i]];
        }
        System.out.println(word);
        return word;
    }

    public String tiles_to_word(int[] tiles) {
        String word = "";
        for(int i = 0; i < tiles.length; i++) {
            word += tile_letters[tiles[i]];
        }
        return word;
    }

    //check for valid click, if so update candidate word and submissionarray
    //returns true if activity needs to update the tile
    public boolean tile_click(int click_pos) {
        if(game_over) {
            return false;
        }
        if(last_click >= 0) {
            //user clicks the tile they last clicked, remove that tile from submission
            if(click_pos == current_submission[last_click]) {
                System.out.println("Deactivate tile");
                tile_state[click_pos] = false;
                last_click--;
                return true;
            } else { //user clicked a different tile, check for adjacency & not already clicked
                if(tile_state[click_pos] == true) { //already active
                    System.out.println("Tile already active");
                    return false;
                } else if(is_adjacent(current_submission[last_click], click_pos)) {
                    System.out.println("clicked adjacent tile");
                    tile_state[click_pos] = true;
                    last_click++;
                    current_submission[last_click] = click_pos;
                    return true;
                } else {
                    System.out.println("clicked non-adjacent tile");
                    return false;
                }
            }
        } else { //first click of this submission
            System.out.println("First click.");
            tile_state[click_pos] = true;
            last_click = 0;
            current_submission[last_click] = click_pos;
            return true;

        }
    }

    public boolean clear_click() {
        if(game_over) {
            return false;
        }
        System.out.println("clear submission");
        for(int i = 0; i < 16; i++) {
            tile_state[i] = false;
        }
        last_click = -1;
        return true;
    }

    public boolean submit_click() {
        if(game_over) {
            return false;
        }
        System.out.println("Clicked the submit button");
        if(last_click >= 2) { //more than 3 letters long
            int result = boggleBoard.checkWordAndUpdateScore(get_candidate_word());
            if(result == 1) { //success
                System.out.println("Word submission successful");
                clear_click();
                return true;
            }
            else if(result == 2) { //word already submitted
                System.out.println("Word submission failed, word already in list.");
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int[] submit_click_client() {
        if(game_over) {
            return null;
        }
        if(last_click >= 2) { //more than 3 letters long
            int[] submit = new int[last_click];
            for(int i = 0; i < last_click; i++) {
                submit[i] = current_submission[i];
            }
            clear_click();
            return submit;
        }
        return null;
    }

    //called when host sends message to tell client they accepted a word
    public boolean submit_click_client_accepted(int[] submission) {
        String word = "";
        for(int i = 0; i <= submission.length; i++) {
            word += tile_letters[submission[i]];
        }
        //TODO replace this with whatever the backend is supposed to do as a client
        int result = boggleBoard.checkWordAndUpdateScore(word);
        if(result == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean end_game() {
        game_over = true;
        if (boggleBoard.checkHighScore()) {
          return true;
        }
        else {
          return false;
        }
    }

    public void send_score_name(String name){
        System.out.println("Submitting high score name: " + name);
        boggleBoard.highScore(name);
    }

    public List<String> get_high_scores(){
        ArrayList<User> hsList;
        List<String> scores = new ArrayList<String>();

        hsList = boggleBoard.highScoreList;
        Collections.sort(hsList, new Comparator<User>() {
            public int compare(User o1, User o2) {
                return Integer.compare(o2.score, o1.score);
            }
        });

        for(User user : hsList) {
            scores.add(String.format("%20s %10d", user.name, user.score));
        }
        for(int i = hsList.size(); i < 5; i++){
            scores.add(String.format("%20s %10d", "Nobody", 0));
        }
        return scores;
    }

    //Private Methods
    private boolean is_adjacent(int pos1, int pos2) {
        for(int i = 0; i < adjacency[pos1].length; i++) {
            if(adjacency[pos1][i] == pos2) {
                return true;
            }
        }
        return false;
    }


}
