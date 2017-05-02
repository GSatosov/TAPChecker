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

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class GoogleSheetsManager {

    private static volatile Sheets service;

    /*
    * Build and return an authorized Sheets API client service.
    */
    public static Sheets getService() throws IOException {
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