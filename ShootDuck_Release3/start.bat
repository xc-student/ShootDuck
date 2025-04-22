@echo off
echo 正在启动水枪大战游戏...
java -Xms512m -Xmx1024m -Dsun.awt.noerasebackground=true -Dsun.java2d.opengl=true -Dsun.java2d.d3d=false -jar game.jar
if errorlevel 1 (
    echo 启动失败！请确保已安装Java运行环境。
    echo 您可以从 https://www.java.com/download/ 下载Java
)
pause 