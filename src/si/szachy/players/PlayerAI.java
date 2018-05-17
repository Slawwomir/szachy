package si.szachy.players;

import org.jetbrains.annotations.NotNull;
import si.szachy.Chessboard;
import si.szachy.Coordinate;
import si.szachy.pieces.Piece;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class PlayerAI extends Player {
    private int DEPTH;
    private ArrayList<tuple<Piece, Coordinate>> moveStack = new ArrayList<>();

    public PlayerAI(Chessboard board, int playerTeam) {
        super(board, playerTeam);
        DEPTH = 2;
    }

    public PlayerAI(Chessboard board, int depth, int playerTeam) {
        super(board, playerTeam);
        DEPTH = depth;
    }

    public void performMove() {
        updateList();
        move();
    }

    private boolean preventLooping(tuple<Piece, Coordinate> m) {
        int threshold = 1;
        int counter = 0;
        for (tuple<Piece, Coordinate> t : moveStack) {
            boolean positionTest = t.value.getX() == m.value.getX() && t.value.getY() == m.value.getY();
            boolean pieceTest = t.key == m.key;
            if (positionTest && pieceTest) counter++;
        }
        return counter >= threshold;
    }

    private void move() {
        List<triple<Piece, Coordinate, Future<Double>>> moves = new ArrayList<>();
        List<tuple<Piece, Coordinate>> bestMoves = new ArrayList<>();
        Piece toMove = null;
        Coordinate destination = null;
        Double bestValue = -99999999.0;
        tuple<Piece, Coordinate> test = new tuple<>(null, null);
        if (moveStack.size() > 20) moveStack.clear();

        for (Piece p : playerPieces) {
            List<Coordinate> possibleMoves = p.getAllValidMoves();
            if (!possibleMoves.isEmpty()) {
                moves.addAll(findBestMove(p, possibleMoves));
            }
        }

        for (triple<Piece, Coordinate, Future<Double>> move : moves) {
            try {
                if (move.ext.get() >= bestValue) {
                    test.key = move.key;
                    test.value = move.value;
                    if (!preventLooping(test)) {
                        if (move.ext.get() > bestValue) {
                            bestMoves.clear();
                            bestValue = move.ext.get();
                        }
                        toMove = move.key;
                        destination = move.value;
                        bestMoves.add(new tuple<>(toMove, destination));
                    } else bestValue -= 100;
                }
            } catch (Exception e) {
            }
        }

        if (toMove != null) {
            Random generator = new Random();
            int choice = 0;
            if (bestMoves.size() > 1)
                generator.nextInt(bestMoves.size() - 1);
            toMove = bestMoves.get(choice).key;
            destination = bestMoves.get(choice).value;
            moveStack.add(bestMoves.get(choice));
            if (board.peek(destination) != null && board.peek(destination).getOwner() != toMove.getOwner()) {
                board.peek(destination).die();
            }
            toMove.setCoord(destination);
        }
    }

    private List<triple<Piece, Coordinate, Future<Double>>> findBestMove(Piece p, @NotNull List<Coordinate> possibleMoves) {
        Double bestValue = -999999.0;
        Double actualValue = 0.0;
        Coordinate bestMove = possibleMoves.get(0);

        List<triple<Piece, Coordinate, Future<Double>>> valuesForeachMove = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(possibleMoves.size());

        for (Coordinate c : possibleMoves) {
            Piece at = board.peek(c);
            Coordinate prev = p.getCoord();

            if (at != null) {
                at.die();
            }

            p.setCoord(c);
            board.setField(c.getX(), c.getY(), p);
            board.setField(prev.x, prev.y, null);

            Chessboard copyBoard = new Chessboard();
            for (Piece piece : board.getPieces()) {
                copyBoard.addPiece(piece.createCopy(copyBoard));
            }
            MiniMax miniMax = new MiniMax(DEPTH, (playerTeam + 1) % 2, playerTeam, copyBoard);
            valuesForeachMove.add(new triple<>(p, c, executorService.submit(miniMax)));

            p.setCoord(prev);
            if (at != null) {
                at.isAlive = true;
                board.addPiece(at);
            }
            board.setField(c.getX(), c.getY(), at);
            board.setField(prev.x, prev.y, p);
        }

        return valuesForeachMove;
    }

    // TODO: to tez jest malo optymalne ale nie chce mi sie myslec
    public int getMoveCount() {
        updateList();
        int count = 0;
        for (Piece p : playerPieces)
            count += p.getAllValidMoves().size();
        return count;
    }
}

class tuple<K, V> {
    K key;
    V value;

