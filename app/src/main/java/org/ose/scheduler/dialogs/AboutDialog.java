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
package org.ose.scheduler.dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javafx.stage.Modality;
import javafx.stage.StageStyle;

import javafx.application.Platform;

import java.net.URI;
import java.net.URISyntaxException;

import java.awt.Desktop;

import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;

import javafx.scene.Cursor;
import javafx.scene.text.Text;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.scene.control.Label;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ButtonType;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class AboutDialog extends Dialog<Void> {
    private Hyperlink hlRepoLink;

    private VBox vbMainContainer = new VBox();
    private String sRepositoryURL = "https://github.com/dresch86/scheduler";
    private static final Logger lMainLogger = LogManager.getLogger(AboutDialog.class.getName());

    public AboutDialog() {
        setTitle("About");
        setHeaderText("About Scheduler");
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        hlRepoLink = new Hyperlink(sRepositoryURL);
        hlRepoLink.setOnAction(event -> {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI(sRepositoryURL));
                } catch (IOException ioe) {
                    lMainLogger.error("Error loading repository");
                } catch (URISyntaxException urse) {
                    lMainLogger.error("Error parsing repository URL");
                }
            } else {
                final ClipboardContent cbcContentHandler = new ClipboardContent();
                cbcContentHandler.putString(sRepositoryURL);

                final Clipboard cbSystemClipboard = Clipboard.getSystemClipboard();
                cbSystemClipboard.setContent(cbcContentHandler);

                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Link Copied");
                alert.setHeaderText(null);
                alert.setContentText("The URL was copied to the system clipboard!");
                alert.showAndWait();
            }
        });

        addProgramInfo();
        addLicenseText();

        vbMainContainer.setSpacing(5);
        getDialogPane().setContent(vbMainContainer);
    }

    private void addProgramInfo() {
        GridPane gpProgramInfoBox = new GridPane();
        gpProgramInfoBox.setHgap(5);
        gpProgramInfoBox.setVgap(5);

        Label lProgram = new Label("Program:");
        Label lDevelopers = new Label("Developer(s):");
        Label lRepository = new Label("Repository:");

        gpProgramInfoBox.add(lProgram, 0, 0);
        gpProgramInfoBox.add(lDevelopers, 0, 1);
        gpProgramInfoBox.add(lRepository, 0, 2);

        gpProgramInfoBox.add(new Text("Scheduler"), 1, 0);
        gpProgramInfoBox.add(new Text("Daniel J. Resch, Ph.D."), 1, 1);
        gpProgramInfoBox.add(hlRepoLink, 1, 2);

        vbMainContainer.getChildren().add(gpProgramInfoBox);
    }

    private void addLicenseText() {
        TextArea taLicense = new TextArea();
        taLicense.setId("taLicense");
        taLicense.setWrapText(true);
        taLicense.setEditable(false);
        taLicense.setCursor(Cursor.DEFAULT);
        taLicense.setFocusTraversable(false);

        try {
            InputStream isLicenseFile = getClass().getClassLoader().getResourceAsStream("license.txt");
            taLicense.setText(IOUtils.toString(isLicenseFile, StandardCharsets.UTF_8.name()));
        } catch (IOException ioe) {
            taLicense.setText("Error loading license file");
            lMainLogger.error("Error loading license file");
        }

        vbMainContainer.getChildren().add(taLicense);
        setOnShown(event -> {
            Platform.runLater(() -> {
                taLicense.getScene().lookup("#taLicense .content").setCursor(Cursor.DEFAULT);
            });
        });
    }
}