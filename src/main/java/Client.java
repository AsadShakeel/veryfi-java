import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client {
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

    public Client(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = null;
        this.apiKey = null;
        this.httpClient = new OkHttpClient().newBuilder().build();
    }

    public Client(String clientId, String clientSecret, String username, String apiKey) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient().newBuilder().build();
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
     */
    public String request(String httpVerb, String endpointName, Map<String, Object> requestArguments) {
        return request(httpVerb, endpointName, requestArguments, false);
    }

    /**
     * Submit the HTTP request.
     * @param httpVerb HTTP Method
     * @param endpointName Endpoint name such as 'documents', 'users', etc.
     * @param requestArguments JSON payload to send to Veryfi
     * @param isFileStream A boolean to check if the file stream is available default is false
     * @return A JSON of the response data.
     */
    public String request(String httpVerb, String endpointName, Map<String, Object> requestArguments, boolean isFileStream) {
        Headers headers = getHeaders(isFileStream);
        String apiUrl = getUrl() + "/partner" + endpointName;
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestArguments);
        } catch (JsonProcessingException e) {
            // TODO: return some error response
        }

        if (clientSecret != null && !clientSecret.isEmpty()) {
            // TODO: Generate timestamp and signature, and update headers
            // timestamp = int(time.time() * 1000)
            // signature = self._generate_signature(request_arguments, timestamp = timestamp)
            // headers.update(
            //         {
            //             "X-Veryfi-Request-Timestamp":str(timestamp),
            //             "X-Veryfi-Request-Signature":signature,
            //         }
            //     )
        }

        // TODO: make http request and return valid response otherwise through exception
        // response = self._session.request(
        //         http_verb,
        //         url=apiUrl,
        //         headers=headers,
        //         data=json.dumps(request_arguments),
        //         timeout=self.timeout,
        //         )
        //
        // if response.status_code not in [200, 201, 202, 204]:
        //    raise VeryfiClientError.from_response(response)

        return "";
    }

    /**
     * Process Document and extract all the fields from it
     * @param filePath Path on disk to a file to submit for data extraction
     * @param categories List of categories Veryfi can use to categorize the document
     * @param deleteAfterProcessing Delete this document from Veryfi after data has been extracted
     * @return Data extracted from the document
     */
    public String processDocument(String filePath, List<String> categories, boolean deleteAfterProcessing) {
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

}
