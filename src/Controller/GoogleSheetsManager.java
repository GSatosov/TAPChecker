package Controller;

import Model.Constants;
import Model.Task;
import Model.Tests;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GoogleSheetsManager {

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /* Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /* Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /* Global instance of the scopes required by this manager.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at {@link Constants.credentialsStoreDir}
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(Constants.CREDENTIALS_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /*
     * Creates an authorized Credential object.
     */
    private static Credential authorize() throws IOException {
        // Load client secrets.
        File file = new File("client_secret.json");
        System.out.print(file.getAbsolutePath());
        InputStream in = GoogleSheetsManager.class.getResourceAsStream("client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        System.out.println("Credentials saved to " + Constants.CREDENTIALS_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /*
     * Build and return an authorized Sheets API client service.
     */
    private static Sheets getSheetsService() throws IOException {
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(Constants.APPLICATION_NAME).build();
    }

    private static Tests getTests(Task task) throws IOException {
        Sheets service = getSheetsService();

        String spreadsheetId = "1ejxrYkoWLKMDsQ3xW0c7DTrezikwIQMMqFv5l-g3Deg";
        String range = task.getSubjectName() + ", " + task.getName();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
            return null;
        } else {
            HashMap<String, ArrayList<String>> testContent = new HashMap<>();
            for (List row : values) {
                if (row.size() > 0 && !row.get(0).toString().isEmpty()) {
                    String test = row.get(0).toString();
                    ArrayList<String> output = new ArrayList<>();
                    int index = 1;
                    for (int i = 1; i < row.size(); i++) {
                        if (!row.get(i).toString().isEmpty()) output.add(row.get(i).toString());
                    }
                    testContent.put(test, output);
                }
            }
            System.out.print(testContent.toString());
            return new Tests(task, testContent);
        }
    }

    public static void main(String[] args) throws IOException {
        Task task = new Task("task 1", "subject 1","source");
        getTests(task);
    }

}