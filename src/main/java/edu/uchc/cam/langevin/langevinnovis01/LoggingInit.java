package edu.uchc.cam.langevin.langevinnovis01;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class LoggingInit {
    public static void configureLogging() {
        ConfigurationBuilder<BuiltConfiguration> builder =
                ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setStatusLevel(org.apache.logging.log4j.Level.ERROR);
        builder.setConfigurationName("CustomConfig");

        // Console appender with correct Log4j2 pattern
        LayoutComponentBuilder layout = builder.newLayout("PatternLayout")
//                .addAttribute("pattern", "%d{ISO8601} %-5level %logger - %msg%n");
                .addAttribute("pattern", "%msg%n");

        AppenderComponentBuilder console = builder.newAppender("Console", "CONSOLE")
                .add(layout);

        builder.add(console);

        // Root logger
        builder.add(builder.newRootLogger(org.apache.logging.log4j.Level.INFO)
                .add(builder.newAppenderRef("Console")));

        Configurator.initialize(builder.build());
    }
}
