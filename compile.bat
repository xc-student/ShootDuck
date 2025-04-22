@echo off
echo 正在编译和运行游戏...
call gradlew.bat clean
call gradlew.bat classes
call gradlew.bat lwjgl3:run 