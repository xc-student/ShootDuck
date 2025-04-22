@echo off
echo Downloading PlantUML jar if not exists...
if not exist plantuml.jar (
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/plantuml/plantuml/releases/download/v1.2024.0/plantuml-1.2024.0.jar' -OutFile 'plantuml.jar'"
)

echo Generating UML diagram in SVG format...
java -jar plantuml.jar -tsvg ShootDuck.puml

echo SVG file generated successfully.
echo You can now open ShootDuck.svg in a web browser or convert it to PDF using online tools.

pause 