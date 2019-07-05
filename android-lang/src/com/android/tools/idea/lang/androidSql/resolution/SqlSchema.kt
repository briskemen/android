/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.lang.androidSql.resolution

import com.android.tools.idea.lang.androidSql.psi.AndroidSqlColumnDefinitionName
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.Processor

typealias PsiElementPointer = SmartPsiElementPointer<out PsiElement>

/** Defines common properties for parts of a SQL schema. */
interface AndroidSqlDefinition {
  /** Name of the entity being defined, if known. */
  val name: String?

  /** [PsiElement] that "created" this definition. */
  val definingElement: PsiElement

  /**
   * [PsiElement] that determines the name of this entity.
   *
   * This may be the same as [definingElement] or an annotation element.
   */
  val resolveTo: PsiElement get() = definingElement
}

interface AndroidSqlTable : AndroidSqlDefinition {
  /**
   * @see [AndroidSqlColumnPsiReference.resolveColumn] for [sqlTablesInProcess]
   */
  fun processColumns(processor: Processor<AndroidSqlColumn>, sqlTablesInProcess: MutableSet<PsiElement>): Boolean
  val isView: Boolean
}

interface AndroidSqlColumn : AndroidSqlDefinition {
  val type: SqlType?
  /**
   * Other names the column can be referred to. This can be the case with e.g 'rowid' column,
   * see https://sqlite.org/lang_createtable.html#rowid
   */
  val alternativeNames: Set<String> get() = emptySet()
  val isPrimaryKey: Boolean get() = false
}

class AliasedTable(
  val delegate: AndroidSqlTable,
  override val name: String?,
  override val resolveTo: PsiElement
) : AndroidSqlTable by delegate

class AliasedColumn(
  val delegate: AndroidSqlColumn,
  override val name: String,
  override val resolveTo: PsiElement
) : AndroidSqlColumn by delegate

class ExprColumn(override val definingElement: PsiElement) : AndroidSqlColumn {
  override val name: String? get() = null
  override val type: SqlType? get() = null
}

class DefinedColumn(override val definingElement: AndroidSqlColumnDefinitionName) : AndroidSqlColumn {
  override val name get() = definingElement.nameAsString
  override val type: SqlType? get() = null
}

interface SqlType {
  /** To be used in completion. */
  val typeName: String?
}

/**
 * A [SqlType] that just uses the name of the underlying java field.
 *
 * TODO: stop using this and track the effective SQL types instead.
 */
class JavaFieldSqlType(override val typeName: String) : SqlType

/**
 * Type used by the special columns added by FTS (full-text search) extensions.
 */
object FtsSqlType : SqlType {
  override val typeName = "(full-text search only)"
}