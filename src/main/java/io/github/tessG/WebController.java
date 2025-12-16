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
                .card {
                    background: white;
                    border-radius: 16px;
                    padding: 35px;
                    margin-bottom: 25px;
                    box-shadow: 0 10px 40px rgba(0,0,0,0.15);
                    transition: transform 0.2s, box-shadow 0.2s;
                }
                
                .card:hover {
                    transform: translateY(-5px);
                    box-shadow: 0 15px 50px rgba(0,0,0,0.2);
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
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
                    border-color: #667eea;
                    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                }
                
                input[type="file"] {
                    padding: 10px;
                }
                
                button {
                    width: 100%;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
                    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
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
                    background: #ebf4ff;
                    border-left: 4px solid #4299e1;
                    padding: 16px;
                    border-radius: 8px;
                    margin-bottom: 30px;
                    font-size: 14px;
                    color: #2c5282;
                    line-height: 1.6;
                }
                
                .info-badge strong {
                    color: #2a4365;
                }
                
                /* Loading overlay */
                .loading-overlay {
                    display: none;
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(102, 126, 234, 0.95);
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
                    border-top-color: #667eea;
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
                    color: #667eea;
                    margin-right: 8px;
                    font-size: 16px;
                }
                
                @media (max-width: 768px) {
                    .brand-name {
                        font-size: 42px;
                    }
                    
                    .card {
                        padding: 25px;
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
                  
                    <p class="tagline">Efterbehandling af dét, de studerende siger</p>
                    
                
                        <ol class="steps">
                            <li> Vælg mellem at analysere data direkte fra et Padlet board eller export upload en CSV fil (header[Category,Statement]) </li>
                            <li> Vent mens data bliver analyseret (10-30 sek.) </li>
                            <li> Preview poster - eller print og hæng op </li>
                        </ol>
        
            
                </div>
                
             
                
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
                            <option value="delphi-4">Delphi (4 categories)</option>
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
                            <option value="delphi-4">4 categories</option>
                        </select>
                        
                        <button type="submit">📤 Generate from CSV</button>
                    </form>
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
                // Delphi evaluation - use dashboard workflow
                posterPath = workflow.executeDelphiWorkflow(padletId, type);
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

            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("Uploaded file is empty");
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                throw new RuntimeException("File must be a CSV file");
            }

            // Save to temporary file
            Path tempCsv = Files.createTempFile("upload-", ".csv");
            file.transferTo(tempCsv.toFile());

            System.out.println("💾 Saved to temp file: " + tempCsv);

            // Use existing DelphiDirectWorkflow
            DelphiDirectWorkflow workflow = new DelphiDirectWorkflow();
            String posterPath = workflow.generatePosterFromCsv(
                    tempCsv.toString(),
                    evaluationType
            );

            System.out.println("✅ Poster generated: " + posterPath);

            // Cleanup temp file
            Files.deleteIfExists(tempCsv);

            // Return preview page
            return generatePreviewPage(posterPath);

        } catch (Exception e) {
            System.err.println("❌ Error processing CSV: " + e.getMessage());
            e.printStackTrace();
            return generateErrorPage("Error processing CSV: " + e.getMessage());
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

    /**
     * Generate preview page with styling appropriate for poster type
     * Detects Delphi dashboard vs DSC poster and applies different wrapper styles
     */
    private String generatePreviewPage(String posterPath) throws IOException {
        Path posterFile = Paths.get(posterPath);
        String posterHtml = Files.readString(posterFile);
        String filename = posterFile.getFileName().toString();

        // Detect poster type
        boolean isDelphiDashboard = posterHtml.contains("contradictory-graph") ||
                posterHtml.contains("dashboard") ||
                filename.toLowerCase().contains("delphi");

        // Build appropriate styles based on poster type
        String posterWrapperStyle;
        String iframeStyle;

        if (isDelphiDashboard) {
            // Delphi dashboard - let it scale naturally, no constraints
            posterWrapperStyle = "background: transparent; padding: 0; border-radius: 0; " +
                    "box-shadow: none; width: auto; max-width: 100%; overflow: auto;";
            iframeStyle = "border: none; width: 100%; height: 800px;";
        } else {
            // DSC poster - constrain to 1000px width
            posterWrapperStyle = "background: white; padding: 20px; border-radius: 8px; " +
                    "box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 1000px;";
            iframeStyle = "border: none; width: 100%; min-height: 1400px;";
        }

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Poster Preview</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            background:#F5F3EE;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: #F5F3EE;\n" +
                "            padding: 20px;\n" +
                "            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "            position: sticky;\n" +
                "            top: 0;\n" +
                "            z-index: 1000;\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "        }\n" +
                "        .header h2 {\n" +
                "            margin: 0;\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        .buttons {\n" +
                "            display: flex;\n" +
                "            gap: 10px;\n" +
                "        }\n" +
                "        .btn {\n" +
                "            padding: 12px 24px;\n" +
                "            border: none;\n" +
                "            border-radius: 4px;\n" +
                "            cursor: pointer;\n" +
                "            font-size: 16px;\n" +
                "            text-decoration: none;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .btn-download {\n" +
                "            background: #4299e1;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .btn-download:hover {\n" +
                "            background: #3182ce;\n" +
                "        }\n" +
                "        .btn-home {\n" +
                "            background: #edf2f7;\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        .btn-home:hover {\n" +
                "            background: #e2e8f0;\n" +
                "        }\n" +
                "        .preview-container {\n" +
                "            margin: 0;\n" +
                "            padding: 20px;\n" +
                "            justify-content: center;\n" +
                "        }\n" +
                "        .poster-wrapper {\n" +
                "            " + posterWrapperStyle + "\n" +
                "        }\n" +
                "        iframe {\n" +
                "            " + iframeStyle + "\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <h2>✅ Poster Generated Successfully!</h2>\n" +
                "        <div class=\"buttons\">\n" +
                "            <a href=\"/\" class=\"btn btn-home\">🏠 Create Another</a>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"preview-container\">\n" +
                "        <div class=\"poster-wrapper\">\n" +
                "            <iframe srcdoc=\"" + escapeHtmlForAttribute(posterHtml) + "\"></iframe>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
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
     * Escape HTML for use in iframe srcdoc attribute
     */
    private String escapeHtmlForAttribute(String html) {
        return html.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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