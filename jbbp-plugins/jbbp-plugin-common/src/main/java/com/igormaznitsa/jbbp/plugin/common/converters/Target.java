package com.igormaznitsa.jbbp.plugin.common.converters;

import javax.annotation.Nonnull;

public enum Target {
  JAVA(new JavaConverter());

  private final JBBPScriptTranslator JBBPScriptTranslator;

  Target(@Nonnull final JBBPScriptTranslator translator) {
    this.JBBPScriptTranslator = translator;
  }

  @Nonnull
  public JBBPScriptTranslator getTranslator() {
    return this.JBBPScriptTranslator;
  }
}
