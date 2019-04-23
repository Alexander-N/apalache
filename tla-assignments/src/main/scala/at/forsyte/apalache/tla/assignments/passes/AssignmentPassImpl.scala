package at.forsyte.apalache.tla.assignments.passes

import at.forsyte.apalache.infra.passes.{Pass, PassOptions}
import at.forsyte.apalache.tla.assignments._
import at.forsyte.apalache.tla.imp.findBodyOf
import at.forsyte.apalache.tla.imp.src.SourceStore
import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.db.BodyDB
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging

// TODO:
// USE SymbolicTransitionPass

/**
  * This pass finds symbolic transitions in a TLA+ specification.
  */
class AssignmentPassImpl @Inject()(options: PassOptions,
                                   environmentHandler: EnvironmentHandler,
                                   sourceStore: SourceStore,
                                   @Named("AfterAssignment") nextPass: Pass with SpecWithTransitionsMixin)
      extends AssignmentPass with LazyLogging {

  var tlaModule: Option[TlaModule] = None
  private var specWithTransitions: Option[SpecWithTransitions] = None

  /**
    * The name of the pass
    *
    * @return the name associated with the pass
    */
  override def name: String = "AssignmentFinder"

  /**
    * Run the pass
    *
    * @return true, if the pass was successful
    */
  override def execute(): Boolean = {
    val uniqueVarDecls = OperatorHandler.uniqueVarRename(tlaModule.get.declarations, sourceStore)

    val varSet = uniqueVarDecls
      .filter(_.isInstanceOf[TlaVarDecl])
      .map(_.name).toSet
    val constSet = uniqueVarDecls
      .filter(_.isInstanceOf[TlaConstDecl])
      .map(_.name).toSet

    // replace every variable x with x', so we can use the assignment solver
    def primeVars(nameSet: Set[String], e: TlaEx): TlaEx = e match {
      case ne @ NameEx(name) if nameSet.contains(name) =>
        val newEx = environmentHandler.identify(tla.prime(e))
        sourceStore.onTransformation(ne, newEx)
        newEx

      case oe @ OperEx(op, args@_*) =>
        val newEx = environmentHandler.identify(OperEx(op, args.map(primeVars(nameSet, _)): _*))
        sourceStore.onTransformation(oe, newEx)
        newEx


      case _ => e
    }

    val initName = options.getOption("checker", "init", "Init").asInstanceOf[String]

    val transformer = new Transformer()

    val bodyDB = new BodyDB

    val letInExpandedDecls = uniqueVarDecls.map(
      {
        case TlaOperDecl( name, params, body ) =>
          TlaOperDecl( name, params, transformer.explicitLetIn( body )( sourceStore ) )
        case e@_ => e
      }
    )

    transformer.extract( letInExpandedDecls  :_* )(bodyDB)

    // TODO: why do we call a pass inside a pass?
    val stp = new SymbolicTransitionPass( bodyDB, sourceStore )

    // since Converter and assignmentSolver do a lot of magic internally, substitute Init with primed variables first
    def replaceInit(decl: TlaDecl): TlaDecl = decl match {
      case TlaOperDecl(name, params, body) if name == initName =>
        if (params.nonEmpty) {
          throw new AssignmentException("Initializing operator %s has %d arguments, expected 0"
            .format(initName, params.length))
        } else {
          TlaOperDecl( name, params, primeVars(varSet, transformer.inlineAll( body )( bodyDB, sourceStore ) ) )
        }

      case e@_ => e
    }

    val initReplacedDecls = letInExpandedDecls.map(replaceInit)

    // drop selections because of lacking implementation further on
    val initTransitions = stp( initReplacedDecls, initName ).map( _._2 ).toList

    for ((t, i) <- initTransitions.zipWithIndex) {
      logger.debug("Initial transition #%d:\n   %s".format(i, t))
    }

    val nextName = options.getOption("checker", "next", "Next").asInstanceOf[String]

    // drop selections because of lacking implementation further on
    val nextTransitions = stp(initReplacedDecls,nextName).map( _._2 ).toList

    for ((t, i) <- nextTransitions.zipWithIndex) {
      logger.debug("Next transition #%d:\n   %s".format(i, t))
    }

    // find constant initializers
    val cinitName = options.getOption("checker", "cinit", None).asInstanceOf[Option[String]]
    val cinitPrime =
      if (cinitName.isEmpty) {
        None
      } else {
        val cinitBody = findBodyOf(cinitName.get, initReplacedDecls: _*)
        if (cinitBody == NullEx) {
          val msg = s"Constant initializer ${cinitName.get} not found"
          logger.error(msg)
          throw new IllegalArgumentException(msg)
        }
        val cinit = transformer.sanitize(cinitBody) (bodyDB, sourceStore)
        val cinitPrime = primeVars(constSet, cinit)
        logger.debug("Constant initializer with primes\n    %s".format(cinitPrime))
        Some(cinitPrime)
      }

    // find the invariant
    val invName = options.getOption("checker", "inv", None).asInstanceOf[Option[String]]
    val (notInvariant, notInvariantPrime) =
      if (invName.isEmpty) {
        (None, None)
      } else {
        val invBody = findBodyOf(invName.get, initReplacedDecls: _*)
        if (invBody == NullEx) {
          val msg = "Invariant definition %s not found".format(invName.get)
          logger.error(msg)
          throw new IllegalArgumentException(msg)
        }
        val notInv = transformer.sanitize(tla.not(invBody))( bodyDB, sourceStore )
        logger.debug("Negated invariant:\n   %s".format(notInv))
        val notInvPrime = primeVars(varSet, notInv)
        logger.debug("Negated invariant with primes\n   %s".format(notInvPrime))
        (Some(notInv), Some(notInvPrime))
      }

    logger.info("Found %d initializing transitions and %d next transitions"
      .format(initTransitions.length, nextTransitions.length))

    val temporalName = options.getOption("checker", "temporal", None).asInstanceOf[Option[String]]
    val temporal =
      if (temporalName.isEmpty) {
        None
      } else {
        val temporalBody = findBodyOf(temporalName.get, initReplacedDecls: _*)
        if (temporalBody == NullEx) {
          val msg = "Definition of temporal property %s not found".format(temporalName.get)
          logger.error(msg)
          throw new IllegalArgumentException(msg)
        }

        val temporal = transformer.sanitize(temporalBody)( bodyDB, sourceStore )

        Some(temporal)
      }

    val propertyExtractor = new PropertyExtractor(bodyDB)
    val liveness =
      if (temporal.isEmpty) {
        None
      } else {
        val liveness = propertyExtractor.extractLivenessProperty(findBodyOf(temporalName.get, initReplacedDecls: _*))
        liveness
      }

    val (enabledActionWeakFairnessHintTuples, enabledActionStrongFairnessHintTuples) =
      if (temporalName.isEmpty) {
        (None, None)
      } else {
        val (weakFairnessTuples, strongFairnessTuples) = propertyExtractor.extractActionHintTuples(temporalName.get)
        (Some(weakFairnessTuples), Some(strongFairnessTuples))
      }

    val newModule = new TlaModule(tlaModule.get.name, tlaModule.get.imports, uniqueVarDecls)
    specWithTransitions = Some(new SpecWithTransitions(newModule,
                                                       initTransitions,
                                                       nextTransitions,
                                                       cinitPrime,
                                                       notInvariant,
                                                       notInvariantPrime,
                                                       temporal,
                                                       liveness,
                                                       enabledActionWeakFairnessHintTuples,
                                                       enabledActionStrongFairnessHintTuples))
    true
  }

  /**
    * Get the next pass in the chain. What is the next pass is up
    * to the module configuration and the pass outcome.
    *
    * @return the next pass, if exists, or None otherwise
    */
  override def next(): Option[Pass] = {
    if (specWithTransitions.isDefined) {
      nextPass.setSpecWithTransitions(specWithTransitions.get)
      Some(nextPass)
    } else {
      None
    }
  }
}
