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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
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

    private static final PropertyDescriptor REST_POSTING_ENABLED = new PropertyDescriptor.Builder()
            .name("POST pressured connections to NiFi Device Registry")
            .description("If true the JSON payload for the pressured connections will be POSTed to the NiFi Device Registry UI")
            .required(true)
            .defaultValue("false")
            .allowableValues("true", "false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final PropertyDescriptor DEVICE_REGISTRY_HOST = new PropertyDescriptor.Builder()
            .name("Host")
            .description("NiFi Device Registry service that the metrics will be transported to")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("localhost")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final PropertyDescriptor DEVICE_REGISTRY_PORT = new PropertyDescriptor.Builder()
            .name("Port")
            .description("Port the target NiFi Device Registry is running on")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("8888")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(BACKPRESSURE_OBJECT_SIZE_THRESHOLD);
        properties.add(REST_POSTING_ENABLED);
        properties.add(DEVICE_REGISTRY_HOST);
        properties.add(DEVICE_REGISTRY_PORT);
        return properties;
    }

    public void onTrigger(ReportingContext reportingContext) {

        List<ConnectionStatus> pressuredConnections = new ArrayList<>();

        String host = reportingContext.getProperty(DEVICE_REGISTRY_HOST).evaluateAttributeExpressions().getValue();
        String port = reportingContext.getProperty(DEVICE_REGISTRY_PORT).evaluateAttributeExpressions().getValue();

        Integer bpCount = new Integer(reportingContext.getProperty(BACKPRESSURE_OBJECT_SIZE_THRESHOLD).evaluateAttributeExpressions().getValue());

        ProcessGroupStatus status = reportingContext.getEventAccess().getControllerStatus();
        Iterator<ConnectionStatus> itr = status.getConnectionStatus().iterator();
        while (itr.hasNext()) {
            ConnectionStatus cs = itr.next();
            if (cs.getBackPressureObjectThreshold() > bpCount.intValue()) {
                pressuredConnections.add(cs);
            }
        }

        try {
            getLogger().info("{}", new Object[]{mapper.writeValueAsString(pressuredConnections)});

            if (reportingContext.getProperty(REST_POSTING_ENABLED).asBoolean()) {
                try {
                    String url = "http://" + host + ":" + port + "/connection/pressured";

                    HttpClient httpClient = HttpClientBuilder.create().build();
                    HttpPost postRequest = new HttpPost(url);

                    StringEntity input = new StringEntity(this.mapper.writeValueAsString(pressuredConnections));
                    input.setContentType("application/json");
                    postRequest.setEntity(input);

                    HttpResponse response = httpClient.execute(postRequest);

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader((response.getEntity().getContent())));

                    String output;
                    while ((output = br.readLine()) != null) {
                        if (getLogger().isDebugEnabled()){
                            getLogger().debug("NiFi Device Registry Response: {}", new Object[]{output});
                        }
                    }

                    //Closes the BufferedReader
                    br.close();

                } catch (Exception ex) {
                    getLogger().error("Error POSTing Workflow pressured connections to NiFi Device Registry {}:{}", new Object[]{host, port}, ex);
                }
            }

        } catch (JsonProcessingException e) {
            getLogger().error("Error Processing pressured connections JSON: {}", new Object[]{e.getMessage()}, e);
        }
    }

}
