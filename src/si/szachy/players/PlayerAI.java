package si.szachy.players;

import org.jetbrains.annotations.NotNull;
import si.szachy.Chessboard;
import si.szachy.Coordinate;
import si.szachy.pieces.Piece;

import java.lang.reflect.Array;
import java.util.*;
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
        Zobrist.initialize();
        DEPTH = 2;
    }

    public PlayerAI(Chessboard board, int depth, int playerTeam) {
        super(board, playerTeam);
        Zobrist.initialize();
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
                e.printStackTrace();
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

    private final int hashRange = 1 << 20;

    private Chessboard board;
    private int depth;
    private int actualPlayer;
    private int owner;
    private int licznik0 = 0;
    private int licznik1 = 0;
    private static final double alpha = -999999.0;
    private static final double beta = 999999.0;
    private List<ArrayList<triple<Piece, Coordinate, Coordinate>>> killerMoves = new ArrayList<>();
    private HashMap<Integer, HashPoint> hashes = new HashMap<>();
    private Zobrist zobrist;

    //private tuple<Piece, Coordinate> bestMove;

    MiniMax(int depth, int firstPlayer, int owner, Chessboard board) {
        this.depth = depth;
        this.board = board;
        actualPlayer = firstPlayer;
        this.owner = owner;
        this.zobrist = new Zobrist(board);
        for(int i = 0; i <= depth; i++)
            killerMoves.add(new ArrayList<>());
    }

    @Override
    public Double call() {

        int actual = 3;
        while(actual < depth){
            evaluateMoves(actual, alpha, beta, actualPlayer);
            actual+=2;
        }

licznik0 = licznik1 = 0;
double x =  evaluateMoves(depth, alpha, beta, actualPlayer);
return x;
    }

    private double evaluateMoves(int depth, double alpha, double beta, int actualPlayer) {
        Double evaluation;
        licznik0++;
        if((evaluation = checkHash(zobrist.getHash(), alpha, beta, depth)) != null) {
            licznik1++;
            return evaluation;
        }

        if (depth == 0) {
            evaluation = evaluateBoard();
            addHash(new HashPoint(zobrist.getHash(), depth, evaluation, HashFlag.HASH_EXACT, null));
            return evaluation;
        }

        double minmax = actualPlayer == owner ? MiniMax.alpha : MiniMax.beta;
        List<Piece> pieces = new ArrayList<>(board.getPieces(actualPlayer));
        boolean checkKillers = false;

        tuple<Piece, Coordinate> addAtBeginning = getBestMove(zobrist.getHash());
        boolean setCoordAtBeginning = false;
        if(addAtBeginning != null){
            if(pieces.contains(addAtBeginning.key)){
                pieces.remove(addAtBeginning.key);
                pieces.add(0, addAtBeginning.key);
                setCoordAtBeginning = true;
            }
        }

        for(triple<Piece, Coordinate, Coordinate> killer : killerMoves.get(depth)){
            if(pieces.contains(killer.key) && killer.key.getX() == killer.value.x && killer.key.getY() == killer.value.y){
                pieces.remove(killer.key);
                pieces.add(0, killer.key);
                checkKillers = true;
            }
        }

        tuple<Piece, Coordinate> bestMove = null;

        HashFlag flag = HashFlag.HASH_ALPHA;
        int counter = 0;
        for (Piece p : pieces) {
            if (p.isAlive) {
                List<Coordinate> coordinates = p.getAllValidMoves();

                if(checkKillers && counter < killerMoves.get(depth).size() && killerMoves.get(depth).get(killerMoves.get(depth).size() - counter - 1).key == p){
                    Coordinate coord = killerMoves.get(depth).get(killerMoves.get(depth).size() - counter - 1).ext;
                    counter++;
                    int co = 0;
                    boolean isInside = false;
                    for(Coordinate c: coordinates){
                        if(c.x == coord.x && c.y == coord.y){
                            isInside = true;
                            break;
                        }
                        co++;
                    }
                    if(isInside) {
                        coordinates.remove(co);
                        coordinates.add(0, coord);
                    }
                } /*else if(setCoordAtBeginning){
                    coordinates.clear();
                    coordinates.add(addAtBeginning.value);

                    int co = 0;
                    boolean isInside = false;
                    for(Coordinate c: coordinates){
                        if(c.x == addAtBeginning.value.x && c.y == addAtBeginning.value.y){
                            isInside = true;
                            break;
                        }
                        co++;
                    }
                    if(isInside) {
                        coordinates.remove(co);
                        coordinates.add(0, addAtBeginning.value);
                    }

                    setCoordAtBeginning = false;
                }
                */


                for (Coordinate destination : coordinates) {
                    Piece opponent = board.peek(destination);
                    Coordinate previousCoords = p.getCoord();


                    //MAKE MOVE/////////////////////////////////////////////////////////////////////////////////////////
                    if (opponent != null) {
                        opponent.die();
                        zobrist.set(opponent, destination);
                    }
                    zobrist.set(p, destination);
                    zobrist.set(p, previousCoords);

                    p.setCoord(destination);
                    board.setField(destination.x, destination.y, p);
                    board.setField(previousCoords.x, previousCoords.y, null);

                    ////////////////////////////////////////////////////////////////////////////////////////////////////

                    double evalNext = evaluateMoves(depth - 1, alpha, beta, (actualPlayer + 1) % 2);
                    if (actualPlayer == owner) {
                        if(minmax < evalNext)
                           bestMove = new tuple<>(p, destination);

                        minmax = Math.max(minmax, evalNext);
                        if(minmax > alpha) {
                            flag = HashFlag.HASH_EXACT;
                        }

                        alpha = Math.max(minmax, alpha);
                    } else {
                        if(minmax > evalNext){
                            bestMove = new tuple<>(p, destination);
                        }
                        minmax = Math.min(minmax, evalNext);
                        beta = Math.min(minmax, beta);
                    }

                    //UNDO MOVE/////////////////////////////////////////////////////////////////////////////////////////


                    p.setCoord(previousCoords);

                    board.setField(destination.getX(), destination.getY(), opponent);
                    board.setField(p.getX(), p.getY(), p);

                    if (opponent != null) {
                        board.wake(opponent);
                        zobrist.set(opponent, destination);
                    }

                    zobrist.set(p, destination);
                    zobrist.set(p, previousCoords);

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

                        addHash(new HashPoint(zobrist.getHash(), depth, evaluateBoard(), HashFlag.HASH_BETA, null));
                        return minmax;
                    }
                }
            }
        }
        addHash(new HashPoint(zobrist.getHash(), depth, evaluateBoard(), flag, bestMove));
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

    private void addHash(HashPoint hash){
        hashes.put((int)(hash.getZobristKey() % hashRange), hash);
    }

    private Double checkHash(long zobristKey, double alpha, double beta, int deepnes){
        HashPoint previous = hashes.get((int)(zobristKey % hashRange));

        if(previous == null)
            return null;

        if(previous.getZobristKey() == zobristKey){
            if(previous.getDepth() >= deepnes){
                if(previous.getFlag() == HashFlag.HASH_EXACT)
                    return previous.getEvaluation();
                if(previous.getFlag() == HashFlag.HASH_ALPHA && previous.getEvaluation() <= alpha)
                    return alpha;
                if(previous.getFlag() == HashFlag.HASH_BETA && previous.getEvaluation() >= beta)
                    return beta;
            }
        }
        return null;
    }

    private tuple<Piece, Coordinate> getBestMove(Long hash){
        HashPoint point = hashes.get((int)(hash % hashRange));
        if(point != null && point.getZobristKey() == hash)
            return point.getBestMove();
        return null;
    }
}

