package com.example.miguel.bogglegame;

import com.example.miguel.bogglegame.AppLogic.BoggleBoard;

import java.util.ArrayList;

/**
 * Created by tpatecky on 2/8/2017.
 */

public class Backend {

    public String[] letters;
    //state of game
    //right now, 0 = game in progress, 1 = game over
    //in game over state, user should only be able to click reset to start a new game
    public int game_state;

     String[] letters;
    //0 is easy, 1 is normal, 2 is difficult
    private int difficulty_level = 0;
    //length to a N*N boggle board
    private int boggleBoardLength = 4;
    private int score = 0;
    private int highScore = 0;
    private ArrayList<String> submitted_words = new ArrayList<>();

    public Backend(String[] wordsInDictionary){

        BoggleBoard boggle = new BoggleBoard(boggleBoardLength, wordsInDictionary, difficulty_level);
        letters = boggle.exportBoard();
    }

    //dummy values until backend connected
    ArrayList<String> submitted_words = new ArrayList<String>();
    int score = 42;


    //METHODS - everything front end will need to call to get backend data

    //returns state of game, 0 for game in progress, 1 for game over
    int get_game_state() {
        return game_state;
    }

    //called by timer once
    void times_up() {

    }

    //return letter in tile at that grid position
    String get_letter(int tile_pos) {
        return letters[tile_pos];
    }

    //return current score
    String get_score() {
        return Integer.toString(score);
        //TODO: replace this with connection to backend scoring system
    }

    ArrayList<String> get_submitted_words() {
        return submitted_words;
        //TODO: replace this with connection to backend
    }

    //submit word, check vs dictionary
    //ret values:
    //0 - success
    //1 - failure, word does not exist in dictionary
    //2 - failure, word already submitted
    //3 - failure, opponent already submitted this word
    int submit_word(int[] submission, int word_length) {
        String candidate = "";
        for(int i = 0; i < word_length; i++) {
            candidate += letters[submission[i]];
        }

        for(int i = 0; i < submitted_words.size(); i++) {
            if(candidate.equals(submitted_words.get(i))) {
                return 2;
            }
        }

        submitted_words.add(candidate);
        return 0;
    }

    //Updates score and sets to new value.
    void updateScore(String word) {
        score += scoreWord(word);
    }

    //Scores a word based on it's length
    //3 or 4 letter words: 1 point
    //5 letter word: 2 points
    //6 letter word: 3 points
    //7 letter words: 5 points
    //8 letter words and more: 10 points
    private int scoreWord(String word) {
        int length = word.length();
        if(length == 3 || length == 4)
            return 1;
        else if(length == 5)
            return 2;
        else if(length == 6)
            return 3;
        else if(length == 7)
            return 5;
        else if(length > 7)
            return 10;

        return 0; //Base case
    }

    //Starts a new game, Clears word list, Resets the score, Updates high score
    void reset_game() {
        submitted_words = new ArrayList<>();
        if(score > highScore)
            highScore = score;
        score = 0;
    }
}
