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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Consumer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 插件界面窗体
public class AutoSaveWindow extends DialogWrapper {
    private AutoSaveFunctional ASF;
    private VersionPanel versionPanel;
    private TreePanel treePanel;
    private TextPanel textPanel;
    private String selectedVersionHash;

    public AutoSaveWindow(Project project) {
        super(project);
        ASF = new AutoSaveFunctional(project);
        setTitle("AutoSave");
        setModal(false);
        setSize(1000, 700);
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

        //上下两层：上层三栏，下层放操作按钮
        FunctionPanel functionPanel = new FunctionPanel();
        ActionPanel actionPanel = new ActionPanel(this::onDeleteAction, this::onRevertAction); // 添加删除和插入按钮事件
        mainPanel.add(functionPanel, BorderLayout.NORTH);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        //上层三栏
        versionPanel = new VersionPanel(ASF, this::updateTreePanel); // 渲染版本列表
        textPanel = new TextPanel(); // 初始化TextPanel
        treePanel = new TreePanel(this::onFileSelected); // 当选中文件时调用回调
        functionPanel.add(versionPanel, BorderLayout.WEST);
        functionPanel.add(treePanel, BorderLayout.CENTER);
        functionPanel.add(textPanel, BorderLayout.EAST);

        //默认展示第一个版本
        if (!ASF.getVersionList().isEmpty()) {
            treePanel.setTree(ASF.getTreeNodeByVersionHash(ASF.getVersionList().get(0).rootObject));
        }

        return mainPanel;
    }

    // 删除按钮的事件处理
    private void onDeleteAction() {
        if (selectedVersionHash == null) {
            JOptionPane.showMessageDialog(null, "Please Choose A Version to Delete");
            return;
        }

        boolean success = ASF.deleteVersion(selectedVersionHash);
        if (success) {
            JOptionPane.showMessageDialog(null, "Delete Successful！");
            versionPanel.refresh(ASF);
        } else {
            JOptionPane.showMessageDialog(null, "Fail，can't detect the version selected");
        }
    }

    // 回溯按钮的事件处理
    private void onRevertAction() {
        if (selectedVersionHash == null) {
            JOptionPane.showMessageDialog(null, "Please Choose A Version to Revert");
            return;
        }

        boolean success = ASF.revertToVersion(selectedVersionHash);
        if (success) {
            JOptionPane.showMessageDialog(null, "Revert Successful！");
        } else {
            JOptionPane.showMessageDialog(null, "Fail，can't detect the version selected");
        }
    }

    // 更新TreePanel中显示的树结构
    private void updateTreePanel(String versionHash) {
        this.selectedVersionHash = versionHash; // 存储选中的版本哈希
        AutoSaveFunctional.CustomTreeNode rootNode = ASF.getTreeNodeByVersionHash(versionHash);
        treePanel.setTree(rootNode); // 更新TreePanel的树结构
    }

    // 当文件被选中时，加载并显示文件内容
    private void onFileSelected(AutoSaveFunctional.CustomTreeNode selectedNode) {
        if (selectedVersionHash != null) {
            String fileContent = ASF.getFileContentForVersionAndPath(selectedVersionHash, selectedNode); // 获取文件内容
            textPanel.setFileContent(fileContent); // 显示文件内容
        }
    }
}

//上层面板
class FunctionPanel extends JPanel {
    public FunctionPanel() {
        setPreferredSize(new Dimension(1000, 600));
        setLayout(new BorderLayout());
    }
}

// 左面板，用于显示版本列表
class VersionPanel extends JPanel {
    private ArrayList<VersionItem> items = new ArrayList<>();
    private Consumer<String> onVersionSelected; // 回调，用于通知AutoSaveWindow版本被选中

