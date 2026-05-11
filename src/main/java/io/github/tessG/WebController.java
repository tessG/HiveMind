package io.github.tessG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

/**
 * Simple web interface for Student Evaluation Tool
 * Supports both Padlet ID input and CSV file upload
 */
@RestController
@SpringBootApplication
public class WebController {

    public static void main(String[] args) {
        SpringApplication.run(WebController.class, args);
    }

    /**
     * Homepage with two input options
     */
    @GetMapping("/")
    public String home() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title></title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    background: #F7F4EA;
                    min-height: 100vh
                    padding: 40px 20px;
                }
                
                .container {
                    max-width: 900px;
                    margin: 0 auto;
                }
                
                .header {
                   display: flex;
                             align-items: center; 
                               justify-content: space-between;
                            
                           
                }
                
                .logo-section {
                    margin-bottom: 20px;
                }
                
                .logo {
                    width: 100px;
                    height: 100px;
                    margin: 0 auto 20px;
                    position: relative;
                }
                
                .brand-name {
                    font-size: 56px;
                    font-weight: 700;
                    letter-spacing: 2px;
                    margin-bottom: 10px;
                    text-shadow: 0 2px 10px rgba(0,0,0,0.2);
                }
                
                .tagline {
                    font-size: 18px;
                    font-weight: 300;
                    flex: 0 0 200px;
                    opacity: 0.95;
                    letter-spacing: 0.5px;
                    
                }
                 .steps {
                   text-align:left;
                    width: 50%;
                    margin-left: auto;
                    margin-right: auto;
                    font-size: 10px;
                    font-weight: 300;
                    opacity: 0.95;
                    letter-spacing: 0.3px;
                }
                .cards-row {
                    display: flex;
                    gap: 25px;
                    align-items: stretch;
                    margin-bottom: 25px;
                }

                .card {
                    flex: 1;
                    background: white;
                    border-radius: 16px;
                    padding: 35px;
                    box-shadow: 0 4px 18px rgba(0,0,0,0.08);
                    transition: transform 0.2s, box-shadow 0.2s;
                    display: flex;
                    flex-direction: column;
                }

                .card form {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                }

                .card form button {
                    margin-top: auto;
                    padding-top: 25px;
                }

                .card:hover {
                    transform: translateY(-4px);
                    box-shadow: 0 8px 28px rgba(0,0,0,0.12);
                }
                
                .card-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 20px;
                    padding-bottom: 20px;
                    border-bottom: 2px solid #f0f0f0;
                }
                
                .card-icon {
                    width: 48px;
                    height: 48px;
                    background: linear-gradient(135deg, #1B3A5C 0%, #2C6E7A 100%);
                    border-radius: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-right: 15px;
                    font-size: 24px;
                }
                
                .card-title {
                    flex: 1;
                }
                
                .card-title h2 {
                    font-size: 24px;
                    color: #2d3748;
                    margin-bottom: 5px;
                    font-weight: 600;
                }
                
                .card-title p {
                    font-size: 14px;
                    color: #718096;
                    font-weight: 400;
                }
                
                label {
                    display: block;
                    font-size: 14px;
                    font-weight: 600;
                    color: #4a5568;
                    margin-bottom: 8px;
                    margin-top: 20px;
                }
                
                label:first-of-type {
                    margin-top: 0;
                }
                
                select, input[type="text"], input[type="file"] {
                    width: 100%;
                    padding: 12px 16px;
                    border: 2px solid #e2e8f0;
                    border-radius: 8px;
                    font-size: 15px;
                    transition: border-color 0.2s, box-shadow 0.2s;
                    font-family: inherit;
                }
                
                select:focus, input[type="text"]:focus {
                    outline: none;
                    border-color: #4AB5BE;
                    box-shadow: 0 0 0 3px rgba(74, 181, 190, 0.1);
                }
                
                input[type="file"] {
                    padding: 10px;
                }
                
                button {
                    width: 100%;
                    background: linear-gradient(135deg, #1B3A5C 0%, #2C6E7A 100%);
                    color: white;
                    padding: 14px 24px;
                    border: none;
                    border-radius: 8px;
                    cursor: pointer;
                    font-size: 16px;
                    font-weight: 600;
                    margin-top: 25px;
                    transition: transform 0.2s, box-shadow 0.2s;
                    letter-spacing: 0.5px;
                }

                button:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 8px 20px rgba(27, 58, 92, 0.4);
                }
                
                button:active {
                    transform: translateY(0);
                }
                
                button:disabled {
                    background: #cbd5e0;
                    cursor: not-allowed;
                    transform: none;
                }
                
                .info-badge {
                    background: #E0F4F7;
                    border-left: 4px solid #4AB5BE;
                    padding: 16px;
                    border-radius: 8px;
                    margin-bottom: 30px;
                    font-size: 14px;
                    color: #1B3A5C;
                    line-height: 1.6;
                }

                .info-badge strong {
                    color: #152C45;
                }
                
                /* Loading overlay */
                .loading-overlay {
                    display: none;
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(27, 58, 92, 0.92);
                    z-index: 9999;
                    justify-content: center;
                    align-items: center;
                }
                
                .loading-overlay.active {
                    display: flex;
                }
                
                .loading-box {
                    background: white;
                    padding: 40px;
                    border-radius: 16px;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    max-width: 500px;
                    text-align: center;
                }
                
                .spinner {
                    width: 60px;
                    height: 60px;
                    margin: 0 auto 25px;
                    position: relative;
                }
                
                .spinner-circle {
                    width: 100%;
                    height: 100%;
                    border: 4px solid #e2e8f0;
                    border-top-color: #4AB5BE;
                    border-radius: 50%;
                    animation: spin 1s linear infinite;
                }
                
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
                
                .loading-box h3 {
                    font-size: 24px;
                    color: #2d3748;
                    margin-bottom: 10px;
                }
                
                .loading-box p {
                    color: #718096;
                    margin-bottom: 20px;
                }
                
                .status-log {
                    margin-top: 20px;
                    text-align: left;
                    background: #f7fafc;
                    padding: 15px;
                    border-radius: 8px;
                    max-height: 200px;
                    overflow-y: auto;
                    font-family: 'Courier New', monospace;
                    font-size: 13px;
                }
                
                .status-message {
                    margin: 8px 0;
                    color: #4a5568;
                    display: flex;
                    align-items: center;
                }
                
                .status-message::before {
                    content: "●";
                    color: #4AB5BE;
                    margin-right: 8px;
                    font-size: 16px;
                }
                
                .demo-section {
                    background: linear-gradient(135deg, #E0F4F7 0%, #B8DDE8 100%);
                    border-radius: 12px;
                    padding: 18px 22px;
                    margin-bottom: 25px;
                    display: flex;
                    align-items: center;
                    gap: 18px;
                }

                .demo-label {
                    font-size: 13px;
                    font-weight: 600;
                    color: #1B3A5C;
                    white-space: nowrap;
                    margin: 0;
                }

                .demo-buttons {
                    display: flex;
                    gap: 12px;
                    flex: 1;
                }

                .demo-btn {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    background: white;
                    border-radius: 10px;
                    padding: 12px 16px;
                    text-decoration: none;
                    color: #1B3A5C;
                    font-weight: 600;
                    font-size: 14px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
                    transition: transform 0.2s, box-shadow 0.2s;
                    cursor: pointer;
                }

                .demo-btn:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 6px 16px rgba(27,58,92,0.14);
                }

                .demo-icon {
                    font-size: 22px;
                    margin-bottom: 4px;
                }

                .demo-sub {
                    font-size: 11px;
                    font-weight: 400;
                    color: #4AB5BE;
                    margin-top: 2px;
                }

                @media (max-width: 768px) {
                    .brand-name {
                        font-size: 42px;
                    }

                    .cards-row {
                        flex-direction: column;
                    }

                    .card {
                        padding: 25px;
                    }

                    .demo-section {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo-section">
                        <div class="logo">
                            <img src="assets/logo.png"></img>
                        </div>
                    </div>
                  
                    <p class="tagline">Alt dét studerende siger</p>
                    
                
                        <ul class="steps">
                            <li> De studerendes svar i en delphi evaluering eller lignende, analyseres og opsummeres med AI, som også finder spændinger og trends i det de studerende siger.  </li>
                            <li> Vis opsummeringen på skærmen  - eller print og hæng op i klassen. </li>
                        </ul>
        
            
                </div>

                <div class="demo-section">
                    <p class="demo-label">👀 Prøv med eksempeldata</p>
                    <div class="demo-buttons">
                        <a href="/demo/delphi" class="demo-btn" onclick="showLoading('Demo')">
                            <span class="demo-icon">📊</span>
                            <span>Delphi</span>
                            <span class="demo-sub">Keep / Stop / Start</span>
                        </a>
                        <a href="/demo/dsc" class="demo-btn" onclick="showLoading('Demo')">
                            <span class="demo-icon">💬</span>
                            <span>Dare Share Care</span>
                            <span class="demo-sub">3 kategorier</span>
                        </a>
                    </div>
                </div>

                <div class="cards-row">
                    <div class="card">
                        <div class="card-header">
                            <div class="card-icon">🌐</div>
                            <div class="card-title">
                                <h2>Option 1: From Padlet Board</h2>
                                <p>Connect to a live Padlet board and analyze student evaluations. (Board must be shared with tess@ek.dk)</p>
                            </div>
                        </div>

                        <form id="padletForm" action="/evaluate/padlet" method="post">
                            <label for="type">Evaluation Type</label>
                            <select name="type" id="type" required>
                                <option value="dare-share-care">Dare-Share-Care</option>
                                <option value="delphi">Delphi (Keep/Stop/Start)</option>
                            </select>

                            <label for="padletId">Padlet Board ID</label>
                            <input type="text" name="padletId" id="padletId"
                                   placeholder="Enter Padlet board ID (e.g., abc123xyz)" required>

                            <button type="submit">🚀 Generate from Padlet</button>
                        </form>
                    </div>

                    <div class="card">
                        <div class="card-header">
                            <div class="card-icon">📄</div>
                            <div class="card-title">
                                <h2>Option 2: Upload CSV File</h2>
                                <p>Upload pre-categorized evaluation data from your device</p>
                            </div>
                        </div>

                        <form id="csvForm" action="/evaluate/csv" method="post" enctype="multipart/form-data">
                            <label for="file">CSV File</label>
                            <input type="file" name="file" id="file" accept=".csv" required>

                            <label for="csvType">Delphi Format</label>
                            <select name="evaluationType" id="csvType">
                                <option value="delphi">Keep/Stop/Start (3 categories)</option>
                            </select>

                            <button type="submit">📤 Generate from CSV</button>
                        </form>
                    </div>
                </div>
            </div>
            
            <!-- Loading Overlay -->
            <div id="loadingOverlay" class="loading-overlay">
                <div class="loading-box">
                    <div class="spinner">
                        <div class="spinner-circle"></div>
                    </div>
                    <h3>Processing...</h3>
                    <p> Analyzing your data using claude AI...</p>
                    <div class="status-log" id="statusLog">
                        <div class="status-message">Initializing...</div>
                    </div>
                </div>
            </div>
            
            <script>
                document.getElementById('padletForm').addEventListener('submit', function(e) {
                    showLoading('Padlet');
                });
                
                document.getElementById('csvForm').addEventListener('submit', function(e) {
                    showLoading('CSV');
                });
                
                function showLoading(type) {
                    const overlay = document.getElementById('loadingOverlay');
                    const log = document.getElementById('statusLog');
                    overlay.classList.add('active');
                    
                    const messages = type === 'Padlet'
                        ? [
                            'Connecting to Padlet board...',
                            'Fetching student responses...',
                            'Analyzing with AI...',
                            'Generating visual poster...',
                            'Almost ready...'
                          ]
                        : type === 'Demo'
                        ? [
                            'Henter eksempeldata...',
                            'Analyserer udsagn med AI...',
                            'Finder mønstre og spændinger...',
                            'Genererer plakat...',
                            'Snart klar...'
                          ]
                        : [
                            'Reading CSV file...',
                            'Parsing evaluation data...',
                            'Calculating similarities...',
                            'Building network graph...',
                            'Generating poster...',
                            'Finalizing...'
                          ];
                    
                    log.innerHTML = '<div class="status-message">Starting ' + type + ' analysis...</div>';
                    
                    let index = 0;
                    const interval = setInterval(() => {
                        if (index < messages.length) {
                            const msg = document.createElement('div');
                            msg.className = 'status-message';
                            msg.textContent = messages[index];
                            log.appendChild(msg);
                            log.scrollTop = log.scrollHeight;
                            index++;
                        } else {
                            clearInterval(interval);
                        }
                    }, 2000);
                }
            </script>
        </body>
        </html>
        """;
    }

    @PostMapping("/evaluate/padlet")
    public String evaluateFromPadlet(
            @RequestParam String type,
            @RequestParam String padletId) {

        try {
            System.out.println("📥 Processing Padlet board: " + padletId + " (type: " + type + ")");

            // Check for Padlet API key
            String padletApiKey = System.getenv("PADLET_API_KEY");
            if (padletApiKey == null || padletApiKey.isEmpty()) {
                throw new RuntimeException("PADLET_API_KEY environment variable not set");
            }

            // Use GenericEvaluationWorkflow with appropriate method
            GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow(padletApiKey);
            String posterPath;

            // Determine which workflow to use based on evaluation type
            if (type.toLowerCase().contains("delphi")) {
                posterPath = workflow.executeDelphiFromPadlet(padletId, type);
            } else {
                // DSC or other evaluations - use simple workflow
                posterPath = workflow.executeDSCWorkflow(padletId, type);
            }

            System.out.println("✅ Poster generated: " + posterPath);

            // Return preview page
            return generatePreviewPage(posterPath);

        } catch (Exception e) {
            System.err.println("❌ Error processing Padlet: " + e.getMessage());
            e.printStackTrace();
            return generateErrorPage("Error processing Padlet: " + e.getMessage());
        }
    }
    @PostMapping("/evaluate/csv")
    public String evaluateFromCsv(
            @RequestParam MultipartFile file,
            @RequestParam String evaluationType) {

        try {
            System.out.println("📥 Processing CSV upload: " + file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new RuntimeException("Uploaded file is empty");
            }
            if (!file.getOriginalFilename().endsWith(".csv")) {
                throw new RuntimeException("File must be a CSV file");
            }

            byte[] fileBytes = file.getBytes();
            String hash = computeSha256(fileBytes);
            Path cacheDir = Paths.get("cache");
            Files.createDirectories(cacheDir);
            Path cachedPoster = cacheDir.resolve(hash + "-" + evaluationType + ".html");

            if (Files.exists(cachedPoster)) {
                System.out.println("✅ Cache hit — returning cached poster");
                return generatePreviewPage(cachedPoster.toString());
            }

            System.out.println("🔄 Cache miss — processing CSV");
            Path tempCsv = Files.createTempFile("upload-", ".csv");
            Files.write(tempCsv, fileBytes);

            GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow();
            String posterPath = workflow.executeDelphiFromCsv(tempCsv.toString(), evaluationType);

            Files.deleteIfExists(tempCsv);

            Files.copy(Paths.get(posterPath), cachedPoster);
            System.out.println("💾 Cached as: " + cachedPoster);

            return generatePreviewPage(posterPath);

        } catch (Exception e) {
            System.err.println("❌ Error processing CSV: " + e.getMessage());
            e.printStackTrace();
            return generateErrorPage("Error processing CSV: " + e.getMessage());
        }
    }

    @GetMapping("/demo/{type}")
    public String demo(@PathVariable String type) {
        try {
            boolean isDsc = type.equals("dsc");
            Path csvPath = isDsc
                    ? Paths.get("data/dscposts.csv")
                    : Paths.get("data/delphiposts.csv");
            String evaluationType = isDsc ? "dare-share-care" : "delphi";

            byte[] fileBytes = Files.readAllBytes(csvPath);
            String hash = computeSha256(fileBytes);
            Path cacheDir = Paths.get("cache");
            Files.createDirectories(cacheDir);
            Path cachedPoster = cacheDir.resolve(hash + "-" + evaluationType + ".html");

            if (Files.exists(cachedPoster)) {
                System.out.println("✅ Demo cache hit for " + type);
                return generatePreviewPage(cachedPoster.toString());
            }

            System.out.println("🔄 Demo cache miss — processing " + type);
            GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow();
            String posterPath = isDsc
                    ? workflow.executeDSCFromCsv(csvPath.toString())
                    : workflow.executeDelphiFromCsv(csvPath.toString(), evaluationType);

            Files.copy(Paths.get(posterPath), cachedPoster);
            System.out.println("💾 Demo cached as: " + cachedPoster);

            return generatePreviewPage(posterPath);

        } catch (Exception e) {
            System.err.println("❌ Demo error: " + e.getMessage());
            e.printStackTrace();
            return generateErrorPage("Demo error: " + e.getMessage());
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash CSV", e);
        }
    }

    /**
     * Download endpoint for generated posters
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadPoster(@PathVariable String filename) {
        try {
            Path file = Paths.get(filename);

            if (!Files.exists(file)) {
                throw new IOException("File not found: " + filename);
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"")
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String generatePreviewPage(String posterPath) throws IOException {
        Path posterFile = Paths.get(posterPath);
        String posterHtml = Files.readString(posterFile);

        String headContent = extractHead(posterHtml);
        String bodyContent = extractBody(posterHtml);

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Poster</title>\n" +
                headContent + "\n" +
                "    <style>\n" +
                "        .back-link {\n" +
                "            display: block;\n" +
                "            padding: 10px 16px;\n" +
                "            color: #666;\n" +
                "            text-decoration: none;\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .back-link:hover { color: #333; }\n" +
                "        @media print {\n" +
                "            .back-link { display: none !important; }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <a href=\"/\" class=\"back-link\">← back</a>\n" +
                bodyContent + "\n" +
                "</body>\n" +
                "</html>";
    }

    private String extractHead(String html) {
        int headStart = html.indexOf("<head>");
        int headEnd = html.indexOf("</head>");
        if (headStart == -1 || headEnd == -1) return "";
        String content = html.substring(headStart + 6, headEnd);
        return content.replaceAll("(?i)<meta[^>]*charset[^>]*>", "")
                      .replaceAll("(?i)<title>.*?</title>", "")
                      .trim();
    }

    private String extractBody(String html) {
        int bodyStart = html.indexOf("<body");
        if (bodyStart == -1) return html;
        int bodyTagEnd = html.indexOf(">", bodyStart) + 1;
        int bodyEnd = html.lastIndexOf("</body>");
        if (bodyEnd == -1) return html.substring(bodyTagEnd);
        return html.substring(bodyTagEnd, bodyEnd).trim();
    }


    /**
     * Generate error page
     */
    private String generateErrorPage(String errorMessage) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Error</title>
                <style>
                    body { 
                        font-family: Arial; 
                        max-width: 600px; 
                        margin: 50px auto; 
                        padding: 20px; 
                    }
                    .error { 
                        background: #fee; 
                        border-left: 4px solid #f56565; 
                        padding: 20px; 
                        border-radius: 4px; 
                    }
                    h1 { color: #c53030; }
                    a { 
                        color: #4299e1; 
                        text-decoration: none;
                        padding: 10px 20px;
                        background: #edf2f7;
                        border-radius: 4px;
                        display: inline-block;
                        margin-top: 20px;
                    }
                    a:hover {
                        background: #e2e8f0;
                    }
                </style>
            </head>
            <body>
                <div class="error">
                    <h1>❌ Error</h1>
                    <p>%s</p>
                    <a href="/">← Back to home</a>
                </div>
            </body>
            </html>
            """.formatted(errorMessage);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public String health() {
        boolean hasAnthropicKey = System.getenv("ANTHROPIC_API_KEY") != null;
        boolean hasPadletKey = System.getenv("PADLET_API_KEY") != null;

        return """
            {
                "status": "ok",
                "anthropic_api_configured": %s,
                "padlet_api_configured": %s
            }
            """.formatted(hasAnthropicKey, hasPadletKey);
    }


}