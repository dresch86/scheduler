/*
 * Scheduler is a tool for assigning schedules to employees with 
 * constraints.
 * 
 * Copyright (C) 2023  Daniel J. Resch, Ph.D.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.ose.scheduler.controllers;

import java.io.File;
import java.util.Iterator;

import javafx.fxml.FXML;

import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.ose.scheduler.data.TimeBlock;
import org.ose.scheduler.dialogs.AboutDialog;
import org.ose.scheduler.logging.TextStackAppender;

public class ApplicationController {
    @FXML
    private VBox vbLogs;

    @FXML
    private MenuItem miClose;

    @FXML
    private MenuItem miRunAssign;

    @FXML
    private MenuItem miOpenXlsx;

    @FXML
    private MenuItem miCopyLogs;

    @FXML
    private MenuItem miAbout;

    @FXML
    private Button btnRun;

    @FXML
    private ToggleGroup tgMode;

    @FXML
    private Button btnSetInputFile;

    @FXML
    private Button btnSetOutputFile;

    @FXML
    private CheckBox cbQualTally;

    @FXML
    private CheckBox cbMetricSum;

    @FXML
    private CheckBox cbOpenReport;

    @FXML
    private RadioButton rbMultiAssign;

    @FXML
    private RadioButton rbQuickAssign;

    @FXML
    private TextField tfInputFilename;

    @FXML
    private TextField tfOutputFilename;

    @FXML
    private TableView<TimeBlock> tvAssignmentsTable;

    private final Stage stMainStage;

    private final DataController dcDataHandler = new DataController();

    private final Clipboard clbClipboard = Clipboard.getSystemClipboard();

    private final ClipboardContent cbcClipboardContent = new ClipboardContent();

    public ApplicationController(Stage mainStage) {
        stMainStage = mainStage;
    }

    private void processInput() {
        String sErrorTitle = "File Error";

        if (!dcDataHandler.hasInputFileSet()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(sErrorTitle);
            alert.setHeaderText("Missing Input File");
            alert.setContentText("Please specify the input file with qualifications, workforce, availability, and time blocks!");
            alert.showAndWait();
        } else if (!dcDataHandler.hasOutputFileSet()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(sErrorTitle);
            alert.setHeaderText("Missing Output File");
            alert.setContentText("Please specify the report filename for output!");
            alert.showAndWait();
        } else {
            dcDataHandler.parseInput();
            
            if (cbOpenReport.isSelected()) {
                dcDataHandler.openReport(error -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle(sErrorTitle);
                    alert.setHeaderText("Open Report Error");
                    alert.setContentText(error);
                    alert.showAndWait();
                });
            }
        }
    }

    public void showInputFileDialog() {
            FileChooser fcFileChooserDialog = new FileChooser();
            fcFileChooserDialog.setTitle("Select Excel File...");
            fcFileChooserDialog.getExtensionFilters().addAll(
                    new ExtensionFilter("Excel File", "*.xlsx")
            );
            fcFileChooserDialog.setInitialDirectory(new File(System.getProperty("user.home")));

            File fiSelectedFile = fcFileChooserDialog.showOpenDialog(stMainStage);
            if (fiSelectedFile != null) {
                dcDataHandler.setInputFile(fiSelectedFile);
                tfInputFilename.setText(fiSelectedFile.getAbsolutePath());
            }
    }

    public void shutdown() {
        dcDataHandler.cleanup();
    }

    @FXML
    public void initialize() {
        TextStackAppender.setStackBox(vbLogs);
        dcDataHandler.setDisplayTable(tvAssignmentsTable);

        rbMultiAssign.setUserData("MULTI");
        rbQuickAssign.setUserData("QUICK");

        ImageView imvRunIcon = new ImageView(new Image("icons/schedule.png"));
        btnRun.setGraphic(imvRunIcon);

        tgMode.selectedToggleProperty().addListener((obs, previous, current) -> {
            if (current != null) {
                dcDataHandler.setMultiAssignMode(((String) current.getUserData())
                    .equalsIgnoreCase("MULTI"));
            } else {
                dcDataHandler.setMultiAssignMode(false);
            }
        });

        miOpenXlsx.setOnAction(event -> showInputFileDialog());

        miRunAssign.setOnAction(event -> processInput());

        miClose.setOnAction(event -> stMainStage.close());

        miAbout.setOnAction(event -> {
            AboutDialog adAboutDialogInfo = new AboutDialog();
            adAboutDialogInfo.showAndWait();
        });

        miCopyLogs.setOnAction(event -> {
            StringBuilder sbLogs = new StringBuilder();
            Iterator<Node> itTextNodes = vbLogs.getChildren().iterator();

            while (itTextNodes.hasNext()) {
                sbLogs.append(((Text) itTextNodes.next()).getText());
                sbLogs.append("\n");
            }

            cbcClipboardContent.clear();
            cbcClipboardContent.putString(sbLogs.toString().trim());

            clbClipboard.clear();
            clbClipboard.setContent(cbcClipboardContent);
        });

        btnSetInputFile.setOnAction(event -> showInputFileDialog());

        btnSetOutputFile.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save");
            fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Excel File", "*.xlsx"));

            File fReportFile = fileChooser.showSaveDialog(stMainStage);
            if (fReportFile != null) {
                tfOutputFilename.setText(fReportFile.getAbsolutePath());
                dcDataHandler.setOutputFile(fReportFile);
            }
        });

        cbQualTally.selectedProperty().addListener((listener, oldVal, newVal) 
            -> dcDataHandler.setQualTallyReport(newVal.booleanValue()));

        cbMetricSum.selectedProperty().addListener((listener, oldVal, newVal) 
            -> dcDataHandler.setMetricSummaryReport(newVal.booleanValue()));

        btnRun.setOnAction(event -> processInput());
    }
}