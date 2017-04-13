package com.github.jdye64.processors.sla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 4/7/17.
 */

@Tags({"start", "SLA"})
@CapabilityDescription("When invoked will trigger the start of an SLA measurement. That measurement will be persisted in the SLAControllerService" +
        " and remain in the 'RUNNING' state until the StopSLAProcessor is called")
@SeeAlso({StopSLAProcessor.class})
public class StartSLAProcessor
        extends AbstractProcessor {

    static final PropertyDescriptor CACHE_SERVER = new PropertyDescriptor.Builder()
            .name("SLA Cache Server")
            .description("Specifies the DistributedMapCacheClient that will be used in conjunction with a DistributedMapCacheServer to" +
                    " keep track of system SLAs")
            .identifiesControllerService(DistributedMapCacheClient.class)
            .required(true)
            .build();

    static final PropertyDescriptor SLA_KEY = new PropertyDescriptor.Builder()
            .name("SLA Key")
            .description("Designates the 'key' that should be used to identify this flowfile for a SLA. You may use any value or expression language technique" +
                    " you please to do this. Please ensure that you key is of the proper granularity to ensure that SLAs are properly calculated. For example" +
                    " if you want to mark an SLA for every flowfile using '${uuid}' which will use the NiFi frameworks flowfile id as the key.")
            .expressionLanguageSupported(true)
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("successfully started SLA window")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failed to start SLA window")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(CACHE_SERVER);
        descriptors.add(SLA_KEY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        final DistributedMapCacheClient cacheServer = context.getProperty(CACHE_SERVER).asControllerService(DistributedMapCacheClient.class);
        String key = context.getProperty(SLA_KEY).evaluateAttributeExpressions(flowFile).getValue();

        //Place the SLA start entry in the cacheserver
        //cacheServer.putIfAbsent()


//        try {
//            StringBuffer strBuffer = new StringBuffer();
//            strBuffer.append(flowFile.getAttribute("absolute.path"));
//            strBuffer.append(flowFile.getAttribute("filename"));
//
//            getLogger().debug("Deleting File: " + strBuffer.toString());
//
//            File f = new File(strBuffer.toString());
//            if (f.delete()) {
//                session.transfer(flowFile, REL_SUCCESS);
//            } else {
//                session.transfer(flowFile, REL_FAILURE);
//            }
//
//        } catch (Exception ex) {
//            getLogger().error(ex.getMessage());
//            session.transfer(flowFile, REL_FAILURE);
//        }
    }
}
