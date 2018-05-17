package si.szachy.pieces;

import si.szachy.Chessboard;
import si.szachy.Coordinate;

import java.util.List;

public class King extends Piece {
    public King(Chessboard board, Coordinate coord, int owner) {
        super(board, coord, owner, 900);
        name = "king";
        number = 11;
        evaluation = new double[][] {
                { -3.0, -4.0, -4.0, -5.0, -5.0, -4.0, -4.0, -3.0},
                { -3.0, -4.0, -4.0, -5.0, -5.0, -4.0, -4.0, -3.0},
                { -3.0, -4.0, -4.0, -5.0, -5.0, -4.0, -4.0, -3.0},
                { -3.0, -4.0, -4.0, -5.0, -5.0, -4.0, -4.0, -3.0},
                { -2.0, -3.0, -3.0, -4.0, -4.0, -3.0, -3.0, -2.0},
                { -1.0, -2.0, -2.0, -2.0, -2.0, -2.0, -2.0, -1.0},
                {  2.0,  2.0,  0.0,  0.0,  0.0,  0.0,  2.0,  2.0 },
                {  2.0,  3.0,  1.0,  0.0,  0.0,  1.0,  3.0,  2.0 }};

        setImage();
    }

    @Override
    public King createCopy(Chessboard b){
        return new King(b, new Coordinate(coord.x, coord.y), owner);
    }

    public boolean leftCastling(){
        if(didMove || isInDanger())
            return false;

        //to left
        Coordinate c = this.coord;
        Piece supposedRook = board.peek(0, c.y);
        if(supposedRook == null || supposedRook.didMove || supposedRook.getClass() != Rook.class)
            return false;

        for(int x = c.x - 1; x > 0; x--){
            if(board.peek(x, c.y) != null)
                return false;
        }

        return !wouldKingBeInDanger(new Coordinate(c.x - 2, c.y));
    }

    public boolean rightCastling(){
        if(didMove || isInDanger())
            return false;

        //to right
        Coordinate c = this.coord;
        Piece supposedRook = board.peek(board.getWidth() - 1, c.y);
        if(supposedRook == null || supposedRook.didMove || supposedRook.getClass() != Rook.class)
            return false;

        for(int x = c.x + 1; x < board.getWidth() - 1; x++){
            if(board.peek(x, c.y) != null)
                return false;
        }

        return !wouldKingBeInDanger(new Coordinate(c.x + 2, c.y));
    }

    @Override
    protected boolean pieceMovement(int x, int y) {
        if (x == this.getX() - 2 && y == this.getY()) return leftCastling();
        if (x == this.getX() + 2 && y == this.getY()) return rightCastling();
        if (!coord.isValid(x, y)) return false;
        if (x == this.getX() && this.getY() == y) return false;
        if (isFieldDangerous(x, y)) return false;
        return x <= getX() + 1 && y <= getY() + 1 && x >= getX() - 1 && y >= getY() - 1;
    }
}
