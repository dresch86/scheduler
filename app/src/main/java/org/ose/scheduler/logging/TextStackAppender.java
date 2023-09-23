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
package org.ose.scheduler.logging;

import java.io.Serializable;

import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.application.Platform;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;

@Plugin(
	  name = "TextStackAppender", 
	  category = Core.CATEGORY_NAME, 
	  elementType = Appender.ELEMENT_TYPE)
	public class TextStackAppender extends AbstractAppender {
		private static VBox vbLogStack;
	 
	    protected TextStackAppender(String name, Filter filter,
		Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
			super(name, filter, layout, ignoreExceptions, null);
	    }
	 
	    @PluginFactory
	    public static TextStackAppender createAppender(
          @PluginAttribute("name") String name, 
		  @PluginElement("Filter") final Filter filter,  
		  @PluginElement("Layout") Layout<? extends Serializable> layout) {
	        return new TextStackAppender(name, filter, layout, false);
	    }
	 
		public static void setStackBox(VBox stackBox) {
			vbLogStack = stackBox;
		}

	    @Override
	    public void append(LogEvent event) {
			if (vbLogStack != null) {
				Text txtLogEntry = new Text(event.getMessage().getFormattedMessage());
				
				switch (event.getLevel().name()) {
					case "FATAL":
					txtLogEntry.setFill(Color.CRIMSON);
					break;
					case "ERROR":
					txtLogEntry.setFill(Color.RED);
					break;
					case "WARN":
					txtLogEntry.setFill(Color.ORANGE);
					break;
					case "INFO":
					txtLogEntry.setFill(Color.BLUE);
					break;
					default:
					txtLogEntry.setFill(Color.BLACK);
					return;
				}

				Platform.runLater(() -> {
					txtLogEntry.wrappingWidthProperty().bind(vbLogStack.widthProperty());
					vbLogStack.getChildren().add(txtLogEntry);
				});
			}
	    }
	}