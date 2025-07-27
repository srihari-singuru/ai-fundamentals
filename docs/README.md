# AI Fundamentals Documentation

This directory contains all project documentation for the AI Fundamentals application.

## Documentation Index

### Setup and Configuration
- [Distributed Tracing Configuration](TRACING.md) - How to configure and use Zipkin tracing

### Development and Maintenance
- [Codebase Cleanup Summary](CLEANUP_SUMMARY.md) - Summary of recent codebase cleanup activities

### Bug Fixes and Improvements
- [Favicon 404 Error Fix](FAVICON_FIX.md) - Fix for favicon-related error logging

## Project Overview

AI Fundamentals is a production-ready AI-powered chat application that provides both web UI and REST API interfaces for interacting with OpenAI's GPT models.

### Key Features
- **Dual Interface**: Interactive web chat UI and programmatic REST API
- **Real-time Streaming**: Reactive token streaming using Spring WebFlux
- **Conversation Memory**: Persistent chat history across user sessions
- **Production Ready**: Comprehensive error handling, monitoring, and resilience patterns

### Quick Start
For quick start instructions and basic usage, see the main [README.md](../README.md) in the project root.

## Contributing

When adding new documentation:
1. Place all documentation files in this `docs/` directory
2. Update this README.md index with links to new documentation
3. Use clear, descriptive filenames
4. Follow the existing documentation style and format

## Documentation Standards

- Use Markdown format (.md) for all documentation
- Include a brief description at the top of each document
- Use clear headings and subheadings for organization
- Include code examples where appropriate
- Keep documentation up-to-date with code changes