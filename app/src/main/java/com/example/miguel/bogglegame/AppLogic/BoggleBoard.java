package com.example.miguel.bogglegame.AppLogic;

import android.content.Context;

import com.example.miguel.bogglegame.GameMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.ArrayList;

public class BoggleBoard {

    //length of a N*N boggle board
    private int boardLength;
    private Context context;
    private char[][] board;
    //dictionary contains all valid words that can be used by player to earn points
    private Trie dictionary = new Trie();
    //validWordsOnBoard contains all valid words that are on boggle board
    public Set<String> validWordsOnBoard = new HashSet<String>();

    private ArrayList<CutThroatWord> validWordsFoundCutThroat = new ArrayList<CutThroatWord>();
    //difficulty level of boggle board, 0 means easy, 1 means normal and 2 means difficult
    //A valid grid of dice must contain at least two valid words in level easy, five valid words in level normal, and seven words in level difficult
    private int difficultyLevel;
    //The list of users on the high score list
    public ArrayList<User> highScoreList = new ArrayList<User>();
    //This is the file name to store high scores
    public String fileName = "";
    //The score of each valid word is counted based on its length, 1 point for 3 or 4 letter words, 2 points for 5 letter words, 3
    //points for 6 letter words, 5 points for 7 letter words, and 10 points for words of 8 or more letters.
    private int score;
    private int prev_rounds_score;

    private int clientScore;
    //validWordsFoundByUser contains all valid words on boggle board that are found by user
    public Set<String> validWordsFoundByUser = new HashSet<String>();
    public Set<String> wordsFoundByOpponent = new HashSet<String>();
    public Set<String> validWordsFoundByHost = new HashSet<String>();

    private GameMode gameMode;

    //NOTE : use this constructor for single player mode
    public BoggleBoard(final int boardLength, final String[] wordsInDictionary, int difficultyLevel, Context c){
        this(boardLength, wordsInDictionary, difficultyLevel, c, GameMode.SinglePlayer);
    }

    //NOTE : use this constructor in following scenarios : MultiPlayer Mode where the player is game initiator
    /**Initializes a boardLength * boardLength board with random characters*/
    public BoggleBoard(final int boardLength, final String[] wordsInDictionary, int difficultyLevel, Context c, GameMode gameMode) {

        score = 0;
        prev_rounds_score = 0;
        clientScore = 0;
        this.context = c;
        this.boardLength = boardLength;
        this.difficultyLevel = difficultyLevel;
        this.gameMode = gameMode;
        loadHighscores();

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

        do{
            generateBoardUsingStandardDice();
            findValidWordsOnBoard();
        }while(validWordsOnBoard.size() <= minValidWordsRequired);

        //if player on this device is the game initiator, push board and valid words to other device
        if(gameMode != GameMode.SinglePlayer){

            pushGameInfoToOtherDevice();
        }
    }

    //NOTE : use this constructor for MultiPlayer Mode where the player is NOT the game initiator
    public BoggleBoard(int boardLength, Context context, char[][] board, int difficultyLevel, GameMode gameMode, final String[] wordsInDictionary) {
        score = 0;
        prev_rounds_score = 0;
        clientScore = 0;
        this.boardLength = boardLength;
        this.context = context;
        this.board = board;
        this.difficultyLevel = difficultyLevel;
        this.gameMode = gameMode;

        loadHighscores();

        //add all words with 3 or more letters to dictionary
        for(String word : wordsInDictionary) {
            if(word.length() >= 3){
                dictionary.add(word);
            }
        }

        findValidWordsOnBoard();
    }

    // NOTE: use this constructor when you want to access high scores without actually playing a game
    public BoggleBoard(Context context, int difficultyLevel, GameMode gameMode){
        this.context = context;
        this.difficultyLevel = difficultyLevel;
        this.gameMode = gameMode;
        loadHighscores();
    }

    // returns true if following are successfully pushed :
    // board, boardLength, validWordsOnBoard, difficultyLevel, GameMode
    // returns false in case of a failure
    private boolean pushGameInfoToOtherDevice(){
        //some code to push board to other device

        return false;
    }

