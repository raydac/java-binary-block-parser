package com.igormaznitsa.jbbp.plugin.common.converters;

import javax.annotation.Nonnull;

public enum Target {
  JAVA_1_6(new Java16Converter());

  private final JBBPScriptTranslator JBBPScriptTranslator;

  Target(@Nonnull final JBBPScriptTranslator translator) {
    this.JBBPScriptTranslator = translator;
  }

  @Nonnull
  public JBBPScriptTranslator getTranslator() {
    return this.JBBPScriptTranslator;
  }
}
