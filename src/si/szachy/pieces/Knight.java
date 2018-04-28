package si.szachy.pieces;

import si.szachy.Chessboard;
import si.szachy.Coordinate;

public class Knight extends Piece {
    public Knight(Chessboard board, Coordinate coord, int owner) {
        super(board, coord, owner);
        name = "knight";
        setImage();
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
