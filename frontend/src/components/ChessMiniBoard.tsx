type ChessMiniBoardProps = {
  board: string[][];
  title?: string;
};

export function ChessMiniBoard({
  board,
  title = 'Position finale',
}: ChessMiniBoardProps) {
  const isLightSquare = (row: number, col: number) => (row + col) % 2 === 0;

  return (
    <div className="bg-white border rounded-xl p-6 shadow-sm">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>

      <div className="border-2 border-gray-300 rounded-lg overflow-hidden w-fit">
        <div
          className="grid"
          style={{
            gridTemplateColumns: 'repeat(8, 48px)',
            gridTemplateRows: 'repeat(8, 48px)',
          }}
        >
          {board.map((row, rowIndex) =>
            row.map((piece, colIndex) => (
              <div
                key={`${rowIndex}-${colIndex}`}
                className={`w-12 h-12 flex items-center justify-center text-2xl select-none ${
                  isLightSquare(rowIndex, colIndex) ? 'bg-amber-100' : 'bg-green-700'
                }`}
              >
                <span className={piece ? 'drop-shadow-sm' : ''}>{piece}</span>
              </div>
            ))
          )}
        </div>
      </div>

      <p className="mt-4 text-sm text-gray-600">
        Cet échiquier montre la position finale reconstruite depuis le PGN.
      </p>
    </div>
  );
}