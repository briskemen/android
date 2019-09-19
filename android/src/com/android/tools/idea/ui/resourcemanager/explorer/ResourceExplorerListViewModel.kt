/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.ui.resourcemanager.explorer

import com.android.resources.ResourceType
import com.android.tools.idea.ui.resourcemanager.model.Asset
import com.android.tools.idea.ui.resourcemanager.model.FilterOptions
import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.android.tools.idea.ui.resourcemanager.rendering.AssetPreviewManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.ui.speedSearch.SpeedSearch
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture

/**
 * Interface for the view model of [com.android.tools.idea.ui.resourcemanager.explorer.ResourceExplorerListView].
 *
 * The production implementation is [ResourceExplorerListViewModelImpl].
 */
interface ResourceExplorerListViewModel {
  /**
   * callback called when the resource model has change. This happens when the facet is changed.
   */
  var resourceChangedCallback: (() -> Unit)?

  /**
   * Callback called when the [AndroidFacet] is changed.
   */
  var facetUpdaterCallback: ((facet: AndroidFacet) -> Unit)?

  /**
   * The current [ResourceType] of resources being fetched.
   */
  var currentResourceType: ResourceType

  val selectedTabName: String get() = ""

  val assetPreviewManager: AssetPreviewManager

  val summaryPreviewManager: AssetPreviewManager

  val facet: AndroidFacet

  val speedSearch: SpeedSearch

  val filterOptions: FilterOptions

  val externalActions: Collection<ActionGroup>

  /**
   * Returns a list of [ResourceSection] with one section per namespace, the first section being the
   * one containing the resource of the current module.
   */
  fun getCurrentModuleResourceLists(): CompletableFuture<List<ResourceSection>>

  /**
   * Similar to [getCurrentModuleResourceLists], but fetches resources for all other modules excluding the ones being displayed.
   */
  fun getOtherModulesResourceLists(): CompletableFuture<List<ResourceSection>>

  /**
   * Delegate method to handle calls to [com.intellij.openapi.actionSystem.DataProvider.getData].
   */
  fun getData(dataId: String?, selectedAssets: List<Asset>): Any?

  /**
   * Returns a map of some specific resource details, typically: name, reference, type, configuration, value.
   */
  fun getResourceSummaryMap(resourceAssetSet: ResourceAssetSet): CompletableFuture<Map<String, String>>

  /**
   * Returns a map for resource configurations, used to map each defined configuration with the resolved value of the resource and some
   * extra details about the resolved value.
   *
   * Eg:
   * > anydpi-v26 &emsp; | &emsp; Adaptive icon - ic_launcher.xml
   *
   * > hdpi &emsp;&emsp;&emsp;&emsp;&nbsp; | &emsp; Mip Map File - ic_launcher.png
   */
  fun getResourceConfigurationMap(resourceAssetSet: ResourceAssetSet): CompletableFuture<Map<String, String>>

  /**
   * Action when selecting an [asset] (double click or select + ENTER key).
   */
  val doSelectAssetAction: (asset: Asset) -> Unit

  val updateSelectedAssetSet: ((assetSet: ResourceAssetSet) -> Unit)

  /**
   * Triggers an [AndroidFacet] change through [facetUpdaterCallback].
   *
   * Eg: Searching for resources matching 'ic' and clicking the LinkLabel to switch to module Foo that contains resources matching the
   * filter. All components of the ResourceExplorer should update to module Foo.
   */
  fun facetUpdated(newFacet: AndroidFacet)
}