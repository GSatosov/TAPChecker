package Controller;

import Model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.sun.xml.internal.ws.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Alexander Baranov on 01.05.2017.
 */
public class ResultsSender implements Runnable {

    private HashMap<String, ArrayList<Result>> results;
    private Callback callback;

    private boolean deleteDirectory(File file) {
        File[] contents = file.listFiles();
        boolean flag = true;
        if (contents != null) {
            for (File f : contents) {
                if (flag) deleteDirectory(f);
            }
        }
        return flag ? file.delete() : flag;
    }

    public ResultsSender(ArrayList<Result> rs, Callback callback) {
        rs.sort((r1, r2) -> r2.getTask().getReceivedDate().compareTo(r1.getTask().getReceivedDate()));
        results = new HashMap<>();
        rs.forEach(result -> {
            String key = result.getSubject();
            ArrayList<Result> res = results.get(key);
            if (res == null) {
                res = new ArrayList<>();
            }
            Optional<Result> firstResult = res.stream().filter(r -> r.getStudent().getName().equals(result.getStudent().getName()) && r.getTask().getName().equals(result.getTask().getName()) && r.getGroup().equals(result.getStudent().getGroupName())).findFirst();
            if (firstResult.isPresent()) {
                Result old = firstResult.get();
                if (old.compareTo(result) < 0) {
                    res.remove(old);
                    File dir = Paths.get(old.getTask().getSourcePath()).getParent().toFile();
                    if (!deleteDirectory(dir)) {
                        throw new RuntimeException("Please, delete the directory: " + dir.getAbsolutePath());
                    }
                    else {
                        System.out.println("Result successfully deleted: " + old);
                    }
                    res.add(result);
                }
            }
            else {
                res.add(result);
            }
            results.put(key, res);
        });
        this.callback = callback;
    }

    @Override
    public void run() {
        if (Settings.getInstance().getResultsTableURL().isEmpty())
            throw new RuntimeException("There is no table for results!");
        try {
            Sheets service = GoogleSheetsManager.getService();
            String spreadsheetId = Settings.getInstance().getResultsTableURL();
            results.forEach((k, v) -> {
                String range = k;
                String subject = v.get(0).getSubject();
                ValueRange response = null;
                try {
                    response = service.spreadsheets().values().get(spreadsheetId, range).execute();
                } catch (IOException e) {
                    List<Request> requests = new ArrayList<>();
                    requests.add(new Request().setAddSheet(new AddSheetRequest().setProperties(new SheetProperties().setTitle(range))));
                    BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
                    try {
                        BatchUpdateSpreadsheetResponse responseCreate = service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
                        response = service.spreadsheets().values().get(spreadsheetId, range).execute();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (response != null) {
                    try {
                        Sheets.Spreadsheets.Get request = service.spreadsheets().get(spreadsheetId);
                        request.setRanges(Arrays.asList(range));
                        Spreadsheet responseID = request.execute();
                        Integer sheetID = responseID.getSheets().get(0).getProperties().getSheetId();
                        List<List<Object>> values = response.getValues();
                        ArrayList<String> tasks = new ArrayList<>();
                        if (values != null && values.size() > 0) {
                            int i = 0;
                            while (i < values.size()) {
                                List<Object> row = values.get(i);
                                if (row.size() > 0) {
                                    final String group = row.get(0).toString();
                                    if (v.stream().anyMatch(vs -> vs.getGroup().equals(group))) {
                                        i++;
                                        row = values.get(i);
                                        for (int j = 1; j < row.size(); j++) {
                                            tasks.add(row.get(j).toString().split("\\.")[0]);
                                        }
                                        i++;
                                        String studentName = "name";
                                        while (row.size() > 0 && !studentName.isEmpty() && i < values.size()) {
                                            row = values.get(i);
                                            if (row.size() > 0) {
                                                studentName = row.get(0).toString();
                                                for (int j = 1; j < row.size(); j++) {
                                                    String result = row.get(j).toString();
                                                    final int index = j;
                                                    final String name = studentName;
                                                    if (!result.isEmpty() && !v.stream().anyMatch(r -> r.getTask().getName().equals(tasks.get(index - 1)) && r.getStudent().getName().equals(name))) {
                                                        Task downloadedTask = new Task(tasks.get(index - 1), subject, null, null);
                                                        Student downloadedStudent = new Student(studentName, group);
                                                        downloadedTask.setAuthor(downloadedStudent);
                                                        Result downloadedResult = new Result(result, downloadedTask);
                                                        v.add(downloadedResult);
                                                    }
                                                }
                                            }
                                            i++;
                                        }
                                    }
                                }
                                i++;
                            }
                        }
                        Set<String> groups = new HashSet<>();
                        v.forEach(vs -> groups.add(vs.getGroup()));
                        v.sort((v1, v2) -> v1.getStudent().getName().compareTo(v2.getStudent().getName()));
                        List<List<Object>> writeData = new ArrayList<>();
                        groups.stream().sorted(String::compareTo).forEach(group -> {
                            writeData.add(Arrays.asList(group));
                            final ArrayList<String> newTasks = new ArrayList<>();
                            v.forEach(r -> {
                                if (r.getGroup().equals(group) && !newTasks.contains(StringUtils.capitalize(r.getTask().getName().split("\\.")[0])))
                                    newTasks.add(StringUtils.capitalize(r.getTask().getName().split("\\.")[0]));
                            });
                            newTasks.sort(String::compareTo);
                            newTasks.add(0, "ФИО");
                            writeData.add(newTasks.stream().collect(Collectors.toList()));
                            HashMap<String, ArrayList<Object>> studentsResults = new HashMap<>();
                            v.forEach(result -> {
                                if (result.getGroup().equals(group)) {
                                    String studName = result.getStudent().getName();
                                    ArrayList<Object> studResults = studentsResults.get(studName);
                                    if (studResults == null) {
                                        studResults = new ArrayList<>();
                                        studResults.add(studName);
                                        for (int i = 0; i < newTasks.size() - 1; i++) studResults.add("");
                                    }
                                    studResults.set(newTasks.indexOf(StringUtils.capitalize(result.getTask().getName().split("\\.")[0])), result.getResult());
                                    studentsResults.put(studName, studResults);
                                }
                            });
                            studentsResults.forEach((key, result) -> writeData.add(result));
                            writeData.add(Arrays.asList("").stream().collect(Collectors.toList()));
                        });

                        ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");;
                        try {
                            service.spreadsheets().values().clear(spreadsheetId, range, new ClearValuesRequest()).execute();
                            service.spreadsheets().values().update(spreadsheetId, range, vr).setValueInputOption("RAW").execute();
                            AutoResizeDimensionsRequest autoResizeDimensions = new AutoResizeDimensionsRequest();
                            Integer newTasksSize = writeData.stream().map(List::size).max(Integer::compareTo).get();
                            DimensionRange dimensions = new DimensionRange().setDimension("COLUMNS").setStartIndex(0).setEndIndex(newTasksSize == null ? 1 : newTasksSize).setSheetId(sheetID);
                            autoResizeDimensions.setDimensions(dimensions);
                            List<Request> requests = new ArrayList<>();
                            requests.add(new Request().setAutoResizeDimensions(autoResizeDimensions));
                            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
                            service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            System.out.println("Results successfully loaded to google spreadsheet!");
            callback.call();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
