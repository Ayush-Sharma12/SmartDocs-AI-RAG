# SmartDocs AI RAG System - Setup Guide

## Overview
SmartDocs is an AI-powered Retrieval-Augmented Generation (RAG) system using Spring Boot 3.2.5 that enables intelligent question-answering about Spring Boot documentation.

## Prerequisites
- Java 17+
- Maven 3.6+
- OpenAI API Key (for ChatModel)
- PostgreSQL 16+ with pgvector extension (optional)
- Docker & Docker Compose (optional)

## Quick Start

### 1. Build the Application
```bash
mvn clean compile
```

### 2. Start PostgreSQL (Optional)
```bash
docker-compose up -d
mvn spring-boot:run
```

### 3. Set OpenAI API Key (Optional)
```powershell
$env:OPENAI_API_KEY = "your-api-key-here"
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

The application starts with Spring Shell interface: `shell:>`

## Usage

### Ask Questions
```
shell:> q What is Spring Boot?
shell:> q How do I configure annotations?
shell:> q Explain dependency injection
```

The system will:
1. Search Spring Boot PDF documentation
2. Retrieve 3 most relevant documents
3. Send to OpenAI's gpt-4o-mini model
4. Return AI-generated answer based on documentation

## Features

### Architecture
- **Spring Boot 3.2.5** - Latest LTS version
- **Spring AI 1.0.0** - AI/ML integration
- **Spring Shell 3.2.4** - CLI interface
- **PostgreSQL + pgvector** - Vector database (optional)
- **OpenAI Integration** - GPT-4o-mini for text generation
- **Token-based Text Splitter** - Semantic chunking

### Key Components

#### PgVectorConfig
- Manages Vector Store bean creation
- Fallback to InMemoryVectorStore if database unavailable
- Supports both production and development modes

#### ReferenceDocsLoader
- Loads Spring Boot Reference PDF on startup
- Processes 974 pages with semantic chunking
- Populates vector store with 2-6 chunks per page

#### SpringAssistantCommand
- Spring Shell command handler
- Command: `q <question>`
- RAG pipeline implementation
- Optional ChatModel and VectorStore support

#### TestConfig
- Mocked VectorStore for unit tests
- Prevents database initialization in tests
- Enables isolated component testing

## Development

### Compile Only
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### View Logs
- Application starts on port 8080
- Spring Shell initialization logs visible
- PDF processing logs show progress

### Database Setup
```bash
# Via Docker Compose
docker-compose up -d

# Or manually create schema
psql -U postgres -d aidocs -f src/main/resources/schema.sql
```

## Troubleshooting

### ChatModel Bean Not Found
- Set `OPENAI_API_KEY` environment variable
- Or run without OpenAI (in-memory mode only)

### Port 8080 Already in Use
```powershell
Get-NetTCPConnection -LocalPort 8080 | Stop-Process -Force
```

### Database Connection Refused
- Application gracefully falls back to in-memory mode
- Check Docker container: `docker-compose ps`
- Start with: `docker-compose up -d`

## Configuration Files
- `application.yaml` - Spring Boot configuration
- `compose.yaml` - Docker Compose for PostgreSQL
- `schema.sql` - Vector store database schema
- `pom.xml` - Maven dependencies

## Future Enhancements
- Admin UI for document management
- Multiple PDF support
- RAG performance metrics
- Vector store caching
- API endpoints for integration
