/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;


public class DevRelQuotesAppTest {

  private static String fromFile(String fileName) throws IOException {
    Path absolutePath = Paths.get("src", "test", "resources",
        fileName);

    return new String(Files.readAllBytes(absolutePath));
  }

  //Pretty-prints response JSON so we can compare it with our pretty-printed sample output in a human-readable way
  private static String prettyPrintJson(String jsonInput) {
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    JsonParser parser = new JsonParser();
    JsonElement element = parser.parse(jsonInput);

    return gson.toJson(element);
  }

  // Mock a URL object that will return the contents of a string instead
  // of making an HTTP request.
  private static URL mockUrlWithResponse(String dataString) throws Exception {
    URL mockUrl = Mockito.mock(URL.class);
    URLConnection mockConnection = Mockito.mock(URLConnection.class);

    InputStream dataStream = new ByteArrayInputStream(dataString.getBytes());
    Mockito.when(mockConnection.getContent()).thenReturn(dataStream);
    Mockito.when(mockUrl.openConnection()).thenReturn(mockConnection);

    return mockUrl;
  }

  // Tests the happy path but with no screen capability
  @Test
  public void testDefaultWelcomeIntent() throws Exception {

    URL mockUrl = mockUrlWithResponse(fromFile("data.json"));

    // Use our secondary constructor that allows us to specify a URL object.
    DevRelQuotesApp app = new DevRelQuotesApp(mockUrl);
    String requestBody = fromFile("request_welcome.json");

    CompletableFuture<String> future = app.handleRequest(requestBody,
        null /* headers */);

    String responseJson = future.get();
    String prettyJsonResponse = prettyPrintJson(responseJson);

    String expectedResponse = fromFile("response_welcome.json");

    Assert.assertEquals("Response was not as expected", expectedResponse, prettyJsonResponse);
  }

  // Tests that we correctly handle the presence of the SCREEN capability
  @Test
  public void testScreenCapability() throws Exception {

    URL mockUrl = mockUrlWithResponse(fromFile("data.json"));

    // Use our secondary constructor that allows us to specify a URL object.
    DevRelQuotesApp app = new DevRelQuotesApp(mockUrl);
    String requestBody = fromFile("request_welcome_screen.json");

    CompletableFuture<String> future = app.handleRequest(requestBody,
        null /* headers */);

    String responseJson = future.get();
    String prettyJsonResponse = prettyPrintJson(responseJson);

    String expectedResponse = fromFile("response_welcome_screen.json");

    Assert.assertEquals("Response was not as expected", expectedResponse, prettyJsonResponse);
  }

  // Tests the handling of an IOException (could be caused by network issues)
  @Test
  public void testNetworkErrorHandling() throws Exception {

    // Mock a URL object that will throw an exception, triggering the error
    // handling
    URL mockUrl = Mockito.mock(URL.class);
    URLConnection mockConnection = Mockito.mock(URLConnection.class);
    Mockito.when(mockConnection.getContent()).thenThrow(new IOException());
    Mockito.when(mockUrl.openConnection()).thenReturn(mockConnection);

    // Use our secondary constructor that allows us to specify a URL object.
    DevRelQuotesApp app = new DevRelQuotesApp(mockUrl);
    String requestBody = fromFile("request_welcome.json");

    CompletableFuture<String> future = app.handleRequest(requestBody,
        null /* headers */);

    String responseJson = future.get();
    String prettyJsonResponse = prettyPrintJson(responseJson);

    String expectedResponse = fromFile("response_networkerror.json");

    Assert.assertEquals("Response was not as expected", expectedResponse, prettyJsonResponse);
  }

  // Tests that we handle an unexpected response from the HTTP fetch
  @Test
  public void testJsonErrorHandling() throws Exception {

    /* Mock a URL object that will return an empty JSON object instead
     * of making an HTTP request. This is designed to simulate an
     * unexpected but non-error response.
     */
    URL mockUrl = mockUrlWithResponse("{}");

    // Use our secondary constructor that allows us to specify a URL object.
    DevRelQuotesApp app = new DevRelQuotesApp(mockUrl);
    String requestBody = fromFile("request_welcome.json");

    CompletableFuture<String> future = app.handleRequest(requestBody,
        null /* headers */);

    String responseJson = future.get();
    String prettyJsonResponse = prettyPrintJson(responseJson);

    String expectedResponse = fromFile("response_jsonerror.json");

    Assert.assertEquals("Response was not as expected", expectedResponse, prettyJsonResponse);
  }

}
