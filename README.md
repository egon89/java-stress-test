# java stress test

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
