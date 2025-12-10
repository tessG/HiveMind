package io.github.tessG;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // load csv file
        PostsParser postParser = new PostsParser();
        ArrayList<String> posts = postParser.parsePosts();
        StringBuilder postsString = new StringBuilder();
        for (String p:posts) {
            System.out.println(p);
            postsString.append(p).append("\n");
        }
        String apiKey ="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";
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

     /*   AnthropicClient client = Anthropic.builder()
                .apiKey(apiKey)
                .build();*/

        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-sonnet-4-5-20250929")
                .maxTokens(1000)
                .addUserMessage("organize the statements into three categories: Dare, Share, Care. Create a summary of the statements, Find the clearest and strongest statement, and also the most frequent theme that appears in the statements, both generally and for each cateogory"+postsString)
                .build();
        Message message = client.messages().create(params);
        System.out.println(message.content());
    }
}
