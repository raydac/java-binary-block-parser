package com.igormaznitsa.jbbp.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;


import org.junit.jupiter.api.Test;

class JBBPTokenizerExceptionTest {
  @Test
  public void testNullAsArg() {
    assertEquals("null (pos=0, errorPart=\"\")",
        new JBBPTokenizerException(null, null, 0).toString());
    assertEquals(" (pos=0, errorPart=\"\")", new JBBPTokenizerException("", null, 0).toString());
    assertEquals("null (pos=0, errorPart=\"\")",
        new JBBPTokenizerException(null, "", 0).toString());
    assertEquals("null (pos=5, errorPart=\"\")",
        new JBBPTokenizerException(null, null, 5).toString());
    assertEquals(" (pos=5, errorPart=\"\")", new JBBPTokenizerException("", null, 5).toString());
    assertEquals("null (pos=5, errorPart=\"\")",
        new JBBPTokenizerException(null, "", 5).toString());
    assertEquals("null (pos=-5, errorPart=\"\")",
        new JBBPTokenizerException(null, null, -5).toString());
    assertEquals(" (pos=-5, errorPart=\"\")", new JBBPTokenizerException("", null, -5).toString());
    assertEquals("null (pos=-5, errorPart=\"\")",
        new JBBPTokenizerException(null, "", -5).toString());
  }

  @Test
  public void testWrongErrorPos() {
    assertEquals("hello (pos=23, errorPart=\"\")",
        new JBBPTokenizerException("hello", "world", 23).toString());
    assertEquals("hello (pos=-1, errorPart=\"\")",
        new JBBPTokenizerException("hello", "world", -1).toString());
  }

  @Test
  public void testCornerPositions() {
    assertEquals("hello (pos=0, errorPart=\"->w<- orld\")",
        new JBBPTokenizerException("hello", "world", 0).toString());
    assertEquals("hello (pos=4, errorPart=\"worl ->d<-\")",
        new JBBPTokenizerException("hello", "world", 4).toString());
  }
}