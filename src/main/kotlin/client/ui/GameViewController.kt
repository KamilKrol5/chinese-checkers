package client.ui

import client.model.GameManager
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import tornadofx.*

class GameViewController : Controller() {
    private lateinit var gameManager: GameManager
    private lateinit var boardViewAdapter: BoardViewAdapter
    private val view: AppGameView by inject()

    internal fun initGameManager(gameManager: GameManager) {
        this.gameManager = gameManager
        boardViewAdapter = BoardViewAdapter(gameManager)
    }

    fun getBoard() : Pane {
        val pane = boardViewAdapter.getBoard(gameManager.playerId,this::fieldClickedHandler)
        pane.setOnMouseClicked { emptyClickedHandler(it) }
        pane.prefWidthProperty().bind(view.root.widthProperty())
        pane.prefHeightProperty().bind(view.root.heightProperty())
        return pane
    }

    private fun emptyClickedHandler(event: MouseEvent) {
        val pane = event.target as Pane
        pane.children.filterIsInstance<Circle>().forEach { c ->
            c.style {
                fill = c("603BB7")
            }
        }
    }


    private fun fieldClickedHandler(node: Node,cord: common.HexCoord) {
        if (node is Circle) {
//            node.fillProperty().set(c("cyan"))
            node.style(append = true) {
                strokeWidth = 5.px
                stroke = c("black")
                fill = (node.fill as Color).deriveColor(0.0, 1.0, 1.5, 1.0)
            }
            println(cord.toString())
        }
    }
}