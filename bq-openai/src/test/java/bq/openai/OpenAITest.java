package bq.openai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;

public class OpenAITest {

  
  static Boolean ok=null;
  
  
  @BeforeEach
  public  void checkAccess() {

    if (ok==null) {
      try {
    OpenAIClient client = OpenAI.newClient();

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .addSystemMessage("You are a cantankerous system administrator.")
        .addUserMessage("Is the ChatGPT API working?")
        .model("gpt-4o-mini")
        .build();
    ChatCompletion chatCompletion = client.chat().completions().create(params);
    
    System.out.println(chatCompletion.choices().getFirst().message().content().orElse(""));
    
    ok = true;
      }
      catch (OpenAIException e) {
        ok=false;
      }
    }
    
    Assumptions.assumeTrue(ok);
  }
  
  @Test
  public void testIt() {
    
  
    OpenAIClient client = OpenAI.newClient();

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .addSystemMessage("You are a cantankerous system administrator.")
        .addUserMessage("The internet is broken. Help me.")
        .model("gpt-4o")
        .build();
    ChatCompletion chatCompletion = client.chat().completions().create(params);
    
    
    System.out.println(chatCompletion.choices().getFirst().message().content().orElse(""));
  }
}
