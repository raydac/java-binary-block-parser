package com.igormaznitsa.jbbp.utils;

import java.lang.reflect.AccessibleObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Auxiliary class collects methods for work with reflection.
 *
 * @since 1.4.0
 */
public final class ReflectUtils {
  /**
   * Inside auxiliary queue for privileged processors to avoid mass creation of
   * processors.
   */
  private static final Queue<PrivilegedProcessor> PROCESSORS_QUEUE = new ArrayBlockingQueue<>(32);

  private ReflectUtils() {
  }

  /**
   * Make accessible an accessible object, AccessController.doPrivileged will be
   * called.
   *
   * @param <T> type of object
   * @param obj an object to make accessible, it can be null.
   * @return the same object
   * @see AccessController#doPrivileged(java.security.PrivilegedAction)
   */
  public static <T extends AccessibleObject> T makeAccessible(final T obj) {
    if (obj != null) {
      PrivilegedProcessor processor = PROCESSORS_QUEUE.poll();
      if (processor == null) {
        processor = new PrivilegedProcessor();
      }
      processor.setAccessibleObject(obj);
      AccessController.doPrivileged(processor);
      if (!PROCESSORS_QUEUE.offer(processor)) {
        throw new Error("Can't place processor into queue");
      }
    }
    return obj;
  }

  /**
   * Create class instance through default constructor call
   *
   * @param klazz class to be instantiated, must not be null
   * @param <T>   type of the class
   * @return instance of class, must not be null
   * @throws RuntimeException if can't create instance for an error
   */
  public static <T> T newInstance(final Class<T> klazz) {
    try {
      return klazz.getConstructor().newInstance();
    } catch (Exception ex) {
      throw new RuntimeException(String.format("Can't create instance of %s for error %s", klazz, ex.getMessage()), ex);
    }
  }

  /**
   * Inside auxiliary class to make makeAccessible as a privileged action.
   */
  private static final class PrivilegedProcessor implements PrivilegedAction<AccessibleObject> {

    private AccessibleObject theObject;

    public void setAccessibleObject(final AccessibleObject obj) {
      this.theObject = obj;
    }

    @Override
    public AccessibleObject run() {
      final AccessibleObject objectToProcess = this.theObject;
      this.theObject = null;
      if (objectToProcess != null) {
        objectToProcess.setAccessible(true);
      }
      return objectToProcess;
    }
  }


}
