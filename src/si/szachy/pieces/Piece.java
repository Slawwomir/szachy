package si.szachy.pieces;

import si.szachy.Chessboard;
import si.szachy.Coordinate;

import java.awt.*;
import java.util.ArrayList;

abstract public class Piece {
    private double value;
    protected double[][] evaluation;
    protected Chessboard board;
    protected Coordinate coord;
    protected int owner;
    protected int number;
    protected Image image;
    protected String name;
    public boolean isAlive = true;
    public boolean didMove = false;
    ArrayList<Coordinate> validMoves;

    public Piece(Chessboard b, Coordinate c, int o, double value) {
        this.board = b;
        this.coord = c;
        this.owner = o;
        this.value = value;
        validMoves = new ArrayList<>();
    }

    public abstract Piece createCopy(Chessboard b);

    public String getName() {
        return name;
    }

    public boolean isFieldDangerous(int x, int y) {
        Coordinate old = coord;
        this.move(x,y);
        board.updateChessboard();
        ArrayList<Piece> pieces = board.getPieces();
        for (Piece p : pieces) {
            if(p.getClass() != King.class) {
                if (p.getOwner() != owner && p.canReach(x, y)) {
                    this.move(old.x, old.y);
                    board.updateChessboard();
                    return true;
                }
            }
            else if(p != this && p.owner != owner){
                if((Math.abs(x - p.getX()) == 1 && Math.abs(y - p.getY()) == 1) || (Math.abs(x - p.getX()) == 0 && Math.abs(y - p.getY()) == 1) || (Math.abs(x - p.getX()) == 1 && Math.abs(y - p.getY()) == 0)) {
                    this.move(old.x, old.y);
                    board.updateChessboard();
                    return true;
                }
            }
        }
        this.move(old.x, old.y);
        board.updateChessboard();
        return false;
    }

    public boolean isInDanger() {
        ArrayList<Piece> pieces = board.getPieces();
        for (Piece p : pieces) {
            if (p.canReach(getX(), getY()))
                return true;
        }
        return false;
    }

    public Image getImage() {
        return image;
    }

    public void die() {
        this.isAlive = false;
        board.removePiece(this);
    }

    public Coordinate getCoord() {
        return coord;
    }

    public int getNumber() { return number; }

    public int getX() {
        return coord.x;
    }

    public int getY() {
        return coord.y;
    }

    public void setCoord(Coordinate c) {
        this.coord = c;
    }

    public double getValue(){
        int blackX = board.getWidth() - coord.x - 1, blackY = board.getHeight() - coord.y - 1;
        int whiteX = coord.x, whiteY = coord.y;
        double eval = (owner == 1 ? evaluation[blackY][blackX] : evaluation[whiteY][whiteX]);
        return value + eval;
    }

    // TODO: do poprawy calosc
    // TODO: dodac bicie
    // test2
    public boolean wouldKingBeInDanger(Coordinate c) {
        Coordinate oldCoord = getCoord();
        boolean test;
        if(board.peek(c) != null && board.peek(c).owner != owner){
            Piece piece = board.peek(c);
            board.removePiece(piece);
            board.updateChessboard();
            test = board.kings[owner].isInDanger();
            board.addPiece(piece);
        }
        else {
            this.setCoord(c);
            board.updateChessboard();
            test = board.kings[owner].isInDanger();
            this.setCoord(oldCoord);
        }
        board.updateChessboard();
        return test;
    }

    protected void setImage() {
        Toolkit t = Toolkit.getDefaultToolkit();
        String path = "./src/si/szachy/images/";
        path = path + name + owner + ".png";
        image = t.getImage(path);
    }

    abstract protected boolean pieceMovement(int x, int y);

    public boolean isValidMove(int x, int y) {
        ArrayList<Coordinate> list = getAllValidMoves();
        for (Coordinate c : list) {
            if (c.x == x && c.y == y) return true;
        }
        return false;
    }

    public boolean canReach(int x, int y) {
        if (pieceMovement(x, y) == false) return false;
        int xDir, yDir, xTemp = getX(), yTemp = getY();
        xDir = x - this.getX() > 0 ? 1 : -1;
        xDir = x - this.getX() == 0 ? 0 : xDir;
        yDir = y - this.getY() > 0 ? 1 : -1;
        yDir = y - this.getY() == 0 ? 0 : yDir;
        while (xTemp != x || yTemp != y) {
            xTemp += xDir;
            yTemp += yDir;
            if (pieceMovement(xTemp, yTemp) == false) return false;
            if (board.peek(xTemp, yTemp) != null && (xTemp != x || yTemp != y)) return false;
        }

        if (board.peek(x, y) != null && board.peek(x, y).getOwner() == getOwner()) return false;
        return (xTemp == x && yTemp == y);
    }

    public int getOwner() {
        return owner;
    }

    public void move(int x, int y) {
        setCoord(new Coordinate(x, y));
    }

    public ArrayList<Coordinate> getAllValidMoves() {
        validMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (canReach(i, j)) {
                    if (wouldKingBeInDanger(new Coordinate(i, j)) == false)
                        validMoves.add(new Coordinate(i, j));
                }
            }
        }
        return validMoves;
    }
}
