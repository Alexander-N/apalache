package at.forsyte.apalache.tla.bmcmt.analyses

import at.forsyte.apalache.tla.bmcmt.{ArenaCell, Binding, CellTheory, SolverContext, SymbState, SymbStateRewriterImpl}
import at.forsyte.apalache.tla.lir.{OperEx, TlaEx}
import at.forsyte.apalache.tla.lir.oper.{TlaOper, TlaSetOper}
import at.forsyte.apalache.tla.lir.convenience.tla

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//TODO (Viktor): write unit-tests
class LoopAnalyser(val nextTransitions: List[TlaEx],
                   val liveness: TlaEx,
                   val enabledActionHintTuples: List[(TlaEx, TlaEx)],
                   var stateStack: List[(SymbState, ArenaCell)],
                   val rewriter: SymbStateRewriterImpl,
                   val solverContext: SolverContext) {

  def findAllLoops: List[Int] = {
    def setActionForLastState(action: TlaEx): SymbState = {
      var last = stateStack.head
      val state = last._1.setRex(action)
      last = (state, last._2)
      stateStack = last :: stateStack.tail

      state
    }

    def checkForLoopWithAction(stateNumber: Int, lastState: SymbState): Boolean = {
      def checkLoopTransition(last: SymbState) = {
        rewriter.push()
        val ex = rewriter.rewriteUntilDone(last.setTheory(CellTheory())).ex
        solverContext.assertGroundExpr(ex)
        val result = solverContext.sat()
        rewriter.pop()
        result
      }

      val lastWithPrimedBinding = addPrimedBinding(lastState, stateStack(stateNumber)._1)
      checkLoopTransition(lastWithPrimedBinding)
    }

    val next = nextTransitions.map { it => convertToEquality(it) }

    val loopStartIndexes = mutable.SortedSet[Int]()
    for (i <- next.indices) {
      val last = setActionForLastState(next(i))

      for (j <- 0 until stateStack.size - 1) {
        if (checkForLoopWithAction(j, last)) {
          val tuple = j
          loopStartIndexes += tuple
        }
      }
    }

    loopStartIndexes.toList
  }

  private def convertToEquality(ex: TlaEx): TlaEx = ex match {
    case OperEx(TlaSetOper.in, arg1, OperEx(TlaSetOper.enumSet, arg)) =>
      OperEx(TlaOper.eq, arg1, arg)
    case OperEx(oper, args@_*) => OperEx(oper, args.map(it => convertToEquality(it)): _*)
    case it => it
  }

  def validateLiveness(loopStartIndexes: List[Int]): List[Int] = {
    val notLoopInvariant = tla.not(liveness)

    val counterExamples = ListBuffer[Int]()

    for (i <- loopStartIndexes.indices) {
      val startIndex = loopStartIndexes(i)

      rewriter.push()

      var j = 0
      var lastState = stateStack.head._1
      while (j <= startIndex) {
        val requiredBinding = stateStack(j)._1.binding
        val state = lastState.setBinding(requiredBinding).setRex(notLoopInvariant)
        lastState = rewriter.rewriteUntilDone(state.setTheory(CellTheory()))
        solverContext.assertGroundExpr(lastState.ex)

        j += 1
      }
      val result = solverContext.sat()

      if (result) {
        counterExamples += startIndex
      }

      rewriter.pop()
    }

    counterExamples.toList
  }

  def checkFairnessOfCounterExamples(counterExampleLoopStartIndexes: List[Int]): List[Int] = {
    def filterByEnabledActions(counterExampleLoopStartIndexes: List[Int]): List[Int] = {
      val filteredCounterExamples = ListBuffer[Int]()

      val weakFairnessConjunction = tla.and(enabledActionHintTuples.map(it => it._2): _*)
      for (startIndex <- counterExampleLoopStartIndexes) {
        rewriter.push()

        var j = 0
        var lastState = stateStack.head._1
        while (j <= startIndex) {
          val requiredBinding = stateStack(j)._1.binding
          val state = lastState.setBinding(requiredBinding).setRex(weakFairnessConjunction)
          lastState = rewriter.rewriteUntilDone(state.setTheory(CellTheory()))
          solverContext.assertGroundExpr(lastState.ex)

          j += 1
        }
        val result = solverContext.sat()
        if (result) {
          filteredCounterExamples += startIndex
        }

        rewriter.pop()
      }

      filteredCounterExamples.toList
    }

    def filterByTakenActions(counterExampleLoopStartIndexes: List[Int]): List[Int] = {

      val filteredCounterExamples = ListBuffer[Int]()

      val next = enabledActionHintTuples.map(it => it._1).map( it => convertToEquality(it))
      var takenCounter = 0
      for (startIndex <- counterExampleLoopStartIndexes) {

        var j = 0
        var lastState = stateStack.head._1
        var taken = false
        while (j <= startIndex && !taken) {
          taken = false
          for (toBeTakenNext <- next) {
            rewriter.push()

            val requiredBinding = stateStack(j)._1.binding
            val state = addPrimedBinding(
              lastState.setBinding(requiredBinding),
              stateStack(if (j - 1 >= 0) j - 1 else 0)._1
              ).setRex(toBeTakenNext)
            lastState = rewriter.rewriteUntilDone(state.setTheory(CellTheory()))
            solverContext.assertGroundExpr(lastState.ex)
            j += 1

            taken = solverContext.sat()

            if (taken) {
              takenCounter += 1
            }

            rewriter.pop()
          }
        }

        if (takenCounter == next.size) {
          filteredCounterExamples += startIndex
        }
      }

      filteredCounterExamples.toList
    }

    val filteredByEnabled = filterByEnabledActions(counterExampleLoopStartIndexes)
    filterByTakenActions(filteredByEnabled)
  }

  def addPrimedBinding(state: SymbState, selected: SymbState): SymbState = {
    val selectedBinding = selected.binding
    val stateBinding = state.binding
    val stateWithBinding = state
                           .setBinding(Binding(stateBinding
                                               .merged(selectedBinding.map { t => (t._1 + "'", t._2) })((k, _) => k)))

    stateWithBinding
  }
}