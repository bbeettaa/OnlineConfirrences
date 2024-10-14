package org.com;


//Отлично! Давай подробнее разберем, какие библиотеки и технологии можно использовать в проекте,
// а что ты можешь реализовать самостоятельно, включая демонстрацию экрана.
//
//Библиотеки и технологии
//Серверная часть:
//Spring Framework:
//
//Используй Spring Boot для создания REST API, который будет обрабатывать запросы от клиентов.
//Spring WebSocket для поддержки WebSocket-соединений, если ты решишь добавлять функциональность
// для обмена сообщениями в реальном времени.
//        Spring Security (опционально):
//
//Для управления правами доступа и аутентификации пользователей.
//        JACKSON:
//
//Для обработки JSON (сериализация и десериализация), что удобно для обмена данными между клиентом и сервером.
//Клиентская часть:
//HTML/CSS/JavaScript:
//
//Основные технологии для создания пользовательского интерфейса.
//CSS-фреймворки (например, Bootstrap) для улучшения внешнего вида интерфейса и адаптивного дизайна.
//JavaScript библиотеки:
//
//jQuery (опционально): для упрощения работы с DOM и AJAX-запросами.
//        WebRTC: для реализации передачи аудио и демонстрации экрана. Это основная библиотека, которая позволит
//        захватывать экран и передавать медиа-потоки.
//        Socket.IO (или чистый WebSocket API):
//
//Для реализации обмена сообщениями в реальном времени между клиентом и сервером.
//        Реализация функциональности
//1. Демонстрация экрана:
//Захват экрана:
//Используй WebRTC для захвата экрана. Это можно сделать с помощью метода getDisplayMedia()
// для получения доступа к экрану пользователя.
//javascript
//Копировать код
//navigator.mediaDevices.getDisplayMedia({ video: true })
//        .then(stream => {
//    // Здесь можно подключить поток к видеоэлементу для отображения
//    const videoElement = document.getElementById('screenPreview');
//    videoElement.srcObject = stream;
//
//    // Отправка потока на сервер или другим участникам
//})
//        .catch(error => {
//        console.error("Error accessing display media.", error);
//  });
//Передача потока:
//После захвата экрана можно передавать видео-поток через WebRTC. Используй RTCPeerConnection
// для установления соединения между пользователями.
//2. Аудиопоток:
//Аналогично демонстрации экрана, захват звука можно осуществить с помощью getUserMedia().
//javascript
//Копировать код
//navigator.mediaDevices.getUserMedia({ audio: true })
//        .then(stream => {
//    // Передай аудиопоток через WebRTC
//})
//        .catch(error => {
//        console.error("Error accessing audio media.", error);
//  });
//          3. Обмен сообщениями:
//Используй REST API для получения информации о текущем состоянии (например, когда пользователь начинает демонстрацию экрана).
//WebSocket или Socket.IO для управления взаимодействием в реальном времени (например, синхронизация состояний между участниками).
//Что реализовать самостоятельно:
//REST API на Spring для управления сессиями и участниками (например, создание и удаление комнат, управление правами).
//Логику многопоточности на сервере для обработки нескольких подключений и потоков аудио/видео.
//Пользовательский интерфейс на HTML/CSS/JavaScript с возможностью взаимодействия с REST API и WebSocket.
//        Синхронизация состояний (например, текущая страница демонстрации экрана или состояние микрофона) через WebSocket.
//Важные аспекты:
//Убедись, что пользователи имеют возможность разрешать доступ к своему экрану и аудиоустройствам.
//Следи за безопасностью передачи данных, особенно если работаешь с личной информацией.
//Этот подход поможет тебе организовать проект эффективно и продемонстрировать как свои навыки,
// так и использование актуальных технологий. Если есть еще вопросы или что-то, что нужно уточнить, дай знать!

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication

public class Main {
    private final Environment environment;

    public Main(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        String host = environment.getProperty("server.address", "localhost");
        int port = event.getWebServer().getPort();
        System.out.println("Application started at http://" + host + ":" + port);
    }


}