    @Deprecated
    private void generateRandomBoard(){

        Random r = new Random();

        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                char c= (char) (r.nextInt(26) + 'A');
                board[i][j] = c;
            }
        }
    }

    private void generateBoardUsingStandardDice(){
        String[] dice = {"AACIOT", "ABILTY", "ABJMOQ", "ACDEMP",
                "ACELRS", "ADENVZ", "AHMORS", "BIFORX",
                "DENOSW", "DKNOTU", "EEFHIY", "EGKLUY",
                "EGINTV", "EHINPS", "ELPSTU", "GILRUW"};
        String temp;

        Random r = new Random();
        int index;
        for (int i = 15; i >= 0; i--){
            index = r.nextInt(i+1);
            temp = dice[index];
            dice[index] = dice[i];
            dice[i] = temp;
        }

        index = 0;
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 4; j++){
                board[i][j] = dice[index++].charAt(r.nextInt(6));
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
        {
            validWordsOnBoard.add(word);
        }


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
    public int checkWordAndUpdateScore(String word) {

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

    // use this function to check for valid words when the game mode is cut throat
    // if the word is checked for host (i.e. checkForClient == false), score is updated for the host (i.e. score is updated)
    // if the word is checked for host (i.e. checkForClient == true), score is updated for the client (i.e. clientScore is updated)
    // if word is valid, return the points that the word is worth
    //return 0 if word is a valid word but has already been submitted by a user before
    //returns -1 if word is not a valid word i.e. not in game's dictionary
    //returns -2 if game is not in CutThroatTwoPLayer mode
    public int checkWordAndUpdateScoreCutThroat(String word, int[] sequenceOfTiles, boolean checkForClient)throws InputMismatchException {

        if(gameMode == GameMode.CutThroatTwoPLayer){

            if(sequenceOfTiles == null){
                System.err.println("error from checkWordAndUpdateScore() in BoggleBoard.java");
                throw new InputMismatchException();
            }

            //represents how many points the word is worth
            int wordScore = 0;

            //if word is valid and is on board
            if(validWordsOnBoard.contains(word.toUpperCase())){

                if(checkForClient == false && validWordsFoundByHost.contains(word)){
                    return 0;
                }

                CutThroatWord cutThroatWord = new CutThroatWord(sequenceOfTiles, word);

                //if word has never been submitted by user before, add word to validWordsFoundCutThroat and update score
                if(!validWordsFoundCutThroat.contains(cutThroatWord)){

                    int wordLength = word.length();

                    if(wordLength == 3 || wordLength == 4)
                        wordScore = 1;
                    else if(wordLength == 5)
                        wordScore = 2;
                    else if(wordLength == 6)
                        wordScore = 3;
                    else if(wordLength == 7)
                        wordScore = 5;
                    else
                        wordScore = 10;

                    validWordsFoundCutThroat.add(cutThroatWord);

                    //if checking for host
                    if(!checkForClient){

                        score += wordScore;
                        validWordsFoundByUser.add(word);
                        validWordsFoundByHost.add(word);
                    }else{
                        clientScore += wordScore;
                    }

                    return wordScore;
                } else{

                    return 0;
                }
            }

            return -1;
        }

        return -2;
    }

    //total score
    public int getScore() {
        return score + prev_rounds_score;
    }

    //score for just this round
    public int getRoundScore() {
        return score;
    }

    //for subsequent rounds we want to add the prev round's score
    public void add_prev_rounds_score(int prevScore) {
        prev_rounds_score += prevScore;
    }

    /**Return valid words not found by user*/
    public Set<String> getWordsNotFoundByUser(){

        Set<String> validWordsOnBoardCopy = new HashSet<String>();
        validWordsOnBoardCopy.addAll(validWordsOnBoard);
        validWordsOnBoardCopy.removeAll(validWordsFoundByUser);

        return validWordsOnBoardCopy;
    }

    //Loads in the high scores from highscores.txt, throws IOException.
    public boolean loadHighscores() {
        this.fileName = "HS" + this.gameMode + this.difficultyLevel + ".txt";
        File file = new File(context.getFilesDir(), fileName);
        Scanner input;
        try{
            if(!file.exists()) {
                return false;
            }
            input = new Scanner(file);
            String line;
            highScoreList.clear();
            while(input.hasNextLine()) {
                line = input.nextLine();
                String[] parts = line.split("\\~\\$\\~");
                if(parts.length != 2) {
                    //File is corrupt.
                    System.out.println ("File " + this.fileName + " is corrupt");
                    return false;
                }
                String name = parts[0];
                int score = Integer.parseInt(parts[1]);
                User user = new User(name, score);
                highScoreList.add(user);
            }
            input.close();
        }catch (IOException ex) {
            System.out.println(ex.toString());
            return false;
        }

        return true;
    }

    public boolean saveHighscores() {
        this.fileName = "HS" + this.gameMode + this.difficultyLevel + ".txt";
        File file = new File(context.getFilesDir(), fileName);
        try {
            if(!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            for(User user : highScoreList) {
                fw.write(user.name + "~$~" + user.score + "\n");
            }
            fw.close();
        }catch (IOException ex) {
            System.out.println(ex.toString());
            return false;
        }

        return true;
    }

    public boolean checkHighScore(){
        //If the list is empty or has less than 5 users, add them in!
        if(highScoreList.isEmpty() || highScoreList.size() < 5){
            return true;
        }
        //Any other case, we have to start comparing
        for(User user : highScoreList) {
            //We found a place to put their score!
            if(score > user.score) {
                return true;
            }
        }
        return false;
    }

    //Function to be called from front end
    public boolean highScore(String name) {
        return highScore(this.score, name);
    }

    //Compare score to high score list, and see if they belong on there
    //If they do, awesome! Put them in, sort, and return TRUE
    //If not, don't do anything, and return FALSE
    public boolean highScore(int score, String name) {
        //If the list is empty or has less than 5 users, add them in!
        if(highScoreList.isEmpty() || highScoreList.size() < 5){
            User newUser = new User(name, score);
            highScoreList.add(newUser);
            saveHighscores();
            return true;
        }
        //Any other case, we have to start comparing
        for(User user : highScoreList) {
            //We found a place to put their score!
            if(score > user.score) {
                User newUser = new User(name, score);
                highScoreList.add(newUser); //Add in the new User and sort
                Collections.sort(highScoreList); //Sort the list
                //Check if there's more than 5 Users, if so, remove one
                if(highScoreList.size() > 5) {
                    highScoreList.remove(0);
                    //Check if the user is still there, if not, they didn't belong anyway
                    if(highScoreList.contains(newUser)){
                        saveHighscores();
                        return true;
                    }else
                        return false;
                }
            }
        }
        return false;
    }

    // *** Getters and Setters are defined below ***

    public int getBoardLength() {
        return boardLength;
    }

    public void setBoardLength(int boardLength) {
        this.boardLength = boardLength;
    }

    public char[][] getBoard() {
        return board;
    }

    public void setBoard(char[][] board) {
        this.board = board;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setClientScore(int clientScore) {
        this.clientScore = clientScore;
    }

    public int getClientScore() {
        return this.clientScore;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void addOpponentWord(String word){
        wordsFoundByOpponent.add(word);
    }
}
