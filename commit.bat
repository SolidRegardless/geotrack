@echo off
cd /d C:\Users\stuar\.openclaw\workspace-personal\geotrack
git branch --show-current
git add -A
git commit -m "fix: properly close executor services and HTTP clients (closes #2)"
git push --force-with-lease
