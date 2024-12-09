@echo off
setlocal

REM Define the Spark submit command
set "SPARK_SUBMIT_CMD=%JOB_SUBMIT_COMMAND%"

REM Execute the Spark submit command
echo Inside spark-job-submit.bat ...
REM echo %SPARK_SUBMIT_CMD%

call %SPARK_SUBMIT_CMD%

REM Check the exit status of the Spark submit command
if %errorlevel% equ 0 (
    echo Spark submit command executed successfully.
) else (
    echo Spark submit command failed.
    exit /b 1
)

endlocal