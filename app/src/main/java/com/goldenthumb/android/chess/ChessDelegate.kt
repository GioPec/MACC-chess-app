package com.goldenthumb.android.chess

interface ChessDelegate {
    fun pieceAt(square: Square) : ChessPiece?
    fun movePiece(from: Square, to: Square)
    fun updateProgressBar(type: String, value: Integer)
    fun updateTurn(player: Player)
}