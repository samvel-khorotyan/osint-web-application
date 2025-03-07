# OSINT Web Application Backend

This is the backend component of the OSINT Web Application, built with Kotlin and Spring Boot. It provides RESTful APIs for initiating and managing domain scans using open-source intelligence tools like Amass.

## Features

*   **Initiate Scan:**  Starts a new domain scan using a Docker container running Amass.
*   **Get Scans:** Retrieves a list of all previous scans, including their status and results.
*   **Get Scan by ID:**  Retrieves a specific scan by its ID.
*   **Update Display Order:** Allows reordering of scan cards on the frontend by updating the `displayOrder` property.
*   **Persistence:** Stores scan data in a PostgreSQL database, ensuring data is preserved across server restarts.
*   **Docker Integration:**  Uses Docker to run scans in isolated environments, enhancing security and scalability.
*   **Fallback Mode:** Provides mock scan results if Docker is unavailable, allowing for frontend development and testing without a Docker setup.
*   **Error Handling:**  Includes comprehensive error handling and validation with informative error responses.
*   **CORS Configuration:** Configured to allow cross-origin requests from a specified frontend origin.
*   **Health Check Endpoint:** Provides a `/api/health` endpoint for monitoring the application's status.
*   **Asynchronous Scan Execution:** Runs scans in the background using Kotlin coroutines, preventing blocking of the main thread.
*  **Configurable via environment variables:** All important settings are loaded using environment variables to ensure the smooth operation of the application in various environments.


## Technologies Used

*   **Kotlin:**  Primary programming language.
*   **Spring Boot:**  Framework for building the REST API.
*   **Spring Data JPA:**  For interacting with the PostgreSQL database.
*   **Hibernate Types:** Used for JSONB column mapping.
*   **PostgreSQL:** Database for storing scan data.
*   **Docker Java:** Library for interacting with the Docker API.
*   **Flyway:**  Database migration tool.
*   **Maven:**  Build tool.
*   **Jackson Module Kotlin:** For JSON serialization/deserialization.
*   **Kotlinx Coroutines:** For asynchronous programming.

## Project Structure

The project follows a standard Maven structure, with the main source code located in `src/main/kotlin`.  Key packages include:

*   `com.ptbox.osint`:  Root package containing the main application class (`OsintApplication`).
*   `com.ptbox.osint.config`:  Configuration classes, including `CorsConfiguration` and `DockerConfig`.
*   `com.ptbox.osint.controller`:  REST controllers (`HealthCheckController`, `ScanController`).
*   `com.ptbox.osint.dto`:  Data Transfer Objects (DTOs) for request and response bodies.
*   `com.ptbox.osint.exception`:  Exception handling classes and custom exceptions.
*   `com.ptbox.osint.model`:  Data models, including the `Scan` entity and `ScanStatus` enum.
*   `com.ptbox.osint.repository`:  Repositories for database interaction (`ScanPersistenceRepository`, `ScanRepository`, `ScanRepositoryImpl`).
*   `com.ptbox.osint.service`:  Service layer classes (`DockerService`, `DockerServiceImpl`, `ScanService`, `ScanServiceImpl`).
*   `db.migration`:  Flyway database migration scripts.
*   `resources`: Contains `application.properties`

## Prerequisites

*   **Java 11:**  The project is built with Java 11.
*   **Maven:** Used for building and managing dependencies.
*   **Docker:**  Required for running scans (unless running in fallback mode).  Docker must be installed and running.
*   **PostgreSQL:** A PostgreSQL database is required.  You can use a local instance or a remote database. Docker compose file is provided.

## Setup and Running

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/samvel-khorotyan/osint-web-application.git
    cd <repository_directory>
    ```

2.  **Configuration (application.properties):**

    The `src/main/resources/application.properties` file contains configuration options. You can override these using environment variables. Here are the key settings:

    *   `server.port`:  The port the application will run on (default: 8080).  Can be overridden with the `PORT` environment variable.
    *   `spring.datasource.url`:  The JDBC URL for the PostgreSQL database. Can be overridden with the `DATABASE_URL` environment variable.
    *   `spring.datasource.username`: Database username. Can be overridden with the `DATABASE_USERNAME` environment variable.
    *   `spring.datasource.password`:  Database password.  Can be overridden with the `DATABASE_PASSWORD` environment variable.
    *   `docker.host`: The Docker host URL (default: `unix:///var/run/docker.sock`). Can be overridden with the `DOCKER_HOST` environment variable.
    *   `docker.amass.image`:  The Docker image to use for Amass (default: `caffix/amass:latest`). Can be overridden with the `DOCKER_AMASS_IMAGE` environment variable.
    *   `docker.logs.directory`: The directory to store Docker logs (default: `/tmp/docker-logs`). Can be overridden with the `DOCKER_LOGS_DIRECTORY` environment variable.
    *   `app.docker.fallback-mode`:  Enable fallback mode (default: `false`).  Set to `true` to use mock scan results if Docker is not available. Can be overridden with the `DOCKER_FALLBACK_MODE` environment variable.
    *  `cors.origins`: Sets the allowed origins for the application. Can be changed to reflect the front-end URL.

3. **Build and Run (with Maven):**

    ```bash
    mvn clean install
    java -jar target/*.jar
    ```
4. **Run using docker compose:**

    ```bash
    docker compose up -d
    ```

   This will start the PostgreSQL database and the backend application. It handles network creation and dependencies automatically. The `-d` flag runs the containers in detached mode.

5. **Access the API:**

   Once the application is running, you can access the API endpoints:

    *   **Health Check:** `GET /api/health`
    *   **Initiate Scan:** `POST /api/scans` (see example request body below)
    *   **Get All Scans:** `GET /api/scans`
    *   **Get Scan by ID:** `GET /api/scans/{id}`
    *   **Update display order**: `PUT /api/scans/order` (see example request body below)
    * Example `POST /api/scans` Request Body:

    ```json
    {
      "domain": "example.com",
      "timeout": 2,
      "passive": true
    }
    ```
    * Example `PUT /api/scans/order` Request Body:

    ```json
    {
      "scanId": 1,
      "newOrder": 2
    }
    ```

## Docker

The `Dockerfile` in the `backend` directory is used to build a Docker image for the backend application. The docker-compose file provides a convenient way to launch the application.

*   **Multi-stage build:** The Dockerfile uses a multi-stage build to reduce the final image size. The first stage builds the application, and the second stage copies the built JAR file into a smaller JRE image.
* **Docker Installation (Optional):** The Dockerfile includes instructions to install the Docker CLI *inside* the container. This is optional, and you can disable it by setting the `INSTALL_DOCKER` build argument to `false`.  This is only needed if you want to run Docker commands *from within* the backend container (which is generally not recommended). You should typically use the host's Docker daemon by mounting the Docker socket (`/var/run/docker.sock`).
* **Log Directory:** A directory (`/tmp/docker-logs`) is created within the container and given full permissions (`chmod 777`).  This is where Docker logs are stored.

## Database Migrations

Flyway is used to manage database migrations.  Migration scripts are located in `src/main/resources/db/migration`.  The initial migration script (`V1__init.sql`) creates the `scans` table.

## Testing
Tests can be run with maven using the command

```bash
    mvn test