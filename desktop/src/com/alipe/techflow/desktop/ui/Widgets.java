package com.alipe.techflow.desktop.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Componentes com o visual do app, desenhados à mão para ficarem iguais em qualquer sistema. */
public final class Widgets {
    private Widgets() {}

    public enum Kind { PRIMARY, DEFAULT, GHOST, DANGER, TAB, LINK }

    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Botão plano com cantos arredondados. */
    public static final class Btn extends JButton {
        private final Kind kind;
        private boolean selected;
        private boolean hover;

        Btn(String text, Kind kind, boolean small) {
            super(text);
            this.kind = kind;
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(kind == Kind.TAB ? Font.BOLD : Font.PLAIN, small ? 13 : 14));
            setMargin(new Insets(0, 0, 0, 0));
            int padX = kind == Kind.TAB ? 2 : small ? 12 : 16;
            if (kind == Kind.LINK) setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 14));
            else setBorder(BorderFactory.createEmptyBorder(small ? 6 : 9, padX, small ? 6 : 9, padX));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }

        public void setSelectedLook(boolean on) { selected = on; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            Color fill = null, stroke = null, text = Theme.ink;
            switch (kind) {
                case PRIMARY -> { fill = Theme.accent; stroke = Theme.accent; text = Theme.accentInk; if (hover) fill = fill.brighter(); }
                case DANGER -> { fill = Theme.danger; stroke = Theme.danger; text = Theme.dark ? Theme.accentInk : Color.WHITE; }
                case DEFAULT -> { fill = hover ? Theme.surface2 : Theme.surface; stroke = Theme.line; }
                case GHOST -> { fill = hover ? Theme.surface2 : null; stroke = Theme.line; }
                case TAB -> { text = selected ? Theme.ink : Theme.muted; }
                case LINK -> { text = hover ? Theme.ink : Theme.muted; }
            }
            if (kind == Kind.DEFAULT && selected) { fill = Theme.accentSoft; stroke = Theme.accent; text = Theme.accent; }
            if (!isEnabled()) text = Theme.muted;
            if (fill != null) { g2.setColor(fill); g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10); }
            if (stroke != null) { g2.setColor(stroke); g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10); }
            if (kind == Kind.TAB && selected) { g2.setColor(Theme.accent); g2.fillRect(0, h - 3, w, 3); }
            g2.setFont(getFont());
            g2.setColor(text);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    public static Btn button(String text, Kind kind) { return new Btn(text, kind, false); }

    public static Btn small(String text, Kind kind) { return new Btn(text, kind, true); }

    /** Controle segmentado de dois lados, como o seletor Demo e API real. */
    public static Btn segment(String text) {
        Btn b = new Btn(text, Kind.DEFAULT, false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return b;
    }

    /** Painel com fundo e borda arredondados. */
    public static class Rounded extends JPanel {
        private Color fill;
        private Color stroke;
        private final int arc;

        public Rounded(Color fill, Color stroke, int arc) {
            this.fill = fill;
            this.stroke = stroke;
            this.arc = arc;
            setOpaque(false);
        }

        public void colors(Color fill, Color stroke) { this.fill = fill; this.stroke = stroke; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            if (fill != null) { g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); }
            if (stroke != null) { g2.setStroke(new BasicStroke(1f)); g2.setColor(stroke); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Etiqueta pequena e arredondada, usada para prioridade e status. */
    public static final class Chip extends JLabel {
        private final Color bg;

        public Chip(String text, Color fg, Color bg) {
            super(text);
            this.bg = bg;
            setForeground(fg);
            setFont(Theme.font(Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(2, 9, 2, 9));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Texto que quebra linha por conta própria, com limite de linhas e reticências. */
    public static final class WrapLabel extends JComponent {
        private final java.util.List<String> lines = new java.util.ArrayList<>();
        private final int width;
        private final Color color;
        private final String text;

        public WrapLabel(String text, int style, float size, Color color, int width, int maxLines) {
            this.text = text == null ? "" : text;
            this.width = width;
            this.color = color;
            setFont(Theme.font(style, size));
            setOpaque(false);
            wrap(text == null ? "" : text, maxLines);
        }

        public String text() { return text; }

        @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    @Override public javax.accessibility.AccessibleRole getAccessibleRole() { return javax.accessibility.AccessibleRole.LABEL; }
                    @Override public String getAccessibleName() { return text; }
                };
            }
            return accessibleContext;
        }

        private void wrap(String text, int maxLines) {
            FontMetrics fm = getFontMetrics(getFont());
            int limit = (int) (width * 0.92);
            StringBuilder line = new StringBuilder();
            java.util.List<String> all = new java.util.ArrayList<>();
            for (String word : text.split("\\s+")) {
                if (word.isEmpty()) continue;
                while (fm.stringWidth(word) > limit) {
                    int cut = word.length();
                    while (cut > 1 && fm.stringWidth(word.substring(0, cut)) > limit) cut--;
                    if (line.length() > 0) { all.add(line.toString()); line.setLength(0); }
                    all.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                String test = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(test) > limit && line.length() > 0) {
                    all.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(test);
                }
            }
            if (line.length() > 0) all.add(line.toString());
            if (all.size() > maxLines) {
                java.util.List<String> cut = new java.util.ArrayList<>(all.subList(0, maxLines));
                String last = cut.get(maxLines - 1);
                while (last.length() > 1 && fm.stringWidth(last + "...") > limit) last = last.substring(0, last.length() - 1);
                cut.set(maxLines - 1, last + "...");
                all = cut;
            }
            lines.addAll(all);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(width, Math.max(1, lines.size()) * getFontMetrics(getFont()).getHeight());
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setFont(getFont());
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            int y = fm.getAscent();
            for (String l : lines) { g2.drawString(l, 0, y); y += fm.getHeight(); }
            g2.dispose();
        }
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.line, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10));
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        style(f);
        return f;
    }

    public static JPasswordField password(int columns) {
        JPasswordField f = new JPasswordField(columns);
        style(f);
        return f;
    }

    private static void style(JTextField f) {
        f.setFont(Theme.font(Font.PLAIN, 14));
        f.setBorder(fieldBorder());
        f.setBackground(Theme.surface);
        f.setForeground(Theme.ink);
        f.setCaretColor(Theme.ink);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 38));
    }

    public static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setFont(Theme.font(Font.PLAIN, 14));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        a.setBackground(Theme.surface);
        a.setForeground(Theme.ink);
        a.setCaretColor(Theme.ink);
        return a;
    }

    public static JScrollPane framed(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(Theme.line, 1, true));
        slim(sp);
        return sp;
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setUI(new FlatComboUI());
        c.setFont(Theme.font(Font.PLAIN, 14));
        c.setBackground(Theme.surface);
        c.setForeground(Theme.ink);
        c.setBorder(BorderFactory.createLineBorder(Theme.line, 1, true));
        c.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object v, int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, v, i, sel, false);
                setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
                setBackground(sel && i >= 0 ? Theme.accentSoft : Theme.surface);
                setForeground(Theme.ink);
                setOpaque(true);
                return this;
            }
        });
        c.setPreferredSize(new Dimension(Math.max(c.getPreferredSize().width, 215), 38));
        return c;
    }

    private static final class FlatComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = aa(g);
                    g2.setColor(Theme.muted);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusable(false);
            b.setPreferredSize(new Dimension(30, 30));
            return b;
        }

        @Override public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
            g.setColor(Theme.surface);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup p = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            p.setBorder(BorderFactory.createLineBorder(Theme.line));
            p.getList().setBackground(Theme.surface);
            return p;
        }
    }

    /** Troca as barras de rolagem por versões finas. */
    public static void slim(JScrollPane sp) { slim(sp, Theme.bg); }

    public static void slim(JScrollPane sp, Color track) {
        sp.getVerticalScrollBar().setUI(new ThinBar());
        sp.getHorizontalScrollBar().setUI(new ThinBar());
        sp.getVerticalScrollBar().setBackground(track);
        sp.getHorizontalScrollBar().setBackground(track);
        sp.getVerticalScrollBar().setOpaque(true);
        sp.getHorizontalScrollBar().setOpaque(true);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getHorizontalScrollBar().setUnitIncrement(18);
    }

    private static final class ThinBar extends BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            if (r.isEmpty() || !((JScrollBar) c).isEnabled()) return;
            Graphics2D g2 = aa(g);
            g2.setColor(Theme.line);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }
    }
}
