Here's a `README.md` template for your trading platform project. This document covers setup, configuration, and basic usage to help users and developers get started.

---

# Trading Platform

This project is a trading platform built with Spring Boot and integrates with the Alpaca API to automate stock trading based on market conditions and specific trading strategies. It supports both regular and extended-hours trading sessions using a martingale strategy and other custom-defined strategies.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Setup](#setup)
    - [Requirements](#requirements)
    - [Configuration](#configuration)
    - [Docker Setup](#docker-setup)
- [Usage](#usage)
- [APIs](#apis)
- [License](#license)

## Overview

The trading platform uses the Alpaca API to place buy and sell orders for selected stocks based on pre-configured strategies. The platform also stores trading history, manages different market sessions, and supports extended hours trading using limit orders.

## Features

- **Automated Trading Strategies**: Supports martingale trading and other user-defined strategies.
- **Extended Hours Trading**: Places limit orders during extended hours (pre-market and after-hours) when specific criteria are met.
- **Environment Flexibility**: Configurable for both paper and live trading environments.
- **Order Caching**: Caches trading history in Redis for quick retrieval.
- **Database-Driven Configuration**: Stores and retrieves trading configurations from a PostgreSQL database.
- **Concurrency Support**: Utilizes a multi-threaded approach to process multiple stock tickers concurrently.

## Architecture

The project is built with the following architecture:

- **Spring Boot**: For REST API and dependency injection.
- **Alpaca API Integration**: Handles market data and order placement.
- **PostgreSQL**: Manages configurations and trading history.
- **Redis**: Caches trading data for fast access.
- **Docker**: Docker Compose configuration for a quick setup of required services.

## Setup

### Requirements

- Java 17+
- Docker and Docker Compose
- Alpaca account with API keys

### Configuration

The project requires a configuration file for Alpaca credentials and database settings. Create an `application.yml` in the `src/main/resources` directory with the following structure:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_platform
    username: postgres
    password: yourpassword
  redis:
    host: localhost
    port: 6379

alpaca:
  environment: PAPER # Set to PAPER or LIVE
```

### Docker Setup

The platform uses Docker Compose to manage PostgreSQL and Redis instances.

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/trading-platform.git
   cd trading-platform
   ```

2. Start services using Docker Compose:
   ```bash
   docker-compose up -d
   ```

3. Verify that the PostgreSQL and Redis services are running:
   ```bash
   docker ps
   ```

### Running the Application

1. Build and run the application:
   ```bash
   ./gradlew bootRun
   ```

2. Access the API at `http://localhost:8080`.

## Usage

### Initial Buy Orders

When the application starts, it checks for any existing buy orders for active tickers. If no previous buy orders are found, it places an initial buy order at the current price to start trading from that point.

### Automated Trading

The `TradeService` handles automated trading for active tickers based on the configured strategy and market conditions. Trades can be scheduled to execute based on the configuration frequency, adjusted dynamically via the API.

## APIs

### Trading History

- **Get All Trades**: `GET /api/trades`
- **Get Trades by Ticker**: `GET /api/trades/{symbol}` (supports pagination and Redis caching)

### Configuration Management

- **Update Frequency**: `PUT /api/config/frequency`
- **Get Current Frequency**: `GET /api/config/frequency`

### Health Check

- **Market Status**: `GET /api/market/status` - Checks if the market is currently open.

## License

This project is licensed under the MIT License.

---

This `README.md` provides a comprehensive overview to help users and developers understand the setup, architecture, and key components of the project. If there are additional requirements or features, feel free to expand upon this document.