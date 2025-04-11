package com.egon89.stresstest;

import picocli.CommandLine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "rate-limiter",
    mixinStandardHelpOptions = true,
    version = "rate-limiter 1.0",
    description = "Rate limiter")
public class StressTestApplication implements Runnable {

  @CommandLine.Option(names = {"-u", "--url"}, description = "The target URL for the stress test", required = true)
  private String url;

  @CommandLine.Option(names = {"-r", "--requests"}, description = "The total number of requests to send")
  private Integer totalRequest = 10;

  @CommandLine.Option(names = {"-c", "--concurrency"}, description = "The number of concurrent requests")
  private Integer concurrentRequest = 2;

  @CommandLine.Option(names = {"-X", "--method"}, description = "The HTTP method (e.g., GET, HEAD, PATCH, POST, PUT)")
  private String method = "GET";

  @CommandLine.Option(names = {"-i", "--interval"}, description = "The interval (in seconds) between requests. Default is 0")
  private Integer intervalSec = 0;

  @CommandLine.Option(names = {"-d", "--body"}, description = "The request body for requests")
  private String body = "";

  @CommandLine.Option(names = {"-H", "--headers"}, description = "Custom headers for the request separated by commas")
  private String headersStr;

  public static void main(String[] args) {
    int code = new CommandLine(new StressTestApplication()).execute(args);
    System.exit(code);
  }

  @Override
  public void run() {
    var input = new HttpStress.HttpStressInput(
        url,
        method,
        totalRequest,
        concurrentRequest,
        intervalSec,
        body,
        parseHeaders(headersStr)
    );
    try {
      new HttpStress().execute(input);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, String> parseHeaders(String headersStr) {
    if (Objects.isNull(headersStr) || headersStr.isBlank()) {
      return Collections.emptyMap();
    }

    return Arrays.stream(headersStr.split(","))
        .map(String::trim)
        .filter(pair -> pair.contains("="))
        .map(pair -> pair.split("=", 2)) // limit=2 to avoid extra '=' in values
        .filter(kv -> kv.length == 2 && !kv[0].isBlank())
        .collect(Collectors.toMap(
            kv -> kv[0].trim(),
            kv -> kv[1].trim(),
            (v1, v2) -> v2 // if a duplicate key is found, keep the second value (v2) and discard the first (v1).
        ));
  }
}
