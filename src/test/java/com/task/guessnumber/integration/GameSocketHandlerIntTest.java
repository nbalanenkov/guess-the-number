package com.task.guessnumber.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static com.task.guessnumber.util.ResponseConstants.*;
import static java.util.Objects.isNull;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class GameSocketHandlerIntTest {

    @LocalServerPort
    private int port;

    @Value("${round.delay}")
    private int ROUND_DELAY;

    private final Map<WebSocketSession, BlockingQueue<String>> clients = new HashMap<>();
    private String serverMessage;
    private String winners;


    @AfterEach
    public void afterEach() throws IOException {
        for (WebSocketSession session : clients.keySet()) {
            session.close();
        }
        clients.clear();
        winners = null;
    }

    @Test
    public void oneRoundWithBets() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String betName1 = "USER_" + UUID.randomUUID();
        int betNumber1 = 3;
        int betAmount1 = 10;
        String potentialWinAmount1 = "99.00";

        String betName2 = "USER_" + UUID.randomUUID();
        int betNumber2 = 3;
        int betAmount2 = 1;
        String potentialWinAmount2 = "9.90";

        WebSocketSession session1 = connectNewClient();
        placeBet(session1, betName1, betNumber1, betAmount1);

        WebSocketSession session2 = connectNewClient();
        placeBet(session2, betName2, betNumber2, betAmount2);

        waitForNumberGeneration();
        validateGameRoundForSession(session1, betName1, potentialWinAmount1, true, false);
        validateGameRoundForSession(session2, betName2, potentialWinAmount2, false, true);
    }

    @Test
    public void oneRoundWithoutBets() throws InterruptedException, ExecutionException, TimeoutException {
        WebSocketSession session1 = connectNewClient();
        WebSocketSession session2 = connectNewClient();

        waitForNumberGeneration();
        validateGameRoundForSession(session1, null, null, true, false);
        validateGameRoundForSession(session2, null, null, false, true);
    }

    @Test
    public void multipleRoundsSimultaneousBetsAndConnections() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        String betName1 = "USER_" + UUID.randomUUID();
        int betNumber1 = 1;
        int betAmount1 = 100;
        String potentialWinAmount1 = "990.00";

        String betName2 = "USER_" + UUID.randomUUID();
        int betNumber2 = 2;
        int betAmount2 = 10;
        String potentialWinAmount2 = "99.00";

        String betName3 = "USER_" + UUID.randomUUID();
        int betNumber3 = 3;
        int betAmount3 = 1;
        String potentialWinAmount3 = "9.90";

        WebSocketSession session1 = connectNewClient();
        placeBet(session1, betName1, betNumber1, betAmount1);

        WebSocketSession session2 = connectNewClient();

        waitForNumberGeneration();
        validateGameRoundForSession(session1, betName1, potentialWinAmount1, true, false);
        validateGameRoundForSession(session2, null, null, false, true);
        winners = null;

        placeBet(session2, betName2, betNumber2, betAmount2);

        WebSocketSession session3 = connectNewClient();
        placeBet(session3, betName3, betNumber3, betAmount3);

        waitForNumberGeneration();
        validateGameRoundForSession(session1, null, null, true, false);
        validateGameRoundForSession(session2, betName2, potentialWinAmount2, true, false);
        validateGameRoundForSession(session3, betName3, potentialWinAmount3, false, true);
        winners = null;

        placeBet(session1, betName1, betNumber1, betAmount1);
        placeBet(session3, betName3, betNumber3, betAmount3);

        waitForNumberGeneration();
        validateGameRoundForSession(session1, betName1, potentialWinAmount1, true, false);
        validateGameRoundForSession(session2, null, null, true, false);
        validateGameRoundForSession(session3, betName3, potentialWinAmount3, true, true);
    }

    @Test
    public void multipleRoundsSimultaneousInvalidBetsAndConnections() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        String betName1 = "USER_" + UUID.randomUUID();
        String betName2 = "USER_" + UUID.randomUUID();

        int invalidBetNumber = -1;
        int invalidBetAmount = -100;
        int validBetNumber = 1;
        int validBetAmount = 100;

        WebSocketSession session1 = connectNewClient();
        WebSocketSession session2 = connectNewClient();
        WebSocketSession session3 = connectNewClient();
        WebSocketSession session4 = connectNewClient();

        placeBet(session1, betName1, invalidBetNumber, validBetAmount);
        placeBet(session2, betName2, validBetNumber, invalidBetAmount);
        String bet = "Completely invalid message!";
        session3.sendMessage(new TextMessage(bet));
        validateBet(session1, INVALID_NUMBER_RANGE);
        validateBet(session2, INVALID_BET_AMOUNT);
        validateBet(session3, INVALID_BET_MESSAGE);

        placeBet(session1, betName1, validBetNumber, validBetAmount);
        Thread.sleep(100);
        placeBet(session4, betName1, validBetNumber, validBetAmount);
        validateBet(session4, NAME_ALREADY_TAKEN);
    }

    private final TextWebSocketHandler clientSocketHandler = new TextWebSocketHandler() {
        @Override
        public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
            clients.get(session).add(message.getPayload());
        }

        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) {
            clients.put(session, new LinkedBlockingQueue<>());
        }
    };

    private WebSocketSession connectNewClient() throws InterruptedException, ExecutionException, TimeoutException {
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/game";
        return webSocketClient.execute(clientSocketHandler, uri).get(1, TimeUnit.SECONDS);
    }

    private void validateBet(WebSocketSession session, String expectedErrorMessage) throws InterruptedException {
        getNextServerMessageForSession(session);
        assertThat(serverMessage, anyOf(
                containsString(GAME_START),
                containsString(GAME_ALREADY_RUNNING)));
        getNextServerMessageForSession(session);
        assertEquals(expectedErrorMessage, serverMessage);
    }

    private void validateGameRoundForSession(WebSocketSession session, String betName, String potentialWinAmount, Boolean connectedBeforeGameStarted, Boolean lastPlayer) throws InterruptedException {
        getNextServerMessageForSession(session);
        if (connectedBeforeGameStarted) {
            assertEquals(GAME_START, serverMessage);
        } else {
            assertEquals(GAME_ALREADY_RUNNING, serverMessage);
        }

        getNextServerMessageForSession(session);
        if (betName != null && potentialWinAmount != null) {
            assertEquals(BET_ACCEPTED, serverMessage);

            getNextServerMessageForSession(session);
            assertThat(serverMessage, anyOf(
                    containsString(LOSS),
                    containsString(WIN)));
        } else {
            assertEquals(DID_NOT_PARTICIPATE, serverMessage);
        }

        if (serverMessage.contains(WIN)) {
            if (isNull(winners)) {
                winners = "Winners of the game: " + betName + " - " + potentialWinAmount;
            } else {
                winners += ", " + betName + " - " + potentialWinAmount;
            }
            getNextServerMessageForSession(session);
            assertTrue(serverMessage.contains(winners));
        } else {
            getNextServerMessageForSession(session);
            if (lastPlayer && isNull(winners)) {
                assertEquals(NO_WINNERS, serverMessage);
            }
        }
    }

    private void getNextServerMessageForSession(WebSocketSession session) throws InterruptedException {
        serverMessage = clients.get(session).poll(1, TimeUnit.SECONDS);
    }

    private void placeBet(WebSocketSession session, String betName, int betNumber, int betAmount) throws IOException {
        String bet = String.format("{\"name\":\"%s\",\"number\":%d,\"betAmount\":%d}", betName, betNumber, betAmount);
        session.sendMessage(new TextMessage(bet));
    }

    private void waitForNumberGeneration() throws InterruptedException {
        Thread.sleep(ROUND_DELAY);
    }
}
