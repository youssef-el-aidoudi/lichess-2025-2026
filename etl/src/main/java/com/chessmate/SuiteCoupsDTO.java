package com.chessmate;

public class SuiteCoupsDTO {
    String pgn;

    //constructeur
    SuiteCoupsDTO(String pgn) {
        this.pgn = ChessMateETL.nettoyerPgnCoups(pgn);
    }

}