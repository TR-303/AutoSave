package com.github.tr303.autosave;

import javax.swing.tree.DefaultMutableTreeNode;

// 界面与功能层共用的树数据结构，用于传递项目树结构
class CustomTreeNode extends DefaultMutableTreeNode {
    private final boolean isDir;

    public CustomTreeNode(String name, boolean isDir) {
        super(name);
        this.isDir = isDir;
    }

    public boolean isDirectory() {
        return isDir;
    }
}