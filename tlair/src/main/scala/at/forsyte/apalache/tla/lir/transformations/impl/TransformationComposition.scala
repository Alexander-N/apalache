package at.forsyte.apalache.tla.lir.transformations.impl

import at.forsyte.apalache.tla.lir.transformations.TlaExTransformation

// TODO: Igor @ 01.07.2019: why do we need this class? Just use functional composition.
// See Function.chain and Function.andThen.
class TransformationComposition( transformations : TlaExTransformation* )
  extends TransformationImpl(
    transformations.foldLeft( _ ) { case (e, tr) => tr( e ) }
  )
