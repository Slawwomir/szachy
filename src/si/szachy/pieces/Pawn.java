package si.szachy.pieces;

import si.szachy.Chessboard;
import si.szachy.Coordinate;

public class Pawn extends Piece {
    private int rotation = 1;
    public Pawn(Chessboard board, Coordinate coord, int owner) {
        this(board, coord, owner, false);
    }

    @Override
    public Pawn createCopy(Chessboard b){
        return new Pawn(b, new Coordinate(coord.x, coord.y), owner);
    }

    public Pawn(Chessboard board, Coordinate coord, int owner, boolean rotated) {
        super(board, coord, owner, 10);
        name = "pawn";
        setImage();
        number = 1;
        evaluation = new double[][] {
                {0,  0,  0,  0,  0,  0,  0,  0},
                {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0},
                {1.0, 1.0, 2.0, 3.0, 3.0, 2.0, 1.0, 1.0},
                {0.5,  0.5, 1.0, 2.5, 2.5, 1.0,  0.5,  0.5},
                {0,  0,  0, 2.0, 2.0,  0,  0,  0},
                {0.5, -0.5,-1.0,  0,  0,-1.0, -0.5,  0.5},
                {0.5, 1.0, 1.0,-2.0,-2.0, 1.0, 1.0,  0.5},
                {0,  0,  0,  0,  0,  0,  0,  0}};

        if (rotated) rotation = -1;

    }
    // TODO: przejrzec to, moga byc tu bugi
    @Override
    protected boolean pieceMovement(int x, int y) {
        if (!coord.isValid(x, y)) return false;
        if (x == this.getX() && this.getY() == y) return false;
        int direction = owner == 0 ? -1 : 1;
        direction *= rotation;
        // 2 kroki na poczatek
        if(didMove == false && (getY() == 1 || getY() == 6) && y == getY()+direction*2 && board.peek(x,y) == null) return true;
        // bicie po skosach
        if (y == (getY() + direction) && board.peek(x,y) != null && board.peek(x,y).getOwner() != owner && (x==getX()-1 || x== getX()+1) && x!=getX()) return true;
        // nie bicie po prostej
        if (board.peek(x,y) != null) return false;
        // zwykly ruch
        return (y == (getY() + direction)) && x == getX();
    }

}
