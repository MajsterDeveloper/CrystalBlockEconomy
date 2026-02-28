package world.bentobox.crystalblockeconomy.economy;

import java.util.Locale;

public enum AccountType {
    CASH,
    BANK;

    public static AccountType fromString(String s) {
        if (s == null) return CASH;
        try {
            return AccountType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CASH;
        }
    }
}