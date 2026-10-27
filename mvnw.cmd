@echo off
setlocal
set "RESOLVEBID_MAVEN=%~dp0.tools\apache-maven-3.9.11\bin\mvn.cmd"
if not exist "%RESOLVEBID_MAVEN%" (
  echo Local Maven is missing. Re-import the project in Eclipse or install Maven 3.9.11. 1>&2
  exit /b 1
)
call "%RESOLVEBID_MAVEN%" "-Dmaven.repo.local=%~dp0.tools\repository" %*
endlocal
