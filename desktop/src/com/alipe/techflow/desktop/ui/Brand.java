package com.alipe.techflow.desktop.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Marca do TechFlow: um quadrado índigo com três colunas, como um quadro de tarefas. */
public final class Brand {
    private Brand() {}

    private static void paintMark(Graphics2D g2, int x, int y, int size, Color bg, Color fg) {
        g2.setColor(bg);
        g2.fillRoundRect(x, y, size, size, size / 4, size / 4);
        g2.setColor(fg);
        int pad = Math.round(size * 0.22f);
        int bar = Math.max(2, Math.round(size * 0.15f));
        int gap = Math.max(1, Math.round(size * 0.07f));
        int h = size - pad * 2;
        g2.fillRoundRect(x + pad, y + pad, bar, h, bar / 2, bar / 2);
        g2.fillRoundRect(x + pad + bar + gap, y + pad, bar, Math.round(h * 0.62f), bar / 2, bar / 2);
        g2.fillRoundRect(x + pad + (bar + gap) * 2, y + pad, bar, Math.round(h * 0.36f), bar / 2, bar / 2);
    }

    public static Image icon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Widgets.aa(img.getGraphics());
        paintMark(g2, 0, 0, size, new Color(0x3347E0), Color.WHITE);
        g2.dispose();
        return img;
    }

    public static JComponent logo(float textSize) {
        int size = Math.round(textSize * 1.25f);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        p.setOpaque(false);
        JComponent mark = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Widgets.aa(g);
                paintMark(g2, 0, 0, size, Theme.accent, Theme.accentInk);
                g2.dispose();
            }
        };
        mark.setPreferredSize(new Dimension(size, size));
        p.add(mark);
        p.add(Widgets.label("TechFlow", Font.BOLD, textSize, Theme.ink));
        return p;
    }
}
