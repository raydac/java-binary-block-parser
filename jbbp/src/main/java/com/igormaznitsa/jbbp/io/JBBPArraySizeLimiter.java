package com.igormaznitsa.jbbp.io;

import com.igormaznitsa.jbbp.exceptions.JBBPReachedArraySizeLimitException;

/**
 * Interface describing an object which provides limit to read array items.
 *
 * @since 2.1.0
 */
@FunctionalInterface
public interface JBBPArraySizeLimiter {
  /**
   * Read arrays without limits.
   */
  JBBPArraySizeLimiter NO_LIMIT_FOR_ARRAY_SIZE = () -> 0;

  /**
   * Check number of read items for whole stream array and return flag if read should be stopped or throw exception if required.
   *
   * @param readItems number of currently read array items
   * @param limiter   limiter provides number of allowed items, must not be null
   * @return true if read must be stopped immediately, false otherwise
   * @throws JBBPReachedArraySizeLimitException it will be thrown if reach of limit is not allowed
   */
  static boolean isBreakReadWholeStream(
      final int readItems,
      final JBBPArraySizeLimiter limiter
  ) {
    final int limit = limiter.getArrayItemsLimit();
    if (limit == 0) {
      return false;
    }
    if (limit > 0) {
      if (readItems > limit) {
        throw new JBBPReachedArraySizeLimitException("", readItems, Math.abs(limit));
      } else {
        return false;
      }
    } else {
      return readItems >= Math.abs(limit);
    }
  }

  /**
   * Get allowed size of array to read.
   *
   * @return 0 means no limit, positive value means allowed number of items with exception throwing if read more items, negative value means number of read values and stop read if exceeded.
   */
  int getArrayItemsLimit();
}
