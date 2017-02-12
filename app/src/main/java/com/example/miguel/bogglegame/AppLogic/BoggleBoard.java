package com.example.miguel.bogglegame.AppLogic;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BoggleBoard {

    //length of a N*N boggle board
    private int boardLength;
    private char[][] board;
    //dictionary contains all valid words that can be used by player to earn points
    private Set<String> dictionary = new HashSet<String>();
    //validWordsOnBoard contains all valid words that are on boggle board
    private Set<String> validWordsOnBoard = new HashSet<String>();
    //difficulty level of boggle board, 0 means easy, 1 means normal and 2 means difficult
    //A valid grid of dice must contain at least two valid words in level easy, five valid words in level normal, and seven words in level difficult
    private int difficultyLevel;

    /**Initializes a boardLength * boardLength board with random characters*/
    public BoggleBoard(final int boardLength, final String[] wordsInDictionary, int difficultyLevel) {

        this.boardLength = boardLength;
        this.difficultyLevel = difficultyLevel;

        //A valid grid of dice must contain at least two valid words in level easy, five valid words in level normal, and seven words in level difficult
        int minValidWordsRequired;

        if(difficultyLevel == 0){
            minValidWordsRequired = 2;

        }else if(difficultyLevel == 1){
            minValidWordsRequired = 5;

        }else if(difficultyLevel == 2){
            minValidWordsRequired = 7;

        }else{
            minValidWordsRequired = 2;      //start game in easy mode by default if no valid difficulty selected
        }

        //add all words with 3 or more letters to dictionary
        for(String word : wordsInDictionary) {
            if(word.length() >= 3){
                dictionary.add(word);
            }
        }

        board = new char[boardLength][boardLength];

        //create a boggle board with random characters and check to see if it has at least minValidWordsRequired valid words on boggle board
        do{
            generateRandomBoard();
            findValidWordsOnBoard();
        }while(validWordsOnBoard.size() <= minValidWordsRequired);
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

    /**find all words starting with each word on board using depth first traversal
     each word found which also appears in dictionary is added to validWordsOnBoard*/
    private void findWordsOnBoard(String word, boolean[][] visited, char[][] board, int i, int j) {

        //mark visited word cell as true so we don't use it again in the formation of a word
        visited[i][j] = true;
        //add the word of cell just visited to the end of the word
        word += board[i][j];

        //if word is valid, add it to validWordsOnBoard, else ignore
        if(dictionary.contains(word.toLowerCase()))
            validWordsOnBoard.add(word);

        for(int row = i - 1; row <= i + 1 && row < board.length; row++) {

            for(int column = j - 1; column <= j+ 1 && column < board[0].length; column++) {

                if(row >= 0 && column >= 0 && !visited[row][column]){
                    findWordsOnBoard(word, visited, board, row, column);
                }
            }
        }

        //take out the last letter (of just visited cell) from word
        String temp = word.substring(0,word.length()-1);
        word = temp;
        visited[i][j] = false;
    }

    /**find all words on boggle board which are in dictionary*/
    private void findValidWordsOnBoard(){

        //mark each visited cell to ensure it gets used only once while forming a word
        boolean[][] visited = new boolean[boardLength][boardLength];
        String word = "";

        for(int i = 0; i < boardLength; i++) {
            for(int j = 0; j < boardLength; j++)
                findWordsOnBoard(word, visited, board, i, j);
        }
    }

    public boolean isAValidWordOnBoard(String word){

        return validWordsOnBoard.contains(word.toUpperCase());
    }
}
