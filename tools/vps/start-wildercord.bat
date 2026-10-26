@echo off
title Wildercord Server Console
cd /d C:\wildercord
set JAVA=C:\java\jdk-25.0.4.1+1\bin\java.exe
echo [%date% %time%] Wildercord server starting...
:loop
start "" /b /wait /abovenormal "%JAVA%" -Xms1G -Xmx8G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCCountTarget=4 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dfabric.debug.discoveryTimeout=300 -jar fabric-server-launch.jar --nogui >> logs\console.log 2>&1
echo [%date% %time%] Server exited with code %errorlevel%. Restarting in 10 seconds...
timeout /t 10 /nobreak >nul
goto loop
