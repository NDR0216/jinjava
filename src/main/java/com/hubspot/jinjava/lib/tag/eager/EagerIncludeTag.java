package com.hubspot.jinjava.lib.tag.eager;

import com.google.common.annotations.Beta;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.IncludeTag;
import com.hubspot.jinjava.lib.tag.eager.importing.EagerImportingStrategyFactory;
import com.hubspot.jinjava.loader.RelativePathResolver;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.util.EagerReconstructionUtils;
import com.hubspot.jinjava.util.HelperStringTokenizer;
import org.apache.commons.lang3.StringUtils;

@Beta
public class EagerIncludeTag extends EagerTagDecorator<IncludeTag> {

  public EagerIncludeTag(IncludeTag tag) {
    super(tag);
  }

  @Override
  public String innerInterpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    int numDeferredTokensStart = interpreter.getContext().getDeferredTokens().size();
    String output = super.innerInterpret(tagNode, interpreter);
    if (interpreter.getContext().getDeferredTokens().size() > numDeferredTokensStart) {
      HelperStringTokenizer helper = new HelperStringTokenizer(tagNode.getHelpers());
      String path = StringUtils.trimToEmpty(helper.next());
      String templateFile = interpreter.resolveString(
        path,
        tagNode.getLineNumber(),
        tagNode.getStartPosition()
      );
      templateFile = interpreter.resolveResourceLocation(templateFile);
      final String initialPathSetter = EagerImportingStrategyFactory.getSetTagForCurrentPath(
        interpreter
      );
      final String newPathSetter = EagerReconstructionUtils.buildBlockOrInlineSetTag(
        RelativePathResolver.CURRENT_PATH_CONTEXT_KEY,
        templateFile,
        interpreter
      );
      return newPathSetter + output + initialPathSetter;
    }
    return output;
  }
}
