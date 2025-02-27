package com.hubspot.jinjava.el.ext.eager;

import com.hubspot.jinjava.el.NoInvokeELContext;
import com.hubspot.jinjava.el.ext.DeferredParsingException;
import com.hubspot.jinjava.el.ext.IdentifierPreservationStrategy;
import de.odysseus.el.tree.Bindings;
import de.odysseus.el.tree.impl.ast.AstChoice;
import de.odysseus.el.tree.impl.ast.AstNode;
import javax.el.ELContext;
import javax.el.ELException;

public class EagerAstChoice extends AstChoice implements EvalResultHolder {
  protected Object evalResult;
  protected boolean hasEvalResult;
  protected final EvalResultHolder question;
  protected final EvalResultHolder yes;
  protected final EvalResultHolder no;

  public EagerAstChoice(AstNode question, AstNode yes, AstNode no) {
    this(
      EagerAstNodeDecorator.getAsEvalResultHolder(question),
      EagerAstNodeDecorator.getAsEvalResultHolder(yes),
      EagerAstNodeDecorator.getAsEvalResultHolder(no)
    );
  }

  private EagerAstChoice(
    EvalResultHolder question,
    EvalResultHolder yes,
    EvalResultHolder no
  ) {
    super((AstNode) question, (AstNode) yes, (AstNode) no);
    this.question = question;
    this.yes = yes;
    this.no = no;
  }

  @Override
  public Object eval(Bindings bindings, ELContext context) throws ELException {
    try {
      setEvalResult(super.eval(bindings, context));
      return checkEvalResultSize(context);
    } catch (DeferredParsingException e) {
      if (question.hasEvalResult()) {
        // the question was evaluated so jump to either yes or no
        throw new DeferredParsingException(this, e.getDeferredEvalResult());
      }
      throw new DeferredParsingException(
        this,
        getPartiallyResolved(
          bindings,
          context,
          e,
          IdentifierPreservationStrategy.RESOLVING
        )
      );
    }
  }

  @Override
  public Object getEvalResult() {
    return evalResult;
  }

  @Override
  public void setEvalResult(Object evalResult) {
    this.evalResult = evalResult;
    hasEvalResult = true;
  }

  @Override
  public boolean hasEvalResult() {
    return hasEvalResult;
  }

  @Override
  public String getPartiallyResolved(
    Bindings bindings,
    ELContext context,
    DeferredParsingException deferredParsingException,
    IdentifierPreservationStrategy identifierPreservationStrategy
  ) {
    return (
      EvalResultHolder.reconstructNode(
        bindings,
        context,
        question,
        deferredParsingException,
        IdentifierPreservationStrategy.RESOLVING
      ) +
      " ? " +
      EvalResultHolder.reconstructNode(
        bindings,
        new NoInvokeELContext(context),
        yes,
        deferredParsingException,
        identifierPreservationStrategy
      ) +
      " : " +
      EvalResultHolder.reconstructNode(
        bindings,
        new NoInvokeELContext(context),
        no,
        deferredParsingException,
        identifierPreservationStrategy
      )
    );
  }
}
