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
package org.ose.scheduler;

import java.util.Optional;
import java.io.IOException;

import javafx.fxml.FXMLLoader;

import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.application.Platform;
import javafx.application.Application;

import org.ose.scheduler.controllers.ApplicationController;

public class SchedulerApplication extends Application {    
    @Override
    public void start(Stage stage) {
        try {
            final ApplicationController acAppHandler = new ApplicationController(stage);
            FXMLLoader fxmlResource = new FXMLLoader(getClass().getResource("/fxml/maingui.fxml"));
            fxmlResource.setController(acAppHandler);

            Parent root = fxmlResource.load();

            stage.setOnCloseRequest(closeEvent -> {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Quit");
                alert.setHeaderText("Close Application");
                alert.setContentText("Are you sure you want to quit?");
                Optional<ButtonType> opQuitResult = alert.showAndWait();

                if (opQuitResult.isPresent() && (opQuitResult.get() == ButtonType.OK)) {
                    acAppHandler.shutdown();
                } else {
                    closeEvent.consume();
                }
            });

            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Scheduler");
            stage.setScene(new Scene(root));
            stage.setX(10.0);
            stage.setY(10.0);
            stage.show();
        } catch (IOException ioe) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Missing Application Resource");
            alert.setContentText("Failed to load the main interface!");
            alert.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
