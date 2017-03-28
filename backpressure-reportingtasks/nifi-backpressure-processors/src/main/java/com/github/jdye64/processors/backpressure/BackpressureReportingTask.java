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
package com.github.jdye64.processors.backpressure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.status.ConnectionStatus;
import org.apache.nifi.controller.status.ProcessGroupStatus;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.AbstractReportingTask;
import org.apache.nifi.reporting.ReportingContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Tags({"backpressure", "reportingtask"})
@CapabilityDescription("Provide a description")
public class BackpressureReportingTask
        extends AbstractReportingTask {

    private static final PropertyDescriptor BACKPRESSURE_OBJECT_SIZE_THRESHOLD = new PropertyDescriptor.Builder()
            .name("Backpressure Object Size Threshold")
            .description("Number of objects that if the connection contains more objects than this will be reported")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("1000")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private List<PropertyDescriptor> descriptors;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(BACKPRESSURE_OBJECT_SIZE_THRESHOLD);
        return properties;
    }

    public void onTrigger(ReportingContext reportingContext) {
        getLogger().info("Running BackpressureReportingTask");

        List<ConnectionStatus> pressuredConnections = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        Integer bpCount = new Integer(reportingContext.getProperty(BACKPRESSURE_OBJECT_SIZE_THRESHOLD).evaluateAttributeExpressions().getValue());

        ProcessGroupStatus status = reportingContext.getEventAccess().getControllerStatus();
        Iterator<ConnectionStatus> itr = status.getConnectionStatus().iterator();
        while (itr.hasNext()) {
            ConnectionStatus cs = itr.next();
            if (cs.getBackPressureObjectThreshold() > bpCount.intValue()) {
                pressuredConnections.add(cs);
            }
        }

        //Write the pressured connections to some sort of application?? REST, JMS, Log, etc
        try {
            getLogger().info("Pressured Connections: {}", new Object[]{mapper.writeValueAsString(pressuredConnections)});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
