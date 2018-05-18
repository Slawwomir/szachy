package si.szachy;

import si.szachy.pieces.King;
import si.szachy.pieces.Pawn;
import si.szachy.pieces.Piece;
import si.szachy.pieces.Queen;
import si.szachy.players.PlayerAI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainWindow extends JFrame {
    private JPanel panelMain;
    private GamePanel gamePanel;
    private JTextArea comments;
    private JButton zapiszButton;
    private JButton wczytajButton;
    private JTabbedPane tabbedPane1;
    private int gameMode = 1, aiDifficulty = 4;
    private final int width = 8, height = 8;
    private final int rectSize = 50;
    int turn = 0;
    private Chessboard board;
    Piece selectedPiece;
    boolean isSelected = false;
    private boolean rotated = false;
    private JMenuBar menuBar;
    private JMenu menu;

    public MainWindow() {
        super("SZACHULCE");

        board = new Chessboard();
        board.newGame(rotated);
        //     PlayerHuman human = new PlayerHuman(board, 0);
        PlayerAI ai = new PlayerAI(board, 6, 1);
        PlayerAI ai2 = new PlayerAI(board, 3, 0);

        initializeWindow();
        gamePanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                if (selectedPiece == null && isSelected == false) {
                    int x = (e.getX() / rectSize);
                    int y = (e.getY() / rectSize);
                    boolean test = gameMode == 2;
                    if (board.peek(x, y) != null && (test || (board.peek(x, y).getOwner() == turn))) { // TODO: ogarnac ten syf z teamami
                        hoverPiece(board.peek(x, y));
                    } else hoverPiece(null);
                }
            }
        });
        // TODO: refactor tego wielkiego, brzydkiego kodu
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (gameMode == 2) {
                    while (true) {
                        if (ai2.getMoveCount() == 0 || ai.getMoveCount() == 0) break;
                        toggleTurnAvA(ai2, ai);
                    }
                } else {
                    int x = e.getX() / rectSize;
                    int y = e.getY() / rectSize;
                    if (isSelected) {
                        if (selectedPiece.isValidMove(x, y)) {

                            if (selectedPiece.getClass() == King.class) {
                                if (selectedPiece.getX() - 2 == x) {
                                    //left castling
                                    board.peek(0, selectedPiece.getY()).
                                            move(selectedPiece.getX() - 1, selectedPiece.getY());
                                } else if (selectedPiece.getX() + 2 == x) {
                                    //right castling
                                    board.peek(width - 1, selectedPiece.getY()).
                                            move(selectedPiece.getX() + 1, selectedPiece.getY());
                                }
                            } else if (selectedPiece.getClass() == Pawn.class) {
                                if (y == board.getWidth() - 1 || y == 0) {
                                    //Promotion to queen
                                    Piece queen = new Queen(board, selectedPiece.getCoord(), selectedPiece.getOwner());
                                    board.addPiece(queen);
                                    board.removePiece(selectedPiece);
                                    board.updateChessboard();
                                    selectedPiece = queen;
                                }
                            }

                            if (board.peek(x, y) != null && board.peek(x, y).getOwner() != selectedPiece.getOwner()) {
                                board.peek(x, y).die();
                            }
                            selectedPiece.move(x, y);
                            selectedPiece.didMove = true;
                            deselect();
                            if (gameMode == 1) toggleTurnPvA(ai);
                            else if (gameMode == 0) toggleTurnPvP();
                        } else
                            deselect();
                    } else if (board.peek(x, y) != null && !isSelected && board.peek(x, y).getOwner() == turn)
                        selectPiece(board.peek(x, y));
                    else
                        deselect();
                }
            }
        });
    }

    private void initializeWindow() {
        setContentPane(gamePanel);
        menu();
        setJMenuBar(menuBar);
        Dimension mainDim = new Dimension(width * rectSize + 500, height * rectSize + 50);
        Dimension gameDim = new Dimension(width * rectSize, height * rectSize);
        panelMain.setPreferredSize(mainDim);
        gamePanel.setPreferredSize(gameDim);
        panelMain.setFocusable(false);
        gamePanel.setFocusable(false);
        comments.setFocusable(false);
        zapiszButton.setFocusable(false);
        wczytajButton.setFocusable(false);
        tabbedPane1.setFocusable(false);
        gamePanel.setBoard(board);
        gamePanel.setRectSize(rectSize);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        pack();
        setVisible(true);
    }

    private void deselect() {
        isSelected = false;
        selectedPiece = null;
        gamePanel.setSelectedPiece(null);
        repaint();
    }

    private void hoverPiece(Piece p) {
        gamePanel.setHoveredPiece(p);
        gamePanel.paintImmediately(gamePanel.getVisibleRect());
    }

    private void selectPiece(Piece p) {
        selectedPiece = p;
        isSelected = true;
        gamePanel.setSelectedPiece(selectedPiece);
        gamePanel.paintImmediately(gamePanel.getVisibleRect());
    }

    private void toggleTurnPvP() {
        board.updateChessboard();
        gamePanel.setHoveredPiece(null);
        turn = (turn + 1) % 2;
    }

    private void toggleTurnAvA(PlayerAI p1, PlayerAI p2) {
        if (p1.getMoveCount() == 0 || p2.getMoveCount() == 0) return;
        p1.performMove();
        gamePanel.paintImmediately(gamePanel.getVisibleRect());
        if (p1.getMoveCount() == 0 || p2.getMoveCount() == 0) return;
        p2.performMove();
        board.updateChessboard();
        gamePanel.paintImmediately(gamePanel.getVisibleRect());
        //   try{
        //  Thread.sleep(1000);
        //   } catch (InterruptedException e){}
    }

    private void toggleTurnPvA(PlayerAI player) {
        board.updateChessboard();
        gamePanel.setHoveredPiece(null);
        gamePanel.paintImmediately(gamePanel.getVisibleRect());
        int oldTurn = turn;
        turn = player.getPlayerTeam();
        Thread aiJob = new Thread(player::performMove);
        aiJob.start();
        try {
            aiJob.join();
        } catch (InterruptedException e) {
        }
        turn = oldTurn;

    }

    // TODO: ogarnac ocb z zaznaczeniami radiobuttonow
    private void menu() {
        JMenu submenu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        menuBar = new JMenuBar();

        menu = new JMenu("Game");
        submenu = new JMenu("New Game");
        JMenuItem pvp = new JMenuItem("Player vs Player");
        submenu.add(pvp);
        pvp.addActionListener(e -> {
            gameMode = 0;
            board.newGame(rotated);
            turn = 0;
            repaint();
        });

        JMenuItem pva = new JMenuItem("Player vs AI");
        submenu.add(pva);
        menu.add(submenu);
        pva.addActionListener(e -> {
            gameMode = 1;
            board.newGame(rotated);
            turn = 0;
            repaint();
        });

        JMenuItem ava = new JMenuItem("AI vs AI");
        submenu.add(ava);
        menu.add(submenu);
        ava.addActionListener(e -> {
            gameMode = 2;
            board.newGame(rotated);
            turn = 0;
            repaint();
        });

        menuBar.add(menu);

        menu = new JMenu("AI Difficulty");
        ButtonGroup group2 = new ButtonGroup();
        JRadioButtonMenuItem d1 = new JRadioButtonMenuItem("Easy");
        group2.add(d1);
        menu.add(d1);
        d1.addActionListener(e -> aiDifficulty = 1);


        JRadioButtonMenuItem d2 = new JRadioButtonMenuItem("Normal");
        d2.setSelected(true);
        group2.add(d2);
        menu.add(d2);
        d2.addActionListener(e -> aiDifficulty = 2);


        JRadioButtonMenuItem d3 = new JRadioButtonMenuItem("Hard");
        group2.add(d3);
        menu.add(d3);
        d3.addActionListener(e -> aiDifficulty = 3);


        JRadioButtonMenuItem d4 = new JRadioButtonMenuItem("Very Hard");
        group2.add(d4);
        menu.add(d4);
        d4.addActionListener(e -> aiDifficulty = 4);

        menuBar.add(menu);


        menu = new JMenu("Preferences");
        ButtonGroup group3 = new ButtonGroup();
        JRadioButtonMenuItem white = new JRadioButtonMenuItem("White");
        white.setSelected(true);
        group3.add(white);
        menu.add(white);
        white.addActionListener(e -> rotated = false);

        JRadioButtonMenuItem black = new JRadioButtonMenuItem("Black");
        group3.add(black);
        menu.add(black);
        black.addActionListener(e -> rotated = true);
        menuBar.add(menu);


    }

}
