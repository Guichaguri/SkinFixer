package guichaguri.skinfixer;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;

/**
 * @author Guilherme Chaguri
 */
public class SkinInstaller extends JFrame implements ActionListener {

    private static final String VERSION_CHOOSER = "VERSION_CHOOSER";
    private static final String INSTALL = "INSTALL";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch(Exception ex) {}

                SkinInstaller installer = new SkinInstaller();
                installer.setVisible(true);
            }
        });
    }

    private static File getSuggestedMinecraftDirectory() {
        // Adapted from Forge installer
        String userHomeDir = System.getProperty("user.home", ".");
        String osType = System.getProperty("os.name").toLowerCase();

        if((osType.contains("win")) && (System.getenv("APPDATA") != null)) {
            return new File(System.getenv("APPDATA"), ".minecraft");
        } else if(osType.contains("mac")) {
            return new File(userHomeDir, "Library/Application Support/minecraft");
        } else {
            return new File(userHomeDir, ".minecraft");
        }
    }

    private JTextField minecraftPath;
    private JComboBox version;

    private SkinInstaller() {
        setTitle("SkinFixer Installer");

        Container container = getContentPane();

        container.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 5;
        c.ipady = 5;
        c.insets = new Insets(5, 5, 5, 5);

        JLabel title = new JLabel("SkinFixer");
        title.setFont(title.getFont().deriveFont(32F));
        container.add(title, c);

        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        minecraftPath = new JTextField(getSuggestedMinecraftDirectory().getAbsolutePath());
        container.add(minecraftPath, c);

        c.gridy = 2;
        JButton install = new JButton("Install");
        install.setActionCommand(VERSION_CHOOSER);
        install.addActionListener(this);
        container.add(install, c);

        setSize(400, 225);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();

        if(action.equals(VERSION_CHOOSER)) {
            File mc = new File(minecraftPath.getText());
            File versions = new File(mc, "versions");

            JDialog dialog = new JDialog();

            dialog.setTitle("Select a Version");
            dialog.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;

            version = new JComboBox();
            for(File f : versions.listFiles()) {
                if(f.isDirectory()) version.addItem(f.getName());
            }
            dialog.add(version, c);

            JButton install = new JButton("Install");
            install.setActionCommand(INSTALL);
            install.addActionListener(this);
            dialog.add(install, c);

            Dimension d = dialog.getPreferredSize();
            dialog.setSize(new Dimension(d.width + 50, d.height + 50));
            dialog.setLocationRelativeTo(this);

            dialog.setVisible(true);
        } else if(action.equals(INSTALL)) {
            //TODO
        }
    }
}
