package at.forsyte.apalache.tla.lir.db

import at.forsyte.apalache.tla.lir.{TlaEx, UID}
import com.google.inject.Singleton

import scala.collection.immutable.Vector

// TODO: Igor: what is the point of collecting all the expressions in a vector?
// I do not see any useful application of that, except leaking memory.
// Deprecated: do not use this class.
@Singleton
class UidDB extends DB[ UID, TlaEx ] {
  override val m_name = "UIDMap"

  private var m_contents : Vector[ TlaEx ] = Vector[ TlaEx ]()

  override def apply( key : UID ) : TlaEx = {
    assert(key.id.isValidInt)
    m_contents( key.id.toInt )
  }

  override def get( key : UID ) : Option[TlaEx] = {
    assert(key.id.isValidInt)
    if (!contains(key)) None
    else Some(m_contents(key.id.toInt))
  }

  def add( ex: TlaEx ) : Unit = {
    if( !ex.ID.valid ){
      ex.setID( UID( m_contents.size ) )
      m_contents :+=  ex
    }
  }

  override def clear() : Unit = m_contents = Vector[ TlaEx ]()

  override def print(): Unit = {
    println( "\n" + m_name + ": \n" )
    m_contents foreach { x => println( x.ID + " -> " + x ) }
  }
  override def contains( key : UID ) : Boolean = key.id < m_contents.size && key.id >= 0

  override def size : Int = m_contents.size

  override def keyCollection : Traversable[UID] = m_contents.map( _.ID ).toSet

}