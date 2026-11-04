package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.Main;
import com.alipe.techflow.desktop.model.Models.Project;
import com.alipe.techflow.desktop.model.Models.ProjectStatus;
import com.alipe.techflow.desktop.model.Models.User;
import com.alipe.techflow.desktop.net.DemoTransport;
import com.alipe.techflow.desktop.net.TechFlowApi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;

/** Janela principal: projetos à esquerda, quadro no centro e console da API embaixo. */
public final class MainFrame extends JFrame implements BoardPanel.Host {
    private final TechFlowApi api;
    private final User user;
    private final DefaultListModel<Project> projectModel = new DefaultListModel<>();
    private final JList<Project> projectList = new JList<>(projectModel);
    private final JLabel status = new JLabel(" ");
    private final Timer statusTimer = new Timer(4500, e -> status.setText(" "));
    private BoardPanel board;
    private ConsolePanel console;
    private boolean updatingList;

    public MainFrame(TechFlowApi api, User user) {
        super("TechFlow");
        this.api = api;
        this.user = user;
        statusTimer.setRepeats(false);
        setIconImage(Brand.icon(64));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        build();
        setSize(1320, 820);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);
        loadProjects(-1);
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.bg);
        root.add(topBar(), BorderLayout.NORTH);
        root.add(sidebar(), BorderLayout.WEST);
        board = new BoardPanel(this);
        root.add(board, BorderLayout.CENTER);
        console = new ConsolePanel(api.log());
        root.add(console, BorderLayout.SOUTH);
        setContentPane(root);
    }

    // ---------- barra superior ----------

    private JComponent topBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(Theme.surface);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.line),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(Brand.logo(18));
        boolean demo = api.transport() instanceof DemoTransport;
        left.add(new Widgets.Chip(demo ? "Demo local" : api.transport().describe(),
                demo ? Theme.muted : Theme.accent, demo ? Theme.surface2 : Theme.accentSoft));
        bar.add(left, BorderLayout.WEST);

        status.setFont(Theme.font(Font.PLAIN, 13));
        status.setForeground(Theme.muted);
        status.setHorizontalAlignment(JLabel.CENTER);
        bar.add(status, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        Widgets.Btn theme = Widgets.small(Theme.dark ? "Tema claro" : "Tema escuro", Widgets.Kind.GHOST);
        theme.addActionListener(e -> switchTheme());
        right.add(theme);
        if (demo) {
            Widgets.Btn reset = Widgets.small("Restaurar exemplo", Widgets.Kind.GHOST);
            reset.addActionListener(e -> resetDemo());
            right.add(reset);
        }
        right.add(Widgets.label(user.name(), Font.PLAIN, 13, Theme.muted));
        Widgets.Btn out = Widgets.small("Sair", Widgets.Kind.DEFAULT);
        out.addActionListener(e -> Main.logout(this, api));
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ---------- lista de projetos ----------

    private JComponent sidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(Theme.surface);
        side.setPreferredSize(new Dimension(250, 100));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.line),
                BorderFactory.createEmptyBorder(16, 14, 14, 14)));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Widgets.label("Projetos", Font.BOLD, 16, Theme.ink), BorderLayout.WEST);
        Widgets.Btn add = Widgets.small("Novo", Widgets.Kind.PRIMARY);
        add.addActionListener(e -> createProject());
        head.add(add, BorderLayout.EAST);
        side.add(head, BorderLayout.NORTH);

        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setBackground(Theme.surface);
        projectList.setFixedCellHeight(58);
        projectList.setCellRenderer((list, p, index, selected, focus) -> {
            Widgets.Rounded row = new Widgets.Rounded(selected ? Theme.accentSoft : Theme.surface, selected ? Theme.accent : null, 10);
            row.setLayout(new BorderLayout());
            row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            JPanel text = new JPanel(new BorderLayout());
            text.setOpaque(false);
            text.add(Widgets.label(p.name(), Font.BOLD, 14, Theme.ink), BorderLayout.NORTH);
            text.add(Widgets.label(p.status().label, Font.PLAIN, 12, Theme.muted), BorderLayout.CENTER);
            row.add(text, BorderLayout.CENTER);
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(true);
            wrap.setBackground(Theme.surface);
            wrap.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            wrap.add(row, BorderLayout.CENTER);
            return wrap;
        });
        projectList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingList) return;
            Project p = projectList.getSelectedValue();
            if (p != null) board.showProject(p);
        });
        JScrollPane sp = new JScrollPane(projectList);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.surface);
        Widgets.slim(sp);
        side.add(sp, BorderLayout.CENTER);
        return side;
    }

    /** Recarrega os projetos e seleciona o de id informado, ou o primeiro. */
    private void loadProjects(long selectId) {
        Async.run(api::projects, list -> {
            updatingList = true;
            projectModel.clear();
            list.forEach(projectModel::addElement);
            updatingList = false;
            if (list.isEmpty()) { board.showEmpty(); return; }
            int index = 0;
            for (int i = 0; i < list.size(); i++) if (list.get(i).id() == selectId) index = i;
            projectList.setSelectedIndex(index);
            board.showProject(list.get(index));
        }, err -> toast(err.getMessage(), true));
    }

    // ---------- BoardPanel.Host ----------

    @Override public TechFlowApi api() { return api; }

    @Override public void toast(String message, boolean error) {
        status.setText(message);
        status.setForeground(error ? Theme.danger : Theme.ok);
        statusTimer.restart();
    }

    @Override public void createProject() {
        Forms.ProjectInput in = Forms.project(this, null);
        if (in == null) return;
        Async.run(() -> api.createProject(in.name(), in.description(), in.status()), p -> {
            toast("Projeto criado.", false);
            loadProjects(p.id());
        }, err -> toast(err.getMessage(), true));
    }

    @Override public void editProject(Project project) {
        Forms.ProjectInput in = Forms.project(this, project);
        if (in == null) return;
        Async.run(() -> api.updateProject(project.id(), in.name(), in.description(), in.status()), p -> {
            toast("Projeto atualizado.", false);
            loadProjects(p.id());
        }, err -> toast(err.getMessage(), true));
    }

    @Override public void deleteProject(Project project) {
        if (!Forms.confirm(this, "Excluir projeto",
                "O projeto \"" + project.name() + "\" e todas as tarefas dele serão removidos.", "Excluir projeto")) return;
        Async.run(() -> { api.deleteProject(project.id()); return project.id(); }, id -> {
            toast("Projeto excluído.", false);
            loadProjects(-1);
        }, err -> toast(err.getMessage(), true));
    }

    // ---------- tema e demo ----------

    private void switchTheme() {
        long selected = board.current() == null ? -1 : board.current().id();
        Theme.toggle();
        console.detach();
        build();
        revalidate();
        repaint();
        loadProjects(selected);
    }

    private void resetDemo() {
        if (!(api.transport() instanceof DemoTransport demo)) return;
        if (!Forms.confirm(this, "Restaurar exemplo", "Os dados da demo voltam ao exemplo inicial. Suas alterações serão perdidas.", "Restaurar")) return;
        Async.run(() -> { demo.reset(); return true; }, ok -> {
            api.log().clear();
            toast("Exemplo restaurado.", false);
            loadProjects(-1);
        }, err -> toast(err.getMessage(), true));
    }
}
