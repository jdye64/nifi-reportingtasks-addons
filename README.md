# nifi-reportingtasks-addons

##BackpressureReportingTask

The ```BackpressureReportingTask``` examines all of the connections in the NiFi workflow. If any of the connections have a depth over the configured
value a log message in generated in the format like

```
2017-03-30 10:38:59,786 BackpressureReportingTask[id=162df652-015b-1000-4244-1ca39eaabd8f] [{"id":"162e5cb2-015b-1000-9adb-f6bcf89fa4cc","groupId":"de471b9d-015a-1000-20a1-a52be4c0a1c5","name":"success","sourceId":"162e3763-015b-1000-1311-0d15add76c37","sourceName":"GenerateFlowFile","destinationId":"162e4916-015b-1000-58fd-8a9755e6c62f","destinationName":"LogAttribute","backPressureDataSizeThreshold":"1 GB","backPressureBytesThreshold":1073741824,"backPressureObjectThreshold":10000,"inputCount":0,"inputBytes":0,"queuedCount":0,"queuedBytes":0,"outputCount":0,"outputBytes":0,"maxQueuedCount":0,"maxQueuedBytes":0}]
```

### Logging Configuration
Configuring the logging configuration for the BackpressureReportingTask can be done by editing $NIFI_HOME/conf/logback.xml and placing something
like the following at the end of the file.

```
    <appender name="BACKPRESSURE_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${org.apache.nifi.bootstrap.config.log.dir}/backpressured-connections.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${org.apache.nifi.bootstrap.config.log.dir}/backpressured-connections_%d.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%date %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.github.jdye64.processors.backpressure.BackpressureReportingTask" level="INFO" additivity="false">
        <appender-ref ref="BACKPRESSURE_FILE"/>
    </logger>

    <appender name="DISABLED_PROCESSORS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${org.apache.nifi.bootstrap.config.log.dir}/disabled-processors.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${org.apache.nifi.bootstrap.config.log.dir}/disabled-processors_%d.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%date %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.github.jdye64.processors.clusterstate.processor.DisabledProcessorsReportingTask" level="INFO" additivity="false">
        <appender-ref ref="DISABLED_PROCESSORS_FILE"/>
    </logger>

    <appender name="STOPPED_PROCESSORS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${org.apache.nifi.bootstrap.config.log.dir}/stopped-processors.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${org.apache.nifi.bootstrap.config.log.dir}/stopped-processors_%d.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%date %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.github.jdye64.processors.clusterstate.processor.StoppedProcessorsReportingTask" level="INFO" additivity="false">
        <appender-ref ref="STOPPED_PROCESSORS_FILE"/>
    </logger>

    <appender name="INVALID_PROCESSORS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${org.apache.nifi.bootstrap.config.log.dir}/invalid-processors.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${org.apache.nifi.bootstrap.config.log.dir}/invalid-processors_%d.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%date %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.github.jdye64.processors.clusterstate.processor.InvalidProcessorsReportingTask" level="INFO" additivity="false">
        <appender-ref ref="INVALID_PROCESSORS_FILE"/>
    </logger>
```