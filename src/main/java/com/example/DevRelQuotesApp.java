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

import com.google.actions.api.ActionRequest;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.DialogflowApp;
import com.google.actions.api.ForIntent;
import com.google.actions.api.response.ResponseBuilder;
import com.google.api.services.actions_fulfillment.v2.model.BasicCard;
import com.google.api.services.actions_fulfillment.v2.model.Image;
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevRelQuotesApp extends DialogflowApp {

  private static final String CONTENT_URL =
      "https://raw.githubusercontent.com/actions-on-google/dialogflow-quotes-java/master/quotes.json";
  private static final String BACKGROUND_IMAGE =
      "https://lh3.googleusercontent.com/t53m5nzjMl2B_9Qhwc81tuwyA2dBEc7WqKPlzZJ9syPUkt9VR8lu4Kq8heMjJevW3GVv9ekRWntyqXIBKEhc5i7v-SRrTan_=s688";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DevRelQuotesApp.class);

  // Used from unit tests to mock API calls.
  // Note that any instance variables should be final, for thread safety.
  private final URL mockUrl;

  protected DevRelQuotesApp() {
    super();
    mockUrl = null;
  }

  // Used for unit testing with mock URL instance
  protected DevRelQuotesApp(URL url) {
    super();
    mockUrl = url;
  }

  // Parses the API response and returns a random quote
  private static QuoteDetails parseQuotesJSON(JsonElement responseRoot) {
    // Deconstruct the JSON returned by the API
    JsonObject rootObj = responseRoot.getAsJsonObject();
    String info = rootObj.get("info").getAsString();
    JsonArray data = rootObj.getAsJsonArray("data");

    // Select a random author
    JsonObject randomAuthor = data.get((int) (Math.random() * data.size()))
        .getAsJsonObject();
    JsonArray quotes = randomAuthor.getAsJsonArray("quotes");
    String author = randomAuthor.get("author").getAsString();

    // Select a random quote
    String quote = quotes.get((int) (Math.random() * quotes.size()))
        .getAsString();

    return new QuoteDetails(info, author, quote);
  }

  @ForIntent("Default Welcome Intent")
  public ActionResponse defaultWelcomeIntent(ActionRequest request) {
    // Contains text resources for responses
    ResourceBundle rb = ResourceBundle.getBundle("resources");
    // Holds quote fetched from API
    QuoteDetails quote;
    // Used to build an AoG response
    ResponseBuilder responseBuilder = getResponseBuilder(request);

    try {
      quote = this.fetchQuote();
    } catch (Exception ex) {
      LOGGER.error(
          "Exception while fetching and parsing data from " + CONTENT_URL + ":",
          ex);

      // Handle error gracefully by sending a response
      return responseBuilder.add(rb.getString("problem")).endConversation()
          .build();
    }

    // Standard response for all platforms
    responseBuilder.add(
        new SimpleResponse()
            .setDisplayText(quote.information)
            .setTextToSpeech(
                String.format(
                    rb.getString("long_attribution"), quote.authorName,
                    quote.quoteText)));

    // Multimodal response for platforms with screen capability
    if (request.hasCapability("actions.capability.SCREEN_OUTPUT")) {
      responseBuilder.add(
          new BasicCard()
              .setTitle(String
                  .format(rb.getString("short_attribution"), quote.authorName))
              .setFormattedText(quote.quoteText)
              .setImage(
                  new Image()
                      .setUrl(BACKGROUND_IMAGE)
                      .setAccessibilityText(
                          rb.getString("accessibility_text"))));
    }

    // Tell the Assistant to end the conversation after this output
    return responseBuilder.endConversation().build();
  }

  // Fetches a quote from the API url
  private QuoteDetails fetchQuote() throws IOException {
    // If a URL instance has been provided for unit testing, use it
    URL quoteUrl = (mockUrl == null) ? new URL(CONTENT_URL) : mockUrl;

    // Make HTTP request and parse JSON response
    URLConnection quotesConnection = quoteUrl.openConnection();
    quotesConnection.connect();

    JsonParser parser = new JsonParser();
    InputStreamReader reader = new InputStreamReader(
        (InputStream) quotesConnection.getContent());

    JsonElement root = parser.parse(reader);

    return parseQuotesJSON(root);
  }

  // Represents a specific quote sourced from the API
  private static class QuoteDetails {

    final String information;
    final String authorName;
    final String quoteText;

    private QuoteDetails(String information, String authorName,
        String quoteText) {
      this.information = information;
      this.authorName = authorName;
      this.quoteText = quoteText;
    }
  }
}
