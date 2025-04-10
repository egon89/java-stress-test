package com.egon89.stresstest;

import java.util.Map;

public class StressTestApplication {
  public static void main(String[] args) {
    var url = "http://example.com";
    var method = "GET";
    var totalRequest = 100;
    var concurrentRequest = 2;
    var intervalSec = 0;
    var body = "";
    var headers = Map.of("User-Agent", "java-stress-test/1.0");

    try {
      new HttpStress().execute(url, method, totalRequest, concurrentRequest, intervalSec, body, headers);
    } catch (Exception e) {
      System.err.println("something goes wrong!");
    }
  }
}
