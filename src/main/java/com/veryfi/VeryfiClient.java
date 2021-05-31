package com.veryfi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VeryfiClient {

    private static final Logger logger = LoggerFactory.getLogger(VeryfiClient.class);

    private static final String API_VERSION = "v7";
    private static final Integer API_TIMEOUT = 120;
    private static final Integer MAX_FILE_SIZE_MB = 20;
    private static final String BASE_URL = "https://api.veryfi.com/api/";
    private static final List<String> CATEGORIES = Arrays.asList(
            "Advertising & Marketing",
            "Automotive",
            "Bank Charges & Fees",
            "Legal & Professional Services",
            "Insurance",
            "Meals & Entertainment",
            "Office Supplies & Software",
            "Taxes & Licenses",
            "Travel",
            "Rent & Lease",
            "Repairs & Maintenance",
            "Payroll",
            "Utilities",
            "Job Supplies",
            "Grocery"
    );

    private final String clientId;
    private final String clientSecret;
    private String username;
    private String apiKey;
    private OkHttpClient httpClient;

    public VeryfiClient(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = null;
        this.apiKey = null;
        this.httpClient = new OkHttpClient()
                .newBuilder()
                .connectTimeout(API_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    public VeryfiClient(String clientId, String clientSecret, String username, String apiKey) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient()
                .newBuilder()
                .connectTimeout(API_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Prepares the headers needed for a request.
     * @param hasFiles Are there any files to be submitted as binary
     * @return headers to put in the request
     */
    public Headers getHeaders(boolean hasFiles) {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("User-Agent", "Python Veryfi-Python/0.1");
        headersMap.put("Accept", "application/json");
        headersMap.put("Content-Type", "application/json");
        headersMap.put("Client-Id", this.clientId);
        if (this.username != null && !this.username.isEmpty()) {
            headersMap.put("Authorization", "apikey " + this.username + ":" + this.apiKey);
        }
        if (hasFiles) {
            headersMap.remove("Content-Type");
        }
        return Headers.of(headersMap);
    }

    /**
     * Get API Base URL with API Version
     * @return Base URL to Veryfi API
     */
    public String getUrl() {
        return BASE_URL + API_VERSION;
    }

    /**
     * Submit the HTTP request.
     * @param httpVerb HTTP Method
     * @param endpointName Endpoint name such as 'documents', 'users', etc.
     * @param requestArguments JSON payload to send to Veryfi
     * @return A JSON String of the response data.
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String request(String httpVerb, String endpointName, Map<String, Object> requestArguments) throws VeryfiClientException {
        return request(httpVerb, endpointName, requestArguments, false);
    }

    /**
     * Submit the HTTP request.
     * @param httpVerb HTTP Method
     * @param endpointName Endpoint name such as 'documents', 'users', etc.
     * @param requestArguments JSON payload to send to Veryfi
     * @param isFileStream A boolean to check if the file stream is available default is false
     * @return A JSON of the response data.
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String request(String httpVerb, String endpointName, Map<String, Object> requestArguments, boolean isFileStream) throws VeryfiClientException {
        Headers defaultHeaders = getHeaders(isFileStream);
        String apiUrl = getUrl() + "/partner" + endpointName;

        Map<String, String> newHeadersMap = new HashMap<>();
        if (clientSecret != null && !clientSecret.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            String signature = generateSignature(requestArguments, timestamp);
            newHeadersMap.put("X-Veryfi-Request-Timestamp", String.valueOf(timestamp));
            newHeadersMap.put("X-Veryfi-Request-Signature", signature);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String requestBodyJSON = objectMapper.writeValueAsString(requestArguments);
            Headers finalHeaders = new Headers.Builder()
                    .addAll(defaultHeaders)
                    .addAll(Headers.of(newHeadersMap))
                    .build();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(requestBodyJSON, mediaType);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .method(httpVerb, body)
                    .headers(finalHeaders)
                    .build();
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new VeryfiClientException("Error in processing the request: " + response.message());
            }
            return String.valueOf(response.body());
        } catch (IOException e) {
            logger.error("Error in processing the request", e);
            throw new VeryfiClientException("Error in processing the request", e);
        }
    }

    /**
     * Generate unique signature for payload params.
     * @param payloadParams JSON params to be sent to API request
     * @param timestamp Unix Long timestamp
     * @return Unique signature generated using the client_secret and the payload
     */
    public String generateSignature(Map<String, Object> payloadParams, Long timestamp) {
        String payload = MessageFormat.format("timestamp:{0}", timestamp);
        for (String key : payloadParams.keySet()) {
            payload = MessageFormat.format("{0},{1}:{2}", payload, key, payloadParams.get(key));
        }
        return createHashAndConvertToBase64String(clientSecret, payload, "HmacSHA256");
    }

    private byte[] createHash(String key, String message, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, algorithm);

        Mac hmac = Mac.getInstance(algorithm);
        hmac.init(secretKey);
        return hmac.doFinal(messageBytes);
    }

    private String createHashAndConvertToBase64String(String key, String message, String algorithm) {
        try {
            byte[] hash = createHash(key, message, algorithm);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get list of documents
     * @return List of previously processed documents
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String getDocuments() throws VeryfiClientException {
        String endpointName = "/documents/";
        String documents = request("GET", endpointName, Collections.emptyMap());
        // TODO: how to fix this?
        // if "documents" in documents:
        //    return documents["documents"]
        return documents;
    }

    /**
     * Retrieve document by ID
     * @param documentId ID of the document you'd like to retrieve
     * @return Data extracted from the Document
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String getDocument(String documentId) throws VeryfiClientException {
        String endpointName = "/documents/" + documentId + "/";
        return request("GET", endpointName, Collections.singletonMap("id", documentId));
    }

    /**
     * Process Document and extract all the fields from it
     * @param filePath Path on disk to a file to submit for data extraction
     * @param categories List of categories Veryfi can use to categorize the document
     * @param deleteAfterProcessing Delete this document from Veryfi after data has been extracted
     * @return Data extracted from the document
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String processDocument(String filePath, List<String> categories, boolean deleteAfterProcessing) throws VeryfiClientException {
        final String endpointName = "/documents/";
        if (categories != null && categories.isEmpty()) {
            categories = CATEGORIES;
        }

        String base64EncodedString = null;
        String fileName = null;
        // TODO: extract fileName from the filePath and convert into base64
        // fileName = os.path.basename(filePath);
        // with open (filePath, "rb")as image_file:
        //      base64EncodedString = base64.b64encode(image_file.read()).decode("utf-8");

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("file_name", fileName);
        requestPayload.put("file_data", base64EncodedString);
        requestPayload.put("categories", categories);
        requestPayload.put("auto_delete", deleteAfterProcessing);
        return this.request("POST", endpointName, requestPayload);
    }

    // TODO: convert other methods as well

//    public static void main(String[] args) {
//        System.out.println(MessageFormat.format("{0}, {1}, {2}, {3}", "a", "b", "c", 40));
//        Map<String, String> newHeadersMap = new HashMap<>();
//        newHeadersMap.put("X-Veryfi-Request-Timestamp", String.valueOf(456));
//        newHeadersMap.put("X-Veryfi-Request-Signature", "signature");
//        System.out.println(newHeadersMap.toString());
//        System.out.println(newHeadersMap.toString().getBytes(StandardCharsets.UTF_8));
//    }

}
