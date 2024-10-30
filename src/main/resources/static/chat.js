let activeUserId = null; // Хранит ID активного пользователя
let pinnedUserId = null; // Хранит ID закрепленного пользователя

// Функція для переключення чату
function toggleChat() {
    const chatContainer = document.getElementById("chatContainer");
    const participantsDiv = document.getElementById("participants");

    if (chatContainer.style.display === "none") {
        chatContainer.style.display = "flex"; // Показати чат
        participantsDiv.style.display = "none"; // Сховати учасників
    } else {
        chatContainer.style.display = "none"; // Сховати чат
    }
}

// Функція для переключення учасників
function toggleParticipants() {
    const chatContainer = document.getElementById("chatContainer");
    const participantsDiv = document.getElementById("participants");

    if (participantsDiv.style.display === "none" || participantsDiv.style.display === "") {
        participantsDiv.style.display = "block"; // Показати учасників
        chatContainer.style.display = "none"; // Сховати чат
    } else {
        participantsDiv.style.display = "none"; // Сховати учасників
    }
}

document.getElementById("messageInput").addEventListener("keypress", function (event) {
    if (event.key === "Enter") {
        event.preventDefault();
        sendMessage();
    }
});

function addParticipantVideo(userId) {
    // Создаем основной контейнер для видео
    const videoContainer = document.createElement('div');
    videoContainer.id = `videoContainer_${userId}`;
    videoContainer.classList.add('video-container');

    // Создаем видеоэлемент
    const videoElement = document.createElement('video');
    videoElement.classList.add('participateVideo');
    videoElement.autoplay = true;
    videoElement.id = `remoteVideo_${userId}`;

    // Создаем контейнер для информации о видео
    const videoInfo = document.createElement('div');
    videoInfo.classList.add('video-info');

    // Добавляем имя пользователя в информацию о видео
    const usernameSpan = document.createElement('span');
    usernameSpan.classList.add('username');
    usernameSpan.textContent = userId;
    videoInfo.appendChild(usernameSpan);

    // Создаем кнопку для закрепления видео
    const videoButtons = document.createElement('div');
    videoButtons.classList.add('video-buttons');

    const pinIcon = document.createElement('span');
    pinIcon.classList.add('pin-icon');
    pinIcon.textContent = '📌';
    pinIcon.onclick = (event) => pinVideo(event);
    videoButtons.appendChild(pinIcon);

    // Добавляем все элементы в основной контейнер
    videoContainer.appendChild(videoElement);
    videoContainer.appendChild(videoInfo);
    videoContainer.appendChild(videoButtons);

    // Добавляем контейнер видео участника в контейнер для всех видео
    document.getElementById('remoteVideos').appendChild(videoContainer);
    return videoContainer;
}

function highlightActiveUser(volume){
    videoContainer = document.getElementById(`videoArea`);
    const borderColor = `rgba(0, 255, 0, ${Math.min(volume / 10, 1)})`;
    videoContainer.style.border = `8px solid ${borderColor}`;
}
function highlightNonActiveUser(){
    videoContainer = document.getElementById(`videoArea`);
    const borderColor = `#ccc`;
    videoContainer.style.border = `8px solid ${borderColor}`;
}

function highlightUser(volume, videoContainer) {
    const borderColor = `rgba(0, 255, 0, ${Math.min(volume / 10, 1)})`;
    videoContainer.style.border = `3px solid ${borderColor}`;

}
function highlightNonUser(videoContainer) {
    // Установка цвета границы в зависимости от громкости
    const borderColor = `rgba(0, 0, 0, 0.5)`;
    videoContainer.style.border = `3px solid ${borderColor}`;

}
function updateActiveUser(userId) {
    const videoContainer = document.getElementById(`videoContainer_${userId}`);

    // Если pinnedUserId равно null, обновляем активного пользователя
    if (pinnedUserId === null) {
        // Скрываем текущий активный видеопоток
        if (activeUserId !== null) {
            // Возвращаем предыдущего активного пользователя в список
            const previousActiveVideoContainer = document.getElementById(`videoContainer_${activeUserId}`);
            if (previousActiveVideoContainer) {
                previousActiveVideoContainer.style.display = 'block'; // Показываем видео предыдущего активного пользователя
            }
        }

        // Устанавливаем нового активного пользователя
        activeUserId = userId;

        // Скрываем видео контейнер текущего активного пользователя
        if (videoContainer) {
            videoContainer.style.display = 'none'; // Скрываем видео текущего активного пользователя
        }

        // Устанавливаем видео активного пользователя в элемент с id="activeVideo"
        const activeVideo = document.getElementById('activeVideo');
        if (activeVideo) {
            activeVideo.srcObject = remoteStreams[userId]; // Устанавливаем поток видео активного пользователя
            const usernameElement = document.querySelector('#videoArea .username');
            if (usernameElement) {
                usernameElement.textContent = userId; // Обновляем имя активного пользователя
            }
        }
    }
}