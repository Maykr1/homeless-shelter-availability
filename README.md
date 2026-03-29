# Homeless Shelter Availability
A full-stack application designed to help users find nearby homeless shelters and check real-time availability. This project combines a structured backend API, a searchable dataset, and a lightweight chatbot interface to improve accessibility to shelter resources.

## Overview
Access to accurate and timely shelter information is a major challenge. Many existing systems:

    - Lack real-time availability data
    - Are fragmented across different platforms
    - Are difficult to search or filter

This project addresses those issues by providing:

    - A centralized shelter dataset
    - Availability tracking (beds/rooms)
    - A simple chatbot interface for querying data

## How it Works
    1. User enters a natural language query
    2. Backend parses keywords (city, availability)
    3. Repository executes filtered database query
    4. Results returned as structured JSON

## Limitations
    - No real-time external API integration
    - Availability is simulated
    - Location search is city based (no geolocation as this point)

## Features

### Shelter Search
    - Search shelters by city
    - Filter by availability
    - View total vs available beds

### Data Management
    - Large dataset of roughly 14,000 entries
    - Structured for fast query speed
    - Designed to simulate real-world data ingestion

### Backend API
    - Built with Spring Boot
    - RESTful endpoints
    - JPS/Hibernate for database interaction

## Tech Stack

### Backend
    - Java (Spring Boot)
    - Spring Data JPA
    - PostgreSQL

### Frontend
    - Lightweight HTML/JS

### DevOps
    - Docker/Docker Compose
    - Maven

## Project Structure
    root/
    │── backend/
    │   ├── controller/
    │   ├── model/
    │   ├── repository/
    │   └── resources/
    │
    │── frontend/
    │   └── simple chat UI
    │
    │── data/
    │   └── shelters_14000_entity_match.csv
    │
    │── docker-compose.yml
    │── README.md

## Data Model
    @Entity
    public class Shelter {

        private Long id;
        private String name;
        private String address;
        private String city;
        private String state;
        private String zipCode;

        private String phoneNumber;
        private String email;

        private Integer totalBeds;
        private Integer availableBeds;

        private String description;
    }

## Dataset
    The dataset includes:
        - 14,000 shelter entries
        - Availability metrics (beds)
        - Contact + location info

    Note:
    Due to limited public data, the dataset is a hybrid of real and simulated data designed to: 
        - Mimic real-world inconsistencies
        - Support testing and demos

# Getting Started

## 1. Clone the repo
    git clone https://github.com/Maykr1homeless-shelter-availability.git
    cd homeless-shelter-availability

## 2. Run Backend
    cd backend
    mvn spring-boot:run

## 3. Load Data into Database
    COPY shelters(
    name,address,city,state,zip_code,phone_number,email,
    total_beds,available_beds,description
    )
    FROM '/path/to/shelters.csv'
    DELIMITER ','
    CSV HEADER;

## 4. Run Frontend
    open index.html

# Other Notes

## Why This Matters
    This system demonstrates how technology can:
        - Improve access to critical resources
        - Simplify complex data systems
        - Provide intuitive interfaces
    Even as a prototype it highlight how a centralized search system could improve real-world outcomes

## Contributors
    Robert Lovett
    Ethan Clark
    Rian Lewis
    Mitchell Koski