package net.unfamily.iskautils.util;

import net.unfamily.iskautils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gated SLF4J logger for IskaUtils. Debug/info/warn require {@link Config#devLoggingEnabled};
 * error requires {@link Config#errorLoggingEnabled}.
 */
public final class ModLogger {
    private final Logger delegate;

    private ModLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public static ModLogger of(Class<?> clazz) {
        return new ModLogger(LoggerFactory.getLogger(clazz));
    }

    /** Underlying SLF4J logger for APIs that still require {@link Logger}. */
    public Logger unwrap() {
        return delegate;
    }

    public boolean isDebugEnabled() {
        return Config.devLoggingEnabled;
    }

    public void trace(String msg) {
        if (Config.devLoggingEnabled) {
            delegate.trace(msg);
        }
    }

    public void trace(String format, Object arg) {
        if (Config.devLoggingEnabled) {
            delegate.trace(format, arg);
        }
    }

    public void trace(String format, Object arg1, Object arg2) {
        if (Config.devLoggingEnabled) {
            delegate.trace(format, arg1, arg2);
        }
    }

    public void trace(String format, Object... arguments) {
        if (Config.devLoggingEnabled) {
            delegate.trace(format, arguments);
        }
    }

    public void trace(String msg, Throwable t) {
        if (Config.devLoggingEnabled) {
            delegate.trace(msg, t);
        }
    }

    public void debug(String msg) {
        if (Config.devLoggingEnabled) {
            delegate.debug(msg);
        }
    }

    public void debug(String format, Object arg) {
        if (Config.devLoggingEnabled) {
            delegate.debug(format, arg);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (Config.devLoggingEnabled) {
            delegate.debug(format, arg1, arg2);
        }
    }

    public void debug(String format, Object... arguments) {
        if (Config.devLoggingEnabled) {
            delegate.debug(format, arguments);
        }
    }

    public void debug(String msg, Throwable t) {
        if (Config.devLoggingEnabled) {
            delegate.debug(msg, t);
        }
    }

    public void info(String msg) {
        if (Config.devLoggingEnabled) {
            delegate.info(msg);
        }
    }

    public void info(String format, Object arg) {
        if (Config.devLoggingEnabled) {
            delegate.info(format, arg);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (Config.devLoggingEnabled) {
            delegate.info(format, arg1, arg2);
        }
    }

    public void info(String format, Object... arguments) {
        if (Config.devLoggingEnabled) {
            delegate.info(format, arguments);
        }
    }

    public void info(String msg, Throwable t) {
        if (Config.devLoggingEnabled) {
            delegate.info(msg, t);
        }
    }

    public void warn(String msg) {
        if (Config.devLoggingEnabled) {
            delegate.warn(msg);
        }
    }

    public void warn(String format, Object arg) {
        if (Config.devLoggingEnabled) {
            delegate.warn(format, arg);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (Config.devLoggingEnabled) {
            delegate.warn(format, arg1, arg2);
        }
    }

    public void warn(String format, Object... arguments) {
        if (Config.devLoggingEnabled) {
            delegate.warn(format, arguments);
        }
    }

    public void warn(String msg, Throwable t) {
        if (Config.devLoggingEnabled) {
            delegate.warn(msg, t);
        }
    }

    public void error(String msg) {
        if (Config.errorLoggingEnabled) {
            delegate.error(msg);
        }
    }

    public void error(String format, Object arg) {
        if (Config.errorLoggingEnabled) {
            delegate.error(format, arg);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (Config.errorLoggingEnabled) {
            delegate.error(format, arg1, arg2);
        }
    }

    public void error(String format, Object... arguments) {
        if (Config.errorLoggingEnabled) {
            delegate.error(format, arguments);
        }
    }

    public void error(String msg, Throwable t) {
        if (Config.errorLoggingEnabled) {
            delegate.error(msg, t);
        }
    }
}
