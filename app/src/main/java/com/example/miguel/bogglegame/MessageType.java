package com.example.miguel.bogglegame;

/**
 * Created by tpatecky on 3/9/17.
 */

public class MessageType {
    static final int SupplyBoard = 0; //host sends to client on game start
    static final int ReadyToStart = 1; //for coordinating timers
    static final int StartGame = 2; //for coordinating timers
    static final int SubmitWord = 3; //client: submit word to host
    static final int RejectWordIllegal = 4; //host: send to client if client submits a non-word
    static final int RejectWorldAlreadyFound = 5; //host: send to client if client submits word already found
    static final int AcceptWord = 6; //host: send to client if client submits word successfully
}