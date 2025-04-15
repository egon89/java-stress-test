# Java Stress Test
The Stress Test Application is a command-line tool designed to perform HTTP stress testing on a target URL.
It allows users to send multiple HTTP requests concurrently, measure response times, and generate a summary report of 
the results. The application is implemented in Java and uses the `picocli` library for command-line parsing.

## Features
- Supports multiple HTTP methods: `GET`, `HEAD`, `PATCH`, `POST`, `PUT`, `DELETE`.
- Allows configuration of:
   - Target URL.
   - Total number of requests.
   - Number of concurrent requests.
   - Interval between requests.
   - Custom request headers.
   - Request body for applicable HTTP methods.
- Provides a detailed summary report, including:
   - Total requests sent.
   - Status code distribution.
   - Average response time.
- Uses structured concurrency for efficient request handling.

## Usage
### Prerequisites
- Docker for build and run the project.

### Build and Run
1. Build the Docker image:
   ```bash
   docker build -t java-http-stress .
   ```
2. Run the Docker container:
   ```bash
    docker run --rm java-http-stress:latest -u http://example.com -r 20 -c 5 -X GET
    ```

   Post example:
    ```bash
    docker run --rm java-http-stress:latest -u https://67f4273dcbef97f40d2d8a5b.mockapi.io/users -H "Content-Type=application/json" -X POST -d "{\"name\": \"John Doe\"}"
    ```

   For default values, you can run the command without specifying all flags:
    ```bash
    docker run --rm java-http-stress:latest -u http://example.com
    ```

   To see all available flags, run:
    ```bash
    docker run --rm go-http-stress:latest --help
   ```
