package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.SetTag;
import com.hubspot.jinjava.tree.TagNode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;

public abstract class EagerSetTagStrategy {
  protected final SetTag setTag;

  protected EagerSetTagStrategy(SetTag setTag) {
    this.setTag = setTag;
  }

  public String run(TagNode tagNode, JinjavaInterpreter interpreter) {
    int eqPos = tagNode.getHelpers().indexOf('=');
    String[] variables;
    String expression;
    if (eqPos > 0) {
      variables = tagNode.getHelpers().substring(0, eqPos).trim().split(",");
      expression = tagNode.getHelpers().substring(eqPos + 1).trim();
    } else {
      int filterPos = tagNode.getHelpers().indexOf('|');
      String var = tagNode.getHelpers().trim();
      if (filterPos >= 0) {
        var = tagNode.getHelpers().substring(0, filterPos).trim();
      }
      variables = new String[] { var };
      expression = tagNode.getHelpers();
    }

    EagerExecutionResult eagerExecutionResult = getEagerExecutionResult(
      tagNode,
      expression,
      interpreter
    );
    if (
      eagerExecutionResult.getResult().isFullyResolved() &&
      !interpreter.getContext().isDeferredExecutionMode()
    ) {
      Optional<String> maybeResolved = resolveSet(
        tagNode,
        variables,
        eagerExecutionResult,
        interpreter
      );
      if (maybeResolved.isPresent()) {
        return maybeResolved.get();
      }
    }
    Triple<String, String, String> triple = getPrefixTokenAndSuffix(
      tagNode,
      variables,
      eagerExecutionResult,
      interpreter
    );
    if (
      eagerExecutionResult.getResult().isFullyResolved() &&
      interpreter.getContext().isDeferredExecutionMode()
    ) {
      attemptResolve(tagNode, variables, eagerExecutionResult, interpreter);
    }
    return buildImage(tagNode, variables, eagerExecutionResult, triple, interpreter);
  }

  protected abstract EagerExecutionResult getEagerExecutionResult(
    TagNode tagNode,
    String expression,
    JinjavaInterpreter interpreter
  );

  protected abstract Optional<String> resolveSet(
    TagNode tagNode,
    String[] variables,
    EagerExecutionResult resolvedValues,
    JinjavaInterpreter interpreter
  );

  protected abstract Triple<String, String, String> getPrefixTokenAndSuffix(
    TagNode tagNode,
    String[] variables,
    EagerExecutionResult eagerExecutionResult,
    JinjavaInterpreter interpreter
  );

  protected abstract void attemptResolve(
    TagNode tagNode,
    String[] variables,
    EagerExecutionResult resolvedValues,
    JinjavaInterpreter interpreter
  );

  protected abstract String buildImage(
    TagNode tagNode,
    String[] variables,
    EagerExecutionResult eagerExecutionResult,
    Triple<String, String, String> triple,
    JinjavaInterpreter interpreter
  );

  protected String getPrefixToPreserveState(
    EagerExecutionResult eagerExecutionResult,
    JinjavaInterpreter interpreter
  ) {
    StringBuilder prefixToPreserveState = new StringBuilder();
    if (interpreter.getContext().isDeferredExecutionMode()) {
      prefixToPreserveState.append(eagerExecutionResult.getPrefixToPreserveState());
    } else {
      interpreter.getContext().putAll(eagerExecutionResult.getSpeculativeBindings());
    }
    prefixToPreserveState.append(
      EagerTagDecorator.reconstructFromContextBeforeDeferring(
        eagerExecutionResult.getResult().getDeferredWords(),
        interpreter
      )
    );
    return prefixToPreserveState.toString();
  }

  protected String getSuffixToPreserveState(
    String variables,
    JinjavaInterpreter interpreter
  ) {
    StringBuilder suffixToPreserveState = new StringBuilder();
    Optional<String> maybeFullImportAlias = interpreter
      .getContext()
      .getImportResourceAlias();
    if (maybeFullImportAlias.isPresent()) {
      String currentImportAlias = maybeFullImportAlias
        .get()
        .substring(maybeFullImportAlias.get().lastIndexOf(".") + 1);
      String updateString = getUpdateString(variables);
      suffixToPreserveState.append(
        interpreter.render(
          EagerTagDecorator.buildDoUpdateTag(
            currentImportAlias,
            updateString,
            interpreter
          )
        )
      );
    }
    return suffixToPreserveState.toString();
  }

  private static String getUpdateString(String variables) {
    List<String> varList = Arrays
      .stream(variables.split(","))
      .map(String::trim)
      .collect(Collectors.toList());
    StringJoiner updateString = new StringJoiner(",");
    // Update the alias map to the value of the set variable.
    varList.forEach(var -> updateString.add(String.format("'%s': %s", var, var)));
    return "{" + updateString + "}";
  }
}