/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jdye64.processors.clusterstate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.status.ProcessGroupStatus;
import org.apache.nifi.controller.status.ProcessorStatus;
import org.apache.nifi.controller.status.RunStatus;
import org.apache.nifi.reporting.ReportingContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jdye64.reportingtasks.AbstractDeviceRegistryReportingTask;

@Tags({"invalid", "configuration", "processor"})
@CapabilityDescription("Searches the designated process group for processors that are considered to be in the 'invalid' configuration state")
public class InvalidConfiguredProcessorsReportingTask
        extends AbstractDeviceRegistryReportingTask {

    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> descriptors = super.getSupportedPropertyDescriptors();
        return Collections.unmodifiableList(descriptors);
    }

    @Override
    public void onTrigger(ReportingContext reportingContext) {

        List<ProcessorStatus> invalidProcessors = new ArrayList<>();

        ProcessGroupStatus status = reportingContext.getEventAccess().getControllerStatus();
        Iterator<ProcessorStatus> itr = status.getProcessorStatus().iterator();
        while (itr.hasNext()) {
            ProcessorStatus ps = itr.next();
            if (ps.getRunStatus().compareTo(RunStatus.Invalid) == 0) {
                invalidProcessors.add(ps);
            }
        }

        try {
            getLogger().info("{}", new Object[]{mapper.writeValueAsString(invalidProcessors)});

            if (reportingContext.getProperty(REST_POSTING_ENABLED).asBoolean()) {
                reportToDeviceRegistry(reportingContext, "/processors/invalid", mapper.writeValueAsString(invalidProcessors));
            }

        } catch (JsonProcessingException e) {
            getLogger().error("Error Processing invalid processors JSON: {}", new Object[]{e.getMessage()}, e);
        }

    }

}
