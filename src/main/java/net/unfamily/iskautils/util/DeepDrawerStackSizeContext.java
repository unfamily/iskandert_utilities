package net.unfamily.iskautils.util;

/**
 * Thread-local context to track when we're accessing a Deep Drawer
 * This allows mixins to modify stack size behavior only for Deep Drawer operations
 */
public class DeepDrawerStackSizeContext {
    private static final ThreadLocal<Boolean> IN_DEEP_DRAWER = ThreadLocal.withInitial(() -> false);
    
    /**
     * Sets whether we're currently inside a Deep Drawer operation
     */
    public static void setInDeepDrawer(boolean inDeepDrawer) {
        IN_DEEP_DRAWER.set(inDeepDrawer);
    }
    
    /**
     * Gets whether we're currently inside a Deep Drawer operation
     */
    public static boolean isInDeepDrawer() {
        return IN_DEEP_DRAWER.get();
    }
    
    /**
     * Executes a runnable with Deep Drawer context enabled
     */
    public static void withDeepDrawerContext(Runnable runnable) {
        boolean oldValue = isInDeepDrawer();
        try {
            setInDeepDrawer(true);
            runnable.run();
        } finally {
            setInDeepDrawer(oldValue);
        }
    }
    
    /**
     * Executes a supplier with Deep Drawer context enabled and returns the result
     */
    public static <T> T withDeepDrawerContext(java.util.function.Supplier<T> supplier) {
        boolean oldValue = isInDeepDrawer();
        try {
            setInDeepDrawer(true);
            return supplier.get();
        } finally {
            setInDeepDrawer(oldValue);
        }
    }
}
