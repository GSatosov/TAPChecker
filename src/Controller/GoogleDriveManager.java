package Controller;

import Model.GlobalSettings;
import Model.Task;
import Model.Test;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
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

import java.io.*;
import java.nio.file.Files;
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
            DATA_STORE_FACTORY = new FileDataStoreFactory(GlobalSettings.getCredentialsStoreDir());
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
    public static Credential authorize() throws IOException {
        // Build flow and trigger user authorization request.
        HashSet tScopes = new HashSet();
        tScopes.addAll(SheetsScopes.all());
        tScopes.add(DriveScopes.DRIVE_METADATA);
        tScopes.add(DriveScopes.DRIVE_FILE);
        tScopes.add(DriveScopes.DRIVE_READONLY);
        Set<String> SCOPES = Collections.unmodifiableSet(tScopes);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, GlobalSettings.getClientId(), GlobalSettings.getClientSecret(), SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    /**
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Drive client service
     * @throws IOException
     */
    static Drive getDriveService() throws IOException {
        if (service == null) {
            Credential credential = authorize();
            service = new Drive.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(GlobalSettings.getApplicationName())
                    .build();
        }
        return service;
    }

    private static volatile Drive service;

    private static String mask = "mimeType != 'application/vnd.google-apps.folder' and " +
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
            "mimeType != 'application/vnd.google-apps.document' and trashed = false";

    public static ByteArrayOutputStream getGlobalSettings() throws IOException {
        Drive service = getDriveService();
        Optional<File> result = service.files().list()
                .setQ("name contains '" + GlobalSettings.getGlobalSettingsFileName() + "' and trashed = false")
                .execute().getFiles().stream().findFirst();
        if (result.isPresent()) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            service.files().get(result.get().getId())
                    .executeMediaAndDownloadTo(byteArrayOutputStream);
            return byteArrayOutputStream;
        }
        return null;
    }

    public static void saveGlobalSettings() throws IOException {
        Drive service = getDriveService();
        FileList result = service.files().list()
                .setQ("name contains '" + GlobalSettings.getGlobalSettingsFileName() + "' and trashed = false")
                .execute();
        result.getFiles().forEach(file -> {
            try {
                service.files().delete(file.getId()).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(GlobalSettings.getGlobalSettingsFileName());
        java.io.File filePath = new java.io.File(GlobalSettings.getDataFolder() + "/" + GlobalSettings.getGlobalSettingsFileName());
        FileContent mediaContent = new FileContent(Files.probeContentType(filePath.toPath()), filePath);
        com.google.api.services.drive.model.File file = GoogleDriveManager.getDriveService().files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
    }

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
                    .setQ("'" + parentId + "' in parents and " + mask)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            String taskName = task.getName().substring(0, task.getName().lastIndexOf('.'));
            Optional<File> oTests = result.getFiles().stream().filter(file -> file.getName().substring(0, file.getName().lastIndexOf('.')).startsWith(taskName)).findFirst();
            if (!oTests.isPresent()) {
                throw new IOException("There is no tests file for: " + taskName + "/" + taskSubject);
            } else {
                String fileId = oTests.get().getId();
                OutputStream outputStream = new ByteArrayOutputStream();
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                parseJsonToTask(outputStream, task);
                return task.getTestContents();
            }
        }
    }

    public static HashMap<String, ArrayList<String>> getTaskNumbersForSubjects() throws IOException {
        HashMap<String, ArrayList<String>> taskNumbersForSubjects = new HashMap<>();
        Set<String> subjects = GlobalSettings.getInstance().getSubjectsAndGroups().keySet();
        Drive service = getDriveService();
        FileList getFolders = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder'").execute();
        getFolders.getFiles().stream().filter(file -> subjects.contains(file.getName().replaceAll("_", " "))).forEach(subjectFile -> {
            try {
                String subjectName = subjectFile.getName().replaceAll("_", " ");
                FileList files = service.files().list()
                        .setQ("'" + subjectFile.getId() + "' in parents and " + mask)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();
                ArrayList<String> taskNumbers = new ArrayList<>();
                files.getFiles().forEach(file -> {
                    String fileId = file.getId();
                    OutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                        JSONObject tests = new JSONObject(outputStream.toString());
                        taskNumbers.add(tests.getString("taskCode"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });
                taskNumbers.sort(String::compareTo);
                taskNumbersForSubjects.put(subjectName, taskNumbers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return taskNumbersForSubjects;
    }

    public static HashMap<String, ArrayList<Task>> getTasksAndSubjects() throws IOException {
        HashMap<String, ArrayList<Task>> tasksAndSubjects = new HashMap<>();
        Set<String> subjects = GlobalSettings.getInstance().getSubjectsAndGroups().keySet();
        Drive service = getDriveService();
        FileList getFolders = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder'").execute();
        getFolders.getFiles().stream().filter(file -> subjects.contains(file.getName().replaceAll("_", " "))).forEach(subjectFile -> {
            try {
                String subjectName = subjectFile.getName().replaceAll("_", " ");
                FileList files = service.files().list()
                        .setQ("'" + subjectFile.getId() + "' in parents and " + mask)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();
                ArrayList<Task> tasks = new ArrayList<>();
                files.getFiles().forEach(file -> {
                    Task task = new Task(file.getName().substring(0, file.getName().lastIndexOf('.')), subjectName, null, null);
                    OutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        service.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
                        parseJsonToTask(outputStream, task);
                        tasks.add(task);
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                    }

                });
                tasks.sort(Comparator.comparing(Task::getName));
                tasksAndSubjects.put(subjectName, tasks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return tasksAndSubjects;
    }

    public static String deleteTask(Task task) throws IOException {
        Drive service = getDriveService();

        FileList getFolders = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder'").execute();
        Optional<File> folder = getFolders.getFiles().stream().filter(file -> task.getSubjectName().equals(file.getName().replaceAll("_", " "))).findFirst();
        String folderId = "";
        if (folder.isPresent()) {
            folderId = folder.get().getId();
        } else {
            File fileMetadata = new File();
            fileMetadata.setName(task.getSubjectName().replaceAll(" ", "_"));
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            File folderFile = service.files().create(fileMetadata)
                    .setFields("id")
                    .execute();

            folderId = folderFile.getId();
        }

        FileList result = service.files().list()
                .setQ("'" + folderId + "' in parents and name contains '" + task.getName() + ".txt' and trashed = false")
                .execute();
        result.getFiles().forEach(file -> {
            try {
                service.files().delete(file.getId()).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return folderId;
    }

    public static void saveTask(Task task) throws IOException {
        String folderId = deleteTask(task);

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setParents(Collections.singletonList(folderId));
        fileMetadata.setName(task.getName() + ".txt");

        JSONObject taskJSON = new JSONObject();
        taskJSON.put("maximumOperatingTimeInMS", task.getTimeInMS());
        taskJSON.put("antiPlagiarism", task.shouldBeCheckedForAntiPlagiarism());
        taskJSON.put("deadline", new SimpleDateFormat("dd.MM.yyyy").format(task.getDeadline()));
        taskJSON.put("hasHardDeadline", task.hasHardDeadline());
        taskJSON.put("taskCode", task.getTaskCode());
        taskJSON.put("additionalTest", task.getAdditionalTest());
        JSONArray tests = new JSONArray();
        task.getTestContents().forEach(test -> {
            JSONObject testJSON = new JSONObject();

            JSONArray testInput = new JSONArray();
            test.getInput().forEach(testInput::put);
            testJSON.put("input", testInput);

            JSONArray testOutput = new JSONArray();
            test.getOutputVariants().forEach(outputVar -> {
                JSONArray outputJson = new JSONArray();
                outputVar.forEach(outputJson::put);
                testOutput.put(outputJson);
            });
            testJSON.put("output", testOutput);
            testJSON.put("applyAdditionalTest", test.hasAnAdditionalTest());
            tests.put(testJSON);
        });
        taskJSON.put("tests", tests);

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(GlobalSettings.getDataFolder() + "/" + task.getName() + ".txt"));
            out.write(taskJSON.toString());
            out.close();

            java.io.File filePath = new java.io.File(GlobalSettings.getDataFolder() + "/" + task.getName() + ".txt");
            FileContent mediaContent = new FileContent(Files.probeContentType(filePath.toPath()), filePath);
            com.google.api.services.drive.model.File file = GoogleDriveManager.getDriveService().files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            filePath.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void parseJsonToTask(OutputStream stream, Task task) throws ParseException {
        ArrayList<Test> testsResult = new ArrayList<>();
        JSONObject tests = new JSONObject(stream.toString());
        Date deadline = new SimpleDateFormat("dd.MM.yyyy").parse(tests.getString("deadline"));
        boolean antiPlagiarism = tests.getBoolean("antiPlagiarism");
        long time = tests.getLong("maximumOperatingTimeInMS");
        boolean hasHardDeadline = tests.getBoolean("hasHardDeadline");
        String taskCode = tests.getString("taskCode");
        String additionalTest = tests.getString("additionalTest");
        task.setAdditionalTest(additionalTest);
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
            Test test = new Test(input, output);
            test.setApplyAdditionalTest(jOnj.getBoolean("applyAdditionalTest"));
            testsResult.add(test);
        });
        task.setTestContents(testsResult);
    }


}