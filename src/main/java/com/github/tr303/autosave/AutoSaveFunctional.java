package com.github.tr303.autosave;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AutoSaveFunctional {
    private final Project project;
    private final AutoSaveData ASD;

    public AutoSaveFunctional(Project project) {
        this.project = project;
        ASD = new AutoSaveData(project);
    }

    public static class VersionInfo {
        String timestamp;
        String tag;
        String rootObject;

        public VersionInfo(String timestamp, String tag, String rootObject) {
            this.timestamp = timestamp;
            this.tag = tag;
            this.rootObject = rootObject;
        }
    }

    // 获得所有版本
    public ArrayList<VersionInfo> getVersionList() {
        String versionContent = ASD.getVersionsFileContent();
        ArrayList<VersionInfo> versionList = new ArrayList<>();

        String[] lines = versionContent.split("\n");

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length == 3) {
                String timestamp = parts[0].trim();
                String tag = parts[1].trim();
                String rootObject = parts[2].trim();

                versionList.add(new VersionInfo(timestamp, tag, rootObject));
            }
        }

        return versionList;
    }

    // 界面与功能层共用的树数据结构，用于传递项目树结构
    public static class CustomTreeNode extends DefaultMutableTreeNode {
        private final boolean isDir;

        public CustomTreeNode(String name, boolean isDir) {
            super(name);
            this.isDir = isDir;
        }

        public boolean isDirectory() {
            return isDir;
        }

        public String getName() {
            return (String) getUserObject();
        }
    }

    // 用时间戳获得某个版本的目录树
    public CustomTreeNode getDirectoryTreeByTimeStamp(String timestamp) {
        ArrayList<VersionInfo> versions = getVersionList();

        VersionInfo target = versions.stream().filter(v -> v.timestamp.equals(timestamp)).findFirst().orElse(null);
        if (target != null) return getTreeNodeByVersionHash(target.rootObject);
        else return null;
    }

    // 递归算法，构造目录树
    public CustomTreeNode getTreeNodeByVersionHash(String hash) {
        String objectContent = ASD.getObjectContentByHash(hash);
        int sepIdx = objectContent.indexOf('\0');
        String fileName = objectContent.substring(4, sepIdx);
        String trueContent = objectContent.substring(sepIdx + 1);
        CustomTreeNode objectNode = new CustomTreeNode(fileName, ASD.isDirectory(objectContent));

        if (ASD.isDirectory(objectContent)) {
            String[] children = trueContent.split("\n");
            for (String child : children) {
                String[] parts = child.split("\0");
                objectNode.add(getTreeNodeByVersionHash(parts[0].trim()));
            }
        }

        return objectNode;
    }

    // 获得某个版本的某个文件内容String
    public String getFileContentForVersionAndPath(String versionHash, CustomTreeNode target) {
        ArrayList<String> path = new ArrayList<String>();
        CustomTreeNode node = target;
        while (node.getParent() != null) {
            path.add(0, node.getName());
            node = (CustomTreeNode) node.getParent();
        }

        String hash = versionHash;
        while (!path.isEmpty()) {
            String objectContent = ASD.getObjectContentByHash(hash);
            String[] entries = objectContent.substring(objectContent.indexOf('\0') + 1).split("\n");
            for (String entry : entries) {
                String[] parts = entry.split("\0");
                if (parts[2].equals(path.get(0)) && (path.size() == 1 || parts[1].equals("DIR")) && (path.size() > 1 || parts[1].equals("FIL"))) {
                    path.remove(0);
                    hash = parts[0];
                }
            }
        }

        String fileContent = ASD.getObjectContentByHash(hash);
        return fileContent.substring(fileContent.indexOf('\0') + 1);
    }

    //
//    // 删除某个版本
//    public void deleteVersion(String versionHash);
//
//    // 回溯到某个版本
//    public void revertToVersion(String versionHash);
//
    // 保存现在的项目作为一个版本
    public Boolean saveCurrentProjectAsVersion(String tag) {
        String projectPath = project.getBasePath();
        if (projectPath == null) return null;
        VirtualFile projectDir = VfsUtil.findFileByIoFile(new File(projectPath), true);

        ArrayList<VersionInfo> versionList = getVersionList();
        if (!versionList.isEmpty()) {
            boolean result = saveVersionTreeRecursive(versionList.get(0).rootObject, projectDir);
        }

    }

    public String saveVersionTreeRecursive(String hash, VirtualFile file) {
        String objectContent = ASD.getObjectContentByHash(hash);

        if (!ASD.isDirectory(objectContent)) {
            String fileContent = ASD.addPrefix(ASD.getFileContent(file), file.getName(), false);
            ;
            String fileHash = ASD.sha256(fileContent);
            if (fileHash.equals(hash)) return hash;
            else {
                ASD.saveObjectWithHash(fileContent, fileHash);
                return fileHash;
            }
        }

        boolean isSame = true;
        String[] entries = objectContent.substring(objectContent.indexOf('\0') + 1).split("\n");
        StringBuilder dirContent = new StringBuilder();
        for (VirtualFile child : file.getChildren()) {
            boolean found = false;
            for (String entry : entries) {
                String[] parts = entry.split("\0");
                if (parts[2].equals(child.getName())) {
                    if ((parts[1].equals("DIR") && child.isDirectory()) || (parts[1].equals("FIL") && !child.isDirectory())) {
                        String childHash = saveVersionTreeRecursive(parts[0], child);
                        dirContent.append(childHash).append('\0').append(parts[1]).append('\0').append(child.getName()).append('\n');
                        if (!parts[0].equals(childHash)) isSame = false;
                        found = true;
                        break;
                    }
                }
            }
            if (found) {

            } else {
                
            }
        }
    }
}
