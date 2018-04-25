/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.tests.gui.framework.fixture.npw;

import com.android.tools.idea.tests.gui.framework.fixture.wizard.AbstractWizardFixture;
import com.android.tools.idea.tests.gui.framework.fixture.wizard.AbstractWizardStepFixture;
import org.fest.swing.core.matcher.JLabelMatcher;
import org.fest.swing.fixture.JComboBoxFixture;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import static com.android.tools.idea.tests.gui.framework.GuiTests.waitUntilShowing;

public class ConfigureDynamicFeatureStepFixture<W extends AbstractWizardFixture>
  extends AbstractWizardStepFixture<ConfigureDynamicFeatureStepFixture, W> {

  ConfigureDynamicFeatureStepFixture(@NotNull W wizard, @NotNull JRootPane target) {
    super(ConfigureDynamicFeatureStepFixture.class, wizard, target);
  }

  @NotNull
  public ConfigureDynamicFeatureStepFixture<W> enterFeatureModuleName(@NotNull String text) {
    JTextComponent textField = findTextFieldWithLabel("Module name");
    replaceText(textField, text);
    return this;
  }

  @NotNull
  public ConfigureDynamicFeatureStepFixture<W> selectBaseApplication(@NotNull String baseName) {
   JComboBoxFixture apiLevelComboBox =
      new JComboBoxFixture(robot(), robot().finder().findByName(target(), "baseComboBox", JComboBox.class));
    apiLevelComboBox.selectItem(baseName);
    return this;
  }

  @NotNull
  public ConfigureDynamicFeatureStepFixture<W> selectMinimumSdkApi(@NotNull String api) {
    ApiLevelComboBoxFixture apiLevelComboBox =
      new ApiLevelComboBoxFixture(robot(), robot().finder().findByName(target(), "Mobile.minSdk", JComboBox.class));
    apiLevelComboBox.selectApiLevel(api);
    return this;
  }

  @NotNull
  public ConfigureDynamicFeatureDeliveryStepFixture<W> clickNextToConfigureDynamicDelivery() {
    wizard().clickNext();
    waitUntilShowing(robot(), target(), JLabelMatcher.withText("Configure On-Demand Options"));
    return new ConfigureDynamicFeatureDeliveryStepFixture<>(wizard(), (JRootPane)wizard().target());
  }
}
