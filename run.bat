@echo off
chcp 65001 > nul
set "PROJECT_DIR=%~dp0"

echo Dang bien dich ma nguon...
dir /s /B "%PROJECT_DIR%src\main\java\*.java" > "%PROJECT_DIR%sources.txt"
javac -encoding UTF-8 -d "%PROJECT_DIR%target\classes" @"%PROJECT_DIR%sources.txt"
del "%PROJECT_DIR%sources.txt"

echo Dang khoi chay ung dung...
java -Dfile.encoding=UTF-8 -cp "%PROJECT_DIR%target\classes" view.MainView
pause
