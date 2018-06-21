package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.sqldelight.core.compiler.model.BindableQuery
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.STATEMENT_NAME
import com.squareup.sqldelight.core.lang.util.isArrayParameter
import com.squareup.sqldelight.core.lang.util.range
import com.squareup.sqldelight.core.lang.util.rawSqlText

abstract class QueryGenerator(private val query: BindableQuery) {
  /**
   * Creates the block of code that prepares [query] as a prepared statement and binds the
   * arguments to it. This code block does not make any use of class fields, and only populates a
   * single variable [STATEMENT_NAME]
   *
   * val numberIndexes = number.mapIndexed { index, _ ->
   *     "?${ index + 2 }"
   *     }.joinToString(prefix = "(", postfix = ")")
   * val statement = database.getConnection().prepareStatement("""
   *     |SELECT *
   *     |FROM player
   *     |WHERE number IN $numberIndexes
   *     """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1 + (number.size - 1))
   * number.forEachIndexed { index, number ->
   *     statement.bindLong(index + 2, number)
   *     }
   */
  protected fun preparedStatementBinder(): CodeBlock {
    val result = CodeBlock.builder()

    val maxIndex = query.arguments.map { it.index }.max()
    val precedingArrays = mutableListOf<String>()
    val bindStatements = CodeBlock.builder()
    val replacements = mutableListOf<Pair<IntRange, String>>()
    val argumentCount = CodeBlock.builder()
        .add("${query.arguments.filterNot { it.type.bindArg!!.isArrayParameter() }.size}")

    // For each parameter in the sql
    query.arguments.forEach { (index, argument, args) ->
      if (argument.bindArg!!.isArrayParameter()) {
        // Need to replace the single argument with a group of indexed arguments, calculated at
        // runtime from the list parameter:
        // val idIndexes = id.mapIndexed { index, _ -> "?${1 + previousArray.size + index}" }.joinToString(prefix = "(", postfix = ")")
        val indexCalculator = (precedingArrays.map { "$it.size" } + "index" + "${maxIndex!! + 1}")
            .joinToString(separator = " + ")
        result.addStatement("""
          |val ${argument.name}Indexes = ${argument.name}.mapIndexed { index, _ ->
          |"?${"$"}{ $indexCalculator }"
          |}.joinToString(prefix = "(", postfix = ")")
        """.trimMargin())

        // Replace the single bind argument with the array of bind arguments:
        // WHERE id IN ${idIndexes}
        args.forEach {
          replacements.add(it.range to "${"$"}${argument.name}Indexes")
        }

        // Perform the necessary binds:
        // id.forEachIndex { index, parameter ->
        //   statement.bindLong(1 + previousArray.size + index, parameter)
        // }
        bindStatements.addStatement("""
          |${argument.name}.forEachIndexed { index, ${argument.name} ->
          |%L}
        """.trimMargin(), argument.preparedStatementBinder(indexCalculator))

        precedingArrays.add(argument.name)
        argumentCount.add(" + ${argument.name}.size")
      } else {
        // Binds each parameter to the statement:
        // statement.bindLong(1, id)
        bindStatements.add(argument.preparedStatementBinder(index.toString()))

        // Replace the argument with an indexed argument. (Do this always to make generated code
        // less bug-prone):
        // :name becomes ?1
        args.forEach {
          replacements.add(it.range to "?$index")
        }
      }
    }

    // Adds the actual SqlPreparedStatement:
    // statement = database.getConnection().prepareStatement("SELECT * FROM test")
    result.addStatement(
        "val $STATEMENT_NAME = $DATABASE_NAME.getConnection().prepareStatement(%S, %L, %L)",
        query.statement.rawSqlText(replacements), query.type(), argumentCount.build()
    )
    result.add(bindStatements.build())

    return result.build()
  }

  protected fun addJavadoc(builder: FunSpec.Builder) {
    query.javadocText()?.let { builder.addKdoc(it) }
  }
}