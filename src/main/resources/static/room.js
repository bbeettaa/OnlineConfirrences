const roomId = "1234";
const userName = prompt("Enter your name:");
const participantsList = document.getElementById("participantsList");


const ws = new WebSocket('wss://' + location.host + '/stream');

let webRtcPeer;

// UI
let uiLocalVideo;
let uiState = null;
const UI_IDLE = 0;
const UI_STARTING = 1;
const UI_STARTED = 2;

const remoteStreams = {};// Object to store remote streams by userId
let candidateWrappers = []; // Массив для хранения кандидатов с userID



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
function sendChatMessage() {
  const messageInput = document.getElementById("messageInput");

  ws.send(JSON.stringify({
    "message": {
      userName,
      roomId,
      "message": messageInput.value
    }, "messageStatus": "MESSAGE"
  }));
  messageInput.value = "";
}


function sendMessage(message) {
  if (ws.readyState !== ws.OPEN) {
    console.warn("[sendMessage] Skip, WebSocket session isn't open");
    return;
  }
  const jsonMessage = JSON.stringify(message);
  console.log("[sendMessage] message: " + jsonMessage);
  ws.send(jsonMessage);
}



ws.onmessage = function (event) {
  const jsonMessage = JSON.parse(event.data);
  console.log("[onmessage] Received message: " + event.data);

  switch (jsonMessage.messageStatus) {
    case 'PARTICIPANTS':
      updateParticipantsList(jsonMessage.message);
      break;
    case 'MESSAGE':
      let msg = JSON.parse(jsonMessage.message);
      displayMessage(`${msg.message}`);
      break;
    case 'PROCESS_SDP_ANSWER':
      handleProcessSdpAnswer(jsonMessage);
      break;
    case 'ADD_ICE_CANDIDATE':
      handleAddIceCandidate(jsonMessage);
      break;
    case 'ERROR':
      handleError(jsonMessage);
      break;
    default:
      // Ignore the message
      console.warn("[onmessage] Invalid message, id: " + jsonMessage.id);
      break;
  }
}

ws.onopen = function () {
  ws.send(JSON.stringify({
    "message": { userName, roomId, "message": "", },
    "messageStatus": "ESTABLISHING"
  }));
};

// Функция для отображения сообщения в HTML
function displayMessage(message) {
  const messagesDiv = document.getElementById("messages");
  messagesDiv.innerHTML += `<div>${message}</div>`;
  messagesDiv.scrollTop = messagesDiv.scrollHeight; // Прокручиваем вниз
}

// Функция для обновления списка участников
function updateParticipantsList(message) {
  participants = JSON.parse(message).message
  participantsList.innerHTML = ''; // Очищаем список
  JSON.parse(participants).forEach((participant) => {
    if (participant && participant != userName) {
      const li = document.createElement("li");
      li.textContent = participant;
      li.className = "list-group-item";
      participantsList.appendChild(li);
      startStream(participant)
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

// PROCESS_SDP_ANSWER ----------------------------------------------------------

function handleProcessSdpAnswer(jsonMessage) {
  console.log("[handleProcessSdpAnswer] SDP Answer from Kurento, process in WebRTC Peer");

  if (webRtcPeer == null) {
    console.warn("[handleProcessSdpAnswer] Skip, no WebRTC Peer");
    return;
  }

  webRtcPeer.processAnswer(jsonMessage.message, (err) => {
    if (err) {
      sendError("[handleProcessSdpAnswer] Error: " + err);
      stop();
      return;
    }

    console.log("[handleProcessSdpAnswer] SDP Answer ready; start remote video");


    // let uiRemoteVideo = addParticipantVideo("testuser").getElementsByTagName("video")[0];
    startVideo(uiRemoteVideo);
  });
  
}


// ADD_ICE_CANDIDATE -----------------------------------------------------------

function handleAddIceCandidate(jsonMessage) {
  candidate = JSON.parse(jsonMessage.message)
  if (webRtcPeer == null) {
    console.warn("[handleAddIceCandidate] Skip, no WebRTC Peer");
    return;
  }

  webRtcPeer.addIceCandidate(candidate, (err) => {
    if (err) {
      console.error("[handleAddIceCandidate] " + err);
      return;
    }
    console.log("[handleAddIceCandidate] ICE Candidate added: ", candidate);
  });
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

function startVideo(video) {
  // Manually start the <video> HTML element
  // This is used instead of the 'autoplay' attribute, because iOS Safari
  // requires a direct user interaction in order to play a video with audio.
  // Ref: https://developer.mozilla.org/en-US/docs/Web/HTML/Element/video
  video.play().catch((err) => {
    if (err.name === 'NotAllowedError') {
      console.error("[start] Browser doesn't allow playing video: " + err);
    }
    else {
      console.error("[start] Error in video.play(): " + err);
    }
  });
}

window.onload = function () {
  console.log("Page loaded");
  uiLocalVideo = document.getElementById('localVideo');
  // startStream("jsonMessage.userId");
  navigator.mediaDevices.getUserMedia({ video: true, audio: true })
  .then((stream) => {
    uiLocalVideo.srcObject = stream;
    uiLocalVideo.play();
  })
  .catch((error) => {
    console.error("Ошибка при получении локального медиа-потока: ", error);
  });
}


// Start -----------------------------------------------------------------------

function startStream(name) {
  uiRemoteVideo =  addParticipantVideo(name).getElementsByTagName("video")[0]
  console.log("[start] Create WebRtcPeerSendrecv");

  const options = {
    // localVideo: uiLocalVideo,
    remoteVideo: uiRemoteVideo,
    mediaConstraints: { audio: true, video: true },
    onicecandidate: (candidate) => sendMessage({
      messageStatus: 'ADD_ICE_CANDIDATE',
      message: candidate,
    }),
  };

  webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
    function (err) {
      if (err) {
        sendError("[start/WebRtcPeerSendrecv] Error: " + explainUserMediaError(err));
        stop();
        return;
      }

      console.log("[start/WebRtcPeerSendrecv] Created; start local video");
      startVideo(uiLocalVideo);

      console.log("[start/WebRtcPeerSendrecv] Generate SDP Offer");
      webRtcPeer.generateOffer((err, sdp) => {
        if (err) {
          sendError("[start/WebRtcPeerSendrecv/generateOffer] Error: " + err);
          stop();
          return;
        }

        sendMessage({
          messageStatus: 'SDP_OFFER',
          message: { sdp }
        });

        // console.log("[start/WebRtcPeerSendrecv/generateOffer] Done!");
      });
    });
}

// Stop ------------------------------------------------------------------------

function uiStop() {
  stop();
}

// -----------------------------------------------------------------------------

function sendError(message) {
  console.error(message);

  sendMessage({
    messageStatus: 'ERROR',
    message: message,
  });
}




