package at.forsyte.apalache.tla.lir.db

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.plugins.IDAllocator

import scala.collection.JavaConverters._

class EquivalenceDB extends SmartDB[TlaEx, EID] {
  type setT[T] = collection.mutable.Set[T]
  type vecT[E] = java.util.Vector[E]
  override val m_name = "EquivalenceDB"

  private val eqClasses : vecT[setT[UID]] = new java.util.Vector[collection.mutable.Set[UID]]
  private val allocator : IDAllocator[TlaEx]    = new IDAllocator[TlaEx]

  implicit def mkOption( id : EID ) : Option[EID] =
    if ( id.valid ) Some( id )
    else None

  implicit def mkOption( id : Int ) : Option[EID] = mkOption( EID( id ) )

  /** Retrieves EID from the allocator, WITHOUT allocating anew. Uses implicit conversion */
  override def peek( key : TlaEx ) : Option[EID] = allocator.getID( key )

  /** Predicts what the EID would be, if allocate were called. Uses implicit conversion */
  override def evaluate( key : TlaEx ) : Option[EID] = allocator.predict( key )

  /**
    * Allocates EID and updates equivalence classes.
    */
  protected def evaluateAndSave( ex : TlaEx ) : Option[EID] = {
    /** Checks for the next ID, to determine whether a new equivalence class should be created */
    val nStart = allocator.nextID()
    val eid = allocator.allocate( ex )

    /** If the number of allocated IDs remains unchanged, add current element to existing equivalence class */
    if ( nStart == allocator.nextID() )
      /** Note: Does nothing if ex.ID is already a member */
      eqClasses.elementAt( eid ).add( ex.ID )

    /** Otherwise create a singleton EC */
    else
      eqClasses.add( collection.mutable.Set[UID]( ex.ID ) )

    eid
  }

  override def apply( key : TlaEx ) : EID = get(key).get

  override def keyCollection( ) : Traversable[TlaEx] = allocator.keys().asScala

  /** Returns the number of distinct equivalence IDs assigned. */
  override def size( ) : Int = allocator.nextID()

  /** Checks if key has an allocated EID. */
  override def contains( key : TlaEx ) : Boolean = allocator.getID( key ) != -1

  /** Prints both the individual EIDs and equivalence classes to std. output. */
  override def print( ) : Unit = {
    println( "\n" + m_name + ": \n" )
    for ( i <- 0 until allocator.nextID() ) {
      println( EID( i ) + " -> " + allocator.getVal( i ) )
    }
    println( "\nEquivalence classes: \n" )
    for ( i <- 0 until eqClasses.size() ) {
      println( EID( i ) + " -> " + eqClasses.elementAt( i ) )
    }
  }


//  /**
//    * Alternative to get that does not use implicit conversion or Option. Returns
//    * EID( -1 ) if not allocated.
//    */
//  def getRaw( tlaEx : TlaEx ) : EquivalenceID = EID( allocator.getID( tlaEx ) )

//  /**
//    * Since EIDs and TLA expressions form a one to one correspondence, one can
//    * request the original expression from the EID. Note that this method always returns an
//    * unidentified copy.
//    */
//  def getEx( eid : EID_old ) : Option[TlaEx] = Option( allocator.getVal( eid.id ) ).map( _.deepCopy( identified = false ) )

  /**
    * TODO: MOVE TO SEPARATE PLUGIN
    */
  def processAll( spec : TlaSpec ) : Unit =
    SpecHandler.sideeffectWithExFun( spec, evaluateAndSave )

  /**
    * Returns a member from the equivalence class represented by eid, if it exists.
    */
  def getRep( eid : EID ) : Option[UID] =
    getEqClass( eid ).map( _.head )

  /**
    * Returns the whole equivalence class corresponding to the key, if it exists.
    */
  def getEqClass( eid : EID ) : Option[setT[UID]] = {
    val id = eid.id
    assert(id.isValidInt) // backward compatibility with integers ids
    if ( id < 0 || id >= eqClasses.size() ) None
    else Some( eqClasses.elementAt( id.toInt ) )
  }

//  /**
//    * Overload for UID.
//    */
//  def getEqClass( key : UniqueID ) : Option[setT[UniqueID]] = {
//    return UniqueDB.get( key ).map( x => getEqClass( getRaw( x ) ) ).getOrElse( None )
//  }
//
//  /**
//    * Overload for TlaEx.
//    */
//  def getEqClass( tlaEx : TlaEx ) : Option[Set[UID_old]] = {
//    return getEqClass( getRaw( tlaEx ) )
//  }

  /**
    * Clears the database. Note that this invalidates all previously allocated EIDs.
    */
  override def clear( ): Unit = {
    allocator.reset()
    eqClasses.clear()
  }

}
