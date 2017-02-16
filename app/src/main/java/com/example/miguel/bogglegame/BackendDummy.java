package com.example.miguel.bogglegame;

import com.example.miguel.bogglegame.AppLogic.BoggleBoard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by tpatecky on 2/8/2017.
 */

//dummy backend to get around slow actual backend.
public class BackendDummy {

/*
    String[] board = {"Z","Z","Z","Z",
                      "B","A","L","L",
                      "X","X","X","X",
                      "Q","Q","Q","Q"};

    boolean ball_found;
    int score;

    public Set<String> validWordsFoundByUser;
    public BackendDummy(final int boardLength, final String[] wordsInDictionary, int difficultyLevel) {
        ball_found = false;
        score = 0;
        validWordsFoundByUser = new HashSet<String>();
    }
    public String[] exportBoard() {
        return board;
    }
    //returns 0 if word is not a valid word i.e. not in game's dictionary
    //returns 1 if the word submitted is a valid word and has never been submitted by user before
    //return 2 if word is a valid word but has been submitted by user before
    // score is updated accordingly
    public int checkWordAndUpdateScore(String word) {
        if(word.equals("BALL")) {
            if(ball_found) {
                return 2;
            } else {
                score += 1;
                ball_found = true;
                validWordsFoundByUser.add("BALL");
                return 1;
            }
        } else {
            return 0;
        }
    }

    public int getScore() {return score;}
    */
}
