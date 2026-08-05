package edu.uchc.cam.langevin.langevinnovis01;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;

public class LoggingInit {

    public static void configureLogging() {

        // 1. Get the active context (may already be initialized)
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        // 2. Stop any existing configuration (JUnit, SIF, CLI, defaults)
        ctx.stop();

        // 3. Get the now-reset configuration object
        Configuration config = ctx.getConfiguration();

        // 4. Build a simple layout (stable across all Log4j2 versions)
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%msg%n")
                .withConfiguration(config)
                .build();

        // 5. Build a console appender (stable API)
        ConsoleAppender console = ConsoleAppender.newBuilder()
                .setName("Console")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .setConfiguration(config)
                .build();
        console.start();

        // 6. Register the appender with the configuration
        config.addAppender(console);

        // 7. Attach the appender to the root logger at INFO level
        config.getRootLogger().addAppender(console, Level.INFO, null);

        // 8. Apply updated configuration
        ctx.updateLoggers();
    }
}
