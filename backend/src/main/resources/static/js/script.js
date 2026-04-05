const API_URL = "/api/chat";
const THEME_KEY = "college-chatbot-theme";
const WELCOME_MESSAGE = "Hello! You can ask any admission help desk question, and I will answer as clearly as possible.";

const chatForm = document.getElementById("chatForm");
const messageInput = document.getElementById("messageInput");
const chatMessages = document.getElementById("chatMessages");
const sendButton = document.getElementById("sendButton");
const apiStatus = document.getElementById("apiStatus");
const suggestionChips = document.querySelectorAll(".suggestion-chip");
const themeToggle = document.getElementById("themeToggle");

const conversationHistory = [
    {
        role: "assistant",
        content: WELCOME_MESSAGE
    }
];

function addMessage(role, text) {
    const message = document.createElement("article");
    message.className = `message ${role}`;

    const avatar = document.createElement("div");
    avatar.className = "avatar";
    avatar.textContent = role === "user" ? "You" : "AI";

    const bubble = document.createElement("div");
    bubble.className = "bubble";
    bubble.textContent = text;

    message.appendChild(avatar);
    message.appendChild(bubble);
    chatMessages.appendChild(message);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function setLoadingState(isLoading) {
    sendButton.disabled = isLoading;
    sendButton.textContent = isLoading ? "Sending..." : "Send";
    apiStatus.textContent = isLoading ? "Typing" : "Ready";
}

function getRecentHistory() {
    return conversationHistory.slice(-10);
}

function applyTheme(theme) {
    document.body.dataset.theme = theme;
    themeToggle.textContent = theme === "dark" ? "Light Mode" : "Dark Mode";
    localStorage.setItem(THEME_KEY, theme);
}

function initializeTheme() {
    const savedTheme = localStorage.getItem(THEME_KEY);
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    applyTheme(savedTheme || (prefersDark ? "dark" : "light"));
}

async function sendMessage(message) {
    addMessage("user", message);
    conversationHistory.push({ role: "user", content: message });
    setLoadingState(true);

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                message,
                history: getRecentHistory().slice(0, -1)
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        const reply = data.reply || "No reply was returned by the server.";
        addMessage("bot", reply);
        conversationHistory.push({ role: "assistant", content: reply });
        apiStatus.textContent = "Online";
    } catch (error) {
        apiStatus.textContent = "Offline";
        const fallback = "Could not connect to the backend. Please make sure the Spring Boot server is running, then try again.";
        addMessage("bot", fallback);
        conversationHistory.push({ role: "assistant", content: fallback });
    } finally {
        setLoadingState(false);
    }
}

chatForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const message = messageInput.value.trim();

    if (!message) {
        return;
    }

    messageInput.value = "";
    messageInput.style.height = "auto";
    await sendMessage(message);
});

messageInput.addEventListener("input", () => {
    messageInput.style.height = "auto";
    messageInput.style.height = `${messageInput.scrollHeight}px`;
});

messageInput.addEventListener("keydown", async (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        chatForm.requestSubmit();
    }
});

suggestionChips.forEach((chip) => {
    chip.addEventListener("click", async () => {
        const question = chip.dataset.question;
        messageInput.value = "";
        await sendMessage(question);
    });
});

themeToggle.addEventListener("click", () => {
    const nextTheme = document.body.dataset.theme === "dark" ? "light" : "dark";
    applyTheme(nextTheme);
});

initializeTheme();
