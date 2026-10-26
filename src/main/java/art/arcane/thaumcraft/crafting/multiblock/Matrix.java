package art.arcane.thaumcraft.crafting.multiblock;

/** TC6 lib.crafting.Matrix: a blueprint layer that can be rotated clockwise in 90 degree steps. */
final class Matrix {
    int rows, cols;
    Part[][] matrix;
    Matrix(Part[][] m) {
        rows = m.length; cols = m[0].length; matrix = new Part[rows][cols];
        for (int i = 0; i < rows; i++) System.arraycopy(m[i], 0, matrix[i], 0, cols);
    }
    void rotate90DegRight(int times) {
        for (int a = 0; a < times; a++) {
            Part[][] n = new Part[cols][rows];
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) n[j][rows - i - 1] = matrix[i][j];
            matrix = n; int tmp = rows; rows = cols; cols = tmp;
        }
    }
}
