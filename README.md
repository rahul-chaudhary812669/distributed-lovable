# My Lovable

> An AI-powered full-stack application platform that enables users to build, preview, and deploy modern web applications using natural language prompts.



![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F)
![React](https://img.shields.io/badge/React-19-61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![Redis](https://img.shields.io/badge/Redis-7-DC382D)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3-231F20)
![Docker](https://img.shields.io/badge/Docker-2496ED)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5)
![Google Cloud](https://img.shields.io/badge/Google_Cloud-GKE-4285F4)

# 🚀 Overview

My Lovable is a cloud-native AI application platform inspired by modern low-code builders. Users can create applications through AI prompts, preview them instantly, and deploy them on Kubernetes.

The platform combines a React frontend with a Java Spring Boot microservices backend and runs on Google Kubernetes Engine (GKE). It also integrates AI models, Redis, Kafka, and object storage to support scalable project generation and deployment.

# ✨ Features

- 🤖 AI-powered application generation
- 🔐 Secure authentication & authorization
- 📁 Project workspace management
- 🌐 Live preview for generated applications
- ⚡ Real-time deployment pipeline
- 📦 Cloud-native microservice architecture
- 📂 Object storage for project files
- 🔄 Background processing with Kafka
- 🚀 Kubernetes-based deployment
- 📊 Responsive React dashboard
- 🔍 Project management interface

# 🏗️ System Architecture

```
React Frontend
        │
        ▼
API Gateway
        │
────────────────────────────────────
│          │          │
▼          ▼          ▼
Auth     Workspace   AI Service
Service   Service
│          │
▼          ▼
PostgreSQL Redis

        │
        ▼
Kafka Event Bus
        │
        ▼
Deployment Service
        │
        ▼
Docker + Kubernetes (GKE)

## Backend

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- REST APIs
- Maven

## Frontend

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router

## Database

- PostgreSQL
- MongoDB
- Redis

## Messaging

- Apache Kafka

## Storage

- MinIO Object Storage

## DevOps

- Docker
- Kubernetes
- Google Kubernetes Engine (GKE)
- GitHub
- GitHub Actions

# Deployment

The project is designed for cloud deployment using:

- Docker
- Kubernetes
- Google Kubernetes Engine
- NGINX Ingress
- GitHub Actions CI/CD