    tuple(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

class triple<K, V, E> {
    K key;
    V value;
    E ext;

    triple(K key, V value, E ext) {
        this.key = key;
        this.value = value;
        this.ext = ext;
    }

    public boolean contains(Piece p){
        return key == p;
    }
}

class MiniMax implements Callable<Double> {

    private Chessboard board;
    private int depth;
    private int actualPlayer;
    private int owner;
    private static final double alpha = -999999.0;
    private static final double beta = 999999.0;
    private List<ArrayList<triple<Piece, Coordinate, Coordinate>>> killerMoves = new ArrayList<ArrayList<triple<Piece, Coordinate,Coordinate>>>();

    MiniMax(int depth, int firstPlayer, int owner, Chessboard board) {
        this.depth = depth;
        this.board = board;
        actualPlayer = firstPlayer;
        this.owner = owner;
        for(int i = 0; i <= depth; i++)
            killerMoves.add(new ArrayList<>());
    }

    @Override
    public Double call() {
        return evaluateMoves(depth, alpha, beta, actualPlayer);
    }

    private double evaluateMoves(int depth, double alpha, double beta, int actualPlayer) {
        if (depth == 0)
            return evaluateBoard();

        double minmax = actualPlayer == owner ? MiniMax.alpha : MiniMax.beta;
        List<Piece> pieces = new ArrayList<>(board.getPieces());
        boolean checkKillers = false;

        for(triple<Piece, Coordinate, Coordinate> killer : killerMoves.get(depth)){
            if(pieces.contains(killer.key) && killer.key.getX() == killer.value.x && killer.key.getY() == killer.value.y){
                pieces.remove(killer.key);
                pieces.add(0, killer.key);
                checkKillers = true;
            }
        }


        int counter = 0;
        for (Piece p : pieces) {
            if (p.isAlive) {
                List<Coordinate> coordinates = p.getAllValidMoves();

                if(checkKillers && counter < killerMoves.get(depth).size()){
                    Coordinate coord = killerMoves.get(depth).get(killerMoves.get(depth).size() - counter - 1).ext;
                    counter++;
                    int co = 0;
                    for(Coordinate c: coordinates){
                        if(c.x == coord.x && c.y == coord.y){
                            break;
                        }
                        co++;
                    }
                    coordinates.add(0, coord);
                }

                for (Coordinate destination : coordinates) {
                    Piece opponent = board.peek(destination);
                    Coordinate previousCoords = p.getCoord();

                    //MAKE MOVE/////////////////////////////////////////////////////////////////////////////////////////
                    if (opponent != null) opponent.die();

                    p.setCoord(destination);
                    board.setField(destination.x, destination.y, p);
                    board.setField(previousCoords.x, previousCoords.y, null);
                    ////////////////////////////////////////////////////////////////////////////////////////////////////

                    if (actualPlayer == owner) {
                        minmax = Math.max(minmax, evaluateMoves(depth - 1, alpha, beta, (actualPlayer + 1) % 2));
                        alpha = Math.max(minmax, alpha);
                    } else {
                        minmax = Math.min(minmax, evaluateMoves(depth - 1, alpha, beta, (actualPlayer + 1) % 2));
                        beta = Math.min(minmax, beta);
                    }

                    //UNDO MOVE/////////////////////////////////////////////////////////////////////////////////////////

                    p.setCoord(previousCoords);

                    if (opponent != null) board.wake(opponent);

                    board.setField(destination.getX(), destination.getY(), opponent);
                    board.setField(p.getX(), p.getY(), p);

                    ////////////////////////////////////////////////////////////////////////////////////////////////////

                    if (alpha >= beta) { //Alpha-beta pruning
                        if(killerMoves.get(depth).size() > 3)
                            killerMoves.get(depth).remove(3);
                        boolean isIn = false;
                        for(triple trp : killerMoves.get(depth)){
                            if(trp.contains(p)) {
                                isIn = true;
                                break;
                            }
                        }

                        if(!isIn)
                            killerMoves.get(depth).add(new triple<>(p, p.getCoord(), destination));
                        return minmax;
                    }
                }
            }
        }
        return minmax;
    }

    private double evaluateBoard() {
        double value = 0;
        for (Piece p : board.getPieces()) {
            if (p.getOwner() == owner)
                value += p.getValue();
            else
                value -= p.getValue();
        }
        return value;
    }
}

class Zobrist {
    private long[][] array = new long[64][12];
    private int[] board = new int[64];
    public static final int whitePawn =     1;
    public static final int blackPawn =     2;
    public static final int whiteKnight =   3;
    public static final int blackKnight =   4;
    public static final int whiteBishop =   5;
    public static final int blackBishop =   6;
    public static final int whiteRook =     7;
    public static final int blackRook =     8;
    public static final int whiteQueen =    9;
    public static final int blackQueen =    10;
    public static final int whiteKing =     11;
    public static final int blackKing =     12;
    public static final int empty =         0;
    public static final int nothing =      -1;

    private long hash;

    public Zobrist(Chessboard board){
        this.board = convert(board);
        for(int i = 0; i < 64; i++)
            array[i] = new Random().longs(12).toArray();
    }

    public void updateHash(){
        for (int i = 0; i < 64; i++)
            if (board[i] != empty) {
                int piece = (int)board[i];
                hash ^= array[i][piece];
            }
    }

    public long getHash() {
        updateHash();
        return hash;
    }

    public int[] convert(Chessboard board){
        int[] b = new int[64];

        for(Piece p: board.getPieces()){
            b[8*p.getY() + p.getX()] = p.getNumber() + p.getOwner();
        }

        return b;
    }

    public void move(Piece p, Coordinate destination){
        int from = p.getY() * 8 + p.getX();
        int to = destination.y * 8 + destination.x;
        int piece = p.getNumber() + p.getOwner();
        int victim = board[to];

        hash ^= array[from][piece];
        hash ^= array[to][victim];
        hash ^= array[to][piece];

        board[to] = piece;
        board[from] = empty;
    }

}

enum HashFlag {
    HASH_EXACT,
    HASH_BETA,      //Evaluation caused cut-off
    HASH_ALPHA
}

class HashPoint {
    long zobristKey;
    int depth;
    int evaluation;
    HashFlag flag;
    tuple<Piece, Coordinate> bestMove;

    public HashPoint(long zobristKey, int depth, int evaluation, HashFlag flag, tuple<Piece, Coordinate> bestMove) {
        this.zobristKey = zobristKey;
        this.depth = depth;
        this.evaluation = evaluation;
        this.flag = flag;
        this.bestMove = bestMove;
    }
}