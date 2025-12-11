package io.github.tessG;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;


import java.util.ArrayList;



public class Main {
    public static void main2(String[] args) {
        // load csv file
        PostsParser postParser = new PostsParser();
        ArrayList<String> posts = postParser.parsePosts();
        StringBuilder postsString = new StringBuilder();
        for (String p : posts) {
            //     System.out.println(p);
            postsString.append(p).append("\n");
        }
       // API key loaded from ANTHROPIC_API_KEY environment variable (not hardcoded)

        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-sonnet-4-5-20250929")
                .maxTokens(1000)
                .addUserMessage("organize the statements into three categories: Dare, Share, Care. Create a summary of the statements. Pick one statement across categories that carries a lot of weight and can be used as a headline. Find the most frequent theme for each cateogory. Make some fun statistic metrices  ie. 'Percentage of posts mentioning some key concept ie. gruppearbejde'. Identify and list key qualities in bullets.  IN DANISH " + postsString)
                .build();
        Message message = client.messages().create(params);

       // Get the first text block
        if (!message.content().isEmpty() && message.content().get(0).text() != null) {
            System.out.println(message.content().get(0).text());
        }
/*

        // Your Miro access token - get this from https://miro.com/app/settings/user-profile/apps
        String accessToken = "YOUR_MIRO_ACCESS_TOKEN_HERE";

        // Create the insights data (this would come from your Claude API analysis)
        StudentInsights insights = createSampleInsights();

        // Create the board
        MiroBoardBuilder builder = new MiroBoardBuilder(accessToken);

        try {
            String boardId = builder.createInsightsBoard(
                    insights,
                    "Student Insights - " + java.time.LocalDate.now()
            );

            System.out.println("✅ Board created successfully!");
            System.out.println("🔗 View at: https://miro.com/app/board/" + boardId);

        } catch (Exception e) {
            System.err.println("❌ Error creating board: " + e.getMessage());
            e.printStackTrace();
        }*/
    }
    /**
     * Creates sample insights data structure
     * In your real application, this would come from Claude API analysis
     */
/*    private static StudentInsights createSampleInsights() {
        String headline = "Vær ikke bange for at spørge om hjælp – og skab et trygt " +
                "fællesskab hvor vi deler viden og tør fejle sammen";

        List<String> dareItems = Arrays.asList(
                "Vær mere åben og social i håbet om at få et bedre kammeratskab",
                "Blive bedre til at udfordre os selv, og hop ud i nye ting",
                "Mod til at spørge om hjælp",
                "Nysgerrighed over for hinanden",
                "Hoppe ud af vores komfortzone",
                "Slippe tvivlen og tro på egen kunnen",
                "Tage mere initiativ til at skubbe gruppen i gang",
                "Skabe et miljø, hvor man tør fejle uden at blive dømt",
                "Åbensindet tilgang til opgaverne",
                "Blive bedre til at kode generelt"
        );

        List<String> shareItems = Arrays.asList(
                "Dele vores fremskridt og deltage i en fælles rejse for udvikling",
                "Hjælp dine klassekammerater, både med skolearbejde og det sociale",
                "Tilbyde sin hjælp og dele sin viden",
                "Dele sine kompetencer og usikkerheder",
                "Have indsigt i hinandens styrker og udviklingsområder",
                "Arbejde sammen mod fælles mål",
                "Dele ens tankergang for opgaveløsning",
                "Dele vores viden og noter med hinanden",
                "Begynde gruppearbejde med forventningsafstemning",
                "Opnå højeste kompetencer ved at dele med andre"
        );

        List<String> careItems = Arrays.asList(
                "Tage os af hinanden og vise omsorg",
                "Vise tillid til klassekammerater",
                "Skabe et trygt fællesskab",
                "Spørge om andre har brug for hjælp",
                "Huske at fejre andres succes (med kage!)",
                "Være åben og tolerant",
                "Tålmodighed",
                "Støtte hinanden i opgaveløsningen",
                "Sikre inddragelse af alle gruppemedlemmer",
                "Sørge for alle er inkluderede",
                "Finde rytmen med hinanden",
                "Overholde aftaler i gruppen",
                "Lyt til sine medstuderende"
        );

        String summary = "Udsagnene afspejler et stærkt ønske om at skabe et trygt, " +
                "inkluderende og udviklende læringsmiljø. Der er en gennemgående " +
                "erkendelse af, at personlig udvikling kræver mod til at turde, " +
                "vilje til at dele og evne til at vise omsorg for hinanden.";

        String keyInsight = "Nøgleindsigt: Fællesskabet vokser når vi tør være sårbare, " +
                "deler vores viden generøst, og passer på hinanden gennem hele rejsen.";

        return new StudentInsights(headline, dareItems, shareItems, careItems,
                summary, keyInsight);
    }*/
}

