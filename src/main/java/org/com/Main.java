package org.com;



//Библиотеки и технологии
//Серверная часть:
//Spring Framework:
//
//Spring Boot для создания REST API, который будет обрабатывать запросы от клиентов.
//Spring WebSocket для поддержки WebSocket-соединений
//Spring Security Для управления правами доступа и аутентификации пользователей.
//JACKSON Для обработки JSON (сериализация и десериализация), что удобно для обмена данными между клиентом и сервером.
//Клиентская часть: HTML/CSS/JavaScript
//
//Основные технологии для создания пользовательского интерфейса.
//CSS-фреймворки (Bootstrap) для улучшения внешнего вида интерфейса и адаптивного дизайна.
//JavaScript библиотеки:
//      jQuery: для упрощения работы с DOM и AJAX-запросами.
//      WebRTC: для реализации передачи аудио и демонстрации экрана. передавать медиа-потоки.
//      WebSocket API: Для реализации обмена сообщениями в реальном времени между клиентом и сервером.
//

//      Функциональность:
//1. Демонстрация экрана:
//2. Аудиопоток:
//3. Обмен сообщениями:

//Что реализовать самостоятельно:
//REST API на Spring для управления сессиями и участниками (например, создание и удаление комнат, управление правами).
//Логику многопоточности на сервере для обработки нескольких подключений и потоков аудио/видео.
//Пользовательский интерфейс на HTML/CSS/JavaScript с возможностью взаимодействия с REST API и WebSocket.
//Синхронизация состояний (например, текущая страница демонстрации экрана или состояние микрофона) через WebSocket.


//Основные шаги:
//        WebSocket для динамического обновления подключенных пользователей.
//        Интеграция WebRTC для передачи аудио и видео.
//        Демонстрация экрана.
//        Обработка отключения пользователей при закрытии вкладки.


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
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

