package at.forsyte.apalache.tla.bmcmt.passes

import at.forsyte.apalache.infra.passes.{Pass, PassOptions}
import at.forsyte.apalache.tla.assignments.SpecWithTransitions
import at.forsyte.apalache.tla.assignments.passes.SpecWithTransitionsMixin
import at.forsyte.apalache.tla.bmcmt.CheckerException
import at.forsyte.apalache.tla.bmcmt.analyses.{FreeExistentialsStoreImpl, SimpleSkolemization}
import at.forsyte.apalache.tla.lir.IdOrdering
import at.forsyte.apalache.tla.lir.process.Renaming
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging

/**
  * Find free-standing existential quantifiers and rename all local bindings, so they have unique names.
  * @param options
  * @param freeExistentialsStoreImpl
  * @param renaming
  * @param nextPass
  */
class SimpleSkolemizationPassImpl @Inject()(val options: PassOptions,
                                            freeExistentialsStoreImpl: FreeExistentialsStoreImpl,
                                            renaming: Renaming,
                                            @Named("AfterSkolem") nextPass: Pass with SpecWithTransitionsMixin)
  extends SimpleSkolemizationPass with LazyLogging {

  private var specWithTransitions: Option[SpecWithTransitions] = None

  /**
    * The pass name.
    *
    * @return the name associated with the pass
    */
  override def name: String = "SimpleSkolemization"

  /**
    * Run the pass.
    *
    * @return true, if the pass was successful
    */
  override def execute(): Boolean = {
    if (specWithTransitions.isEmpty) {
      throw new CheckerException(s"The input of $name pass is not initialized")
    }
    val spec = specWithTransitions.get
    // rename bound variables, so each of them is unique. This is required by TrivialTypeFinder.
    // hint by Markus Kuppe: sort init and next to get a stable ordering between the runs
    val initRenamed = spec.initTransitions.map(renaming.renameBindingsUnique).sorted(IdOrdering)
    val nextRenamed = spec.nextTransitions.map(renaming.renameBindingsUnique).sorted(IdOrdering)
    val notInvRenamed = spec.notInvariant match {
      case Some(ni) => Some(renaming.renameBindingsUnique(ni))
      case None => None
    }
    var newSpec = new SpecWithTransitions(spec.rootModule, initRenamed, nextRenamed, notInvRenamed)
    val skolem = new SimpleSkolemization(freeExistentialsStoreImpl)
    newSpec = skolem.transformAndLabel(newSpec)

    logger.debug("Transitions after renaming and skolemization")
    for ((t, i) <- newSpec.initTransitions.zipWithIndex) {
      logger.debug("Initial transition #%d:\n   %s".format(i, t))
    }
    for ((t, i) <- newSpec.nextTransitions.zipWithIndex) {
      logger.debug("Next transition #%d:\n   %s".format(i, t))
    }
    logger.debug("Negated invariant:\n   %s".format(newSpec.notInvariant))

    nextPass.setSpecWithTransitions(newSpec)
    val nfree = freeExistentialsStoreImpl.store.size
    logger.info(s"Found $nfree free existentials in the transitions")
    true
  }

  /**
    * Get the next pass in the chain. What is the next pass is up
    * to the module configuration and the pass outcome.
    *
    * @return the next pass, if exists, or None otherwise
    */
  override def next(): Option[Pass] = {
    Some(nextPass)
  }

  override def setSpecWithTransitions(spec: SpecWithTransitions): Unit = {
    specWithTransitions = Some(spec)
  }
}
