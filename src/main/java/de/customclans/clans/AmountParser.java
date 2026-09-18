package de.customclans.clans;

/**
 * Parses money amounts typed by players, allowing shorthand suffixes so people don't have to
 * write out huge numbers: "500", "1.5k" (=1,500), "100m" (=100,000,000), "2b" (=2,000,000,000).
 */
public final class AmountParser {

    private AmountParser() {
    }

    /** @return the parsed amount, or null if the input isn't a valid number/shorthand */
    public static Double parse(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }

        double multiplier = 1;
        String numberPart = trimmed;
        char last = trimmed.charAt(trimmed.length() - 1);
        switch (last) {
            case 'k' -> {
                multiplier = 1_000;
                numberPart = trimmed.substring(0, trimmed.length() - 1);
            }
            case 'm' -> {
                multiplier = 1_000_000;
                numberPart = trimmed.substring(0, trimmed.length() - 1);
            }
            case 'b' -> {
                multiplier = 1_000_000_000;
                numberPart = trimmed.substring(0, trimmed.length() - 1);
            }
            default -> {
                // no suffix, plain number
            }
        }

        try {
            double value = Double.parseDouble(numberPart);
            return value * multiplier;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
