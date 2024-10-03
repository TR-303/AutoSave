package com.github.tr303.autosave;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

// 插件界面窗体
public class AutoSaveWindow extends DialogWrapper {
    public AutoSaveWindow(Project project) {
        super(project);
        setTitle("AutoSave");
        setModal(false);
        setSize(1000, 800);
        init();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0]; // 返回空数组，不显示任何按钮
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        mainPanel.add(new VersionPanel(), BorderLayout.WEST);
        mainPanel.add(new TreePanel(), BorderLayout.CENTER);
        mainPanel.add(new TextPanel(), BorderLayout.EAST);

        return mainPanel;
    }
}

// 左面板，用于显示版本列表
class VersionPanel extends JPanel {
    private ArrayList<VersionItem> items = new ArrayList<>();

    public VersionPanel() {
        setPreferredSize(new Dimension(200, 600));

        setLayout(new BorderLayout()); // 使用 BorderLayout

        JPanel itemContainer = new JPanel();
        itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS)); // 纵向排列


        for (int i = 1; i <= 20; ++i) {
            VersionItem item = new VersionItem("Version " + i, new Date());
            items.add(item);
            itemContainer.add(item);
        }

        items.get(0).mark(true);

        JBScrollPane scrollPane = new JBScrollPane(itemContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
    }
}

// 左面板的item
class VersionItem extends JPanel {
    public VersionItem(String name, Date date) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(nameLabel);

        JLabel timeLabel = new JLabel(date.toString());
        timeLabel.setFont(nameLabel.getFont().deriveFont(10f));
        timeLabel.setForeground(JBColor.gray);
        timeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(timeLabel);

        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    public void mark(boolean selected) {
        if (selected) setBackground(UIManager.getColor("Selection.background"));
        else setBackground(UIManager.getColor("Panel.background"));
        revalidate();
        repaint();
    }
}

// 中间面板，用于显示项目树结构
class TreePanel extends JPanel {
    private Tree tree;

    public TreePanel() {
        setPreferredSize(new Dimension(250, 600));

        setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new AutoSaveFunctional.CustomTreeNode("autosave", true);

        root.add(new AutoSaveFunctional.CustomTreeNode("child1", true));
        root.add(new AutoSaveFunctional.CustomTreeNode("child2", false));
        root.add(new AutoSaveFunctional.CustomTreeNode("child3", true));

        tree = new Tree(root);

        tree.setBackground(UIManager.getColor("Tree.background"));
        tree.setForeground(UIManager.getColor("Tree.foreground"));

        tree.setCellRenderer(new CustomTreeCellRender());

        add(new JBScrollPane(tree), BorderLayout.CENTER);
    }
}

// 用于自定义控制树节点图表
class CustomTreeCellRender extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        AutoSaveFunctional.CustomTreeNode node = (AutoSaveFunctional.CustomTreeNode) value;
        if (node.isDirectory()) this.setIcon(AllIcons.Nodes.Folder);
        else this.setIcon(AllIcons.Nodes.FilePrivate);

        if (sel) {
            setBackground(UIManager.getColor("Tree.selectionBackground"));
            setForeground(UIManager.getColor("Tree.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Tree.background"));
            setForeground(UIManager.getColor("Tree.foreground"));
        }

        return this;
    }
}

class TextPanel extends JPanel {
    private JTextPane textPane;

    public TextPanel() {
        setPreferredSize(new Dimension(550, 600));

        setLayout(new BorderLayout());

        // 创建 JTextPane 并设置为只读
        textPane = new JTextPane();
        textPane.setEditable(false); // 设置为只读
        textPane.setContentType("text/plain"); // 设置文本类型

        textPane.setFont(UIManager.getFont("Editor.Font"));
        textPane.setBackground(UIManager.getColor("Editor.background")); // 深色背景
        textPane.setForeground(UIManager.getColor("Editor.foreground")); // 白色文本

        add(new JBScrollPane(textPane), BorderLayout.CENTER);
    }

    // 设置文件内容的方法
    public void setFileContent(String content) {
        textPane.setText(content);
    }
}

class ToolPanel extends JPanel {

}