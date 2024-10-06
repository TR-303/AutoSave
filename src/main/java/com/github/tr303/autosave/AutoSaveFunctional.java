package com.github.tr303.autosave;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class AutoSaveFunctional {
    private final Project project;
    private final AutoSaveData ASD;

    private final Logger log = Logger.getInstance(AutoSaveData.class);

    public AutoSaveFunctional(Project project) {
        this.project = project;
        ASD = new AutoSaveData(project);
    }

    public static class VersionInfo {
        String timestamp;
        String rootObject;
        String tag;

        public VersionInfo(String timestamp, String rootObject, String tag) {
            this.timestamp = timestamp;
            this.rootObject = rootObject;
            this.tag = tag;
        }
    }

    // 获得所有版本
    public ArrayList<VersionInfo> getVersionList() {
        String versionContent = ASD.getVersionsFileContent();
        ArrayList<VersionInfo> versionList = new ArrayList<>();

        String[] lines = versionContent.split("\n");

        for (String line : lines) {
            String[] parts = line.split("\0");
            if (parts.length == 3) {
                String timestamp = parts[0].trim();
                String tag = parts[1].trim();
                String rootObject = parts[2].trim();

                versionList.add(new VersionInfo(timestamp, tag, rootObject));
            }
        }

        return versionList;
    }

    public Boolean saveVersionList(ArrayList<VersionInfo> versionList) {
        StringBuilder versionContent = new StringBuilder();
        for (VersionInfo info : versionList) {
            versionContent.append(info.timestamp).append('\0').append(info.rootObject).append('\0').append(info.tag).append('\n');
        }
        return ASD.saveVersionFileContent(versionContent.toString());
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
            for (String child : children)
                if (!child.equals(".autosave")) {
                    String[] parts = child.split("\0");
                    objectNode.add(getTreeNodeByVersionHash(parts[0].trim()));
                }
        }

        return objectNode;
    }

    // 获得某个版本的某个文件内容String
    public String getFileContentForVersionAndPath(String versionHash, CustomTreeNode target) {
        ArrayList<String> path = new ArrayList<>();
        CustomTreeNode node = target;
        while (node.getParent() != null) {
            path.add(0, node.getName());
            node = (CustomTreeNode) node.getParent();
        }

        // 递归查找文件哈希
        String hash = findFileHashInVersion(versionHash, path);

        if (hash == null) {
            // 如果当前版本没有找到该文件，回退到前一个版本
            hash = findFileInPreviousVersions(versionHash, path);
        }

        if (hash != null) {
            // 获取文件内容并返回
            String fileContent = ASD.getObjectContentByHash(hash);
            return fileContent.substring(fileContent.indexOf('\0') + 1); // 返回文件内容部分
        }

        return null; // 如果未找到文件，返回 null
    }

    private String findFileHashInVersion(String versionHash, ArrayList<String> path) {
        String hash = versionHash;
        while (!path.isEmpty()) {
            String objectContent = ASD.getObjectContentByHash(hash);
            if (objectContent == null) {
                return null; // 如果当前哈希无效，返回 null
            }

            String[] entries = objectContent.substring(objectContent.indexOf('\0') + 1).split("\n");
            boolean found = false;

            // 遍历目录条目，查找路径中对应的文件或目录
            for (String entry : entries) {
                String[] parts = entry.split("\0");
                if (parts[2].equals(path.get(0)) && (path.size() == 1 || parts[1].equals("DIR")) && (path.size() > 1 || parts[1].equals("FIL"))) {
                    path.remove(0); // 匹配成功，移除当前路径部分
                    hash = parts[0]; // 更新哈希值
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null; // 如果未找到匹配项，返回 null
            }
        }
        return hash; // 返回最终的文件哈希
    }

    private String findFileInPreviousVersions(String versionHash, ArrayList<String> path) {
        ArrayList<AutoSaveFunctional.VersionInfo> versionList = getVersionList();

        // 获取当前版本的索引
        int currentVersionIndex = -1;
        for (int i = 0; i < versionList.size(); i++) {
            if (versionList.get(i).rootObject.equals(versionHash)) {
                currentVersionIndex = i;
                break;
            }
        }

        // 回退到前一个版本查找
        for (int i = currentVersionIndex + 1; i < versionList.size(); i++) {
            String previousVersionHash = versionList.get(i).rootObject;
            String fileHash = findFileHashInVersion(previousVersionHash, new ArrayList<>(path)); // 使用新的路径副本
            if (fileHash != null) {
                return fileHash; // 找到文件时返回其哈希
            }
        }

        return null; // 未找到文件内容
    }


    public static String getCurrentTimeFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }

    // 删除某个版本
    public Boolean deleteVersion(String versionHash) {
        ArrayList<VersionInfo> versions = getVersionList();
        String targetHash = null;

        for (VersionInfo version : versions) {
            if (version.rootObject.equals(versionHash)) {
                targetHash = version.rootObject;
                versions.remove(version);
                saveVersionList(versions);
                break;
            }
        }

        if (targetHash == null) {
            return false;
        }

        AutoSaveData.ReferenceCounter rc = ASD.getReferenceCounter();
        rc.loadReferences();
        deleteVersionTreeRecursive(targetHash, rc);
        rc.saveReferences();
        return true;
    }

    private void deleteVersionTreeRecursive(String hash, AutoSaveData.ReferenceCounter rc) {
        String content = ASD.getObjectContentByHash(hash);
        if (ASD.isDirectory(content)) {
            String trueContent = content.substring(content.indexOf('\0') + 1);
            String[] entries = trueContent.split("\n");
            for (String entry : entries) {
                String[] parts = entry.split("\0");
                deleteVersionTreeRecursive(parts[0], rc);
            }
        }
        if (rc.decrement(hash)) ASD.deleteObjectOfHash(hash);
    }

    // 回溯到某个版本
    public Boolean revertToVersion(String versionHash) {
        ArrayList<VersionInfo> versions = getVersionList();
        String targetHash = null;
        String targetTime = null;

        for (VersionInfo version : versions) {
            if (version.rootObject.equals(versionHash)) {
                targetHash = version.rootObject;
                targetTime = version.timestamp;
                break;
            }
        }

        if (targetHash == null) {
            return false;
        }

        VirtualFile projectDir = VfsUtil.findFileByIoFile(new File(project.getBasePath()), true);
        if (projectDir == null) return null;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                for (VirtualFile child : projectDir.getChildren())
                    if (!child.getName().equals(".autosave")) {
                        child.delete(this);
                    }
            } catch (IOException e) {
                log.error(e);
            }
        });
        revertVersionTreeRecursive(versionHash, projectDir);

        for (VersionInfo info : versions) {
            if (info.timestamp.compareTo(targetTime) > 0) deleteVersion(info.rootObject);
        }

        return true;
    }

    private void revertVersionTreeRecursive(String hash, VirtualFile file) {
        String objectContent = ASD.getObjectContentByHash(hash);
        if (ASD.isDirectory(objectContent)) {
            String trueContent = objectContent.substring(objectContent.indexOf('\0') + 1);
            String[] entries = trueContent.split("\n");
            for (String entry : entries) {
                String[] parts = entry.split("\0");
                String entryHash = parts[0];
                String entryType = parts[1];
                String entryName = parts[2];

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        if ("DIR".equals(entryType)) {
                            VirtualFile newDir = file.createChildDirectory(this, entryName);
                            revertVersionTreeRecursive(entryHash, newDir);
                        } else if ("FIL".equals(entryType)) {
                            VirtualFile newFile = file.createChildData(this, entryName);
                            revertVersionTreeRecursive(entryHash, newFile);
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                });
            }
        } else {
            String trueContent = objectContent.substring(objectContent.indexOf('\0') + 1);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    file.setBinaryContent(trueContent.getBytes());
                } catch (IOException e) {
                    log.error(e);
                }
            });
        }
    }

    // 保存现在的项目作为一个版本
    public Boolean saveCurrentProjectAsVersion(String tag) {
        String projectPath = project.getBasePath();
        if (projectPath == null) return null;
        VirtualFile projectDir = VfsUtil.findFileByIoFile(new File(projectPath), true);

        ArrayList<VersionInfo> versionList = getVersionList();

        AutoSaveData.ReferenceCounter rc = ASD.getReferenceCounter();
        rc.loadReferences();

        if (!versionList.isEmpty()) {
            String hash = saveVersionTreeRecursive(versionList.get(0).rootObject, projectDir, rc);
            if (versionList.get(0).rootObject.equals(hash)) return false;
            else {
                versionList.add(0, new VersionInfo(getCurrentTimeFormatted(), hash, tag));
                rc.increment(hash);
                rc.saveReferences();
                return saveVersionList(versionList);
            }
        } else {
            String hash = saveVersionTreeRecursive(null, projectDir, rc);
            versionList.add(0, new VersionInfo(getCurrentTimeFormatted(), hash, tag));
            rc.increment(hash);
            rc.saveReferences();
            return saveVersionList(versionList);
        }
    }

    private String saveVersionTreeRecursive(String hash, VirtualFile file, AutoSaveData.ReferenceCounter rc) {
        if (hash == null) {
            if (file.isDirectory()) {
                StringBuilder dirContent = new StringBuilder();
                for (VirtualFile child : file.getChildren())
                    if (!child.getName().equals(".autosave")) {
                        String childHash = saveVersionTreeRecursive(null, child, rc);
                        rc.increment(childHash);
                        dirContent.append(childHash).append(child.isDirectory() ? "\0DIR\0" : "\0FIL\0").append(child.getName()).append('\n');
                    }
                String finalContent = ASD.addPrefix(String.valueOf(dirContent), file.getName(), true);
                String finalHash = ASD.sha256(finalContent);
                ASD.saveObjectWithHash(finalContent, finalHash);
                return finalHash;
            } else {
                String fileContent = ASD.getFileContent(file);
                String finalContent = ASD.addPrefix(fileContent, file.getName(), false);
                String finalHash = ASD.sha256(finalContent);
                ASD.saveObjectWithHash(finalContent, finalHash);
                return finalHash;
            }
        }

        String objectContent = ASD.getObjectContentByHash(hash);

        if (!ASD.isDirectory(objectContent)) {
            String fileContent = ASD.addPrefix(ASD.getFileContent(file), file.getName(), false);

            String fileHash = ASD.sha256(fileContent);
            if (fileHash.equals(hash)) return hash;
            else {
                ASD.saveObjectWithHash(fileContent, fileHash);
                return fileHash;
            }
        }

        String[] entries = objectContent.substring(objectContent.indexOf('\0') + 1).split("\n");
        StringBuilder dirContent = new StringBuilder();
        for (VirtualFile child : file.getChildren())
            if (!child.getName().equals(".autosave")) {
                boolean found = false;
                for (String entry : entries) {
                    String[] parts = entry.split("\0");
                    if (parts[2].equals(child.getName())) {
                        if ((parts[1].equals("DIR") && child.isDirectory()) || (parts[1].equals("FIL") && !child.isDirectory())) {
                            String childHash = saveVersionTreeRecursive(parts[0], child, rc);
                            rc.increment(childHash);
                            dirContent.append(childHash).append('\0').append(parts[1]).append('\0').append(child.getName()).append('\n');
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    String childHash = saveVersionTreeRecursive(null, child, rc);
                    rc.increment(childHash);
                    dirContent.append(childHash).append(child.isDirectory() ? "\0DIR\0" : "\0FIL\0").append(child.getName()).append('\n');
                }
            }

        String finalContent = ASD.addPrefix(String.valueOf(dirContent), file.getName(), true);
        String finalHash = ASD.sha256(finalContent);
        if (!finalHash.equals(hash)) ASD.saveObjectWithHash(finalContent, finalHash);
        return finalHash;
    }
}
