package Controller;

import Model.Result;
import Model.Settings;
import Model.Task;
import Model.Test;
import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * Created by Alexander Baranov on 01.05.2017.
 */
public class ResultsSender implements Runnable {

    private ArrayList<Result> results;

    public ResultsSender(ArrayList<Result> results) {
        this.results = results;
    }

    @Override
    public void run() {
        if (Settings.getInstance().getResultsTableURL().isEmpty())
            throw new RuntimeException("There is no table for results!");
        try {
            Sheets service = GoogleSheetsManager.getSheetsService();
            String spreadsheetId = Settings.getInstance().getResultsTableURL();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
