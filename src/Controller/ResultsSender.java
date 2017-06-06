package Controller;

import Model.GlobalSettings;
import Model.LocalSettings;
import Model.Result;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResultsSender implements Runnable {

    @Override
    public void run() {
        if (LocalSettings.getInstance().getResults().size() == 0) return;
        System.out.println("Running thread for results sender...");
        try {
            final Sheets service = GoogleSheetsManager.getService();
            String spreadsheetId;
            Pattern pattern = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
            Matcher matcher = pattern.matcher(GlobalSettings.getInstance().getResultsTableURL());
            if (matcher.find()) {
                spreadsheetId = matcher.group().replace("/spreadsheets/d/", "");
                HashMap<String, ArrayList<String>> taskNumbersForSubjects = GoogleDriveManager.getTaskNumbersForSubjects();
                final CellFormat centerFormat = new CellFormat().setHorizontalAlignment("CENTER");
                final CellFormat centerBoldFormat = new CellFormat().setHorizontalAlignment("CENTER").setTextFormat(new TextFormat().setBold(true));

                Spreadsheet responseAllSheets = service.spreadsheets().get(spreadsheetId).execute();
                for (int i = 1; i < responseAllSheets.getSheets().size(); i++) {
                    Sheet sheet = responseAllSheets.getSheets().get(i);
                    BatchUpdateSpreadsheetRequest deleteSheetBody = new BatchUpdateSpreadsheetRequest().setRequests(
                            Collections.singletonList(new Request().setDeleteSheet(new DeleteSheetRequest().setSheetId(sheet.getProperties().getSheetId())))
                    );
                    try {
                        service.spreadsheets().batchUpdate(spreadsheetId, deleteSheetBody).execute();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }

                List<Request> renameRequests = new ArrayList<>();
                renameRequests.add(new Request()
                        .setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                                .setProperties(new SheetProperties()
                                        .setTitle(" ").setSheetId(responseAllSheets.getSheets().get(0).getProperties().getSheetId()))
                                .setFields("title")));

                service.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(renameRequests)).execute();

                // grouping results by subjects
                LocalSettings.getInstance().getResults().stream().collect(Collectors.groupingBy(Result::getSubject)).forEach((subject, subjectResults) -> {
                    // get or create sheet for subject
                    BatchUpdateSpreadsheetRequest addSheetBody = new BatchUpdateSpreadsheetRequest().setRequests(
                            Collections.singletonList(new Request().setAddSheet(new AddSheetRequest().setProperties(
                                    new SheetProperties().setTitle(subject)))));
                    try {
                        service.spreadsheets().batchUpdate(spreadsheetId, addSheetBody).execute();
                        service.spreadsheets().values().get(spreadsheetId, subject).execute();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    Sheets.Spreadsheets.Get requestSheetId;
                    try {
                        requestSheetId = service.spreadsheets().get(spreadsheetId);
                        requestSheetId.setRanges(Collections.singletonList(subject));
                        Spreadsheet responseID = requestSheetId.execute();
                        Integer sheetID = responseID.getSheets().get(0).getProperties().getSheetId();

                        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(Collections.singletonList(new Request().setUpdateCells(new UpdateCellsRequest()
                                .setRange(new GridRange().setSheetId(sheetID))
                                .setFields("*")
                        )));
                        service.spreadsheets().batchUpdate(spreadsheetId, body).execute();


                        List<List<Object>> writeData = new ArrayList<>();
                        final ArrayList<Request> requests = new ArrayList<>();
                        AtomicInteger lineIndex = new AtomicInteger(0);

                        // grouping subject results by groups
                        subjectResults.stream().collect(Collectors.groupingBy(Result::getGroup)).forEach((group, groupResults) -> {
                            ArrayList<String> taskNumbersForSubject = taskNumbersForSubjects.get(subject);
                            ArrayList<String> groupAndTasks = new ArrayList<>();
                            groupAndTasks.add("Group " + group);
                            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                                    .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(lineIndex.get()).setEndRowIndex(lineIndex.get() + 2))
                                    .setCell(new CellData().setUserEnteredFormat(centerBoldFormat))
                                    .setFields("userEnteredFormat(textFormat,horizontalAlignment)")));
                            Border borderDotted = new Border().setStyle("DOUBLE").setColor(new Color().setRed(0f).setGreen(0f).setBlue(0f));
                            requests.add(new Request().setUpdateBorders(new UpdateBordersRequest()
                                    .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(lineIndex.get()).setEndRowIndex(lineIndex.get() + 1).setStartColumnIndex(0).setEndColumnIndex(taskNumbersForSubjects.get(subject).size() + 1))
                                    .setTop(borderDotted).setLeft(borderDotted).setBottom(borderDotted).setRight(borderDotted).setInnerHorizontal(borderDotted).setInnerVertical(borderDotted)));
                            Map<String, List<String>> groupedTaskNumbers = taskNumbersForSubject.stream().collect(Collectors.groupingBy(s -> s.split("\\.")[0]));
                            final AtomicInteger taskNumCounter = new AtomicInteger();
                            new ArrayList<>(groupedTaskNumbers.keySet()).stream().sorted(String::compareTo).forEach(taskNum -> {
                                Integer taskCount = groupedTaskNumbers.get(taskNum).size();
                                groupAndTasks.add(taskNum);
                                for (int i = 0; i < taskCount - 1; i++) groupAndTasks.add("");
                                requests.add(new Request().setMergeCells(
                                        new MergeCellsRequest().setMergeType("MERGE_ALL").setRange(
                                                new GridRange().setSheetId(sheetID).setStartRowIndex(lineIndex.get()).setEndRowIndex(lineIndex.get() + 1)
                                                        .setStartColumnIndex(1 + taskNumCounter.get()).setEndColumnIndex(1 + taskNumCounter.get() + taskCount))));
                                taskNumCounter.addAndGet(taskCount);
                            });
                            writeData.add(new ArrayList<>(groupAndTasks));
                            final ArrayList<String> taskNumbers = new ArrayList<>();
                            taskNumbers.addAll(taskNumbersForSubject);
                            taskNumbers.add(0, "Full Name");
                            writeData.add(new ArrayList<>(taskNumbers));
                            Integer currentIndex = lineIndex.get() + 2;
                            lineIndex.set(groupResults.stream().map(result -> result.getStudent().getName()).collect(Collectors.toSet()).size() + currentIndex);
                            HashMap<String, ArrayList<Object>> studentsResults = new HashMap<>();
                            groupResults.forEach(result -> {
                                String studName = result.getStudent().getName();
                                ArrayList<Object> studResults = studentsResults.get(studName);
                                if (studResults == null) {
                                    studResults = new ArrayList<>();
                                    studResults.add(studName);
                                    for (int i = 0; i < taskNumbers.size() - 1; i++) studResults.add("");
                                }
                                studentsResults.put(studName, studResults);
                            });
                            final AtomicInteger studentsRowsCounter = new AtomicInteger(currentIndex);
                            studentsResults.keySet().stream().sorted(String::compareTo).forEach(key -> {
                                ArrayList<Object> studResults = studentsResults.get(key);
                                List<Result> results = groupResults.stream().filter(result1 -> result1.getStudent().getName().equals(key)).collect(Collectors.toList());
                                results.forEach(result -> {
                                    int columnNum = taskNumbers.indexOf(result.getTask().getTaskCode());
                                    studResults.set(columnNum, result.getMessage());
                                    if (result.getTask().getReceivedDate().compareTo(result.getTask().getDeadline()) > 0) {
                                        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                                                .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(studentsRowsCounter.get()).setEndRowIndex(studentsRowsCounter.get() + 1).setStartColumnIndex(columnNum).setEndColumnIndex(columnNum + 1))
                                                .setCell(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(new Color().setRed(1.0f).setGreen(result.getTask().hasHardDeadline() ? 0 : 1.0f))))
                                                .setFields("userEnteredFormat(backgroundColor)")));
                                    }
                                });
                                studentsRowsCounter.incrementAndGet();
                            });
                            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                                    .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(currentIndex).setEndRowIndex(lineIndex.get()).setStartColumnIndex(1))
                                    .setCell(new CellData().setUserEnteredFormat(centerFormat))
                                    .setFields("userEnteredFormat(horizontalAlignment)")));
                            Border border = new Border().setStyle("SOLID").setWidth(1).setColor(new Color().setRed(0f).setGreen(0f).setBlue(0f));
                            requests.add(new Request().setUpdateBorders(new UpdateBordersRequest()
                                    .setRange(new GridRange().setSheetId(sheetID).setStartRowIndex(currentIndex - 1).setEndRowIndex(lineIndex.get()).setStartColumnIndex(0).setEndColumnIndex(taskNumbersForSubjects.get(subject).size() + 1))
                                    .setLeft(borderDotted).setRight(border).setBottom(border).setInnerHorizontal(border).setInnerVertical(border)));
                            lineIndex.incrementAndGet();
                            studentsResults.keySet().stream().sorted(String::compareTo).forEach(key -> writeData.add(studentsResults.get(key)));
                            writeData.add(new ArrayList<>(Collections.singletonList("")));
                        });
                        ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
                        try {
                            service.spreadsheets().values().clear(spreadsheetId, subject, new ClearValuesRequest()).execute();
                            service.spreadsheets().values().update(spreadsheetId, subject, vr).setValueInputOption("RAW").execute();
                            AutoResizeDimensionsRequest autoResizeDimensions = new AutoResizeDimensionsRequest();
                            Optional<Integer> newTasksSize = writeData.stream().map(List::size).max(Integer::compareTo);
                            DimensionRange dimensions = new DimensionRange().setDimension("COLUMNS").setStartIndex(0).setEndIndex(newTasksSize.orElse(1)).setSheetId(sheetID);
                            autoResizeDimensions.setDimensions(dimensions);
                            requests.add(new Request().setAutoResizeDimensions(autoResizeDimensions));
                            BatchUpdateSpreadsheetRequest bodyR = new BatchUpdateSpreadsheetRequest().setRequests(requests);
                            service.spreadsheets().batchUpdate(spreadsheetId, bodyR).execute();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                throw new IllegalArgumentException("Wrong table url format!");
            }

            // Delete all redundant sheets
            Spreadsheet responseAllSheets = service.spreadsheets().get(spreadsheetId).execute();
            responseAllSheets.getSheets().forEach(sheet -> {
                if (LocalSettings.getInstance().getResults().stream().noneMatch(result -> result.getSubject().equals(sheet.getProperties().getTitle()))) {
                    BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(
                            Collections.singletonList(new Request().setDeleteSheet(new DeleteSheetRequest().setSheetId(sheet.getProperties().getSheetId())))
                    );
                    try {
                        service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            });

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

}