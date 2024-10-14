//package org.com;
//
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//public class MessageWebSocketHandler extends TextWebSocketHandler {
//    @Override
//    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        // Здесь можно обработать входящее сообщение
//        System.out.println("Received: " + message.getPayload());
//        session.sendMessage(new TextMessage("Echo: " + message.getPayload())); // Отправляем обратно
//    }
//}
