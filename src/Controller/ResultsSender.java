package Controller;

import Model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.sun.istack.internal.NotNull;
import com.sun.xml.internal.ws.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Alexander Baranov on 01.05.2017.
 */
public class ResultsSender implements Runnable {

    private HashMap<String, ArrayList<Result>> results;
    private Callback callback;
    private boolean updateTable;
    private ArrayList<Task> classSystem;

    private boolean deleteDirectory(File file) {
        File[] contents = file.listFiles();
        boolean flag = true;
        if (contents != null) {
            for (File f : contents) {
                if (flag) {
                    flag = deleteDirectory(f);
                }
            }
        }
        return flag && file.delete();
    }

    private ResultsSender(ArrayList<Result> rs, Callback callback, boolean updateTable, @NotNull ArrayList<Task> classSystem) {
        this.results = new HashMap<>();
        if (rs != null) {
            rs.sort((r1, r2) -> r2.getTask().getReceivedDate().compareTo(r1.getTask().getReceivedDate()));
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
                        } else {
                            System.out.println("Result successfully deleted: " + old);
                        }
                        res.add(result);
                    }
                } else {
                    res.add(result);
                }
                results.put(key, res);
            });
        }
        this.callback = callback;
        this.updateTable = updateTable;
        this.classSystem = classSystem;
    }


    ResultsSender(ArrayList<Result> rs, Callback callback, @NotNull ArrayList<Task> classSystem) {
        this(rs, callback, true, classSystem);
    }

    public ResultsSender(Callback callback, @NotNull ArrayList<Task> classSystem) {
        this(null, callback, false, classSystem);
    }

    @Override
    public void run() {
        if (Settings.getInstance().getResultsTableURL().isEmpty()) {
            callback.call();
            throw new RuntimeException("There is no table for results!");
        }
        try {
            this.classSystem.clear();
            Sheets service = GoogleSheetsManager.getService();
            String spreadsheetId = Settings.getInstance().getResultsTableURL();
            final Spreadsheet responseAllSheets = service.spreadsheets().get(spreadsheetId).execute();
            responseAllSheets.getSheets().forEach(sheet -> {
                if (!results.containsKey(sheet.getProperties().getTitle())) {
                    results.put(sheet.getProperties().getTitle(), new ArrayList<>());
                }
            });
            results.forEach((k, v) -> {
                String range = k;
                String subject = k;
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
                        callback.call();
                        Thread.currentThread().interrupt();
                    }
                }
                if (response != null) {
                    try {
                        Sheets.Spreadsheets.Get request = service.spreadsheets().get(spreadsheetId);
                        request.setRanges(Collections.singletonList(range));
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
                                    tasks.clear();
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
                                                if (!result.isEmpty() && v.stream().noneMatch(r -> r.getTask().getName().split("\\.")[0].toLowerCase().equals(tasks.get(index - 1).toLowerCase()) && r.getStudent().getName().equals(name) && r.getGroup().equals(group))) {
                                                    String taskName = tasks.get(index - 1);

                                                    Optional<Path> path = Files.find(Paths.get(Settings.getDataFolder() + "/" + subject.replaceAll(" ", "_") + "/" + group + "/" + name.replaceAll(" ", "_")), Integer.MAX_VALUE,
                                                            (filePath, fileAttr) -> !fileAttr.isDirectory() && filePath.getFileName().toString().split("\\.")[0].toLowerCase().equals(taskName.toLowerCase()))
                                                            .sorted((f1, f2) -> f2.getParent().getFileName().compareTo(f1.getParent().getFileName())).findFirst();
                                                    if (!path.isPresent()) {
                                                        callback.call();
                                                        throw new RuntimeException("There is no file with source code: " + name + ", " + taskName);
                                                    }
                                                    Task downloadedTask = new Task(path.get().getFileName().toString(), subject, path.get().toAbsolutePath().toString(), new SimpleDateFormat(Settings.getSourcesDateFormat(), Locale.ENGLISH).parse(path.get().getParent().getFileName().toString()));
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
                        }
                        Set<String> groups = new HashSet<>();
                        v.forEach(vs -> groups.add(vs.getGroup()));
                        v.sort(Comparator.comparing(v1 -> v1.getStudent().getName()));
                        this.classSystem.addAll(getFileSystem(v));
                        if (updateTable) {
                            List<List<Object>> writeData = new ArrayList<>();
                            groups.stream().sorted(String::compareTo).forEach(group -> {
                                writeData.add(Collections.singletonList(group));
                                final ArrayList<String> newTasks = new ArrayList<>();
                                v.forEach(r -> {
                                    if (r.getGroup().equals(group) && !newTasks.contains(StringUtils.capitalize(r.getTask().getName().split("\\.")[0])))
                                        newTasks.add(StringUtils.capitalize(r.getTask().getName().split("\\.")[0]));
                                });
                                newTasks.sort(String::compareTo);
                                newTasks.add(0, "ФИО");
                                writeData.add(new ArrayList<>(newTasks));
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
                                writeData.add(new ArrayList<>(Collections.singletonList("")));
                            });

                            ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
                            try {
                                service.spreadsheets().values().clear(spreadsheetId, range, new ClearValuesRequest()).execute();
                                service.spreadsheets().values().update(spreadsheetId, range, vr).setValueInputOption("RAW").execute();
                                AutoResizeDimensionsRequest autoResizeDimensions = new AutoResizeDimensionsRequest();
                                Optional<Integer> newTasksSize = writeData.stream().map(List::size).max(Integer::compareTo);
                                DimensionRange dimensions = new DimensionRange().setDimension("COLUMNS").setStartIndex(0).setEndIndex(newTasksSize.orElse(1)).setSheetId(sheetID);
                                autoResizeDimensions.setDimensions(dimensions);
                                List<Request> requests = new ArrayList<>();
                                requests.add(new Request().setAutoResizeDimensions(autoResizeDimensions));
                                BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
                                service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                callback.call();
                                Thread.currentThread().interrupt();
                            }
                        }
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                        callback.call();
                        Thread.currentThread().interrupt();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            callback.call();
            Thread.currentThread().interrupt();
        }
        if (updateTable) System.out.println("Results successfully loaded to Google spreadsheet!");
        this.classSystem.forEach(System.out::println);
        callback.call();
    }

    private ArrayList<Task> getFileSystem(ArrayList<Result> results) {
        ArrayList<Task> fileSystem = new ArrayList<>();
        results.forEach(result -> {
            Task task = new Task(result.getTask().getName(), result.getSubject(), result.getTask().getSourcePath(), result.getTask().getReceivedDate());
            task.setAuthor(new Student(result.getStudent().getName(), result.getStudent().getGroupName()));
            fileSystem.add(task);
        });
        return fileSystem;
    }

}
