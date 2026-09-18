package de.customclans.clans;

import java.awt.Color;

/**
 * Builds a per-character hex color gradient (Minecraft 1.16+ "§x§R§R§G§G§B§B" format) between
 * two hex colors, used for /clan color change.
 */
public final class GradientUtil {

    private GradientUtil() {
    }

    public static String apply(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Color start = decode(startHex);
        Color end = decode(endHex);
        if (start == null || end == null) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            double ratio = length == 1 ? 0 : (double) i / (length - 1);
            int r = (int) Math.round(start.getRed() + ratio * (end.getRed() - start.getRed()));
            int g = (int) Math.round(start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int b = (int) Math.round(start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            if (Character.isWhitespace(c)) {
                // No point coloring a space - keep the gradient step but skip the color code.
                result.append(c);
                continue;
            }
            result.append(toMinecraftHex(r, g, b)).append(c);
        }
        return result.toString();
    }

    /** @return true if the given string is a valid 6-digit hex color, with or without a leading '#' */
    public static boolean isValidHex(String hex) {
        if (hex == null) {
            return false;
        }
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return clean.matches("[0-9a-fA-F]{6}");
    }

    private static Color decode(String hex) {
        if (!isValidHex(hex)) {
            return null;
        }
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return new Color(Integer.parseInt(clean, 16));
    }

    private static String toMinecraftHex(int r, int g, int b) {
        String hex = String.format("%02x%02x%02x", r, g, b);
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }
}
