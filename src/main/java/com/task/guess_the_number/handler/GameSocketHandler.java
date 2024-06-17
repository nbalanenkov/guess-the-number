package com.task.guess_the_number.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.guess_the_number.model.Bet;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.task.guess_the_number.util.MessageConstants.*;

public class GameSocketHandler extends TextWebSocketHandler {

    private final Map<WebSocketSession, Bet> players = new HashMap<>();
    private final Random random = new Random();
    private final ObjectMapper mapper = new ObjectMapper();
    private Timer timer;

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
        try {
            Bet bet = mapper.readValue(message.getPayload(), Bet.class);
            String validationMessage = validateBet(session, bet);
            sendMessage(session, validationMessage);
        } catch (JsonProcessingException e) {
            sendMessage(session, INCORRECT_BET_MESSAGE);
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        players.put(session, null);
        startGameIfNotRunning(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        players.remove(session);
        if(players.isEmpty()){
            resetTimer();
        }
    }

    private String validateBet(WebSocketSession session, Bet bet) {
        if(players.get(session) != null){
            return ONLY_ONE_BET_ALLOWED;
        }
        else if (bet.getNumber() < 1 || bet.getNumber() > 10) {
            return INCORRECT_NUMBER_RANGE;
        } else if (bet.getBetAmount() <= 0) {
            return INCORRECT_BET_AMOUNT;
        } else {
            players.put(session, bet);
            return BET_ACCEPTED;
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            System.err.printf(FAILED_MESSAGE_SENDING, e.getMessage(), session.getId());
        }
    }

    private void startGameIfNotRunning(WebSocketSession session) {
        if (timer == null) {
            startGameRound();
        } else {
            sendMessage(session, GAME_ALREADY_RUNNING);
        }
    }

    public void startGameRound() {
        sendMessageToAllPlayers();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int generatedNumber = random.nextInt(1, 11);
                System.out.println(generatedNumber);
                Map<String, BigDecimal> winners = determineWinners(generatedNumber);
                notifyPlayers(winners, generatedNumber);
                handleNextRoundStart();
            }
        }, 10000);
    }

    private void sendMessageToAllPlayers() {
        for (WebSocketSession session : players.keySet()) {
            sendMessage(session, GAME_START);
        }
    }

    private Map<String, BigDecimal> determineWinners(int generatedNumber) {
        Map<String, BigDecimal> winners = new HashMap<>();
        for (Map.Entry<WebSocketSession, Bet> player : players.entrySet()) {
            if (player.getValue() != null) {
                String playerName = player.getValue().getName();
                if (player.getValue().getNumber() == generatedNumber) {
                    BigDecimal winAmount = calculateWinAmount(player.getValue().getBetAmount());
                    winners.put(playerName, winAmount);
                }
            }
        }
        return winners;
    }

    private BigDecimal calculateWinAmount(int betAmount) {
        return BigDecimal.valueOf(betAmount * 9.9).setScale(2, RoundingMode.HALF_UP);
    }

    private void notifyPlayers(Map<String, BigDecimal> winners, int generatedNumber) {
        for (Map.Entry<WebSocketSession, Bet> player : players.entrySet()) {
            String resultMessage;
            if (player.getValue() != null) {
                if (player.getValue().getNumber() == generatedNumber) {
                    resultMessage = WIN + winners.get(player.getValue().getName());
                } else {
                    resultMessage = LOSS + generatedNumber;
                }
            } else {
                resultMessage = DID_NOT_PARTICIPATE;
            }
            sendMessage(player.getKey(), resultMessage);
            sendMessage(player.getKey(), getTableOfWinners(winners));
            player.setValue(null);
        }
    }

    private String getTableOfWinners(Map<String, BigDecimal> winners) {
        if (winners.isEmpty()) {
            return NO_WINNERS;
        }
        LinkedHashMap<String, BigDecimal> sortedWinners = winners.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        StringBuilder result = new StringBuilder();
        result.append(WINNERS);
        for (Map.Entry<String, BigDecimal> winner : sortedWinners.entrySet()) {
            result.append(winner.getKey()).append(" - ").append(winner.getValue()).append(", ");
        }
        result.setLength(result.length() - 2);
        return result.toString();
    }

    private void handleNextRoundStart() {
        if (!players.isEmpty()) {
            startGameRound();
        } else {
            resetTimer();
        }
    }

    private void resetTimer() {
        timer.cancel();
        timer = null;
    }
}
