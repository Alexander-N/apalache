package at.forsyte.apalache.tla.bmcmt

import at.forsyte.apalache.tla.bmcmt.types.{CellType, UnknownType}

import scala.collection.immutable.HashMap

object Arena {
  def create(): Arena = {
    new Arena(0, new ArenaCell(-1, UnknownType()), new HashMap())
  }
}

/**
  * A memory arena represents a memory layout. The arena is dynamically populated, when new objects are created.
  * Currently, an arena is a directed acyclic graph, where edges are pointing from a container object
  * to the associated cells, e.g., a set cell points to the cells that store its elements.
  *
  * @author Igor Konnov
  */
class Arena private(val cellCount: Int,
                    val topCell: ArenaCell,
                    private val hasEdges: Map[ArenaCell, List[ArenaCell]]) {
  // since the edges in arenas have different structure, for the moment, we keep them in different maps
  /*
    private val domEdges: Map[ArenaCell, ArenaCell] = new HashMap[ArenaCell, ArenaCell]
    private val codomEdges: Map[ArenaCell, ArenaCell] = new HashMap[ArenaCell, ArenaCell]
  */

  /**
    * Append a new cell to arena. This method returns a new arena, not the new cell.
    * The new cell can be accessed with topCell.
    *
    * @param cellType a cell type
    * @return new arena
    */
  def appendCell(cellType: CellType): Arena = {
    val newCell = new ArenaCell(cellCount, cellType)
    new Arena(cellCount + 1, newCell, hasEdges)
  }

  /**
    * Append a 'has' edge to connect a cell that corresponds to a set with a cell that corresponds to its element.
    *
    * @param setCell  a set cell
    * @param elemCell an element cell
    * @return a new arena
    */
  def appendHas(setCell: ArenaCell, elemCell: ArenaCell): Arena = {
    val es =
      hasEdges.get(setCell) match {
        case Some(list) => list :+ elemCell
        case None => List(elemCell)
      }

    new Arena(cellCount, topCell, hasEdges + (setCell -> es))
  }

  /**
    * Get all the edges that are labelled with 'has'.
    *
    * @param setCell a set cell
    * @return all element cells that were added with appendHas, or an empty list, if none were added
    */
  def getHas(setCell: ArenaCell): List[ArenaCell] = {
    hasEdges.get(setCell) match {
      case Some(list) => list
      case None => List()
    }
  }
}
