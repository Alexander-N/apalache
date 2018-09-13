package at.forsyte.apalache.tla.lir.plugins

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.db._

import scala.collection.immutable.Vector

object UniqueDB extends DB[ UID, TlaEx ] {
  // TODO: @Igor, let's get rid of a singleton here. Make a class.
  override val m_name = "UniqueDB"

  private var expressions : Vector[ TlaEx ] = Vector[ TlaEx ]()

  override def get( key: UID ): Option[ TlaEx ] = {
    if( key.id < 0 || key.id >= expressions.size ) return None
    else return Some( expressions( key.id ) )
  }

  override def apply( key : UID ) : TlaEx = expressions( key.id )
  override def size() : Int = expressions.size

  override def contains( key : UID ) : Boolean = 0 <= key.id && key.id < expressions.size
  override def clear() : Unit = expressions = Vector()
  override def print(): Unit = {
    println( "\n" + m_name + ": \n" )
    for ( i <- 0 until expressions.size  ) {
      println( UID( i ) + " -> " + expressions( i ) )
    }
  }

  def add( ex: TlaEx ) : Unit = {
    if( !ex.ID.valid ){
      ex.setID( UID( expressions.size ) )
      expressions :+=  ex
    }
  }

  override def keyCollection( ) : Traversable[UID] = expressions.indices.map(UID).toSet

}

/**
  * Created by jkukovec on 11/28/16.
  */
package object Identifier {
  def identify( spec : TlaSpec ) : Unit = SpecHandler.sideeffectWithExFun( spec, UniqueDB.add )
  def identify( decl : TlaDecl ) : Unit = SpecHandler.sideeffectOperBody( decl , SpecHandler.sideeffectEx( _, UniqueDB.add ) )
  def identify( ex : TlaEx ) : Unit = SpecHandler.sideeffectEx( ex, UniqueDB.add )
}

