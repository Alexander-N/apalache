package at.forsyte.apalache.tla.lir.transformations.impl

import at.forsyte.apalache.tla.lir.{OperEx, TlaEx}
import at.forsyte.apalache.tla.lir.transformations.ExprTransformer


object aux {
  def fn( transformation : ExprTransformer )( ex : TlaEx ) : TlaEx = ex match {
    case OperEx( op, args@_* ) =>
      val newEx = OperEx( op,
        args map fn( transformation )
          : _* )
      transformation( newEx )
    case _ => transformation( ex )
  }
}

// TODO: Igor @ 01.07.2019: we do not need this class.
// The user can simply decorate any recursive function with TransformationFactory.listenTo.
class RecursiveTransformationImpl( transformation : ExprTransformer )
  extends TransformationImpl( aux.fn( transformation ) )
