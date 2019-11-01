package at.forsyte.apalache.tla.pp.passes

import java.io.File
import java.nio.file.Path

import at.forsyte.apalache.infra.passes.{Pass, PassOptions, TlaModuleMixin}
import at.forsyte.apalache.tla.lir.TlaModule
import at.forsyte.apalache.tla.lir.io.PrettyWriter
import at.forsyte.apalache.tla.lir.storage.BodyMapFactory
import at.forsyte.apalache.tla.lir.transformations.{TlaExTransformation, TransformationTracker}
import at.forsyte.apalache.tla.lir.transformations.standard._
import at.forsyte.apalache.tla.pp.{Desugarer, Keramelizer, UniqueNameGenerator}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging

class PreproPassImpl @Inject()( val options: PassOptions,
                                gen: UniqueNameGenerator,
                                tracker: TransformationTracker,
                                @Named("AfterPrepro") nextPass: Pass with TlaModuleMixin)
    extends PreproPass with LazyLogging {

  private var outputTlaModule: Option[TlaModule] = None

  /**
    * The pass name.
    *
    * @return the name associated with the pass
    */
  override def name: String = "PreprocessingPass"

  /**
    * Run the pass.
    *
    * @return true, if the pass was successful
    */
  override def execute(): Boolean = {
    logger.info("Running Desugarer")
    val afterDesugarer = ModuleByExTransformer(Desugarer(tracker)) (tlaModule.get)
    logger.info("Running KerAmelizer")
    val keraSpec = ModuleByExTransformer(new Keramelizer(gen, tracker)) (afterDesugarer)
    logger.info("Renaming variables uniquely")
    val renaming = new IncrementalRenaming( tracker )
    val uniqueVarDecls =
      new TlaModule(
        keraSpec.name,
        renaming.syncAndNormalizeDs(keraSpec.declarations).toSeq
      ) ///

    val bodyMap = BodyMapFactory.makeFromDecls( uniqueVarDecls.operDeclarations )

    val transformationSequence : List[TlaExTransformation] =
      List(
        Inline( bodyMap, tracker ),
        ExplicitLetIn( tracker, keepNullary = true ),
        EqualityAsContainment( tracker ),
        ExplicitUnchanged( tracker ),
        SimplifyRecordAccess( tracker )
      )

    logger.info("Applying standard transformations")
    val preprocessed = transformationSequence.foldLeft( uniqueVarDecls ){
      case (m,tr) => ModuleByExTransformer( tr )( m )
    }

    val outdir = options.getOptionOrError("io", "outdir").asInstanceOf[Path]
    PrettyWriter.write(preprocessed, new File(outdir.toFile, "out-prepro.tla"))

    outputTlaModule = Some(preprocessed)
    true
  }

  /**
    * Get the next pass in the chain. What is the next pass is up
    * to the module configuration and the pass outcome.
    *
    * @return the next pass, if exists, or None otherwise
    */
  override def next(): Option[Pass] = {
    outputTlaModule map { m =>
      nextPass.setModule( m )
      nextPass
    }
  }
}
