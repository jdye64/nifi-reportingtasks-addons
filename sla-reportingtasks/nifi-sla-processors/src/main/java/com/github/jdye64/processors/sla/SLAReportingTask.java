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
package com.github.jdye64.processors.sla;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.status.ConnectionStatus;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceEventRepository;
import org.apache.nifi.reporting.AbstractReportingTask;
import org.apache.nifi.reporting.ReportingContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Tags({"sla", "reportingtasks"})
@CapabilityDescription("Checks to ensure that SLAs have been met for a particular granularity in the NiFi workflow. If the " +
        "ProcessGroupID is specified on that particular ProcessGroup is checked for meeting SLAs. The SLAs are met at a Flowfile" +
        " level. The SLA is determined by checking the point when a particular FlowFile enters the specified ProcessGroup or " +
        "the root process group and measures the unit of time until it exists the specified ProcessGroup or root ProcessGroup.")
public class SLAReportingTask
        extends AbstractReportingTask {

    private static final PropertyDescriptor PROCESS_GROUP_ID = new PropertyDescriptor.Builder()
            .name("Process Group ID to measure SLA")
            .description("Process Group that should be checked to determine if the SLA has been met or not")
            .required(false)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    private static final PropertyDescriptor SLA_MILLIS = new PropertyDescriptor.Builder()
            .name("SLA Milliseconds")
            .description("SLA for the specified ProcessGroup context in milliseconds")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("1000")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();


    private Long lastLargestProvEventId = new Long(0);

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(PROCESS_GROUP_ID);
        properties.add(SLA_MILLIS);
        return properties;
    }

    public void onTrigger(ReportingContext reportingContext) {
        getLogger().info("Running SLAReportingTask");

        List<ConnectionStatus> pressuredConnections = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        String processGroupId = reportingContext.getProperty(PROCESS_GROUP_ID).evaluateAttributeExpressions().getValue();
        Integer slaMillis = new Integer(reportingContext.getProperty(SLA_MILLIS).evaluateAttributeExpressions().getValue());

        ProvenanceEventRepository provRepo = reportingContext.getEventAccess().getProvenanceRepository();



        try {
            List<ProvenanceEventRecord> events = provRepo.getEvents(lastLargestProvEventId, 100);
            getLogger().info("Found {} provenance events in {} to {} event range",
                    new Object[]{
                            events.size(), lastLargestProvEventId, (lastLargestProvEventId + 100)
                    });

            for (ProvenanceEventRecord event : events) {
                getLogger().info("Prov Event: {}", new Object[]{mapper.writeValueAsString(event)});
            }

        } catch (IOException e) {
            getLogger().error("Error retrieving provenance events", e);
        }
    }

}
