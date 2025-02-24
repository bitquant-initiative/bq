package bq.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;

import bq.util.Config;

public class OpenAI {

  private OpenAI() {
    
  }
  public static OpenAIClient newClient() {
    return OpenAIOkHttpClient.builder().apiKey(Config.get("OPENAI_API_KEY").orElse("INVALID_KEY")).build();
  }
  public static OpenAIClientAsync newClientAsync() {
    return OpenAIOkHttpClientAsync.builder().apiKey(Config.get("OPENAI_API_KEY").orElse("INVALID_KEY")).build();
  }
}
