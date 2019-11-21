package at.forsyte.apalache.tla.types

/**
  * Instead of encoding record field names as strings in SMT, we instead opt to enumerate the
  * finite, and usually small, collection of record fields appearing in a specification and
  * use the fields' ids (integers) instead.
  * StringEnumerator keeps track of this enumeration.
  */
class StringEnumerator {

  private var idMap        : Map[String, Int] = Map.empty
  private var stringVector : Vector[String]   = Vector.empty

  /**
    * Attempts to add a new string to the enumeration.
    * If the string was already enumerated, returns the known ID, otheriwse
    * adds the string to the collection and returns its new ID.
    */
  def add( s : String ) : Int =
    stringToId( s ) getOrElse {
      val newId = stringVector.length
      stringVector = stringVector :+ s
      idMap += s -> newId
      newId
    }

  /**
    * Accessors
    */
  def idToString( i : Int ) : Option[String] =
    if ( 0 <= i && i < stringVector.length )
      Some( stringVector( i ) )
    else None

  def stringToId( s : String ) : Option[Int] = idMap.get( s )

  /**
    * Content collections
    */
  def allStringIds : Traversable[Int] = stringVector.indices

  def allStrings : Traversable[String] = stringVector
}
