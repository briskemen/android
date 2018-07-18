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
package com.android.tools.idea.resourceExplorer.sketchImporter.structure.deserializers;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.lang.reflect.Type;

public class ColorDeserializer implements JsonDeserializer<Color> {
  @Override
  public Color deserialize(@NotNull JsonElement json,
                           @NotNull Type typeOfT,
                           @NotNull JsonDeserializationContext context) {
    final JsonObject jsonObject = json.getAsJsonObject();

    final int alpha = jsonObject.get("alpha").getAsInt();
    final int blue = jsonObject.get("blue").getAsInt();
    final int green = jsonObject.get("green").getAsInt();
    final int red = jsonObject.get("red").getAsInt();

    return new Color(red, green, blue, alpha);
  }
}
