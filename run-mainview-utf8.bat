@echo off
chcp 65001 > nul
java "-Dfile.encoding=UTF-8" "-Dsun.stdin.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" -cp target\classes view.MainView
pause
