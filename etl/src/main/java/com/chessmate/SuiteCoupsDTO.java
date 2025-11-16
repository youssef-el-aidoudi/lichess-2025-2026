package com.chessmate;

public class SuiteCoupsDTO {
    String pgn;
    String pgnHash;  // SHA-256 en hex

    //constructeur
    SuiteCoupsDTO(String rawPgn) {
        this.pgn = ChessMateETL.nettoyerPgnCoups(rawPgn);
        this.pgnHash = ChessMateETL.sha256Hex(this.pgn); // bien hasher le pgn nettoyé et pas le raw
    }
}