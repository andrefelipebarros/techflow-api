package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.model.Models.Priority;
import com.alipe.techflow.desktop.model.Models.Project;
import com.alipe.techflow.desktop.model.Models.Task;
import com.alipe.techflow.desktop.model.Models.TaskStatus;
import com.alipe.techflow.desktop.net.TechFlowApi;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Quadro de tarefas de um projeto, com uma coluna por status e arrastar e soltar. */
public final class BoardPanel extends JPanel {

    public interface Host {
        TechFlowApi api();
        void toast(String message, boolean error);
        void createProject();
        void editProject(Project project);
        void deleteProject(Project project);
    }

    private static final String ALL = "Todas as prioridades";
    private static final int TEXT_WIDTH = 186;

    private final Host host;
    private Project project;
    private List<Task> tasks = new ArrayList<>();
    private boolean loading;

    private final JTextField search = Widgets.field(16);
    private final JComboBox<Object> priorityFilter;

    public BoardPanel(Host host) {
        super(new BorderLayout());
        this.host = host;
        setBackground(Theme.bg);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        Object[] items = new Object[Priority.values().length + 1];
        items[0] = ALL;
        System.arraycopy(Priority.values(), 0, items, 1, Priority.values().length);
        priorityFilter = Widgets.combo(items);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { render(); }
            @Override public void removeUpdate(DocumentEvent e) { render(); }
            @Override public void changedUpdate(DocumentEvent e) { render(); }
        });
        priorityFilter.addActionListener(e -> render());
        showEmpty();
    }

    // ---------- estados da tela ----------

    public void showEmpty() {
        project = null;
        removeAll();
        JPanel box = new JPanel(new GridBagLayout());
        box.setOpaque(false);
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        JLabel title = Widgets.label("Nenhum projeto ainda", Font.BOLD, 20, Theme.ink);
        JLabel text = Widgets.label("Crie um projeto para começar a organizar as tarefas.", Font.PLAIN, 14, Theme.muted);
        Widgets.Btn create = Widgets.button("Novo projeto", Widgets.Kind.PRIMARY);
        create.addActionListener(e -> host.createProject());
        for (JComponent c : new JComponent[]{title, text, create}) c.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(title);
        inner.add(javax.swing.Box.createVerticalStrut(6));
        inner.add(text);
        inner.add(javax.swing.Box.createVerticalStrut(16));
        inner.add(create);
        box.add(inner);
        add(box, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showProject(Project p) {
        boolean changed = project == null || project.id() != p.id();
        project = p;
        if (changed) { search.setText(""); priorityFilter.setSelectedIndex(0); }
        loading = true;
        render();
        Async.run(() -> host.api().tasks(p.id()), list -> {
            if (project == null || project.id() != p.id()) return;
            tasks = list;
            loading = false;
            render();
        }, err -> {
            loading = false;
            tasks = new ArrayList<>();
            render();
            host.toast(err.getMessage(), true);
        });
    }

    public Project current() { return project; }

    private void render() {
        removeAll();
        if (project == null) { showEmpty(); return; }
        add(header(), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(filters(), BorderLayout.NORTH);
        if (loading) {
            JLabel l = Widgets.label("Carregando tarefas...", Font.PLAIN, 14, Theme.muted);
            l.setBorder(BorderFactory.createEmptyBorder(20, 4, 0, 0));
            center.add(l, BorderLayout.CENTER);
        } else {
            center.add(columns(), BorderLayout.CENTER);
        }
        add(center, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JComponent header() {
        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel name = Widgets.label(project.name(), Font.BOLD, 26, Theme.ink);
        name.setAlignmentX(0f);
        left.add(name);
        if (project.description() != null && !project.description().isBlank()) {
            JComponent d = new Widgets.WrapLabel(project.description(), Font.PLAIN, 14, Theme.muted, 640, 2);
            d.setAlignmentX(0f);
            d.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            left.add(d);
        }
        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chipRow.setOpaque(false);
        chipRow.setAlignmentX(0f);
        chipRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        chipRow.add(new Widgets.Chip(project.status().label, Theme.accent, Theme.accentSoft));
        left.add(chipRow);
        head.add(left, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        Widgets.Btn add = Widgets.button("Nova tarefa", Widgets.Kind.PRIMARY);
        Widgets.Btn edit = Widgets.button("Editar projeto", Widgets.Kind.DEFAULT);
        Widgets.Btn del = Widgets.button("Excluir projeto", Widgets.Kind.GHOST);
        add.addActionListener(e -> newTask());
        edit.addActionListener(e -> host.editProject(project));
        del.addActionListener(e -> host.deleteProject(project));
        actions.add(add);
        actions.add(edit);
        actions.add(del);
        head.add(actions, BorderLayout.EAST);
        return head;
    }

    private JComponent filters() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        search.setToolTipText("Buscar por título ou descrição");
        row.add(Widgets.label("Buscar", Font.BOLD, 13, Theme.muted));
        row.add(search);
        row.add(priorityFilter);
        return row;
    }

    private JComponent columns() {
        ScrollablePanel grid = new ScrollablePanel(new GridLayout(1, 4, 12, 0));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(4 * 240 + 36, 200));
        String q = search.getText().trim().toLowerCase();
        Object sel = priorityFilter.getSelectedItem();
        for (TaskStatus status : TaskStatus.values()) {
            List<Task> inColumn = new ArrayList<>();
            for (Task t : tasks) {
                if (t.status() != status) continue;
                if (sel instanceof Priority p && t.priority() != p) continue;
                if (!q.isEmpty() && !(t.title().toLowerCase().contains(q)
                        || (t.description() != null && t.description().toLowerCase().contains(q)))) continue;
                inColumn.add(t);
            }
            inColumn.sort(Comparator.comparing((Task t) -> t.priority()).reversed().thenComparing(Task::id));
            grid.add(column(status, inColumn));
        }
        JScrollPane sp = new JScrollPane(grid);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.bg);
        Widgets.slim(sp);
        return sp;
    }

    private JComponent column(TaskStatus status, List<Task> items) {
        Widgets.Rounded col = new Widgets.Rounded(Theme.surface2, null, 14);
        col.setLayout(new BorderLayout(0, 8));
        col.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Widgets.label(status.label, Font.BOLD, 15, Theme.ink), BorderLayout.WEST);
        head.add(Widgets.label(String.valueOf(items.size()), Font.PLAIN, 13, Theme.muted), BorderLayout.EAST);
        col.add(head, BorderLayout.NORTH);

        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        if (items.isEmpty()) {
            JLabel none = Widgets.label("Nenhuma tarefa", Font.PLAIN, 13, Theme.muted);
            none.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
            cards.add(none);
        }
        for (Task t : items) {
            cards.add(card(t));
            cards.add(javax.swing.Box.createVerticalStrut(8));
        }
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(cards, BorderLayout.NORTH);
        col.add(top, BorderLayout.CENTER);

        col.setTransferHandler(new DropHandler(status));
        return col;
    }

    private JComponent card(Task t) {
        Widgets.Rounded card = new Widgets.Rounded(Theme.surface, Theme.line, 10);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(11, 12, 8, 12));
        card.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));

        JComponent title = new Widgets.WrapLabel(t.title(), Font.BOLD, 14, Theme.ink, TEXT_WIDTH, 3);
        title.setAlignmentX(0f);
        card.add(title);
        if (t.description() != null && !t.description().isBlank()) {
            JComponent desc = new Widgets.WrapLabel(t.description(), Font.PLAIN, 12, Theme.muted, TEXT_WIDTH, 2);
            desc.setAlignmentX(0f);
            desc.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            card.add(desc);
        }

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        meta.setOpaque(false);
        meta.setAlignmentX(0f);
        meta.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        meta.add(new Widgets.Chip(t.priority().label, Theme.priorityFg(t.priority()), Theme.priorityBg(t.priority())));
        if (t.dueDate() != null) {
            meta.add(javax.swing.Box.createHorizontalStrut(8));
            boolean late = t.dueDate().isBefore(LocalDate.now()) && t.status() != TaskStatus.DONE && t.status() != TaskStatus.CANCELED;
            String text = (late ? "Venceu em " : "Prazo ") + t.dueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            meta.add(Widgets.label(text, late ? Font.BOLD : Font.PLAIN, 12, late ? Theme.danger : Theme.muted));
        }
        card.add(meta);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(0f);
        actions.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        Widgets.Btn move = Widgets.small("Mover", Widgets.Kind.LINK);
        Widgets.Btn edit = Widgets.small("Editar", Widgets.Kind.LINK);
        Widgets.Btn del = Widgets.small("Excluir", Widgets.Kind.LINK);
        move.addActionListener(e -> moveMenu(t, move));
        edit.addActionListener(e -> editTask(t));
        del.addActionListener(e -> deleteTask(t));
        actions.add(move);
        actions.add(edit);
        actions.add(del);
        card.add(actions);

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        card.setTransferHandler(new CardHandler(t));
        MouseAdapter drag = new MouseAdapter() {
            private Point press;
            @Override public void mousePressed(MouseEvent e) { press = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (press != null && press.distance(e.getPoint()) > 6) {
                    card.getTransferHandler().exportAsDrag(card, e, TransferHandler.MOVE);
                    press = null;
                }
            }
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) editTask(t); }
        };
        card.addMouseListener(drag);
        card.addMouseMotionListener(drag);
        return card;
    }

    // ---------- ações ----------

    private void moveMenu(Task t, Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        for (TaskStatus s : TaskStatus.values()) {
            JMenuItem item = new JMenuItem(s.label);
            item.setEnabled(s != t.status());
            item.addActionListener(e -> moveTask(t.id(), s));
            menu.add(item);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    public void newTask() {
        if (project == null) return;
        Forms.TaskInput in = Forms.task(this, null);
        if (in == null) return;
        Async.run(() -> host.api().createTask(project.id(), in.title(), in.description(), in.priority(), in.dueDate()), t -> {
            tasks.add(t);
            render();
            host.toast("Tarefa criada.", false);
        }, err -> host.toast(err.getMessage(), true));
    }

    private void editTask(Task t) {
        Forms.TaskInput in = Forms.task(this, t);
        if (in == null) return;
        Async.run(() -> {
            Task updated = host.api().updateTask(t.id(), in.title(), in.description(), in.priority(), in.dueDate());
            return in.status() != updated.status() ? host.api().changeStatus(t.id(), in.status()) : updated;
        }, updated -> {
            replace(updated);
            render();
            host.toast("Tarefa atualizada.", false);
        }, err -> host.toast(err.getMessage(), true));
    }

    private void deleteTask(Task t) {
        if (!Forms.confirm(this, "Excluir tarefa", "A tarefa \"" + t.title() + "\" será removida.", "Excluir tarefa")) return;
        Async.run(() -> { host.api().deleteTask(t.id()); return t.id(); }, id -> {
            tasks.removeIf(x -> x.id() == id);
            render();
            host.toast("Tarefa excluída.", false);
        }, err -> host.toast(err.getMessage(), true));
    }

    /** Muda o status na hora e desfaz se a API recusar. */
    void moveTask(long id, TaskStatus status) {
        Task before = tasks.stream().filter(x -> x.id() == id).findFirst().orElse(null);
        if (before == null || before.status() == status) return;
        replace(new Task(before.id(), before.projectId(), before.title(), before.description(), status, before.priority(), before.dueDate()));
        render();
        Async.run(() -> host.api().changeStatus(id, status), updated -> {
            replace(updated);
            render();
            host.toast("Movida para " + status.label.toLowerCase() + ".", false);
        }, err -> {
            replace(before);
            render();
            host.toast(err.getMessage(), true);
        });
    }

    private void replace(Task t) {
        for (int i = 0; i < tasks.size(); i++) if (tasks.get(i).id() == t.id()) { tasks.set(i, t); return; }
        tasks.add(t);
    }

    // ---------- arrastar e soltar ----------

    private class DropHandler extends TransferHandler {
        private final TaskStatus target;

        DropHandler(TaskStatus target) { this.target = target; }

        @Override public boolean canImport(TransferSupport s) { return s.isDataFlavorSupported(DataFlavor.stringFlavor); }

        @Override public boolean importData(TransferSupport s) {
            try {
                String id = (String) s.getTransferable().getTransferData(DataFlavor.stringFlavor);
                moveTask(Long.parseLong(id), target);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /** Cada cartão também aceita soltar, para não bloquear o alvo da coluna por baixo dele. */
    private final class CardHandler extends DropHandler {
        private final Task task;

        CardHandler(Task task) { super(task.status()); this.task = task; }

        @Override public int getSourceActions(JComponent c) { return MOVE; }

        @Override protected Transferable createTransferable(JComponent c) { return new StringSelection(String.valueOf(task.id())); }
    }

    /** Painel que acompanha a largura da janela, mas rola quando ela fica estreita. */
    private static final class ScrollablePanel extends JPanel implements Scrollable {
        ScrollablePanel(java.awt.LayoutManager lm) { super(lm); }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() != null && getParent().getWidth() > getPreferredSize().width;
        }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int h = 0;
            for (Component c : getComponents()) h = Math.max(h, c.getPreferredSize().height);
            return new Dimension(Math.max(d.width, 4 * 240 + 36), Math.max(h, 160));
        }
    }
}
