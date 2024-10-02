package com.github.tr303.autosave;

import com.intellij.openapi.project.Project;

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

        public VersionInfo(String timestamp, String tag) {
            this.timestamp = timestamp;
            this.tag = tag;
        }
    }

    // 获得所有版本
    public ArrayList<VersionInfo> getVersionList() {
        String versionContent = ASD.getVersionsFileContent();
        ArrayList<VersionInfo> versionList = new ArrayList<>();

        String[] lines = versionContent.split("\n");

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                String timestamp = parts[0].trim();
                String tag = parts[1].trim();

                versionList.add(new VersionInfo(timestamp, tag));
            }
        }

        return versionList;
    }
//
//    // 获得某个版本的目录树
//    public CustomTreeNode getDirectoryTreeForVersion(String versionHash);
//
//    // 获得某个版本的某个文件内容String
//    public String getFileContentForVersionAndPath(String versionHash, String filePath);
//
//    // 删除某个版本
//    public void deleteVersion(String versionHash);
//
//    // 回溯到某个版本
//    public void revertToVersion(String versionHash);
//
//    // 保存现在的项目作为一个版本
//    public void saveCurrentProjectAsVersion(String tag);
}
