package com.veryfi;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @return JSON List of previously processed documents
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String getDocuments() throws VeryfiClientException {
        String endpointName = "/documents/";
        return request("GET", endpointName, Collections.emptyMap());
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
        if (categories == null || categories.isEmpty()) {
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

    /**
     * Process Document by sending it to Veryfi as multipart form
     * @param filePath Path on disk to a file to submit for data extraction
     * @param categories List of categories Veryfi can use to categorize the document
     * @param deleteAfterProcessing Delete this document from Veryfi after data has been extracted
     * @return document file after processing
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String processDocumentFile(String filePath, List<String> categories, boolean deleteAfterProcessing) throws VeryfiClientException {
        final String endpointName = "/documents/";
        if (categories == null || categories.isEmpty()) {
            categories = CATEGORIES;
        }
        String fileName = null;
        // TODO: get the filename
        // file_name = os.path.basename(file_path)
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("file_name", fileName);
        requestPayload.put("categories", categories);
        requestPayload.put("auto_delete", deleteAfterProcessing);
        return request("POST", endpointName, requestPayload, true);
    }

    /**
     * Process Document from url and extract all the fields from it
     * @param fileUrl Required if file_urls isn't specified. Publicly accessible URL to a file, e.g. "https://cdn.example.com/receipt.jpg"
     * @param fileUrls Required if file_url isn't specifies. List of publicly accessible URLs to multiple files, e.g. ["https://cdn.example.com/receipt1.jpg", "https://cdn.example.com/receipt2.jpg"]
     * @param categories List of categories to use when categorizing the document
     * @param deleteAfterProcessing Delete this document from Veryfi after data has been extracted
     * @param maxPagesToProcess When sending a long document to Veryfi for processing, this paremeter controls how many pages of the document will be read and processed, starting from page 1.
     * @param boostMode Flag that tells Veryfi whether boost mode should be enabled. When set to 1, Veryfi will skip data enrichment steps, but will process the document faster. Default value for this flag is 0
     * @param externalId Optional custom document identifier. Use this if you would like to assign your own ID to documents
     * @return Data extracted from the document
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String processDocumentUrl(
            String fileUrl,
            List<String> fileUrls,
            List<String> categories,
            boolean deleteAfterProcessing,
            int maxPagesToProcess,
            int boostMode,
            String externalId
    ) throws VeryfiClientException {
        final String endpointName = "/documents/";
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("file_url", fileUrl);
        requestPayload.put("file_urls", fileUrls);
        requestPayload.put("categories", categories);
        requestPayload.put("auto_delete", deleteAfterProcessing);
        requestPayload.put("max_pages_to_process", maxPagesToProcess);
        requestPayload.put("boost_mode", boostMode);
        requestPayload.put("external_id", externalId);
        return request("POST", endpointName, requestPayload);
    }

    /**
     * Delete Document from Veryfi
     * @param documentId ID of the document you'd like to delete
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public void deleteDocument(int documentId) throws VeryfiClientException {
        final String endpointName = "/documents/" + documentId + "/";
        request("DELETE", endpointName, Collections.singletonMap("id", documentId));
    }

    /**
     * Update data for a previously processed document, including almost any field like `vendor`, `date`, `notes` and etc.
     * @param id the id of the document to update
     * @param fieldsToUpdate key value pair of the fields to update
     * @return A document json with updated fields, if fields are writible. Otherwise a document with unchanged fields.
     * @throws VeryfiClientException if there is any error in making request to veryfi APIs
     */
    public String updateDocument(int id, Map<String, Object> fieldsToUpdate) throws VeryfiClientException {
        final String endpointName = "/documents/" + id + "/";
        return request("PUT", endpointName, fieldsToUpdate);
    }

}
