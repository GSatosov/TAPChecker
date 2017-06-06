package Controller;

import com.sun.istack.internal.NotNull;

class DamerauLevenshteinDistance {

    private static int DamerauLevenshtein(int[][] ds, int i, int j, String a, String b) {
        if (Math.min(i, j) == 0) return Math.max(i, j);
        else return min(
                ds[i - 1][j] + 1,
                ds[i][j - 1] + 1,
                ds[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) ? ds[i - 2][j - 2] + 1 : Integer.MAX_VALUE
        );
    }

    private static int WagnerFischer(String a, String b) {
        int[][] ds = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                ds[i][j] = DamerauLevenshtein(ds, i, j, a, b);
            }
        }
        return ds[a.length()][b.length()];
    }

    static int compare(String a, String b) {
        return WagnerFischer(a, b);
    }

    private static int min(@NotNull int... numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Function requires at least one number!");
        }
        int minValue = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] < minValue) minValue = numbers[i];
        }
        return minValue;
    }

}
