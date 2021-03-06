package client.ui

import client.model.GameManager
import common.HexCoord
import common.HexMove
import javafx.animation.PathTransition
import javafx.beans.property.ObjectProperty
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.util.Duration
import tornadofx.*
import kotlin.math.cos


class BoardViewAdapter(
    private val gameManager: GameManager,
    private val availableColors: List<Color>,
    private val chosenColorProperty: ObjectProperty<Paint>
) {
    private val fields get() = gameManager.game.board.fields
    private val players get() = gameManager.game.players
    private val currentNumberOfPlayers get() = gameManager.game.players.count()
    private val corners get() = gameManager.game.corners
    private lateinit var fieldCircles: Map<HexCoord, Circle>
    private var chosenPawn: Pawn? = null
    var chosenMove: HexMove? = null
    private val highlightedCircles = mutableListOf<Circle>()
    private val cornersAndColors: Map<Int, Color>
    private val pawns = mutableListOf<Pawn>()

    init {
        gameManager.game.fillBoardCorners(corners)
        cornersAndColors = availableColors.sortedByDescending { it == chosenColorProperty.get() }
            .zip(corners.entries.sortedByDescending { it.key == gameManager.playerId })
            .associate { it.second.value to it.first }
    }

    private data class Pawn(var position: HexCoord, val circle: Circle, val color: Color)

    fun getBoard(): Pane {
        val pane = Pane()
        val circles = mutableMapOf<HexCoord, Circle>()
        pawns.clear()
        for ((position, field) in fields) {
            val c = Circle(15.0)
            c.addClass(Styles.unselectedField)
            setLocationOfCircle(c, position, pane)
            //c.setOnMouseClicked { event: MouseEvent -> onFieldClickedHandler(c,field); event.consume() }
            c.setOnMouseClicked { println(position.toString()) }
            circles[position] = c
            pane.add(c)
            field.piece?.let {
                val color = cornersAndColors.getValue(it.cornerId)
                val pawnCircle = Circle(15.0, color)
                pawnCircle.viewOrder = -1.0
                val pawn = Pawn(position, pawnCircle, color)
                setLocationOfCircle(pawnCircle, position, pane)
                pane.add(pawnCircle)
                pawns.add(pawn)
                if (it.cornerId == corners.getValue(gameManager.playerId)) {
                    pawnCircle.setOnMouseClicked { event ->
                        pawnClickedHandler(pawn)
                        event.consume()
                    }
                } else {
                    pawnCircle.isMouseTransparent = true
                }
            }
        }
        fieldCircles = circles
        pane.setOnMouseClicked { emptyClickedHandler() }
        pane.isDisable = true
        return pane
    }

    private fun setLocationOfCircle(c: Circle, hexCoord: HexCoord, parent: Pane) {
        c.layoutXProperty().bind(parent.widthProperty() / 2)
        c.layoutYProperty().bind(parent.heightProperty() / 2)
        c.translateY = -0.5 * (hexCoord.x + hexCoord.y) * 54
        c.translateX = -hexCoord.x * 17 * cos(60.0) + hexCoord.y * 17 * cos(60.0)
    }

    private fun emptyClickedHandler() {
        println("empty clicked handler called")
        chosenPawn?.let { (_, circle, _) -> circle.removeClass(Styles.selectedField) }
        chosenPawn = null
        chosenMove = null
        for (c in highlightedCircles) {
            c.removeClass(Styles.highlightedField)
            c.removeClass(Styles.chosenAsDestination)
            c.addClass(Styles.unselectedField)
            c.setOnMouseClicked { emptyClickedHandler(); it.consume() }
        }
        highlightedCircles.clear()
    }

    private fun pawnClickedHandler(pawn: Pawn) {
        emptyClickedHandler()
        chosenPawn = null
        chosenMove = null
        fields.getValue(pawn.position).piece?.let {
            chosenPawn = pawn
            pawn.circle.addClass(Styles.selectedField)
            gameManager.requestAvailableMoves(pawn.position)
        }
    }

    fun clearAllHighlights() {
        emptyClickedHandler()
    }

    fun highlightCircle(c: Circle) {
        c.addClass(Styles.highlightedField)
    }

    fun highlightPossibleMoves() {
        gameManager.possibleMoves?.map { fieldCircles.getValue(it.destination) to it }?.forEach { (circle, move) ->
            highlightedCircles.add(circle)
            highlightCircle(circle)

            circle.setOnMouseClicked { event ->
                println("highlighted clicked")
                chosenMove = move
                for (highlightedCircle in highlightedCircles) {
                    highlightedCircle.removeClass(Styles.chosenAsDestination)
                    //.addClass(Styles.highlightedField)
                }
                circle.addClass(Styles.chosenAsDestination)
                event.consume()
            }
        }
    }

    fun performMove(move: HexMove) {
        val path = Path()
        val movedPawn = pawns.first { it.position == move.origin }
        path.elements.add(MoveTo(movedPawn.circle.translateX, movedPawn.circle.translateY))
        path.elements.addAll(move.movements.map {
            LineTo(fieldCircles.getValue(it.second).translateX, fieldCircles.getValue(it.second).translateY)
        })
        val pathTransition = PathTransition(Duration(100.0 * path.elements.size), path, movedPawn.circle)
        pawns.firstOrNull { it.position == move.destination }?.let {
            val otherPath = Path()
            otherPath.elements.add(MoveTo(it.circle.translateX, it.circle.translateY))
            otherPath.elements.add(fieldCircles.getValue(move.origin).let { pos -> LineTo(pos.translateX, pos.translateY) })
            val otherPathTransition = PathTransition(Duration(100.0), otherPath, it.circle)
            it.position = move.origin
            otherPathTransition.play()
        }
        movedPawn.position = move.destination
        pathTransition.play()
        emptyClickedHandler()
    }
}
