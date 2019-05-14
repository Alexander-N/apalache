package at.forsyte.apalache.tla

import java.io.File

import at.forsyte.apalache.tla.imp.src.SourceStore
import at.forsyte.apalache.tla.lir._

package object imp {


  def declarationsFromFile(environmentHandler: EnvironmentHandler, p_path : String ) : Seq[TlaDecl] = {
    val (rootName, modules) = new SanyImporter(environmentHandler, new SourceStore).loadFromFile( new File( p_path ) )
    modules( rootName ).declarations
  }

  // TODO: should not be this method moved to *.lir.process?
  def findBodyOf( p_opName : String, decls: TlaDecl* ) : TlaEx = {
    decls.find {
      _.name == p_opName
    }.withFilter( _.isInstanceOf[TlaOperDecl]).map( _.asInstanceOf[TlaOperDecl].body  ).getOrElse( NullEx )
  }

}