    public VersionPanel(AutoSaveFunctional ASF, Consumer<String> onVersionSelected) {
        this.onVersionSelected = onVersionSelected; // 设置回调
        setPreferredSize(new Dimension(200, 600));
        setLayout(new BorderLayout()); // 使用 BorderLayout

        JPanel itemContainer = new JPanel();
        itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS)); // 纵向排列

        // 从 ASF 中获取所有版本信息并渲染
        ArrayList<AutoSaveFunctional.VersionInfo> versionList = ASF.getVersionList();
        for (AutoSaveFunctional.VersionInfo versionInfo : versionList) {
            Date versionDate = parseDate(versionInfo.timestamp); // 解析时间戳
            VersionItem item = new VersionItem(versionInfo.tag, versionDate, this, versionInfo.rootObject);
            items.add(item);
            itemContainer.add(item);
        }

        if (!items.isEmpty()) {
            items.get(0).mark(true); // 默认选中第一个版本
        }

        JBScrollPane scrollPane = new JBScrollPane(itemContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
    }

    // 将字符串时间戳解析为Date对象
    private Date parseDate(String timestamp) {
        // 使用DateTimeFormatter来解析字符串时间戳
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return new Date(); // 如果解析失败，返回当前时间（但最好处理错误情况）
        }
    }

    // 选中某个版本
    public void itemClicked(VersionItem clickedItem) {
        for (VersionItem item : items) {
            if (item != clickedItem) {
                item.mark(false); // 取消标记其他项
            }
        }
        clickedItem.mark(true); // 标记被点击的项
        // 通知AutoSaveWindow更新中间栏
        if (onVersionSelected != null) {
            onVersionSelected.accept(clickedItem.getVersionHash()); // 传递选中的版本哈希
        }
    }

    //刷新版本列表
    public void refresh(AutoSaveFunctional ASF) {
        removeAll(); // 清空当前面板的内容
        ArrayList<AutoSaveFunctional.VersionInfo> versionList = ASF.getVersionList();// 重新获取版本列表

        setLayout(new BorderLayout()); // 使用 BorderLayout
        JPanel itemContainer = new JPanel();
        itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS)); // 纵向排列
        for (AutoSaveFunctional.VersionInfo versionInfo : versionList) {
            Date versionDate = parseDate(versionInfo.timestamp); // 解析时间戳
            VersionItem item = new VersionItem(versionInfo.tag, versionDate, this, versionInfo.rootObject);
            items.add(item); // 添加到列表中
            itemContainer.add(item);
        }
        JBScrollPane scrollPane = new JBScrollPane(itemContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        revalidate(); // 重新布局
        repaint();    // 重新绘制
    }

}

//左面板的item
class VersionItem extends JPanel {
    public boolean isSelected;
    private VersionPanel parentPanel; // 引用父面板
    private String versionHash;       // 版本的唯一标识

    public VersionItem(String name, Date date, VersionPanel parentPanel, String versionHash) {
        this.parentPanel = parentPanel; // 初始化父面板
        this.versionHash = versionHash; // 初始化版本哈希
        isSelected = false;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(nameLabel);

        JLabel timeLabel = new JLabel(date.toString());
        timeLabel.setFont(nameLabel.getFont().deriveFont(10f));
        timeLabel.setForeground(JBColor.blue);
        timeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(timeLabel);

        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 添加鼠标监听器
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected)
                    setBackground(JBColor.gray);
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

    public String getVersionHash() {
        return versionHash; // 返回该版本的唯一标识
    }

    public void mark(boolean selected) {
        if (selected) {
            setBackground(JBColor.cyan);
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
    private Consumer<AutoSaveFunctional.CustomTreeNode> onFileSelected; // 回调接口

    public TreePanel(Consumer<AutoSaveFunctional.CustomTreeNode> onFileSelected) {
        this.onFileSelected = onFileSelected; // 初始化回调接口
        setPreferredSize(new Dimension(250, 600));
        setLayout(new BorderLayout());

        tree = new Tree();
        JBScrollPane scrollPane = new JBScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);

        // 添加树节点选择监听器
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                // 获取选中的节点
                AutoSaveFunctional.CustomTreeNode selectedNode = (AutoSaveFunctional.CustomTreeNode) tree.getLastSelectedPathComponent();
                if (selectedNode != null && !selectedNode.isDirectory()) {
                    // 只处理文件节点，调用回调方法
                    onFileSelected.accept(selectedNode);
                }
            }
        });
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
        textPane.setForeground(UIManager.getColor("Editor.foreground"));

        add(new JBScrollPane(textPane), BorderLayout.CENTER);
    }

    // 设置文件内容的方法
    public void setFileContent(String content) {
        textPane.setText(content);
    }
}

//操作按钮
class ActionPanel extends JPanel {
    public ActionPanel(Runnable deleteAction, Runnable revertAction) {
        setPreferredSize(new Dimension(1000, 40));
        setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton deleteButton = new JButton("Delete");
        JButton revertButton = new JButton("Revert");

        deleteButton.addActionListener(e -> deleteAction.run());
        revertButton.addActionListener(e -> revertAction.run());

        add(revertButton);
        add(deleteButton);
    }
}

class ToolPanel extends JPanel {

}