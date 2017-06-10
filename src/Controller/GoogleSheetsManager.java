package Controller;

import Model.GlobalSettings;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GoogleSheetsManager {

    private static volatile Sheets service;

    /*
    * Build and return an authorized Sheets API client service.
    */
    static Sheets getService() throws IOException {
        if (service == null) {
            synchronized (Sheets.class) {
                if (service == null) {
                    Credential credential = GoogleDriveManager.authorize();
                    service = new Sheets.Builder(GoogleDriveManager.HTTP_TRANSPORT, GoogleDriveManager.JSON_FACTORY, credential).setApplicationName(GlobalSettings.getApplicationName()).build();
                }
            }
        }
        return service;
    }

    public static String createSpreadsheet() throws IOException {
        Spreadsheet requestBody = new Spreadsheet();

        Sheets sheetsService = getService();
        Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);

        Spreadsheet spreadsheet = request.execute();

        List<Request> requests = new ArrayList<>();
        requests.add(new Request()
                .setUpdateSpreadsheetProperties(new UpdateSpreadsheetPropertiesRequest()
                        .setProperties(new SpreadsheetProperties()
                                .setTitle("Результаты"))
                        .setFields("title")));

        service.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();

        Drive driveService = GoogleDriveManager.getDriveService();

        String rootId = GoogleDriveManager.getRootFolder();

        FileList getFolders = driveService.files().list().setQ("'" + rootId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false").execute();

        Optional<File> resultsFolderOpt = getFolders.getFiles().stream().filter(file -> file.getName().equals("Результаты")).findFirst();
        File resultsFolder;
        if (!resultsFolderOpt.isPresent()) {
            File fileMetadata = new File();
            fileMetadata.setName("Результаты");
            fileMetadata.setParents(Collections.singletonList(rootId));
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            resultsFolder = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
        }
        else resultsFolder = resultsFolderOpt.get();

        File file = driveService.files().get(spreadsheet.getSpreadsheetId())
                .setFields("parents")
                .execute();
        StringBuilder previousParents = new StringBuilder();
        for(String parent: file.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }

        driveService.files().update(spreadsheet.getSpreadsheetId(), null)
                .setAddParents(resultsFolder.getId())
                .setRemoveParents(previousParents.toString())
                .setFields("id, parents")
                .execute();

        return spreadsheet.getSpreadsheetUrl();
    }

}