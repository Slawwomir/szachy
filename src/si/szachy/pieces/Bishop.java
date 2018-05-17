package si.szachy.pieces;

import si.szachy.Chessboard;
import si.szachy.Coordinate;

import java.awt.*;
import java.sql.Array;

public class Bishop extends Piece {
    public Bishop(Chessboard board, Coordinate coord, int owner) {
        super(board, coord, owner, 30);
        name = "bishop";
        number = 5;
        evaluation = new double[][]  {
                { -2.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -2.0},
                { -1.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -1.0},
                { -1.0,  0.0,  0.5,  1.0,  1.0,  0.5,  0.0, -1.0},
                { -1.0,  0.5,  0.5,  1.0,  1.0,  0.5,  0.5, -1.0},
                { -1.0,  0.0,  1.0,  1.0,  1.0,  1.0,  0.0, -1.0},
                { -1.0,  1.0,  1.0,  1.0,  1.0,  1.0,  1.0, -1.0},
                { -1.0,  0.5,  0.0,  0.0,  0.0,  0.0,  0.5, -1.0},
                { -2.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -2.0}};
        setImage();
    }

    @Override
    public Bishop createCopy(Chessboard b){
        return new Bishop(b, new Coordinate(coord.x, coord.y), owner);
    }

    @Override
    protected boolean pieceMovement(int x, int y) {
        if (!coord.isValid(x, y)) return false;
        if (x == this.getX() && this.getY() == y) return false;
        return Math.abs(x - this.getX()) == Math.abs(y - this.getY());
    }
}
