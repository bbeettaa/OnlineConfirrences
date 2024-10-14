const localVideo = document.getElementById('localVideo');
const remoteVideo = document.getElementById('remoteVideo');
const roomIdInput = document.getElementById('roomId');
const createRoomButton = document.getElementById('createRoom');
const joinRoomButton = document.getElementById('joinRoom');

let localStream;
let remoteStream;
let peerConnection;

// Получаем доступ к аудио и видео
async function startLocalStream() {
    localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    localVideo.srcObject = localStream;
}

// Создаем комнату
async function createRoom() {
    const roomId = roomIdInput.value;
    const response = await fetch('/api/rooms', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ roomId }),
    });
    const room = await response.json();
    console.log('Room created:', room);
}

// Присоединяемся к комнате
async function joinRoom() {
    const roomId = roomIdInput.value;
    const uri = `/api/rooms/${roomId}`;
    const response = await fetch(uri);
    const room = await response.json();
    console.log('Joined room:', room);
}

// Инициализация
startLocalStream();
createRoomButton.addEventListener('click', createRoom);
joinRoomButton.addEventListener('click', joinRoom);
