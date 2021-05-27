
import java.util.Arrays;
import java.util.List;

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

    private String clientId;
    private String clientSecret;
    private String username;
    private String apiKey;
    private String headers;
    private String session;

    public Client(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = null;
        this.apiKey = null;
    }

    public Client(String clientId, String clientSecret, String username, String apiKey) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.apiKey = apiKey;
    }

    /**
     * Prepares the headers needed for a request.
     * @param hasFiles Are there any files to be submitted as binary
     */
    public List<h> getHeaders(boolean hasFiles) {
        Client
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n    \"first_name\": \"Emily1\",\n    \"last_name\": \"Gao\",\n    \"fullname\": \"Emily Gao\",\n    \"url_unique_profile\": \"https://www.linkedin.com/in/emily-gao-15848a54\",\n    \"summary\": \"n/a\",\n    \"connectionDegree\":\"1\",\n    \"positions\": [\n        {\n            \"type\": \"Current\", \n            \"title\": \"Marketing Manager\",\n            \"companyName\": \"FSAstore.com\",\n            \"companyUrl\": \"https://www.linkedin.com/recruiter/company/1440905\",\n            \"startDateMonth\": 6,\n            \"startDateYear\":  2017\n        }\n    ]\n}");
        Request request = new Request.Builder()
                .url("http://localhost:8000/api/v1/request/30/expert?api_key=81d2425b-5171-4b61-8ff2-c75d28714095")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());

        """
                Prepares the headers needed for a request.
                :param has_files: Are there any files to be submitted as binary
                :return: Dictionary with headers
                """
        final_headers = {
                "User-Agent":"Python Veryfi-Python/0.1",
                "Accept":"application/json",
                "Content-Type":"application/json",
                "Client-Id":self.client_id,
    }

        if self.username:
        final_headers.update(
                {"Authorization":"apikey {}:{}".format(self.username, self.api_key)}
        )

        if has_files:
        final_headers.pop("Content-Type", "application/x-www-form-urlencoded")

        return final_headers
    }

    def _get_url(self):
            """
        Get API Base URL with API Version
        :return: Base URL to Veryfi API
        """
            return self.base_url + self.api_version

    def _request(self, http_verb, endpoint_name, request_arguments, file_stream=None):
            """
        Submit the HTTP request.
        :param http_verb: HTTP Method
        :param endpoint_name: Endpoint name such as 'documents', 'users', etc.
        :param request_arguments: JSON payload to send to Veryfi
        :return: A JSON of the response data.
        """

    has_files = file_stream is not None
    headers = self._get_headers(has_files=has_files)
    api_url = "{0}/partner{1}".format(self._get_url(), endpoint_name)

            if self.client_secret:
    timestamp = int(time.time() * 1000)
    signature = self._generate_signature(request_arguments, timestamp=timestamp)
            headers.update(
    {
        "X-Veryfi-Request-Timestamp": str(timestamp),
            "X-Veryfi-Request-Signature": signature,
    }
        )

    response = self._session.request(
    http_verb,
    url=api_url,
    headers=headers,
    data=json.dumps(request_arguments),
    timeout=self.timeout,
            )

            if response.status_code not in [200, 201, 202, 204]:
    raise VeryfiClientError.from_response(response)

        return response.json()

    def _generate_signature(self, payload_params, timestamp):
            """
        Generate unique signature for payload params.
        :param payload_params: JSON params to be sent to API request
        :param timestamp: Unix Long timestamp
        :return: Unique signature generated using the client_secret and the payload
        """
    payload = "timestamp:{}".format(timestamp)
        for key in payload_params.keys():
    value = payload_params[key]
    payload = "{0},{1}:{2}".format(payload, key, value)

    secret_bytes = bytes(self.client_secret, "utf-8")
    payload_bytes = bytes(payload, "utf-8")
    tmp_signature = hmac.new(secret_bytes, msg=payload_bytes, digestmod=hashlib.sha256).digest()
    base64_signature = base64.b64encode(tmp_signature).decode("utf-8").strip()
        return base64_signature

    def get_documents(self):
            """
        Get list of documents
        :return: List of previously processed documents
        """
    endpoint_name = "/documents/"
    request_arguments = {}
    documents = self._request("GET", endpoint_name, request_arguments)
            if "documents" in documents:
            return documents["documents"]
            return documents

    def get_document(self, document_id):
            """
        Retrieve document by ID
        :param document_id: ID of the document you'd like to retrieve
        :return: Data extracted from the Document
        """
    endpoint_name = "/documents/{}/".format(document_id)
    request_arguments = {"id": document_id}
    document = self._request("GET", endpoint_name, request_arguments)
            return document

    def process_document(self, file_path, categories=None, delete_after_processing=False):
            """
        Process Document and extract all the fields from it
        :param file_path: Path on disk to a file to submit for data extraction
        :param categories: List of categories Veryfi can use to categorize the document
        :param delete_after_processing: Delete this document from Veryfi after data has been extracted
        :return: Data extracted from the document
        """
    endpoint_name = "/documents/"
            if not categories:
    categories = self.CATEGORIES
            file_name = os.path.basename(file_path)
    with open(file_path, "rb") as image_file:
    base64_encoded_string = base64.b64encode(image_file.read()).decode("utf-8")
    request_arguments = {
        "file_name": file_name,
                "file_data": base64_encoded_string,
                "categories": categories,
                "auto_delete": delete_after_processing,
    }
    document = self._request("POST", endpoint_name, request_arguments)
            return document

    def _process_document_file(self, file_path, categories=None, delete_after_processing=False):
            """
        Process Document by sending it to Veryfi as multipart form
        :param file_path: Path on disk to a file to submit for data extraction
        :param categories: List of categories Veryfi can use to categorize the document
        :param delete_after_processing: Delete this document from Veryfi after data has been extracted
        :return: Data extracted from the document
        """
    endpoint_name = "/documents/"
            if not categories:
    categories = self.CATEGORIES
            file_name = os.path.basename(file_path)
    request_arguments = {
        "file_name": file_name,
                "categories": categories,
                "auto_delete": delete_after_processing,
    }
    with open(file_path) as file_stream:
    document = self._request("POST", endpoint_name, request_arguments, file_stream)
            return document

    def process_document_url(
            self,
            file_url: Optional[str] = None,
            categories: Optional[List[str]] = None,
            delete_after_processing=False,
            boost_mode: int = 0,
            external_id: Optional[str] = None,
            max_pages_to_process: Optional[int] = None,
            file_urls: Optional[List[str]] = None,
            ) -> Dict:
            """
        Process Document from url and extract all the fields from it
        :param file_url: Required if file_urls isn't specified. Publicly accessible URL to a file, e.g. "https://cdn.example.com/receipt.jpg"
        :param file_urls: Required if file_url isn't specifies. List of publicly accessible URLs to multiple files, e.g. ["https://cdn.example.com/receipt1.jpg", "https://cdn.example.com/receipt2.jpg"]
        :param categories: List of categories to use when categorizing the document
        :param delete_after_processing: Delete this document from Veryfi after data has been extracted
        :param max_pages_to_process: When sending a long document to Veryfi for processing, this paremeter controls how many pages of the document will be read and processed, starting from page 1.
        :param boost_mode: Flag that tells Veryfi whether boost mode should be enabled. When set to 1, Veryfi will skip data enrichment steps, but will process the document faster. Default value for this flag is 0
        :param external_id: Optional custom document identifier. Use this if you would like to assign your own ID to documents

        :return: Data extracted from the document
        """
    endpoint_name = "/documents/"
    request_arguments = {
        "auto_delete": delete_after_processing,
                "boost_mode": boost_mode,
                "categories": categories,
                "external_id": external_id,
                "file_url": file_url,
                "file_urls": file_urls,
                "max_pages_to_process": max_pages_to_process,
    }

        return self._request("POST", endpoint_name, request_arguments)

    def delete_document(self, document_id):
            """
        Delete Document from Veryfi
        :param document_id: ID of the document you'd like to delete
        """
    endpoint_name = "/documents/{0}/".format(document_id)
    request_arguments = {"id": document_id}
        self._request("DELETE", endpoint_name, request_arguments)

    def update_document(self, id: int, **kwargs) -> Dict:
            """
        Update data for a previously processed document, including almost any field like `vendor`, `date`, `notes` and etc.

        ```veryfi_client.update_document(id, date="2021-01-01", notes="look what I did")```

        :param kwargs: fields to update

        :return: A document json with updated fields, if fields are writible. Otherwise a document with unchanged fields.
        """
    endpoint_name = f"/documents/{id}/"

            return self._request("PUT", endpoint_name, kwargs)

}
