package com.igormaznitsa.jbbp.plugin.common.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.io.File;
import org.junit.jupiter.api.Test;

class JavaConverterTest {

  @Test
  void testTranslateParameters() {
    final JBBPScriptTranslator.Parameters parameters = new JBBPScriptTranslator.Parameters();

    assertTrue(parameters.setAddNewInstanceMethods(true).isAddNewInstanceMethods());
    assertFalse(parameters.setAddNewInstanceMethods(false).isAddNewInstanceMethods());

    assertTrue(parameters.setAddBinAnnotations(true).isAddBinAnnotations());
    assertFalse(parameters.setAddBinAnnotations(false).isAddBinAnnotations());

    assertTrue(parameters.setAddGettersSetters(true).isAddGettersSetters());
    assertFalse(parameters.setAddGettersSetters(false).isAddGettersSetters());

    assertTrue(parameters.setDisableGenerateFields(true).isDisableGenerateFields());
    assertFalse(parameters.setDisableGenerateFields(false).isDisableGenerateFields());

    assertTrue(parameters.setDoAbstract(true).isDoAbstract());
    assertFalse(parameters.setDoAbstract(false).isDoAbstract());

    assertTrue(parameters.setDoInternalClassesNonStatic(true).isDoInternalClassesNonStatic());
    assertFalse(parameters.setDoInternalClassesNonStatic(false).isDoInternalClassesNonStatic());

    assertEquals(new File("some_test"),
        parameters.setOutputDir(new File("some_test")).getOutputDir());
    assertEquals(new File("some_test.script"),
        parameters.setScriptFile(new File("some_test.script")).getScriptFile());
    assertEquals("some.test.package",
        parameters.setPackageName("some.test.package").getPackageName());
    assertEquals("bit a;", parameters.setScriptText("bit a;").getScriptText());
    assertEquals("SomeSuperClass", parameters.setSuperClass("SomeSuperClass").getSuperClass());
    assertEquals(1234, parameters.setParserFlags(1234).getParserFlags());
    assertEquals("UTF-8", parameters.setEncodingIn("UTF-8").getEncodingIn());
    assertEquals("UTF-8", parameters.setEncodingOut("UTF-8").getEncodingOut());

    parameters.setScriptFile(null);
  }

}