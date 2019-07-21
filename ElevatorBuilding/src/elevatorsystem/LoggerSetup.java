package elevatorsystem;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

class LoggerSetup {
    static void setUpLogger(Logger LOGGER, Level level) {
        Handler fileHandler;
        try {
            fileHandler = new FileHandler("./logs/logfile.log");
            SimpleFormatter simple = new SimpleFormatter();
            fileHandler.setFormatter(simple);

            LOGGER.addHandler(fileHandler);
        }
        catch (Exception exception ) {  }
        LOGGER.setLevel(level);
    }
}
