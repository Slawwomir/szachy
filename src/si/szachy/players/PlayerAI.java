package si.szachy.players;

import org.jetbrains.annotations.NotNull;
import si.szachy.Chessboard;
import si.szachy.Coordinate;
import si.szachy.pieces.Piece;

import java.util.List;

public class PlayerAI extends Player {
    private static int DEPTH = 4;
    public int counter = 0;

    public PlayerAI(Chessboard board, int playerTeam) {
        super(board, playerTeam);
    }

    private class tuple<K, V>{
        K key;
        V value;

        tuple(K key, V value){
            this.key = key;
            this.value = value;
        }
    }

    public void performMove() {
        updateList();
        move();
    }

    private void move(){
        tuple<Coordinate, Double> move;
        Piece toMove = null;
        Coordinate destination = null;
        Double bestValue = -Double.MAX_VALUE;

        for(Piece p: playerPieces){
            List<Coordinate> possibleMoves = p.getAllValidMoves();
            if(!possibleMoves.isEmpty()) {
                move = findBestMove(p, possibleMoves);
                if (move.value > bestValue) {
                    toMove = p;
                    destination = move.key;
                    bestValue = move.value;
                }
            }
        }

        //To niżej można przerzucić do jakiejś oddzielnej funckji
        counter = 0;
        if(toMove != null) {
            if (board.peek(destination) != null && board.peek(destination).getOwner() != toMove.getOwner()) {
                board.peek(destination).die();
            }
            toMove.setCoord(destination);
        }
    }

    private tuple<Coordinate, Double> findBestMove(Piece p, @NotNull List<Coordinate> possibleMoves){
        Double bestValue = -Double.MAX_VALUE;
        Double actualValue = 0.0;
        Coordinate bestMove = possibleMoves.get(0);

        for(Coordinate c : possibleMoves){
            Piece at = board.peek(c);
            Coordinate prev = p.getCoord();

            if(at != null){
                at.die();
            }

            p.setCoord(c);
            board.setField(c.getX(), c.getY(), p);
            board.setField(prev.x, prev.y, null);

            actualValue = minimax(PlayerAI.DEPTH, playerTeam, -Double.MAX_VALUE, Double.MAX_VALUE);

            if(actualValue > bestValue){
                bestMove = c;
                bestValue = actualValue;
            }

            p.setCoord(prev);
            if(at != null) {
                at.isAlive = true;
                board.addPiece(at);
            }
            board.setField(c.getX(), c.getY(), at);
            board.setField(prev.x, prev.y, p);
        }

        return new tuple<>(bestMove, bestValue);
    }

    private double evaluateBoard(){
        double value = 0;
        for(Piece p: board.getPieces()){
            if(p.getOwner() == playerTeam)
                value += p.getValue();
            else
                value -= p.getValue();
        }

        return value;
    }

    private Double minimax(int depth, int playerTeam, double alfa, double beta){
        if(depth == 0)
            return evaluateBoard();

        tuple<Coordinate, Double> bestMove = null;
        Piece toMove;
        Double bestValue = this.playerTeam == playerTeam ? -Double.MAX_VALUE : Double.MAX_VALUE, nextMoveValue;

            for (Piece p : playerTeam == this.playerTeam ? playerPieces : oppositorPieces) {
                if(p.isAlive) {
                    List<Coordinate> possibleMoves = p.getAllValidMoves();

                    for (Coordinate destination : possibleMoves) {

                        Piece at = board.peek(destination);
                        Coordinate previousCoords = p.getCoord();

                        if (at != null) {
                            at.die();
                        }

                        p.setCoord(destination);
                        board.setField(destination.getX(), destination.getY(), p);
                        board.setField(previousCoords.x, previousCoords.y, null);
                        //*******************************************************************

                        nextMoveValue = minimax(depth - 1, (playerTeam + 1) % 2, alfa, beta);
                        bestValue = playerTeam == this.playerTeam ?
                                (alfa = Math.max(nextMoveValue, alfa)) :
                                (beta = Math.min(nextMoveValue, beta));

                        p.setCoord(previousCoords);
                        if (at != null) {
                            at.isAlive = true;
                            board.addPiece(at);
                        }
                        board.setField(destination.getX(), destination.getY(), at);
                        board.setField(p.getX(), p.getY(), p);

                       if (alfa >= beta)
                           return bestValue;
                    }
                }
            }
            counter++;
            return bestValue;
    }

}
