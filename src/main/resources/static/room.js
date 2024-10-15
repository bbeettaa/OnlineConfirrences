    const roomId = "1234";
    const userName = prompt("Enter your name:");
    const participantsList = document.getElementById("participantsList");
    let socket;
    
    // Функция для отправки сообщения
    function sendMessage() {
      const messageInput = document.getElementById("messageInput");
      const message = messageInput.value;
      const messageStatus = "MESSAGE";

      if (message) {
        socket.send(JSON.stringify({ userName, roomId, message, messageStatus }));
        messageInput.value = ""; // Очищаем поле ввода
      }
    }

    // Функция для отображения сообщения в HTML
    function displayMessage(message) {
      const messagesDiv = document.getElementById("messages");
      messagesDiv.innerHTML += `<div>${message}</div>`;
      messagesDiv.scrollTop = messagesDiv.scrollHeight; // Прокручиваем вниз
    }


    async function createRoom() {
      const message = "";
      const messageStatus = "ESTABLISHING";

      const url = `${window.location.origin}/ws/` + roomId;
      // Открываем WebSocket соединение
      socket = new WebSocket(url);

      // Отправляем сообщение о подключении
      socket.onopen = function () {
        socket.send(JSON.stringify({ userName, roomId, message, messageStatus }));
      };

      // Получаем сообщение от сервера
      socket.onmessage = function (event) {
        onmessage(event);
      };

      // Обрабатываем закрытие соединения
      socket.onclose = function () {
        console.log("Соединение закрыто");
      };

      // Обрабатываем ошибки
      socket.onerror = function (error) {
        console.log("Ошибка: " + error.message);
      };
    }

    // Обработка сообщений от сервера
    function onmessage(event) {
      const data = JSON.parse(event.data);

      // Проверяем статус сообщения
      if (data.messageStatus == "PARTICIPANTS") {
        updateParticipantsList(data.message);
      } else if (data.messageStatus == "MESSAGE") {
        // Обработка обычного сообщения
        displayMessage(`${data.userName}: ${data.message}`);
      }
    }

    // Функция для обновления списка участников
    function updateParticipantsList(participants) {
      participantsList.innerHTML = ''; // Очищаем список
      JSON.parse(participants).forEach((participant) => {
        if (participant) {
          const li = document.createElement("li");
          li.textContent = participant;
          li.className = "list-group-item";
          participantsList.appendChild(li);
        }
      });
    }

    createRoom();