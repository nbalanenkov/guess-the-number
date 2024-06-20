package com.task.guessnumber.unit;

import com.task.guessnumber.service.GameSocketService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;

import static com.task.guessnumber.util.ResponseConstants.*;
import static org.mockito.Mockito.*;

public class GameSocketServiceTest {

    private static final Random random = mock(Random.class);
    private static final int generatedNumber = 1;

    private final int ROUND_DELAY = 1000;
    private final GameSocketService gameSocketService = new GameSocketService(random, ROUND_DELAY);

    @Test
    public void handleTextMessageValidBet() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);

        placeBet(session, "John", 1, 3);

        verify(session).sendMessage(new TextMessage(BET_ACCEPTED));
    }

    @Test
    public void handleTextMessageNameAlreadyTaken() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        placeBet(session, "John", 1, 3);
        placeBet(session2, "John", 1, 3);

        verify(session).sendMessage(new TextMessage(BET_ACCEPTED));
        verify(session2).sendMessage(new TextMessage(NAME_ALREADY_TAKEN));
    }

    @Test
    public void handleTextMessageInvalidNumber() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);

        placeBet(session, "John", -1, 3);

        verify(session).sendMessage(new TextMessage(INVALID_NUMBER_RANGE));
    }

    @Test
    public void handleTextMessageInvalidBetAmount() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);

        placeBet(session, "John", 1, -3);

        verify(session).sendMessage(new TextMessage(INVALID_BET_AMOUNT));
    }

    @Test
    public void handleTextMessageInvalidBetMessageFormat() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);

        String bet = "Blah";
        TextMessage message = new TextMessage(bet);
        gameSocketService.handleTextMessage(session, message);

        verify(session).sendMessage(new TextMessage(INVALID_BET_MESSAGE));
    }

    @Test
    public void afterConnectionEstablishedIfFirstPlayerReceiveGameStart() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);

        gameSocketService.afterConnectionEstablished(session);

        verify(session).sendMessage(new TextMessage(GAME_START));
    }

    @Test
    public void afterConnectionEstablishedIfNotFirstPlayerReceiveGameAlreadyRunning() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        gameSocketService.afterConnectionEstablished(session);
        gameSocketService.afterConnectionEstablished(session2);

        verify(session).sendMessage(new TextMessage(GAME_START));
        verify(session2).sendMessage(new TextMessage(GAME_ALREADY_RUNNING));
    }

    @Test
    public void afterConnectionEstablishedIfCorrectNumberReceiveWinMessage() throws IOException, InterruptedException {
        WebSocketSession session = mock(WebSocketSession.class);
        gameSocketService.afterConnectionEstablished(session);

        placeBet(session, "John", 1, 100);

        BigDecimal winAmount = BigDecimal.valueOf(990.00).setScale(2, RoundingMode.HALF_UP);

        when(random.nextInt(1, 11)).thenReturn(generatedNumber);

        waitForNumberGeneration();

        verify(session).sendMessage(new TextMessage(WIN + winAmount));
    }

    @Test
    public void afterConnectionEstablishedIfIncorrectNumberReceiveLossMessage() throws InterruptedException, IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        gameSocketService.afterConnectionEstablished(session);

        placeBet(session, "John", 2, 100);

        when(random.nextInt(1, 11)).thenReturn(generatedNumber);

        waitForNumberGeneration();

        verify(session).sendMessage(new TextMessage(LOSS + generatedNumber));
    }

    @Test
    public void afterConnectionEstablishedIfNoNumberReceiveDidNotParticipateMessage() throws InterruptedException, IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        gameSocketService.afterConnectionEstablished(session);

        Thread.sleep(ROUND_DELAY + 200);

        verify(session).sendMessage(new TextMessage(DID_NOT_PARTICIPATE));
    }

    @Test
    public void afterConnectionEstablishedIfWinnersPresentReceiveWinnersMessage() throws InterruptedException, IOException {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);

        gameSocketService.afterConnectionEstablished(session1);
        gameSocketService.afterConnectionEstablished(session2);
        gameSocketService.afterConnectionEstablished(session3);

        String name1 = "USER_" + UUID.randomUUID();
        int number1 = 1;
        int betAmount1 = 1;
        BigDecimal winAmount1 = BigDecimal.valueOf(9.90).setScale(2, RoundingMode.HALF_UP);

        String name2 = "USER_" + UUID.randomUUID();
        int number2 = 1;
        int betAmount2 = 10;
        BigDecimal winAmount2 = BigDecimal.valueOf(99.00).setScale(2, RoundingMode.HALF_UP);

        String name3 = "USER_" + UUID.randomUUID();
        int number3 = 1;
        int betAmount3 = 100;
        BigDecimal winAmount3 = BigDecimal.valueOf(990.00).setScale(2, RoundingMode.HALF_UP);

        placeBet(session1, name1, number1, betAmount1);
        placeBet(session2, name2, number2, betAmount2);
        placeBet(session3, name3, number3, betAmount3);

        when(random.nextInt(1, 11)).thenReturn(generatedNumber);

        waitForNumberGeneration();

        String expectedResult = String.format("%s%s - %s, %s - %s, %s - %s", WINNERS, name3, winAmount3, name2, winAmount2, name1, winAmount1);

        verify(session1).sendMessage(new TextMessage(expectedResult));
        verify(session2).sendMessage(new TextMessage(expectedResult));
        verify(session3).sendMessage(new TextMessage(expectedResult));
    }

    @Test
    public void afterConnectionEstablishedIfWinnersNotPresentReceiveNoWinnersMessage() throws IOException, InterruptedException {
        WebSocketSession session = mock(WebSocketSession.class);
        gameSocketService.afterConnectionEstablished(session);

        when(random.nextInt(1, 11)).thenReturn(generatedNumber);

        waitForNumberGeneration();

        verify(session).sendMessage(new TextMessage(NO_WINNERS));
    }

    private void placeBet(WebSocketSession session, String name, int number, int betAmount) {
        String bet = String.format("{\"name\":\"%s\",\"number\":%d,\"betAmount\":%d}", name, number, betAmount);
        gameSocketService.handleTextMessage(session, new TextMessage(bet));
    }

    private void waitForNumberGeneration() throws InterruptedException {
        Thread.sleep(ROUND_DELAY + 200);
    }
}
