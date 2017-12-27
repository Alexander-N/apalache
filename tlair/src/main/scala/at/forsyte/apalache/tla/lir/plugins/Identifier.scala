package at.forsyte.apalache.tla.lir.plugins

import java.util.Vector

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.db._

object UniqueDB extends DB[ UID, TlaEx ] {
  // Igor: let's get rid of a singleton here. Make a class.
  override val name = "UniqueDB"

  private val expressions : Vector[ TlaEx ] = new Vector[ TlaEx ]

  override def apply( key: UID ): Option[ TlaEx ] = {
    if( key.id < 0 || key.id >= expressions.size() ) return None
    else return Some( expressions.elementAt( key.id ) )
  }

  override def get( key : UID ) : TlaEx = expressions.elementAt( key.id )
  override def size() : Int = expressions.size()

  override def contains( key : UID ) : Boolean = 0 until expressions.size() contains key.id
  override def clear() : Unit = expressions.clear()
  override def print(): Unit = {
    println( "\n" + name + ": \n" )
    for ( i <- 0 until expressions.size()  ) {
      println( UID( i ) + " -> " + expressions.get( i ) )
    }
  }

  def add( ex: TlaEx ) : Unit = {
    if( !ex.ID.valid ){
      ex.setID( UID( expressions.size() ) )
      expressions.add( ex )
    }
  }

  override def keySet( ) : Set[UID] = List.range(0,expressions.size()).map(UID).toSet

}


/**
  * Created by jkukovec on 11/28/16.
  */
package object Identifier {
  def identify( spec : TlaSpec ) : Unit = SpecHandler.sideeffectWithExFun( spec, UniqueDB.add )
  def identify( decl : TlaDecl ) : Unit = SpecHandler.sideeffectOperBody( decl , SpecHandler.sideeffectEx( _, UniqueDB.add ) )
  def identify( ex : TlaEx ) : Unit = SpecHandler.sideeffectEx( ex, UniqueDB.add )
}

/**
  * @deprecated use Pass
  */
package object FirstPass extends Plugin {
  override val name = "FirstPass"
  override val dependencies : List[String] = Nil

  /** Cannot produce errors (?)*/
  override def translate(): Unit = {
    output = input.deepCopy()
    SpecHandler.sideeffectWithExFun( output, UniqueDB.add )
  }

  override def reTranslate( err: PluginError ): Unit = {
    /** Forwards errors */
    throwError = err
  }

}
