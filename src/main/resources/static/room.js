const roomId = "1234";
const userName = prompt("Enter your name:");
const participantsList = document.getElementById("participantsList");
let socket;

let localStream;
let remoteStream;
let peerConnection;

let pendingCandidates = [];


const videoConstraints = {
  video: true,
  audio: true
};

// ICE-сервера для WebRTC
const iceServers = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' } // Вы можете использовать свой STUN/TURN сервер
  ]
};

// Функция для отправки сообщения
function sendMessage() {
  const messageInput = document.getElementById("messageInput");

  socket.send(JSON.stringify({
    "message": {
      userName,
      roomId,
      "message": messageInput.value
    }, "messageStatus": "MESSAGE"
  }));
  messageInput.value = ""; // Очищаем поле ввода
}


async function createRoom() {
  const url = `${window.location.origin}/ws/` + roomId;
  // Открываем WebSocket соединение
  socket = new WebSocket(url);

  // Отправляем сообщение о подключении
  socket.onopen = function () {
    socket.send(JSON.stringify({
      "message": { userName, roomId, "message": "", },
      "messageStatus": "ESTABLISHING"
    }));
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
  const msg = JSON.parse(data.message);

  // Проверяем статус сообщения
  if (data.messageStatus == "PARTICIPANTS") {
    updateParticipantsList(msg.message);
  } else if (data.messageStatus == "MESSAGE") {
    // Обработка обычного сообщения
    displayMessage(`${msg.message}`);
  } else if (data.messageStatus === "ICE_CANDIDATE") {
    // Получили ICE-кандидат
    handleNewIceCandidate(msg);
  } else if (data.messageStatus === "VIDEO_ANSWER") {
    // Получили видео-ответ
    handleVideoAnswer(msg);
  }else if (data.messageStatus === "VIDEO_OFFER") { 
    handleVideoOffer(msg);
  }
}

// Функция для отображения сообщения в HTML
function displayMessage(message) {
  const messagesDiv = document.getElementById("messages");
  messagesDiv.innerHTML += `<div>${message}</div>`;
  messagesDiv.scrollTop = messagesDiv.scrollHeight; // Прокручиваем вниз
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

function toggleChat() {
  const chatContainer = document.getElementById("chatContainer");
  const participantsDiv = document.getElementById("participants");

  if (chatContainer.style.display === "none") {
    chatContainer.style.display = "flex"; // Show chat
    participantsDiv.style.display = "none"; // Hide participants
  } else {
    chatContainer.style.display = "none"; // Hide chat
  }
}


// 7. Обработка видео-предложения (получено через WebSocket)
function handleVideoOffer(offer) {
  if (!peerConnection) {
    createPeerConnection();
  }


  peerConnection.setRemoteDescription(new RTCSessionDescription(offer))
    .then(() => {
      // Добавляем отложенные ICE-кандидаты
      pendingCandidates.forEach(candidate => {
        peerConnection.addIceCandidate(candidate)
          .catch(error => console.error("Ошибка при добавлении отложенного ICE-кандидата: ", error));
      });
      pendingCandidates = []; // Очищаем очередь кандидатов
    })
    .then(() => {
      // Получение локального медиа и добавление его в PeerConnection
      return navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    })
    .then(stream => {
      localStream = stream;
      document.getElementById('localVideo').srcObject = stream;
      stream.getTracks().forEach(track => peerConnection.addTrack(track, stream));
    })
    .then(() => {
      // Создание и отправка SDP-ответа
      return peerConnection.createAnswer();
    })
    .then(answer => peerConnection.setLocalDescription(answer))
    .then(() => {
      socket.send(JSON.stringify({
        messageStatus: 'VIDEO_ANSWER',
        message: peerConnection.localDescription
      }));
    })
    .catch(error => console.error("Ошибка обработки видео-предложения: ", error));
}

// 8. Обработка видео-ответа (получено через WebSocket)
function handleVideoAnswer(answer) {
  peerConnection.setRemoteDescription(new RTCSessionDescription(answer))
    .catch(error => console.error("Ошибка при установке удалённого SDP: ", error));
}

// 9. Обработка ICE-кандидата (получено через WebSocket)
function handleNewIceCandidate(candidate) {
  if (peerConnection.remoteDescription && peerConnection.remoteDescription.type) {
    peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
      .catch(error => console.error("Ошибка при добавлении ICE-кандидата: ", error));
  } else {
    // Сохраняем кандидат в очередь, если удалённая дескрипция ещё не установлена
    pendingCandidates.push(candidate);
  }
}










function toggleParticipants() {
  const chatContainer = document.getElementById("chatContainer");
  const participantsDiv = document.getElementById("participants");

  if (participantsDiv.style.display === "none" || participantsDiv.style.display === "") {
    participantsDiv.style.display = "block"; // Show participants
    chatContainer.style.display = "none"; // Hide chat
  } else {
    participantsDiv.style.display = "none"; // Hide participants
  }
}

async function startVideoCall() {
  // Создаем PeerConnection заранее
  createPeerConnection();

  console.log(navigator.mediaDevices);

  // Get user media
  localStream = await navigator.mediaDevices.getUserMedia(videoConstraints);
  document.getElementById('localVideo').srcObject = localStream;

  // Set up WebRTC connection
  peerConnection = new RTCPeerConnection();

  // Add local stream to the connection
  localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));

  // Handle remote stream
  peerConnection.ontrack = event => {
    remoteStream = event.streams[0];
    document.getElementById('remoteVideo').srcObject = remoteStream;
  };

  // Send signaling data via WebSocket
  peerConnection.onicecandidate = event => {
    if (event.candidate) {
      socket.send(JSON.stringify({
        "messageStatus": 'ICE_CANDIDATE',
        "message": event.candidate
      }));
    }
  };

  // Create and send offer
  const offer = await peerConnection.createOffer();
  await peerConnection.setLocalDescription(offer);
  socket.send(JSON.stringify({
    "messageStatus": 'VIDEO_OFFER',
    "message": offer
  }));
}




// 1. Инициализация соединения WebRTC
function createPeerConnection() {
  peerConnection = new RTCPeerConnection(iceServers);

  // 2. Добавление локальных ICE-кандидатов
  peerConnection.onicecandidate = event => {
    if (event.candidate) {
      socket.send(JSON.stringify({
        messageStatus: 'ICE_CANDIDATE',
        candidate: event.candidate
      }));
    }
  };

  // 3. Получение удалённого потока и добавление его на видеоэлемент
  peerConnection.ontrack = event => {
    if (!remoteStream) {
      remoteStream = new MediaStream();
      document.getElementById('remoteVideo').srcObject = remoteStream;
    }
    remoteStream.addTrack(event.track);
  };
}


