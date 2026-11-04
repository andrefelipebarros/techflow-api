package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.net.RequestLog;
import com.alipe.techflow.desktop.net.RequestLog.Entry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Console fixo no rodapé que mostra cada chamada HTTP feita pelo app. */
public final class ConsolePanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RequestLog log;
    private final List<Entry> rows = new ArrayList<>();
    private final JLabel last = new JLabel(" ");
    private final JLabel count = new JLabel("0");
    private final JPanel body = new JPanel(new BorderLayout());
    private final JTable table;
    private final JTextArea detail = new JTextArea();
    private final AbstractTableModel model;
    private final Consumer<Entry> listener = e -> SwingUtilities.invokeLater(() -> append(e));
    private boolean open = true;

    public ConsolePanel(RequestLog log) {
        super(new BorderLayout());
        this.log = log;
        setBackground(Theme.TERM_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TERM_LINE));

        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(Theme.TERM_BG);
        bar.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        bar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel title = new JLabel("Console da API");
        title.setFont(Theme.mono(13).deriveFont(Font.BOLD));
        title.setForeground(Theme.TERM_INK);
        last.setFont(Theme.mono(12));
        last.setForeground(Theme.TERM_DIM);
        count.setFont(Theme.mono(12));
        count.setForeground(Theme.TERM_INK);
        bar.add(title, BorderLayout.WEST);
        bar.add(last, BorderLayout.CENTER);
        JPanel east = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 14, 0));
        east.setOpaque(false);
        JLabel clear = new JLabel("Limpar");
        clear.setFont(Theme.mono(12));
        clear.setForeground(Theme.TERM_DIM);
        clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clear.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { e.consume(); log.clear(); }
        });
        east.add(clear);
        east.add(count);
        bar.add(east, BorderLayout.EAST);
        bar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
        });
        add(bar, BorderLayout.NORTH);

        model = new AbstractTableModel() {
            private final String[] cols = {"Hora", "Método", "Rota", "Status", "Tempo"};
            @Override public int getRowCount() { return rows.size(); }
            @Override public int getColumnCount() { return cols.length; }
            @Override public String getColumnName(int c) { return cols[c]; }
            @Override public Object getValueAt(int r, int c) {
                Entry e = rows.get(r);
                return switch (c) {
                    case 0 -> e.time().format(TIME);
                    case 1 -> e.method();
                    case 2 -> e.path();
                    case 3 -> e.status() == 0 ? "sem resposta" : String.valueOf(e.status());
                    default -> e.millis() + " ms";
                };
            }
        };
        table = new JTable(model);
        table.setBackground(Theme.TERM_BG);
        table.setForeground(Theme.TERM_INK);
        table.setFont(Theme.mono(12));
        table.setRowHeight(22);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Theme.TERM_LINE);
        table.setSelectionForeground(Theme.TERM_INK);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Theme.TERM_BG);
        table.getTableHeader().setForeground(Theme.TERM_DIM);
        table.getTableHeader().setFont(Theme.mono(11));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.TERM_LINE));
        table.getTableHeader().setDefaultRenderer((t, v, sel, foc, r, c) -> {
            JLabel l = new JLabel(String.valueOf(v));
            l.setOpaque(true);
            l.setBackground(Theme.TERM_BG);
            l.setForeground(Theme.TERM_DIM);
            l.setFont(Theme.mono(11));
            l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 6));
            return l;
        });
        int[] widths = {70, 70, 360, 90, 70};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
                setBackground(sel ? Theme.TERM_LINE : Theme.TERM_BG);
                Entry e = rows.get(r);
                Color fg = Theme.TERM_INK;
                if (c == 0 || c == 4) fg = Theme.TERM_DIM;
                if (c == 1) fg = Theme.TERM_METHOD;
                if (c == 3) fg = e.status() >= 400 || e.status() == 0 ? Theme.TERM_ERR : Theme.TERM_OK;
                setForeground(fg);
                return this;
            }
        });
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int r = table.getSelectedRow();
            showDetail(r < 0 ? null : rows.get(r));
        });

        JScrollPane left = new JScrollPane(table);
        left.setBorder(BorderFactory.createEmptyBorder());
        left.getViewport().setBackground(Theme.TERM_BG);
        Widgets.slim(left, Theme.TERM_BG);

        detail.setEditable(false);
        detail.setBackground(new Color(0x0A0C1A));
        detail.setForeground(Theme.TERM_INK);
        detail.setFont(Theme.mono(12));
        detail.setLineWrap(true);
        detail.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        detail.setText("Selecione uma chamada para ver o corpo da requisição e da resposta.");
        JScrollPane right = new JScrollPane(detail);
        right.setBorder(BorderFactory.createEmptyBorder());
        Widgets.slim(right, new Color(0x0A0C1A));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.6);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(1);
        split.setBackground(Theme.TERM_LINE);
        body.add(split, BorderLayout.CENTER);
        body.setPreferredSize(new Dimension(100, 190));
        add(body, BorderLayout.CENTER);

        for (Entry e : log.all()) rows.add(e);
        refreshHeader();
        log.listen(listener);
        log.onClear(() -> SwingUtilities.invokeLater(() -> {
            rows.clear();
            model.fireTableDataChanged();
            refreshHeader();
            showDetail(null);
        }));
    }

    /** Para de ouvir o histórico, usado quando a tela é reconstruída. */
    public void detach() { log.stopListening(listener); }

    private void toggle() {
        open = !open;
        body.setVisible(open);
        revalidate();
    }

    private void append(Entry e) {
        rows.add(e);
        if (rows.size() > 300) rows.remove(0);
        model.fireTableDataChanged();
        refreshHeader();
        int lastRow = rows.size() - 1;
        table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
    }

    private void refreshHeader() {
        count.setText(String.valueOf(rows.size()));
        if (rows.isEmpty()) {
            last.setText("Nenhuma chamada ainda");
        } else {
            Entry e = rows.get(rows.size() - 1);
            last.setText(e.method() + " " + e.path() + "  " + (e.status() == 0 ? "sem resposta" : e.status()) + "  " + e.millis() + " ms");
        }
    }

    private void showDetail(Entry e) {
        if (e == null) {
            detail.setText("Selecione uma chamada para ver o corpo da requisição e da resposta.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(e.method()).append(' ').append(e.path()).append('\n');
        sb.append("Status ").append(e.status() == 0 ? "sem resposta" : e.status()).append(" em ").append(e.millis()).append(" ms\n");
        if (!e.request().isEmpty()) sb.append("\nRequisição\n").append(e.request()).append('\n');
        if (!e.response().isEmpty()) sb.append("\nResposta\n").append(e.response()).append('\n');
        detail.setText(sb.toString());
        detail.setCaretPosition(0);
    }
}
