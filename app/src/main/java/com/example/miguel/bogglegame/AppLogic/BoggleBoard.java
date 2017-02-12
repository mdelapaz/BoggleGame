package com.example.miguel.bogglegame.AppLogic;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BoggleBoard {

    //length of a N*N boggle board
    private int boardLength;
    private char[][] board;
    //dictionary contains all valid words that can be used by player to earn points
    private Set<String> dictionary = new HashSet<String>();

    /**Initializes a boardLength * boardLength board with random characters*/
    public BoggleBoard(final int boardLength, final String[] wordsInDictionary) {

        this.boardLength = boardLength;
        for(String word : wordsInDictionary) dictionary.add(word);
        board = new char[boardLength][boardLength];
        generateRandomBoard();
    }

    private void generateRandomBoard(){

        Random r = new Random();

        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                char c= (char) (r.nextInt(26) + 'A');
                board[i][j] = c;
            }
        }
    }

    public String[] exportBoard(){

        String[] board = new String[boardLength * boardLength];
        int i = 0;

        for(char[] cArray : this.board){
            for(char c : cArray) board[i++] = String.valueOf(c).toUpperCase();
        }

        return board;
    }
}
