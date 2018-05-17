package si.szachy.pieces;

import si.szachy.Chessboard;
import si.szachy.Coordinate;

public class Knight extends Piece {
    public Knight(Chessboard board, Coordinate coord, int owner) {
        super(board, coord, owner, 30);
        name = "knight";
        number = 3;
        evaluation = new double[][] {
                {-5.0, -4.0, -3.0, -3.0, -3.0, -3.0, -4.0, -5.0},
                {-4.0, -2.0,  0.0,  0.0,  0.0,  0.0, -2.0, -4.0},
                {-3.0,  0.0,  1.0,  1.5,  1.5,  1.0,  0.0, -3.0},
                {-3.0,  0.5,  1.5,  2.0,  2.0,  1.5,  0.5, -3.0},
                {-3.0,  0.0,  1.5,  2.0,  2.0,  1.5,  0.0, -3.0},
                {-3.0,  0.5,  1.0,  1.5,  1.5,  1.0,  0.5, -3.0},
                {-4.0, -2.0,  0.0,  0.5,  0.5,  0.0, -2.0, -4.0},
                {-5.0, -4.0, -3.0, -3.0, -3.0, -3.0, -4.0, -5.0}
        };
        setImage();
    }

    @Override
    public Knight createCopy(Chessboard b){
        return new Knight(b, new Coordinate(coord.x, coord.y), owner);
    }

    @Override
    protected boolean pieceMovement(int x, int y) {
        if (!coord.isValid(x, y)) return false;
        if (x == this.getX() && this.getY() == y) return false;
        int curX = getX(), curY = getY();
        return (x == curX + 2 && y == curY - 1) ||
                (x == curX - 2 && y == curY - 1) ||
                (x == curX - 2 && y == curY + 1) ||
                (x == curX + 1 && y == curY + 2) ||
                (x == curX - 1 && y == curY + 2) ||
                (x == curX + 1 && y == curY - 2) ||
                (x == curX - 1 && y == curY - 2) ||
                (x == curX + 2 && y == curY + 1);
    }

    @Override
    public boolean canReach(int x, int y) {
        if(board.peek(x,y) != null && board.peek(x,y).getOwner() == getOwner()) return false;
        return pieceMovement(x, y);
    }
}
