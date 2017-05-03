package Controller;

import Model.Settings;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;

class GoogleSheetsManager {

    private static volatile Sheets service;

    /*
    * Build and return an authorized Sheets API client service.
    */
    static Sheets getService() throws IOException {
        if (service == null) {
            synchronized (Sheets.class) {
                if (service == null) {
                    Credential credential = GoogleDriveManager.authorize();
                    service = new Sheets.Builder(GoogleDriveManager.HTTP_TRANSPORT, GoogleDriveManager.JSON_FACTORY, credential).setApplicationName(Settings.getApplicationName()).build();
                }
            }
        }
        return service;
    }

}