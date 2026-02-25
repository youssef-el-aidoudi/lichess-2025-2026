import { useState } from 'react';
import { searchPlayerGames } from '../services/api';

interface ParsedGame {
    event: string;
    white: string;
    black: string;
    result: string;
    date: string;
    whiteElo: string;
    blackElo: string;
    opening: string;
    timeControl: string;
    site: string;
    moves: string;
    variant: string;
}

/** Parse un bloc PGN multi-parties en tableau d'objets */
function parsePgnGames(pgn: string): ParsedGame[] {
    if (!pgn || pgn.trim() === '') return [];

    const games: ParsedGame[] = [];
    const blocks = pgn.split(/\n\n(?=\[)/);

    let currentHeaders: Record<string, string> = {};
    let currentMoves = '';

    for (const block of blocks) {
        const trimmed = block.trim();
        if (!trimmed) continue;

        if (trimmed.startsWith('[')) {
            if (Object.keys(currentHeaders).length > 0 && currentMoves) {
                games.push(buildGame(currentHeaders, currentMoves));
                currentMoves = '';
            }
            currentHeaders = {};
            const headerLines = trimmed.split('\n');
            for (const line of headerLines) {
                const match = line.match(/^\[(\w+)\s+"(.*)"\]$/);
                if (match) {
                    currentHeaders[match[1]] = match[2];
                }
            }
        } else {
            currentMoves = trimmed.replace(/\n/g, ' ').trim();
        }
    }

    if (Object.keys(currentHeaders).length > 0) {
        games.push(buildGame(currentHeaders, currentMoves));
    }

    return games;
}

function buildGame(headers: Record<string, string>, moves: string): ParsedGame {
    return {
        event: headers['Event'] || '?',
        white: headers['White'] || '?',
        black: headers['Black'] || '?',
        result: headers['Result'] || '?',
        date: headers['UTCDate'] || headers['Date'] || '?',
        whiteElo: headers['WhiteElo'] || '?',
        blackElo: headers['BlackElo'] || '?',
        opening: headers['Opening'] || headers['ECO'] || '?',
        timeControl: headers['TimeControl'] || '?',
        site: headers['Site'] || '',
        moves: moves,
        variant: headers['Variant'] || 'Standard',
    };
}

function getResultLabel(result: string, playerPseudo: string, white: string): { label: string; color: string } {
    const isWhite = white.toLowerCase() === playerPseudo.toLowerCase();
    if (result === '1-0') {
        return isWhite
            ? { label: 'Victoire', color: 'bg-green-100 text-green-800' }
            : { label: 'Defaite', color: 'bg-red-100 text-red-800' };
    }
    if (result === '0-1') {
        return isWhite
            ? { label: 'Defaite', color: 'bg-red-100 text-red-800' }
            : { label: 'Victoire', color: 'bg-green-100 text-green-800' };
    }
    return { label: 'Nulle', color: 'bg-gray-100 text-gray-800' };
}

function formatTimeControl(tc: string): string {
    if (!tc || tc === '?') return '?';
    const parts = tc.split('+');
    const base = parseInt(parts[0]);
    const inc = parts[1] ? parseInt(parts[1]) : 0;
    if (base < 60) return `${base}s+${inc}s`;
    const minutes = Math.floor(base / 60);
    if (inc > 0) return `${minutes}+${inc}`;
    return `${minutes} min`;
}

export function PlayerSearch() {
    return (
        <div className="space-y-6">
            <div>
                <h2 className="text-gray-900 mb-2">Recherche de parties Lichess</h2>
                <p className="text-gray-600">
                    Recherchez les parties d'un joueur directement depuis l'API Lichess
                </p>
            </div>
        </div>
    );
}
