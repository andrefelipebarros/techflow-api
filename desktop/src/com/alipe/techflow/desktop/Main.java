package com.alipe.techflow.desktop;

import com.alipe.techflow.desktop.model.Models.User;
import com.alipe.techflow.desktop.net.RequestLog;
import com.alipe.techflow.desktop.net.TechFlowApi;
import com.alipe.techflow.desktop.ui.LoginFrame;
import com.alipe.techflow.desktop.ui.MainFrame;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Ponto de entrada do TechFlow Desktop. */
public final class Main {
    private Main() {}

    private static final RequestLog LOG = new RequestLog();

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // segue com o visual padrão
            }
            new LoginFrame(LOG).setVisible(true);
        });
    }

    public static void openMain(JFrame from, TechFlowApi api, User user) {
        MainFrame main = new MainFrame(api, user);
        main.setVisible(true);
        from.dispose();
    }

    public static void logout(JFrame from, TechFlowApi api) {
        api.logout();
        LOG.clear();
        new LoginFrame(LOG).setVisible(true);
        from.dispose();
    }
}
