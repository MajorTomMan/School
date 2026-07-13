# Architecture

## Product boundary

The first usable slice focuses on one learner, one Android device, and one chapter:

- Subject: Mathematics
- Book: Grade 7, volume 1
- Chapter: Rational numbers
- Core loop: learn → practice → diagnose → review

No login, class, ranking, social feed, or admin console is included.

## Runtime layers

1. **Compose UI** — today plan, course path, learning, practice, review, settings.
2. **Domain models** — lessons, mastery states, reviews, answer evaluation.
3. **Local data** — the prototype uses static sample data; Room will replace it.
4. **AI provider boundary** — llama.cpp and OpenAI-compatible endpoints implement one interface.
5. **Material package** — PDF pages and structured JSON remain outside the APK and are imported separately.

## Planned modules

The prototype deliberately starts as one Android module so the product loop can be validated quickly. Split modules only after boundaries become real:

- `core-model`
- `core-database`
- `core-ai`
- `feature-today`
- `feature-course`
- `feature-learning`
- `feature-review`

## Material processing contract

The future Python processor exports a package similar to:

```text
book-package/
├── manifest.json
├── catalog.json
├── concepts.json
├── exercises.json
├── pages/
└── thumbnails/
```

Each lesson references exact source pages, concepts, examples, exercises, prerequisites, and figures. The app never asks a language model to infer the whole book on every request.
