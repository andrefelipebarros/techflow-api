package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.model.Models.Priority;
import com.alipe.techflow.desktop.model.Models.Project;
import com.alipe.techflow.desktop.model.Models.ProjectStatus;
import com.alipe.techflow.desktop.model.Models.Task;
import com.alipe.techflow.desktop.model.Models.TaskStatus;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Janelas de cadastro e confirmação. */
public final class Forms {
    private Forms() {}

    public static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record ProjectInput(String name, String description, ProjectStatus status) {}

    public record TaskInput(String title, String description, Priority priority, LocalDate dueDate, TaskStatus status) {}

    /** Base das janelas: título, corpo, mensagem de erro e botões. */
    private static final class Dialog extends JDialog {
        final JLabel error = Widgets.label(" ", Font.PLAIN, 13, Theme.danger);
        final JPanel body = new JPanel(new GridBagLayout());
        boolean confirmed;
        Supplier<String> validator = () -> null;

        Dialog(Component owner, String title, String okLabel, boolean danger) {
            super(owner == null ? null : SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
            JPanel root = new JPanel(new BorderLayout(0, 14));
            root.setBackground(Theme.surface);
            root.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
            root.add(Widgets.label(title, Font.BOLD, 19, Theme.ink), BorderLayout.NORTH);
            body.setOpaque(false);
            JPanel south = new JPanel(new BorderLayout());
            south.setOpaque(false);
            south.add(error, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
            buttons.setOpaque(false);
            Widgets.Btn cancel = Widgets.button("Cancelar", Widgets.Kind.DEFAULT);
            Widgets.Btn ok = Widgets.button(okLabel, danger ? Widgets.Kind.DANGER : Widgets.Kind.PRIMARY);
            cancel.addActionListener(e -> dispose());
            ok.addActionListener(e -> {
                String problem = validator.get();
                if (problem != null) { error.setText(problem); return; }
                confirmed = true;
                dispose();
            });
            buttons.add(cancel);
            buttons.add(ok);
            south.add(buttons, BorderLayout.SOUTH);
            root.add(body, BorderLayout.CENTER);
            root.add(south, BorderLayout.SOUTH);
            setContentPane(root);
            getRootPane().setDefaultButton(ok);
            getRootPane().registerKeyboardAction(e -> dispose(), javax.swing.KeyStroke.getKeyStroke("ESCAPE"),
                    javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        int row;

        void addRow(String label, Component c) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0; g.gridy = row++; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(0, 0, 3, 0);
            body.add(Widgets.label(label, Font.BOLD, 13, Theme.ink), g);
            g.gridy = row++;
            g.insets = new Insets(0, 0, 12, 0);
            body.add(c, g);
        }

        boolean show(int width) {
            pack();
            setSize(new Dimension(width, getHeight()));
            setLocationRelativeTo(getOwner());
            setVisible(true);
            return confirmed;
        }
    }

    public static ProjectInput project(Component owner, Project existing) {
        Dialog d = new Dialog(owner, existing == null ? "Novo projeto" : "Editar projeto", "Salvar", false);
        JTextField name = Widgets.field(24);
        JTextArea desc = Widgets.area(4);
        JComboBox<ProjectStatus> status = Widgets.combo(ProjectStatus.values());
        if (existing != null) {
            name.setText(existing.name());
            desc.setText(existing.description() == null ? "" : existing.description());
            status.setSelectedItem(existing.status());
        }
        d.addRow("Nome", name);
        d.addRow("Descrição", Widgets.framed(desc));
        d.addRow("Status", status);
        d.validator = () -> {
            String n = name.getText().trim();
            if (n.isEmpty()) return "Informe o nome do projeto.";
            if (n.length() > 150) return "O nome pode ter até 150 caracteres.";
            if (desc.getText().length() > 1000) return "A descrição pode ter até 1000 caracteres.";
            return null;
        };
        if (!d.show(460)) return null;
        return new ProjectInput(name.getText().trim(), desc.getText().trim(), (ProjectStatus) status.getSelectedItem());
    }

    public static TaskInput task(Component owner, Task existing) {
        Dialog d = new Dialog(owner, existing == null ? "Nova tarefa" : "Editar tarefa", "Salvar", false);
        JTextField title = Widgets.field(24);
        JTextArea desc = Widgets.area(4);
        JComboBox<Priority> priority = Widgets.combo(Priority.values());
        priority.setSelectedItem(Priority.MEDIUM);
        JTextField due = Widgets.field(10);
        JComboBox<TaskStatus> status = Widgets.combo(TaskStatus.values());
        if (existing != null) {
            title.setText(existing.title());
            desc.setText(existing.description() == null ? "" : existing.description());
            priority.setSelectedItem(existing.priority());
            if (existing.dueDate() != null) due.setText(existing.dueDate().format(BR));
            status.setSelectedItem(existing.status());
        }
        d.addRow("Título", title);
        d.addRow("Descrição", Widgets.framed(desc));
        JPanel pair = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        pair.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(0, 3));
        left.setOpaque(false);
        left.add(Widgets.label("Prioridade", Font.BOLD, 13, Theme.ink), BorderLayout.NORTH);
        left.add(priority, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(0, 3));
        right.setOpaque(false);
        right.add(Widgets.label("Prazo (dd/mm/aaaa)", Font.BOLD, 13, Theme.ink), BorderLayout.NORTH);
        right.add(due, BorderLayout.CENTER);
        pair.add(left);
        pair.add(right);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = d.row++; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 12, 0);
        d.body.add(pair, g);
        if (existing != null) d.addRow("Status", status);
        d.validator = () -> {
            String t = title.getText().trim();
            if (t.isEmpty()) return "Informe o título da tarefa.";
            if (t.length() > 180) return "O título pode ter até 180 caracteres.";
            if (desc.getText().length() > 2000) return "A descrição pode ter até 2000 caracteres.";
            if (!due.getText().isBlank() && parseDate(due.getText()) == null) return "Use o prazo no formato dd/mm/aaaa.";
            return null;
        };
        if (!d.show(480)) return null;
        return new TaskInput(title.getText().trim(), desc.getText().trim(), (Priority) priority.getSelectedItem(),
                parseDate(due.getText()), (TaskStatus) status.getSelectedItem());
    }

    private static LocalDate parseDate(String text) {
        String t = text.trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t, BR);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean confirm(Component owner, String title, String message, String okLabel) {
        Dialog d = new Dialog(owner, title, okLabel, true);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        d.body.add(Widgets.label("<html><div style='width:300px'>" + escape(message) + "</div></html>", Font.PLAIN, 14, Theme.muted), g);
        return d.show(420);
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static Window window(Component c) { return SwingUtilities.getWindowAncestor(c); }
}
