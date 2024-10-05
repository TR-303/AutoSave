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
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 插件界面窗体
public class AutoSaveWindow extends DialogWrapper {
    private AutoSaveFunctional ASF;

    public AutoSaveWindow(Project project) {
        super(project);
        ASF = new AutoSaveFunctional(project);
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

        TreePanel treePanel = new TreePanel();

        mainPanel.add(new VersionPanel(), BorderLayout.WEST);
        mainPanel.add(treePanel, BorderLayout.CENTER);
        mainPanel.add(new TextPanel(), BorderLayout.EAST);

        treePanel.setTree(ASF.getTreeNodeByVersionHash(ASF.getVersionList().get(0).rootObject));

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
            VersionItem item = new VersionItem("Version " + i, new Date(), this); // 传入父面板引用
            items.add(item);
            itemContainer.add(item);
        }

        items.get(0).mark(true);

        JBScrollPane scrollPane = new JBScrollPane(itemContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void itemClicked(VersionItem clickedItem) {
        for (VersionItem item : items) {
            if (item != clickedItem) {
                item.mark(false); // 取消标记其他项
            }
        }
        clickedItem.mark(true); // 标记被点击的项
    }
}

//左面板的item
class VersionItem extends JPanel {
    public boolean isSelected;
    private VersionPanel parentPanel; // 引用父面板

    public VersionItem(String name, Date date, VersionPanel parentPanel) {
        this.parentPanel = parentPanel; // 初始化父面板
        isSelected = false;

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

        // 添加鼠标监听器
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected)
                    setBackground(Color.GRAY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected)
                    setBackground(UIManager.getColor("Panel.background"));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                parentPanel.itemClicked(VersionItem.this); // 通知父面板该项被点击
            }
        });
    }

    public void mark(boolean selected) {
        if (selected) {
            setBackground(Color.YELLOW);
        } else {
            setBackground(UIManager.getColor("Panel.background"));
        }
        revalidate();
        repaint();
        isSelected = selected;
    }
}

// 中间面板，用于显示项目树结构
class TreePanel extends JPanel {
    private Tree tree;

    public TreePanel() {
        setPreferredSize(new Dimension(250, 600));
        setLayout(new BorderLayout());

        tree = new Tree();
        JBScrollPane scrollPane = new JBScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setTree(AutoSaveFunctional.CustomTreeNode node) {
        tree.setModel(new DefaultTreeModel(node));
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