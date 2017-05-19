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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoogleDriveManager {

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(Settings.getCredentialsStoreDir());
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    static Credential authorize() throws IOException {
        // Build flow and trigger user authorization request.
        HashSet tScopes = new HashSet();
        tScopes.addAll(SheetsScopes.all());
        tScopes.add(DriveScopes.DRIVE_METADATA);
        tScopes.add(DriveScopes.DRIVE_FILE);
        tScopes.add(DriveScopes.DRIVE_READONLY);
        Set<String> SCOPES = Collections.unmodifiableSet(tScopes);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, Settings.getClientId(), Settings.getClientSecret(), SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    /**
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Drive client service
     * @throws IOException
     */
    private static Drive getDriveService() throws IOException {
        if (service == null) {
            Credential credential = authorize();
            service = new Drive.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(Settings.getApplicationName())
                    .build();
        }
        return service;
    }

    public static Drive getService() throws IOException {
        if (service == null) {
            synchronized (Sheets.class) {
                if (service == null) {
                    Credential credential = authorize();
                    service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).build();
                }
            }
        }
        return service;
    }

    private static volatile Drive service;

    static ArrayList<Test> getTests(Task task) throws IOException, ParseException {
        Drive service = getDriveService();
        FileList result = service.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder'")
                .execute();
        String taskSubject = task.getSubjectName();
        Optional<File> parentFile = result.getFiles().stream().filter(file -> file.getName().equals(taskSubject)).findFirst();
        String parentId = parentFile.map(File::getId).orElse(null);
        if (parentId == null) throw new IOException("There is no subject folder!");
        else {
            result = service.files().list()
                    .setQ("'" + parentId + "' in parents and " +
                            "mimeType != 'application/vnd.google-apps.folder' and " +
                            "mimeType != 'application/vnd.google-apps.audio' and " +
                            "mimeType != 'application/vnd.google-apps.drawing' and " +
                            "mimeType != 'application/vnd.google-apps.file' and " +
                            "mimeType != 'application/vnd.google-apps.folder' and " +
                            "mimeType != 'application/vnd.google-apps.form' and " +
                            "mimeType != 'application/vnd.google-apps.fusiontable' and " +
                            "mimeType != 'application/vnd.google-apps.map' and " +
                            "mimeType != 'application/vnd.google-apps.photo' and " +
                            "mimeType != 'application/vnd.google-apps.presentation' and " +
                            "mimeType != 'application/vnd.google-apps.script' and " +
                            "mimeType != 'application/vnd.google-apps.sites' and " +
                            "mimeType != 'application/vnd.google-apps.spreadsheet' and " +
                            "mimeType != 'application/vnd.google-apps.unknown' and " +
                            "mimeType != 'application/vnd.google-apps.video' and " +
                            "mimeType != 'application/vnd.google-apps.drive-sdk' and " +
                            "mimeType != 'application/vnd.google-apps.document' and trashed = false")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            String taskName = task.getName().split("\\.")[0];

            Optional<File> oTests = result.getFiles().stream().filter(file -> file.getName().split("\\.")[0].equals(taskName)).findFirst();
            if (!oTests.isPresent()) {
                throw new IOException("There is no tests file for: " + taskName + "/" + taskSubject);
            } else {
                String fileId = oTests.get().getId();
                OutputStream outputStream = new ByteArrayOutputStream();
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                ArrayList<Test> testsResult = new ArrayList<>();
                JSONObject tests = new JSONObject(outputStream.toString());
                Date deadline = new SimpleDateFormat("dd.MM.yyyy").parse(tests.getString("deadline"));
                boolean antiPlagiarism = tests.getBoolean("antiPlagiarism");
                long time = tests.getLong("maximumOperatingTimeInMS");
                boolean hasHardDeadline = tests.getBoolean("hasHardDeadline");
                String taskCode = tests.getString("taskCode");
                JSONArray aTests = tests.getJSONArray("tests");
                task.setTestFields(time, antiPlagiarism, deadline, taskCode, hasHardDeadline);
                aTests.forEach(t -> {
                    ArrayList<String> input = new ArrayList<>();
                    ArrayList<ArrayList<String>> output = new ArrayList<>();
                    JSONObject jOnj = (JSONObject) t;
                    JSONArray jInput = jOnj.getJSONArray("input");
                    jInput.forEach(jI -> input.add((String) jI));
                    JSONArray jOutput = jOnj.getJSONArray("output");
                    jOutput.forEach(jO -> {
                        JSONArray aJO = (JSONArray) jO;
                        ArrayList<String> outputVar = new ArrayList<>();
                        aJO.forEach(jAJO -> outputVar.add((String) jAJO));
                        output.add(outputVar);
                    });
                    testsResult.add(new Test(input, output));
                });
                return testsResult;
            }
        }
    }

}