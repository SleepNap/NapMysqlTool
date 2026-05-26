## NapMysqlTool

### 介绍
mysql启停软件，这个项目诞生其实是因为我的另一个项目HeavenMS-Nap。  
冒险岛的服务端依赖mysql，但是我又不想用PHPStudy，所以就弄了一个这个东西。

gitee不能上传大于100M的东西，但是github可以。对不起，github太香了。  
整个项目已经迁移到了github，下载链接：https://github.com/SleepNap/NapMysqlTool/releases/latest  

补充：  
mysql文件夹可以是我项目下的mysql文件夹，我项目的文件夹版本为MySQL5.7.40，也是mysql5.7最新的版本，取自于mysql官方。同时也支持PHPStudy下的mysql文件夹，PHPStudy下的mysql文件夹更精简。  

### 常见启动问题

**1. 未安装VC++依赖包**  
MySQL运行依赖Visual C++。程序目录下有MySQL依赖库（如`vcredist_x64.exe`），需要手动安装。

**2. 端口被占用（已有MySQL实例运行）**  
如果本机之前安装过MySQL，安装后默认会自启动，导致3306端口被占用，NapMysqlTool无法再启动自己的MySQL。排查步骤：
- 打开任务管理器，在进程列表中搜索`mysqld`，有则结束进程
- 打开服务管理器（`services.msc`），查找MySQL相关服务（如`MySQL`、`MySQL57`等），将其停止并设置为"禁用"

**3. 异常关机导致无法启动**  
关机前没有先停止MySQL就直接关机，可能导致pid文件未被删除，再次启动失败。解决方法：在工具"其他功能"面板中点击"修复"按钮，会自动尝试修复并清理残留的pid文件。  