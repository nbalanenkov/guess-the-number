package com.task.guessnumber.handler;

import com.task.guessnumber.service.GameSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static com.task.guessnumber.util.LoggerConstants.CONNECTION_CLOSED;
import static com.task.guessnumber.util.LoggerConstants.CONNECTION_ESTABLISHED;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSocketHandler extends TextWebSocketHandler {

    private final GameSocketService gameSocketService;

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        gameSocketService.handleTextMessage(session, message);
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.info(String.format(CONNECTION_ESTABLISHED, session.getId()));
        gameSocketService.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        log.info(String.format(CONNECTION_CLOSED, session.getId()));
        gameSocketService.afterConnectionClosed(session);
    }
}
