package com.example.kinginstaller;

interface IMyUserService {
    void setInstaller(String packageName, String installerPackageName);
    void execCommand(String command);
    void destroy();
}