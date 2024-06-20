package com.task.guessnumber.util;

public class ResponseConstants {
    public static final String NAME_ALREADY_TAKEN = "This name is already taken, choose another one!";
    public static final String INVALID_NUMBER_RANGE = "Picked number should be from 1 to 10!";
    public static final String INVALID_BET_AMOUNT = "Bet amount should be more than 0!";
    public static final String INVALID_BET_MESSAGE = "Invalid bet! It should include name, number and bet amount presented in JSON format.";
    public static final String BET_ACCEPTED = "Bet is accepted!";
    public static final String GAME_START = "The number will be generated in 10 seconds, place your bets!";
    public static final String GAME_ALREADY_RUNNING = "The game is already running, place your bet!";
    public static final String WIN = "Congratulations, you won! Your winnings are: ";
    public static final String LOSS = "Unfortunately, you lost! The number was ";
    public static final String DID_NOT_PARTICIPATE = "You did not participate in current round.";
    public static final String NO_WINNERS = "There were no winners in current round.";
    public static final String WINNERS = "Winners of the game: ";
    public static final String FAILED_MESSAGE_SENDING = "Failed to send message: %s%n to session %s%n";
    public static final String ONLY_ONE_BET_ALLOWED = "Only one bet per round is allowed!";
}
