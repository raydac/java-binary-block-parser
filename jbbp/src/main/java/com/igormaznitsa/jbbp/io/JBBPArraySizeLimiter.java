package com.igormaznitsa.jbbp.io;

/**
 * Interface describing an object which provides limit to read array items.
 *
 * @since 2.1.0
 */
@FunctionalInterface
public interface JBBPArraySizeLimiter {
  /**
   * Get allowed size of array to read.
   *
   * @return 0 means no limit, positive value means allowed number of items with exception throwing if read more items, negative value means number of read values and stop read if exceeded.
   */
  int getArrayItemsLimit();
}
