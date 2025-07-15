# 💬 AI Chatbot with Spring WebFlux

This project implements an AI-powered conversational chatbot using **Spring WebFlux**, **Spring AI**, **Thymeleaf**, and **Resilience4j**. It supports both a Web UI and a REST API, and adheres to clean architecture principles with full reactive programming support.

---

## ✨ Features

* Real-time token streaming with Project Reactor
* Stateful chat memory across sessions
* Dynamic system prompt customization
* Retry, fallback, and circuit breaker using Resilience4j
* Full separation of concerns: Controller, Service, Client, View Mapper
* Clean, testable, and modular design following SOLID principles

---

## 💠 Tech Stack

| Component      | Technology                            |
| -------------- | ------------------------------------- |
| Web Framework  | Spring WebFlux                        |
| AI Integration | Spring AI (OpenAI)                    |
| UI Layer       | Thymeleaf                             |
| REST API       | Token streaming via `Flux<String>`    |
| Resilience     | Resilience4j (Retry + CircuitBreaker) |
| Reactive Core  | Reactor: Mono / Flux                  |
| View Layer     | ChatMessageView DTO Mapping           |

---

## 🚀 Getting Started

### 1. Clone the Project

```bash
git clone https://github.com/srihari-singuru/ai-fundamentals.git
cd ai-fundamentals
```

### 2. Configure OpenAI API Key

#### Option 1: via environment variable

```bash
export OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

#### Option 2: in `application.yml`

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

---

## 🌐 How to Use

Access your chatbot at:

```
http://localhost:8080/chat
```

* Enter your message and press **Enter** or click **Send**
* Conversation persists per user session
* Update the system prompt at any time
* Click **Reset Conversation** to clear memory

Alternatively, for REST API:

```bash
curl http://localhost:8080/v1/chat-completion?userMessage=Hello
```

Response is streamed token-by-token.

---

## 📦 API Endpoints

### Web UI (Chat)

* `GET /chat` — loads chat page with history and form
* `POST /chat` — submits message and receives assistant response

### REST API (Streaming)

* `GET /v1/chat-completion?userMessage=...`

    * Returns `Flux<String>` of streamed tokens
    * Accepts `text/event-stream` -- I commented this, because thymeleaf doesn't support SSE (Server sent events)

---

## ♻️ Resilience Strategy

Implemented using **Resilience4j** with the following:

* **Retry**

    * On timeouts, IO errors, and rate-limiting
    * Backoff with jitter

* **Circuit Breaker**

    * Prevents flooding backend on repeated failure

```yaml
resilience4j:
  circuitbreaker:
    instances:
      chatCompletionCB:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
```

---

## 🧠 Architecture Overview

```
src/main/java/com/srihari/ai
├── api                  # REST API for chat completions
├── controller           # Web UI controller
├── service              # ChatService (business logic)
├── client               # ChatClientFactory
├── model               
│   ├── ConversationModel  # Form model for POST /chat
│   └── ChatMessageView    # DTO for rendering in UI
└── util                 # Helper methods (safe(), retry filters)
```

* Controller → Service → ChatClient
* Model to ViewMapper ensures clean DTO separation
* All error handling via `onErrorResume`

---

## 🧺 Clean Code Practices

* ✅ SOLID principles enforced
* ✅ No controller contains business logic
* ✅ `safe(Supplier, fallback)` guards risky UI calls
* ✅ All exceptions logged centrally
* ✅ Retry and fallback applied selectively
* ✅ Circuit breaker for external AI service

---

## 💡 Sample Flow

### Web UI

```
User: Tell me a joke
AI: Why don't skeletons fight each other? They don't have the guts.
```

### REST API

```bash
curl http://localhost:8080/v1/chat-completion?userMessage=Hi
```

Streamed response:

```
Hello!
How can I assist you today?
```

---

## 🔐 Error Handling & Logging

* Handled exceptions:

    * Timeouts
    * IOException
    * Rate-limiting (HTTP 429)
    * Server errors (HTTP 5xx)
* Unhandled fallback for client-visible messages
* All exceptions logged via `Slf4j`