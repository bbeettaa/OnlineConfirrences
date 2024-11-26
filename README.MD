Багатопотоковий Додаток для Відеоконференцій

Опис
Цей проєкт є багатопотоковим додатком, що дозволяє користувачам брати участь у відеоконференціях і обмінюватися медіапотоками в реальному часі.

Основний функціонал
* Підключення до конференції через WebSocket.
* Передача аудіо та відео через WebRTC.
* Обробка медіапотоків на сервері за допомогою GStreamer.
* Інтерактивний інтерфейс для управління потоками та чатом.

Архітектура Додатку

* Клієнтська частина
    - Використовує HTML/CSS/JavaScript.
* Серверна частина
    - Написана на Spring Boot.
    - Використовує Spring WebSocket для підтримки WebSocket-з'єднань.
    - Реалізує логіку управління конференціями.
    - Реалізує обмін повідомленнями між учасниками.
    - Реалізує обробку медіапотоків за допомогою GStreamer.
    - Багатопотокова обробка реалізована за допомогою WebRTC.

Процес підключення до конференції
* Встановлення WebSocket-з'єднання:
```javascript
const ws = new WebSocket('wss://' + location.host + '/stream');
```

Створення WebRTC-з'єднання:
```javascript
let webRtcPeer;

function createPeerConnection() {
webRtcPeer = new RTCPeerConnection(iceServers);
webRtcPeer.onicecandidate = (event) => {
if (event.candidate) {
ws.send(JSON.stringify({ type: 'iceCandidate', candidate: event.candidate }));
}
};
webRtcPeer.onaddstream = (event) => {
remoteStreams[event.stream.id] = event.stream;
};
}
```


Додавання ICE-кандидатів:
```javascript
function addIceCandidate(candidate) {
webRtcPeer.addIceCandidate(new RTCIceCandidate(candidate));
}
```
Обробка медіапотоків.
Додавання медіапотоку в з'єднання:
```javascript
function addMediaStream(stream) {
webRtcPeer.addStream(stream);
}
```

Обробка медіапотоку:
```javascript
function processMediaStream(stream) {
const video = document.getElementById('video');
video.srcObject = stream;
}
```
Управління SDP-повідомленнями.
Обробка SDP-повідомлення:

```javascript
function processSdpMessage(message) {
webRtcPeer.setRemoteDescription(new RTCSessionDescription({ type: message.type, sdp: message.sdp }));
}
```
Надсилання SDP-пропозиції:

```javascript
function sendOffer() {
ws.send(JSON.stringify({ type: 'offer', sdp: offer.sdp }));
}
```

Сценарій роботи додатку:
1) Користувач A відкриває вебдодаток і підключається до конференції через WebSocket.
2) Користувач A створює медіапотік (аудіо/відео) і відправляє SDP-пропозицію серверу.
3) Користувач B підключається до тієї ж конференції та отримує SDP-пропозицію від сервера.
4) B створює з'єднання, додає медіапотік від A і відправляє SDP-відповідь.
5) Сервер маршрутизує медіапотоки між A і B. Обмін медіапотоками відбувається в реальному часі.

