package com.github.tr303.autosave;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AutoSaveWindow extends DialogWrapper {

    private JPanel versionPanel;
    private JPanel treePanel;
    private JPanel textPanel;

    public AutoSaveWindow(Project project) {
        super(project);
        setTitle("AutoSave");
        setModal(false);
        setSize(800, 600);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();

        mainPanel.add(toolBar, BorderLayout.NORTH);

        versionPanel = new JPanel();
        versionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel("nothing");
        versionPanel.add(label);
        versionPanel.add(label);

        mainPanel.add(versionPanel, BorderLayout.WEST);

        return mainPanel;
    }
}
