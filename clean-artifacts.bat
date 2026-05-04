@echo off
setlocal

cd /d "%~dp0"
echo [INFO] Start cleaning build artifacts...

call :remove_dir "build"
call :remove_dir ".gradle"
call :remove_dir "workplace"

call :remove_dir "ui\build"
call :remove_dir "ui\node_modules"
call :remove_dir "ui\.gradle"

echo [INFO] Clean finished.
exit /b 0

:remove_dir
if exist "%~1" (
  rmdir /s /q "%~1"
  if exist "%~1" (
    echo [WARN] %~1 still exists (maybe locked by running process)
  ) else (
    echo [DELETED] %~1
  )
) else (
  echo [SKIP] %~1 (not found)
)
exit /b 0
