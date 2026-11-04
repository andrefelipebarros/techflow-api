package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.model.Models.Priority;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/** Cores e fontes dos dois temas, no mesmo estilo índigo e tinta da identidade do projeto. */
public final class Theme {
    private Theme() {}

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);

    public static boolean dark;
    public static Color bg, surface, surface2, ink, muted, line, accent, accentInk, accentSoft, danger, dangerSoft, ok;
    public static final Color TERM_BG = new Color(0x0D1020);
    public static final Color TERM_INK = new Color(0xD8DCF5);
    public static final Color TERM_DIM = new Color(0x8089B3);
    public static final Color TERM_LINE = new Color(0x1F2547);
    public static final Color TERM_OK = new Color(0x5FD39B);
    public static final Color TERM_WARN = new Color(0xF2B45B);
    public static final Color TERM_ERR = new Color(0xFF7B93);
    public static final Color TERM_METHOD = new Color(0x9AA8FF);

    private static String family;

    static {
        load(PREFS.getBoolean("dark", false));
    }

    public static void toggle() {
        load(!dark);
        PREFS.putBoolean("dark", dark);
    }

    public static void load(boolean useDark) {
        dark = useDark;
        if (dark) {
            bg = c(0x0F1220); surface = c(0x171B2E); surface2 = c(0x1E2339); ink = c(0xE8EAF6); muted = c(0xA2A9CB);
            line = c(0x2B3151); accent = c(0x8393FF); accentInk = c(0x0F1220); accentSoft = c(0x252C58);
            danger = c(0xFF8AA1); dangerSoft = c(0x3A1C27); ok = c(0x5FD39B);
        } else {
            bg = c(0xF2F3F8); surface = c(0xFFFFFF); surface2 = c(0xECEEF6); ink = c(0x161B33); muted = c(0x565C7A);
            line = c(0xD9DCEA); accent = c(0x3347E0); accentInk = c(0xFFFFFF); accentSoft = c(0xE4E7FC);
            danger = c(0xB42343); dangerSoft = c(0xFBE6EB); ok = c(0x1F7A55);
        }
        applyDefaults();
    }

    private static Color c(int rgb) { return new Color(rgb); }

    private static void applyDefaults() {
        UIManager.put("Panel.background", new ColorUIResource(bg));
        UIManager.put("Label.foreground", new ColorUIResource(ink));
        UIManager.put("TextField.background", new ColorUIResource(surface));
        UIManager.put("TextField.foreground", new ColorUIResource(ink));
        UIManager.put("TextField.caretForeground", new ColorUIResource(ink));
        UIManager.put("PasswordField.background", new ColorUIResource(surface));
        UIManager.put("PasswordField.foreground", new ColorUIResource(ink));
        UIManager.put("PasswordField.caretForeground", new ColorUIResource(ink));
        UIManager.put("TextArea.background", new ColorUIResource(surface));
        UIManager.put("TextArea.foreground", new ColorUIResource(ink));
        UIManager.put("TextArea.caretForeground", new ColorUIResource(ink));
        UIManager.put("ComboBox.background", new ColorUIResource(surface));
        UIManager.put("ComboBox.foreground", new ColorUIResource(ink));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(accentSoft));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(ink));
        UIManager.put("ComboBox.buttonBackground", new ColorUIResource(surface));
        UIManager.put("List.background", new ColorUIResource(surface));
        UIManager.put("List.foreground", new ColorUIResource(ink));
        UIManager.put("List.selectionBackground", new ColorUIResource(accentSoft));
        UIManager.put("List.selectionForeground", new ColorUIResource(ink));
        UIManager.put("PopupMenu.background", new ColorUIResource(surface));
        UIManager.put("PopupMenu.foreground", new ColorUIResource(ink));
        UIManager.put("MenuItem.background", new ColorUIResource(surface));
        UIManager.put("MenuItem.foreground", new ColorUIResource(ink));
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(accentSoft));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(ink));
        UIManager.put("ScrollPane.background", new ColorUIResource(bg));
        UIManager.put("Viewport.background", new ColorUIResource(bg));
        UIManager.put("ToolTip.background", new ColorUIResource(surface));
        UIManager.put("ToolTip.foreground", new ColorUIResource(ink));
        UIManager.put("OptionPane.background", new ColorUIResource(bg));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(ink));
    }

    public static Font font(int style, float size) {
        if (family == null) {
            List<String> available = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            family = Font.SANS_SERIF;
            for (String f : List.of("Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Ubuntu", "Noto Sans")) {
                if (available.contains(f)) { family = f; break; }
            }
        }
        return new Font(family, style, Math.round(size));
    }

    public static Font mono(float size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
    }

    public static Color priorityFg(Priority p) {
        return switch (p) {
            case LOW -> dark ? c(0x5FD39B) : c(0x1F7A55);
            case MEDIUM -> dark ? c(0x8FB0FF) : c(0x2A56B8);
            case HIGH -> dark ? c(0xF2B45B) : c(0x9A5A00);
            case URGENT -> dark ? c(0xFF8AA1) : c(0xB42343);
        };
    }

    public static Color priorityBg(Priority p) {
        return switch (p) {
            case LOW -> dark ? c(0x17342A) : c(0xE1F3EA);
            case MEDIUM -> dark ? c(0x1C2A52) : c(0xE2EBFB);
            case HIGH -> dark ? c(0x3A2C12) : c(0xFDEED3);
            case URGENT -> dark ? c(0x3A1C27) : c(0xFBE6EB);
        };
    }
}
