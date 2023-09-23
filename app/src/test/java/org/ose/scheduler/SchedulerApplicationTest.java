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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerApplicationTest {
    @Test void appHasAGreeting() {
        SchedulerApplication classUnderTest = new SchedulerApplication();
        //assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
    }
}
