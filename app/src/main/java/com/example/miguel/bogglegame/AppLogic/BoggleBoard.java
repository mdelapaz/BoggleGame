package com.example.miguel.bogglegame.AppLogic;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BoggleBoard {

    //length of a N*N boggle board
    private int boardLength;
    private char[][] board;
    //dictionary contains all valid words that can be used by player to earn points
    private Trie dictionary = new Trie();
    //validWordsOnBoard contains all valid words that are on boggle board
    private Set<String> validWordsOnBoard = new HashSet<String>();
    //difficulty level of boggle board, 0 means easy, 1 means normal and 2 means difficult
    //A valid grid of dice must contain at least two valid words in level easy, five valid words in level normal, and seven words in level difficult
    private int difficultyLevel;

    //The score of each valid word is counted based on its length, 1 point for 3 or 4 letter words, 2 points for 5 letter words, 3
    //points for 6 letter words, 5 points for 7 letter words, and 10 points for words of 8 or more letters.
    private int score;
    //validWordsFoundByUser contains all valid words on boggle board that are found by user
    public Set<String> validWordsFoundByUser = new HashSet<String>();

    /**Initializes a boardLength * boardLength board with random characters*/
    public BoggleBoard(final int boardLength, final String[] wordsInDictionary, int difficultyLevel) {

        score = 0;
        this.boardLength = boardLength;
        this.difficultyLevel = difficultyLevel;

        //A valid grid of dice must contain at least two valid words in level easy, five valid words in level normal, and seven words in level difficult
        int minValidWordsRequired;

        switch(difficultyLevel) {
            case 0:
                minValidWordsRequired = 2;
                break;
            case 1:
                minValidWordsRequired = 5;
                break;
            case 2:
                minValidWordsRequired = 7;
                break;
            default:
                minValidWordsRequired = 2; //start game in easy mode by default if no valid difficulty selected
                break;
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

        //if word is valid / in dictionary, add it to validWordsOnBoard, else ignore
        if(word.length() >= 3 && dictionary.search(word.toLowerCase()))
            validWordsOnBoard.add(word);

        for(int row = i - 1; row <= i + 1 && row < board.length; row++) {

            for(int column = j - 1; column <= j+ 1 && column < board[0].length; column++) {

                if(row >= 0 && column >= 0 && !visited[row][column]){

                    if(dictionary.hasPrefix(word.toLowerCase())){
                        findWordsOnBoard(word, visited, board, row, column);
                    }
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

    //returns 0 if word is not a valid word i.e. not in game's dictionary
    //returns 1 if the word submitted is a valid word and has never been submitted by user before
    //return 2 if word is a valid word but has been submitted by user before
    // score is updated accordingly
    public int checkWordAndUpdateScore(String word){

        //if word is valid and is on board
        if(validWordsOnBoard.contains(word.toUpperCase())){

            //if word has never been submitted by user before, add word to validWordsFoundByUser and update score
            if(!validWordsFoundByUser.contains(word.toUpperCase())){

                int wordLength = word.length();
                if(wordLength < 3)
                    score +=0;
                else if(wordLength == 3 || wordLength == 4)
                    score += 1;
                else if(wordLength == 5)
                    score += 2;
                else if(wordLength == 6)
                    score += 3;
                else if(wordLength == 7)
                    score += 5;
                else
                    score += 10;

                validWordsFoundByUser.add(word);
                return 1;
            } else{

                return 2;
            }
        }
        return 0;
    }

    public int getScore() {
        return score;
    }

    /**Return valid words not found by user*/
    public Set<String> getWordsNotFoundByUser(){

        Set<String> validWordsOnBoardCopy = new HashSet<String>();
        validWordsOnBoardCopy.addAll(validWordsOnBoard);
        validWordsOnBoardCopy.removeAll(validWordsFoundByUser);

        return validWordsOnBoardCopy;
    }
}
