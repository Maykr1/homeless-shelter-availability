# Homeless Shelter Availability Data

## Overview

This project sets up a local PostgreSQL database instance to manage data related to homeless shelter availability. It uses Docker to create a consistent and isolated environment for development and testing.

## Setup

To set up this project, follow these steps:

1. Build the Docker image:
   ```
   docker build -t homeless-shelter-db .
   ```

2. Run the Docker container:
   ```
   docker run --name homeless-shelter-db -d -p 5432:5432 homeless-shelter-db
   ```

This setup will allow you to run a local PostgreSQL instance for managing homeless shelter availability data. Make sure you have Docker installed on your machine before proceeding with these steps.