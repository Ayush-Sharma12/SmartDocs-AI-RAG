# SmartDocs Startup Guide

## Quick Start

### Start Everything
```powershell
.\start-all.ps1
```

### Start Backend Only
```powershell
.\start-backend.ps1
```

### Start Frontend Only
```powershell
.\start-frontend.ps1
```

## What The Scripts Do

### `start-backend.ps1`
- Loads environment variables from `.env`
- Verifies `OPENAI_API_KEY` is present
- Builds the backend if the JAR is missing
- Starts the backend with the `h2` local profile for reliable local development
- Uses Spring AI `SimpleVectorStore` for in-memory retrieval in the local profile
- Stops early with a clear message if the configured port is already in use

### `start-frontend.ps1`
- Installs frontend dependencies if needed
- Starts the Vite dev server on `http://localhost:3000`

### `start-all.ps1`
- Opens backend and frontend in separate PowerShell windows

## Requirements
- Java 17+
- Node.js 18+
- A `.env` file in the project root
- `OPENAI_API_KEY` set in `.env`

## Common Issues

### Port Already In Use
```powershell
netstat -ano | findstr :8080
```

- Stop the matching PID, or change `SERVER_PORT` in `.env`.

### PostgreSQL Is Broken Locally
- The helper scripts use the `h2` profile, so local startup does not depend on PostgreSQL.
- If you want the PostgreSQL/pgvector path, start the app without the `h2` profile after fixing your database service.

### OpenAI Key Missing
- Set `OPENAI_API_KEY` in `.env`.

## Notes
- The backend loads `.env` automatically at startup.
- The default profile still uses PostgreSQL + pgvector.
- The `h2` profile is intended for local fallback and development.
