package Controller;

import Model.Settings;
import Model.Task;
import Model.Test;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class GoogleSheetsManager {
    static private Sheets service;

    private static void setSheetsService() throws IOException {
        service = getSheetsService();
    }

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /* Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /* Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /* Global instance of the scopes required by this manager.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at {@link Settings.credentialsStoreDir}
     */
    private static final Set<String> SCOPES = SheetsScopes.all();

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(Settings.getCredentialsStoreDir());
            setSheetsService();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /*
     * Creates an authorized Credential object.
     */
    private static Credential authorize() throws IOException {
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, Settings.getClientId(), Settings.getClientSecret(), SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        System.out.println("Credentials saved to " + Settings.getCredentialsStoreDir().getAbsolutePath());
        return credential;
    }

    /*
     * Build and return an authorized Sheets API client service.
     */
    private static Sheets getSheetsService() throws IOException {
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(Settings.getApplicationName()).build();
    }

    static ArrayList<Test> getTests(Task task) throws IOException {
        /*String spreadsheetId = "1ejxrYkoWLKMDsQ3xW0c7DTrezikwIQMMqFv5l-g3Deg";
        String range = task.getSubjectName() + ", " + task.getName();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
            return null;
        } else {
            ArrayList<Test> testContent = new ArrayList<>();
            for (List row : values) {
                if (row.size() > 0 && !row.get(0).toString().isEmpty()) {
                    String test = row.get(0).toString();
                    ArrayList<String> output = new ArrayList<>();
                    for (int i = 1; i < row.size(); i++) {
                        if (!row.get(i).toString().isEmpty()) output.add(row.get(i).toString());
                    }
                    testContent.add(new Test(test, output));
                }
            }
            System.out.println(testContent.toString());
            return testContent;
        }*/
        return null;
    }

}