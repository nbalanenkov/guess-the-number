package com.task.guessnumber.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.guessnumber.model.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.task.guessnumber.util.LoggerConstants.*;
import static com.task.guessnumber.util.ResponseConstants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSocketService {

    private final Random random;
    private final int ROUND_DELAY;
    private final Map<WebSocketSession, Bet> players = new HashMap<>();
    private Timer timer;

    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Bet bet = mapper.readValue(message.getPayload(), Bet.class);
            String validationMessage = validateBet(session, bet);
            sendMessage(session, validationMessage);
        } catch (JsonProcessingException e) {
            sendMessage(session, INVALID_BET_MESSAGE);
        }
    }

    public void afterConnectionEstablished(WebSocketSession session) {
        players.put(session, null);
        startGameIfNotRunning(session);
    }

    public void afterConnectionClosed(WebSocketSession session) {
        players.remove(session);
        if (players.isEmpty() && nonNull(timer)) {
            resetTimer();
        }
    }

    private String validateBet(WebSocketSession session, Bet bet) {
        if (isNameTaken(bet.getName())) {
            return NAME_ALREADY_TAKEN;
        } else if (nonNull(players.get(session))) {
            return ONLY_ONE_BET_ALLOWED;
        } else if (bet.getNumber() < 1 || bet.getNumber() > 10) {
            return INVALID_NUMBER_RANGE;
        } else if (bet.getBetAmount() <= 0) {
            return INVALID_BET_AMOUNT;
        } else {
            players.put(session, bet);
            log.info(String.format(RECEIVED_BET, bet, session.getId()));
            return BET_ACCEPTED;
        }
    }

    private boolean isNameTaken(String name) {
        return players.values().stream().filter(Objects::nonNull).anyMatch(player -> name.equals(player.getName()));
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            System.err.printf(FAILED_MESSAGE_SENDING, e.getMessage(), session.getId());
        }
    }

    private void startGameIfNotRunning(WebSocketSession session) {
        if (isNull(timer)) {
            startGameRound();
        } else {
            sendMessage(session, GAME_ALREADY_RUNNING);
        }
    }

    private void startGameRound() {
        sendMessageToAllPlayers();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int generatedNumber = random.nextInt(1, 11);
                log.info(String.format(GENERATED_NUMBER, generatedNumber));
                Map<String, BigDecimal> winners = determineWinners(generatedNumber);
                notifyPlayers(winners, generatedNumber);
                handleNextRoundStart();
            }
        }, ROUND_DELAY);
    }

    private void sendMessageToAllPlayers() {
        for (WebSocketSession session : players.keySet()) {
            sendMessage(session, GAME_START);
        }
    }

    private Map<String, BigDecimal> determineWinners(int generatedNumber) {
        Map<String, BigDecimal> winners = new HashMap<>();
        for (Map.Entry<WebSocketSession, Bet> player : players.entrySet()) {
            if (nonNull(player.getValue())) {
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
        String tableOfWinners = getTableOfWinners(winners);
        for (Map.Entry<WebSocketSession, Bet> player : players.entrySet()) {
            String resultMessage;
            if (nonNull(player.getValue())) {
                if (player.getValue().getNumber() == generatedNumber) {
                    resultMessage = WIN + winners.get(player.getValue().getName());
                } else {
                    resultMessage = LOSS + generatedNumber;
                }
            } else {
                resultMessage = DID_NOT_PARTICIPATE;
            }
            sendMessage(player.getKey(), resultMessage);
            sendMessage(player.getKey(), tableOfWinners);
            player.setValue(null);
        }
        log.info(tableOfWinners);
        log.info(ROUND_ENDED);
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
            log.info(NEW_ROUND_STARTED);
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
