@echo off
chcp 65001 > nul
echo Dang bien dich ma nguon...
dir /s /B src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d target\classes @sources.txt
del sources.txt

echo Dang khoi chay ung dung...
java -Dfile.encoding=UTF-8 -cp target\classes view.MainView
pause
