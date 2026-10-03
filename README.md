Bookshelf
=========

A site for persisting the tales from your childhood, exchanging your favourite novels and searching matching literature suggestions.<br/>

Used frameworks<br/>
JSF/Primefaces + Hibernate<br/>

Content
Catalogue - main information:<br/>
Surroundings - environment (rooms, bookcases, shelves)<br/>
Library - available books<br/>
Heritage - book search, recommended, TOP<br/>
Neighbours - friends, guests, suggested friends<br/>
Contribution - personal contribution to the project, achievements, influence, authorship<br/>
Description - settings, "about me" configuration<br/>
Statistics - number of books, percentage by genre, year, pages etc.<br/>

---

# Home Library

Find any book in your home library in seconds: **by title, author or ISBN**, see **exactly where it stands**
(room · shelf row · position from the left · front/back row) and **whether you've read it**.

- **`android/`** – Kotlin + Jetpack Compose app. The home screen draws your shelves as wooden planks with
  book spines, covers from the camera or gallery, and AI summaries.
- **`backend/`** – Spring Boot REST API with a SQLite database and Spring AI for book summaries.

```
Android app ──REST/JSON──► Spring Boot (backend/) ──JPA──► SQLite file (library.db)
     │  X-LLM-Api-Key / X-LLM-Base-Url / X-LLM-Model headers      │
     └─────────────────────────────────────────────────────────────► any OpenAI-compatible LLM (Groq, OpenRouter, Gemini…)
```

## Data model

| Table | Columns |
|---|---|
| `book` | name, description, photo, isbn, genre, language, publish_year, pages, is_read, date_read, author_id, shelf_id, position_number, depth_row |
| `shelf` | location (text), row_num, orientation (`HORIZONTAL` = lying flat / `VERTICAL` = standing) |
| `author` | name |

- A book's place is its **shelf** (location + row), plus **position_number** (slot counted from the left, starting at 1)
  and **depth_row** (1 = front row, 2 = the row behind it, …). Each slot holds at most one book.
- An ISBN is optional. It must be a valid ISBN-10/13 and is stored without hyphens.
- Flyway (`backend/src/main/resources/db/migration`) owns the schema.

Data access uses Spring Data JPA with one JPQL query for the combined search. Spring Data JDBC ships no SQLite
dialect and doesn't map the book → author/shelf links, so it would only add code here.

## Run the backend

```bash
cd backend
./gradlew bootRun            # SQLite file at ./data/library.db (override with LIBRARY_DB_PATH)
./gradlew build              # compile + tests (each test class runs on its own SQLite file)
```

Or with Docker (the database lives in the `library-data` volume):

```bash
docker compose up --build
```

API docs: http://localhost:8080/swagger-ui/index.html

### Main endpoints

| | |
|---|---|
| `GET /api/books?q=&shelfId=&read=&language=` | search (title/author substring or exact ISBN) |
| `POST /api/books`, `PUT /api/books/{id}`, `DELETE /api/books/{id}` | create/update/delete (409 if the slot or ISBN is taken) |
| `PATCH /api/books/{id}/read` | `{ "read": true, "dateRead": "2026-10-03" }` (date defaults to today) |
| `PUT /api/books/{id}/cover` (multipart `file`), `GET /api/books/{id}/cover` | cover photo |
| `POST /api/ai/summary` | `{ "title", "author", "isbn", "language" }` → `{ "summary" }` |
| `GET/POST/PUT/DELETE /api/shelves`, `GET /api/shelves/{id}/books`, `GET /api/shelves/{id}/next-position?depthRow=` | shelves and their first free slot |
| `GET /api/authors?q=`, `GET /api/books/languages` | autocomplete |

## AI summaries

When you save a book with an empty description, or tap **✨ Generate summary**, the backend asks an LLM for a
3–4 sentence spoiler-free summary, written in the book's language. The LLM only gets the title, author and ISBN.

The backend has no key of its own. Each user enters one in the app under **Settings → AI book summaries**.
The app sends it in a request header, and the server never stores it. Free options:

| Provider | Base URL | Model | Key |
|---|---|---|---|
| Groq (default) | `https://api.groq.com/openai/v1` | `llama-3.1-8b-instant` | https://console.groq.com/keys |
| OpenRouter | `https://openrouter.ai/api/v1` | `meta-llama/llama-3.3-70b-instruct:free` | https://openrouter.ai/keys |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.5-flash` | https://aistudio.google.com/apikey |

Without a key, descriptions are simply typed by hand.

## Run the Android app

Open `android/` in Android Studio and run it, or:

```bash
cd android
./gradlew installDebug
```

Under **Settings**, set the server URL: `http://10.0.2.2:8080/` from the emulator (the default), or
`http://<your computer's LAN IP>:8080/` from a phone on the same Wi-Fi. Then:

1. **Menu → Manage shelves**: add a shelf for every row of your bookcases (e.g. *Living room*, rows 1–5).
2. **+**: add a book with its title and author, take a photo of the cover, pick its shelf and depth row
   (the first free position is filled in), and mark whether you've read it.
3. On the home screen, search by title, author or ISBN. Matching books light up on their shelves, and
   a list shows exactly where each one is.

CI (`.github/workflows`) builds the backend with tests and builds a debug APK, which you can download from
the workflow run's artifacts.
