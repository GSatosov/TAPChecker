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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Alexander Baranov on 01.05.2017.
 */
public class ResultsSender implements Runnable {

    private HashMap<String, ArrayList<Result>> results;
    private Callback onExit;
    private Callback onClassSystemReady;
    private boolean updateTable;
    private List<Result> classSystem;

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

    private ResultsSender(ArrayList<Result> rs, Callback onExit, boolean updateTable, @NotNull List<Result> classSystem, Callback onClassSystemReady) {
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
        this.onExit = onExit;
        this.updateTable = updateTable;
        this.classSystem = classSystem;
        this.onClassSystemReady = onClassSystemReady;
    }


    ResultsSender(ArrayList<Result> rs, Callback onExit, @NotNull List<Result> classSystem, Callback onClassSystemReady) {
        this(rs, onExit, true, classSystem, onClassSystemReady);
    }

    public ResultsSender(Callback onExit, @NotNull List<Result> classSystem, Callback onClassSystemReady) {
        this(null, onExit, false, classSystem, onClassSystemReady);
    }

    @Override
    public void run() {
        if (Settings.getInstance().getResultsTableURL().isEmpty()) {
            onExit.call();
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
                        onExit.call();
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
                                    final String group = row.get(0).toString().replaceAll("Группа ", "");
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

                                                    Optional<Path> path = Files.find(Paths.get(Settings.getDataFolder() + "/" + Transliteration.cyr2lat(subject.replaceAll(" ", "_")) + "/" + Transliteration.cyr2lat(group) + "/" + Transliteration.cyr2lat(name.replaceAll(" ", "_"))), Integer.MAX_VALUE,
                                                            (filePath, fileAttr) -> !fileAttr.isDirectory() && filePath.getFileName().toString().split("\\.")[0].toLowerCase().equals(taskName.toLowerCase()))
                                                            .sorted((f1, f2) -> f2.getParent().getFileName().compareTo(f1.getParent().getFileName())).findFirst();
                                                    if (!path.isPresent()) {
                                                        onExit.call();
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
                            final ArrayList<Request> requests = new ArrayList<>();
                            AtomicInteger lineIndex = new AtomicInteger(0);
                            final CellFormat centerFormat = new CellFormat().setHorizontalAlignment("CENTER");
                            final CellFormat centerBoldFormat = new CellFormat().setHorizontalAlignment("CENTER").setTextFormat(new TextFormat().setBold(true));
                            final Integer maxStudentsInGroup = groups.stream().map(group -> v.stream().filter(result -> result.getGroup().equals(group)).map(result -> result.getStudent().getName()).collect(Collectors.toSet()).size()).max(Integer::compareTo).get();
                            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(Collections.singletonList(new Request().setUpdateCells(new UpdateCellsRequest()
                                    .setRange(new GridRange().setSheetId(sheetID))
                                    .setFields("*")
                            )));
                            service.spreadsheets().batchUpdate(spreadsheetId, body).execute();

                            groups.stream().sorted(String::compareTo).forEach(group -> {
                                writeData.add(Collections.singletonList("Группа " + group));
                                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                                        .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(lineIndex.get()).setEndRowIndex(lineIndex.get() + 2))
                                        .setCell(new CellData().setUserEnteredFormat(centerBoldFormat))
                                        .setFields("userEnteredFormat(textFormat,horizontalAlignment)")));
                                Border borderDotted = new Border().setStyle("DOUBLE").setColor(new Color().setRed(0f).setGreen(0f).setBlue(0f));
                                requests.add(new Request().setUpdateBorders(new UpdateBordersRequest()
                                        .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(lineIndex.get()).setEndRowIndex(lineIndex.get() + 1).setStartColumnIndex(0).setEndColumnIndex(maxStudentsInGroup + 1))
                                        .setTop(borderDotted).setLeft(borderDotted).setBottom(borderDotted).setRight(borderDotted).setInnerHorizontal(borderDotted).setInnerVertical(borderDotted)));
                                final ArrayList<String> newTasks = new ArrayList<>();
                                v.forEach(r -> {
                                    if (r.getGroup().equals(group) && !newTasks.contains(StringUtils.capitalize(r.getTask().getName().split("\\.")[0])))
                                        newTasks.add(StringUtils.capitalize(r.getTask().getName().split("\\.")[0]));
                                });
                                newTasks.sort(String::compareTo);
                                newTasks.add(0, "ФИО");
                                writeData.add(new ArrayList<>(newTasks));
                                Integer currentIndex = lineIndex.get() + 2;
                                lineIndex.set(v.stream().filter(result -> result.getGroup().equals(group)).map(result -> result.getStudent().getName()).collect(Collectors.toSet()).size() + currentIndex);
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
                                        studResults.set(newTasks.indexOf(StringUtils.capitalize(result.getTask().getName().split("\\.")[0])), result.getMessage());
                                        studentsResults.put(studName, studResults);
                                    }
                                });
                                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                                        .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(currentIndex).setEndRowIndex(lineIndex.get()).setStartColumnIndex(1))
                                        .setCell(new CellData().setUserEnteredFormat(centerFormat))
                                        .setFields("userEnteredFormat(horizontalAlignment)")));
                                Border border = new Border().setStyle("SOLID").setWidth(1).setColor(new Color().setRed(0f).setGreen(0f).setBlue(0f));
                                requests.add(new Request().setUpdateBorders(new UpdateBordersRequest()
                                        .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(currentIndex - 1).setEndRowIndex(lineIndex.get()).setStartColumnIndex(0).setEndColumnIndex(maxStudentsInGroup + 1))
                                        .setLeft(borderDotted).setRight(border).setBottom(border).setInnerHorizontal(border).setInnerVertical(border)));
                                lineIndex.incrementAndGet();
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
                                requests.add(new Request().setAutoResizeDimensions(autoResizeDimensions));
                                BatchUpdateSpreadsheetRequest bodyR = new BatchUpdateSpreadsheetRequest().setRequests(requests);
                                service.spreadsheets().batchUpdate(spreadsheetId, bodyR).execute();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                onExit.call();
                                Thread.currentThread().interrupt();
                            }
                        }
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                        onExit.call();
                        Thread.currentThread().interrupt();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            onExit.call();
            Thread.currentThread().interrupt();
        }
        if (updateTable) System.out.println("Results successfully loaded to Google spreadsheet!");
        onExit.call();
        onClassSystemReady.call();
    }

    private List<Result> getFileSystem(ArrayList<Result> results) {
        List<Result> fileSystem = new ArrayList<>();
        results.forEach(result -> {
            Task task = new Task(result.getTask().getName(), result.getSubject(), result.getTask().getSourcePath(), result.getTask().getReceivedDate());
            task.setAuthor(new Student(result.getStudent().getName(), result.getStudent().getGroupName()));
            fileSystem.add(new Result(result.getMessage(), task));
        });
        return fileSystem;
    }

}