class Zobrist {
    public static final long[][] array = new long[64][13];
    public int[] board = new int[64];
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

    private Chessboard chessboard;
    private long hash = 0;

    public static void initialize(){
        for(int i = 0; i < 64; i++)
            array[i] = new Random().longs(13).toArray();
    }

    public Zobrist(Chessboard board){
        this.chessboard = board;
        this.board = convert(board);
        updateHash();
    }

    public void updateHash(){
        for (int i = 0; i < 64; i++)
            if (board[i] != empty) {
                int piece = (int)board[i];
                hash ^= array[i][piece];
            }
    }

    public long getHash() {
        //updateHash();
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

    public void set(Piece p, Coordinate coordinate){
        int to = coordinate.y * 8 + coordinate.x;
        int piece = p.getNumber() + p.getOwner();

        hash ^= array[to][piece];
    }
}

enum HashFlag {
    HASH_EXACT,
    HASH_BETA,      //Evaluation caused cut-off
    HASH_ALPHA
}

final class HashPoint {
    private long zobristKey;
    private int depth;
    private double evaluation;
    private HashFlag flag;
    private tuple<Piece, Coordinate> bestMove;

    public HashPoint(long zobristKey, int depth, double evaluation, HashFlag flag, tuple<Piece, Coordinate> bestMove) {
        this.zobristKey = zobristKey;
        this.depth = depth;
        this.evaluation = evaluation;
        this.flag = flag;
        this.bestMove = bestMove;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() == HashPoint.class){
            HashPoint in = (HashPoint) obj;
            return  in.zobristKey == zobristKey;
        }
        else
            return false;
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public int getDepth() {
        return depth;
    }

    public double getEvaluation() {
        return evaluation;
    }

    public HashFlag getFlag() {
        return flag;
    }

    public tuple<Piece, Coordinate> getBestMove() {
        return bestMove;
    }
}

