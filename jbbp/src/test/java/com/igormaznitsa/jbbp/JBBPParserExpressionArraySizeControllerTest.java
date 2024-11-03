package com.igormaznitsa.jbbp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JBBPParserExpressionArraySizeControllerTest {

  @Test
  public void testGetterAndSetter() {
    final JBBPParserExpressionArraySizeController controller =
        (parser, expressionEvaluator, fieldInfo, calculatedArraySize) -> 0;
    final JBBPParser parser = JBBPParser.prepare("ubyte len; byte [len*2] a;")
        .setExpressionArraySizeController(controller);
    assertSame(controller, parser.getExpressionArraySizeController());
  }

  @Test
  public void testNoChangeSize() throws Exception {
    final AtomicInteger calls = new AtomicInteger();
    final JBBPParser parser =
        JBBPParser.prepare("ubyte len; byte [len*2] a;").setExpressionArraySizeController(
            (parser1, expressionEvaluator, fieldInfo, calculatedArraySize) -> {
              calls.incrementAndGet();
              assertEquals("a", fieldInfo.getFieldName());
              assertEquals(4, calculatedArraySize);
              return calculatedArraySize;
            });
    parser.parse(new ByteArrayInputStream(new byte[] {2, 1, 2, 3, 4}));
    assertEquals(1, calls.get());
  }

  @Test
  public void testThrowException() {
    final JBBPParser parser =
        JBBPParser.prepare("ubyte len; byte [len*2] a;").setExpressionArraySizeController(
            (parser1, expressionEvaluator, fieldInfo, calculatedArraySize) -> {
              assertEquals("a", fieldInfo.getFieldName());
              if (calculatedArraySize > 2) {
                throw new IllegalArgumentException();
              }
              return calculatedArraySize;
            });
    assertThrows(IllegalArgumentException.class,
        () -> parser.parse(new ByteArrayInputStream(new byte[] {2, 1, 2, 3, 4})));
  }

  @Test
  public void testChangeSize() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("ubyte len; byte [len*2] a;").setExpressionArraySizeController(
            (parser1, expressionEvaluator, fieldInfo, calculatedArraySize) -> {
              assertEquals("a", fieldInfo.getFieldName());
              return calculatedArraySize - 1;
            });
    assertEquals(3, parser.parse(new ByteArrayInputStream(new byte[] {2, 1, 2, 3, 4}))
        .findFieldForNameAndType("a",
            JBBPFieldArrayByte.class).size());
  }
}