package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.Main;
import com.alipe.techflow.desktop.net.DemoTransport;
import com.alipe.techflow.desktop.net.HttpTransport;
import com.alipe.techflow.desktop.net.RequestLog;
import com.alipe.techflow.desktop.net.TechFlowApi;
import com.alipe.techflow.desktop.net.Transport;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Tela de entrada: escolhe entre a demo local e a API real, e faz login ou cadastro. */
public final class LoginFrame extends JFrame {
    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginFrame.class);

    private final RequestLog log;
    private final Transport demo;
    private boolean live = PREFS.getBoolean("live", false);
    private boolean register;
    private boolean busy;

    private final Widgets.Btn demoSeg = Widgets.segment("Demo no computador");
    private final Widgets.Btn liveSeg = Widgets.segment("API real");
    private final JPanel liveBox = new JPanel(new BorderLayout(0, 6));
    private final JTextField url = Widgets.field(22);
    private final JLabel testResult = Widgets.label(" ", Font.PLAIN, 13, Theme.muted);
    private final Widgets.Btn loginTab = new Tab("Entrar").btn;
    private final Widgets.Btn registerTab = new Tab("Criar conta").btn;
    private final JPanel nameRow = new JPanel(new BorderLayout(0, 4));
    private final JTextField name = Widgets.field(22);
    private final JTextField email = Widgets.field(22);
    private final JPasswordField password = Widgets.password(22);
    private final JLabel error = Widgets.label(" ", Font.PLAIN, 13, Theme.danger);
    private final Widgets.Btn submit = Widgets.button("Entrar", Widgets.Kind.PRIMARY);

    private record Tab(Widgets.Btn btn) {
        Tab(String text) { this(Widgets.button(text, Widgets.Kind.TAB)); }
    }

    public LoginFrame(RequestLog log) {
        super("TechFlow");
        this.log = log;
        this.demo = new DemoTransport();
        setIconImage(Brand.icon(64));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        url.setText(PREFS.get("url", "http://localhost:8080"));

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Theme.bg);
        root.add(intro());
        root.add(card());
        setContentPane(root);
        refresh();
        setSize(1040, 660);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
    }

    // ---------- lado esquerdo ----------

    private JPanel intro() {
        JPanel p = new JPanel();
        p.setBackground(Theme.bg);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(56, 56, 40, 24));
        p.add(left(Brand.logo(20)));
        p.add(Box.createVerticalStrut(34));
        p.add(left(Widgets.label("<html><div style='width:350px'>Projetos e tarefas, com cada chamada à API à vista</div></html>", Font.BOLD, 32, Theme.ink)));
        p.add(Box.createVerticalStrut(16));
        p.add(left(Widgets.label("<html><div style='width:340px'>Uma interface para a TechFlow API. Na demo tudo roda neste computador. Com a API real, o app fala com o seu servidor Spring Boot.</div></html>", Font.PLAIN, 15, Theme.muted)));
        p.add(Box.createVerticalStrut(28));

        Widgets.Rounded term = new Widgets.Rounded(Theme.TERM_BG, null, 12);
        term.setLayout(new BorderLayout());
        term.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        String html = "<html><div style='font-family:monospace;font-size:11px;color:#D8DCF5;width:360px'>"
                + "<span style='color:#9AA8FF'>POST</span> /api/v1/auth/login<br>"
                + "<span style='color:#8089B3'>{ \"email\": \"demo@techflow.dev\",<br>&nbsp;&nbsp;\"password\": \"********\" }</span><br><br>"
                + "<span style='color:#5FD39B'>200</span> <span style='color:#8089B3'>18 ms</span><br>"
                + "<span style='color:#8089B3'>{ \"accessToken\": \"eyJhbGciOiJIUzI1...\",<br>&nbsp;&nbsp;\"tokenType\": \"Bearer\",<br>&nbsp;&nbsp;\"expiresInSeconds\": 3600 }</span></div></html>";
        term.add(new JLabel(html), BorderLayout.CENTER);
        term.setAlignmentX(0f);
        term.setMaximumSize(new Dimension(400, 170));
        p.add(term);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel left(java.awt.Component c) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(0f);
        row.add(c);
        return row;
    }

    // ---------- cartão de acesso ----------

    private JPanel card() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(Theme.bg);
        Widgets.Rounded card = new Widgets.Rounded(Theme.surface, Theme.line, 16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        card.setPreferredSize(new Dimension(400, 500));

        JPanel seg = new JPanel(new GridLayout(1, 2, 4, 0));
        seg.setOpaque(false);
        seg.add(demoSeg);
        seg.add(liveSeg);
        demoSeg.addActionListener(e -> { live = false; refresh(); });
        liveSeg.addActionListener(e -> { live = true; refresh(); });
        card.add(full(seg));
        card.add(Box.createVerticalStrut(14));

        liveBox.setOpaque(false);
        JPanel urlRow = new JPanel(new BorderLayout(8, 0));
        urlRow.setOpaque(false);
        urlRow.add(url, BorderLayout.CENTER);
        Widgets.Btn test = Widgets.button("Testar", Widgets.Kind.DEFAULT);
        test.addActionListener(e -> testConnection());
        urlRow.add(test, BorderLayout.EAST);
        liveBox.add(Widgets.label("Endereço da API", Font.BOLD, 13, Theme.ink), BorderLayout.NORTH);
        liveBox.add(urlRow, BorderLayout.CENTER);
        liveBox.add(testResult, BorderLayout.SOUTH);
        card.add(full(liveBox));
        card.add(Box.createVerticalStrut(10));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setOpaque(false);
        loginTab.addActionListener(e -> { register = false; refresh(); });
        registerTab.addActionListener(e -> { register = true; refresh(); });
        tabs.add(loginTab);
        tabs.add(Box.createHorizontalStrut(22));
        tabs.add(registerTab);
        card.add(full(tabs));
        card.add(Box.createVerticalStrut(12));

        nameRow.setOpaque(false);
        nameRow.add(Widgets.label("Nome", Font.BOLD, 13, Theme.ink), BorderLayout.NORTH);
        nameRow.add(name, BorderLayout.CENTER);
        card.add(full(nameRow));
        card.add(Box.createVerticalStrut(10));
        card.add(full(labeled("E-mail", email)));
        card.add(Box.createVerticalStrut(10));
        card.add(full(labeled("Senha", password)));
        card.add(Box.createVerticalStrut(8));
        card.add(full(error));
        card.add(Box.createVerticalStrut(6));

        submit.addActionListener(e -> submit());
        password.addActionListener(e -> submit());
        card.add(full(submit));
        card.add(Box.createVerticalStrut(8));
        Widgets.Btn demoLogin = Widgets.button("Usar a conta demo", Widgets.Kind.GHOST);
        demoLogin.addActionListener(e -> { live = false; refresh(); email.setText(DemoTransport.DEMO_EMAIL); password.setText(DemoTransport.DEMO_PASSWORD); register = false; refresh(); submit(); });
        card.add(full(demoLogin));
        card.add(Box.createVerticalGlue());

        wrap.add(card, new GridBagConstraints());
        return wrap;
    }

    private static JPanel labeled(String text, java.awt.Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(Widgets.label(text, Font.BOLD, 13, Theme.ink), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static <T extends javax.swing.JComponent> T full(T p) {
        p.setAlignmentX(0f);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private void refresh() {
        demoSeg.setSelectedLook(!live);
        liveSeg.setSelectedLook(live);
        liveBox.setVisible(live);
        loginTab.setSelectedLook(!register);
        registerTab.setSelectedLook(register);
        nameRow.setVisible(register);
        submit.setText(register ? "Criar conta" : "Entrar");
        error.setText(" ");
        full(liveBox);
        full(nameRow);
        revalidate();
        repaint();
    }

    // ---------- ações ----------

    private Transport transport() {
        return live ? new HttpTransport(url.getText()) : demo;
    }

    private void testConnection() {
        testResult.setForeground(Theme.muted);
        testResult.setText("Testando...");
        TechFlowApi probe = new TechFlowApi(transport(), new RequestLog());
        Async.run(probe::health, up -> {
            testResult.setForeground(up ? Theme.ok : Theme.danger);
            testResult.setText(up ? "A API respondeu e está no ar." : "A API respondeu, mas não está saudável.");
        }, err -> {
            testResult.setForeground(Theme.danger);
            testResult.setText(err.getMessage());
        });
    }

    private void submit() {
        if (busy) return;
        String mail = email.getText().trim();
        String pass = new String(password.getPassword());
        String who = name.getText().trim();
        if (register && who.isEmpty()) { error.setText("Informe seu nome."); return; }
        if (mail.isEmpty() || pass.isEmpty()) { error.setText("Informe e-mail e senha."); return; }
        if (register && pass.length() < 8) { error.setText("A senha precisa ter de 8 a 72 caracteres."); return; }
        if (live && url.getText().isBlank()) { error.setText("Informe o endereço da API."); return; }

        busy = true;
        error.setText(" ");
        submit.setEnabled(false);
        PREFS.putBoolean("live", live);
        PREFS.put("url", url.getText().trim());
        TechFlowApi api = new TechFlowApi(transport(), log);
        Async.run(() -> {
            if (register) api.register(who, mail, pass); else api.login(mail, pass);
            return api.me();
        }, user -> {
            busy = false;
            Main.openMain(this, api, user);
        }, err -> {
            busy = false;
            submit.setEnabled(true);
            error.setText(err.getMessage());
        });
    }
}
